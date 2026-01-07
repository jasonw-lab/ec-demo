package com.demo.ec.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.demo.ec.es.config.EsServiceProperties;
import com.demo.ec.es.model.ImportError;
import com.demo.ec.es.model.ImportRequest;
import com.demo.ec.es.model.ImportResult;
import com.demo.ec.es.model.ProductDocument;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportService {
    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final ElasticsearchClient client;
    private final EsServiceProperties properties;
    private final MinioStorageService minioStorageService;
    private final ThumbnailService thumbnailService;
    private final IndexAdminService indexAdminService;

    public ImportService(ElasticsearchClient client,
                         EsServiceProperties properties,
                         MinioStorageService minioStorageService,
                         ThumbnailService thumbnailService,
                         IndexAdminService indexAdminService) {
        this.client = client;
        this.properties = properties;
        this.minioStorageService = minioStorageService;
        this.thumbnailService = thumbnailService;
        this.indexAdminService = indexAdminService;
    }

    public ImportResult importCsv(ImportRequest request) throws IOException {
        indexAdminService.initIndexIfMissing();
        minioStorageService.ensureBucket();

        int batchSize = request.batchSize() == null ? properties.getImport().getBatchSize() : request.batchSize();
        String index = properties.getIndex().getAlias();

        long total = 0;
        long success = 0;
        List<ImportError> errors = new ArrayList<>();
        List<BulkOperation> operations = new ArrayList<>();
        List<ImportError> bulkItems = new ArrayList<>();

        Path csvPath = Path.of(request.csvPath());
        Path imagesDir = Path.of(request.imagesDir());

        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                total++;
                long lineNo = record.getRecordNumber() + 1;

                try {
                    String productIdStr = record.get("productId");
                    String title = record.get("title");
                    String description = record.get("description");
                    String priceStr = record.get("price");
                    String status = record.get("status");
                    String createdAtStr = record.get("createdAt");
                    String imageFile = record.get("imageFile");

                    Long productId = Long.parseLong(productIdStr);
                    Long price = Long.parseLong(priceStr);
                    Instant createdAt = Instant.parse(createdAtStr);

                    Path imagePath = imagesDir.resolve(imageFile);
                    String ext = "jpg";
                    int dot = imageFile.lastIndexOf('.');
                    if (dot >= 0 && dot < imageFile.length() - 1) {
                        ext = imageFile.substring(dot + 1).toLowerCase();
                    }

                    String origObject = "products/" + productId + "/orig." + ext;
                    String thumbObject = "products/" + productId + "/thumb.jpg";

                    minioStorageService.uploadFile(origObject, imagePath.toString(), contentTypeForExt(ext));
                    byte[] thumbBytes;
                    try (var in = Files.newInputStream(imagePath)) {
                        thumbBytes = thumbnailService.createThumbnail(in);
                    }
                    minioStorageService.uploadBytes(thumbObject, thumbBytes, "image/jpeg");

                    String thumbnailUrl = minioStorageService.buildPublicUrl("/" + thumbObject);

                    ProductDocument doc = new ProductDocument(productId, title, description, price, status, thumbnailUrl, createdAt);
                    operations.add(BulkOperation.of(op -> op
                            .index(i -> i.index(index).id(productId.toString()).document(doc))));
                    bulkItems.add(new ImportError(lineNo, productId.toString(), null));

                    if (operations.size() >= batchSize) {
                        success += flushBulk(index, operations, bulkItems, errors);
                    }
                } catch (Exception ex) {
                    String productId = record.isMapped("productId") ? record.get("productId") : "";
                    log.warn("Import row failed line={} productId={} reason={}", lineNo, productId, ex.getMessage());
                    errors.add(new ImportError(lineNo, productId, ex.getMessage()));
                }
            }
        }

        if (!operations.isEmpty()) {
            success += flushBulk(index, operations, bulkItems, errors);
        }

        long failed = total - success;
        return new ImportResult(total, success, failed, errors);
    }

    private long flushBulk(String index, List<BulkOperation> operations, List<ImportError> bulkItems, List<ImportError> errors) throws IOException {
        BulkRequest request = new BulkRequest.Builder()
                .index(index)
                .operations(new ArrayList<>(operations))
                .build();

        var response = client.bulk(request);
        long success = operations.size();
        if (response.errors()) {
            for (int i = 0; i < response.items().size(); i++) {
                var item = response.items().get(i);
                if (item.error() != null) {
                    success--;
                    ImportError ctx = bulkItems.size() > i ? bulkItems.get(i) : new ImportError(-1, "", "");
                    String message = item.error().type() + ": " + item.error().reason();
                    log.warn("Bulk item failed line={} productId={} error={}", ctx.lineNo(), ctx.productId(), message);
                    errors.add(new ImportError(ctx.lineNo(), ctx.productId(), message));
                }
            }
        }
        operations.clear();
        bulkItems.clear();
        return success;
    }

    private String contentTypeForExt(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
