package com.demo.ec.es.application;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.demo.ec.es.config.EsServiceProperties;
import com.demo.ec.es.domain.ElasticsearchOperationException;
import com.demo.ec.es.domain.ImportError;
import com.demo.ec.es.domain.ImportRequest;
import com.demo.ec.es.domain.ImportResult;
import com.demo.ec.es.domain.ProductDocument;
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

/**
 * Service for importing product data from CSV files.
 * Handles CSV parsing, image upload to MinIO, and bulk indexing to Elasticsearch.
 */
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

    /**
     * Imports product data from CSV file with idempotent bulk upsert.
     *
     * @param request import request containing CSV path and images directory
     * @return import result with success/failure counts and error details
     */
    public ImportResult importCsv(ImportRequest request) {
        log.info("Starting CSV import: {}", request);
        
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
                    ProductDocument doc = parseAndUploadProduct(record, imagesDir, lineNo);
                    
                    operations.add(BulkOperation.of(op -> op
                            .index(i -> i.index(index).id(doc.productId().toString()).document(doc))));
                    bulkItems.add(new ImportError(lineNo, doc.productId().toString(), null));

                    if (operations.size() >= batchSize) {
                        success += flushBulk(index, operations, bulkItems, errors);
                    }
                } catch (Exception ex) {
                    String productId = record.isMapped("productId") ? record.get("productId") : "";
                    log.warn("Import row failed: line={}, productId={}, reason={}", lineNo, productId, ex.getMessage());
                    errors.add(new ImportError(lineNo, productId, ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            log.error("Failed to read CSV file: {}", csvPath, ex);
            throw new IllegalArgumentException("Failed to read CSV file: " + csvPath, ex);
        }

        if (!operations.isEmpty()) {
            success += flushBulk(index, operations, bulkItems, errors);
        }

        long failed = total - success;
        log.info("Import completed: total={}, success={}, failed={}", total, success, failed);
        return new ImportResult(total, success, failed, errors);
    }

    private ProductDocument parseAndUploadProduct(CSVRecord record, Path imagesDir, long lineNo) throws Exception {
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
        if (!Files.exists(imagePath)) {
            log.warn("Image file not found: {} (line {})", imagePath, lineNo);
        }

        String ext = extractExtension(imageFile);
        String origObject = "products/" + productId + "/orig." + ext;
        String thumbObject = "products/" + productId + "/thumb.jpg";

        // Upload original image
        minioStorageService.uploadFile(origObject, imagePath.toString(), contentTypeForExt(ext));
        
        // Generate and upload thumbnail
        byte[] thumbBytes;
        try (var in = Files.newInputStream(imagePath)) {
            thumbBytes = thumbnailService.createThumbnail(in);
        }
        minioStorageService.uploadBytes(thumbObject, thumbBytes, "image/jpeg");

        String thumbnailUrl = minioStorageService.buildPublicUrl("/" + thumbObject);

        return new ProductDocument(productId, title, description, price, status, thumbnailUrl, createdAt);
    }

    private long flushBulk(String index, List<BulkOperation> operations, List<ImportError> bulkItems, List<ImportError> errors) {
        BulkRequest request = new BulkRequest.Builder()
                .index(index)
                .operations(new ArrayList<>(operations))
                .build();

        try {
            var response = client.bulk(request);
            long success = operations.size();
            
            if (response.errors()) {
                for (int i = 0; i < response.items().size(); i++) {
                    var item = response.items().get(i);
                    if (item.error() != null) {
                        success--;
                        ImportError ctx = bulkItems.size() > i ? bulkItems.get(i) : new ImportError(-1, "", "");
                        String message = item.error().type() + ": " + item.error().reason();
                        log.warn("Bulk item failed: line={}, productId={}, error={}", ctx.lineNo(), ctx.productId(), message);
                        errors.add(new ImportError(ctx.lineNo(), ctx.productId(), message));
                    }
                }
            }
            
            operations.clear();
            bulkItems.clear();
            return success;
        } catch (IOException ex) {
            log.error("Bulk request failed", ex);
            throw new ElasticsearchOperationException("Bulk request failed", ex);
        }
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            return filename.substring(dot + 1).toLowerCase();
        }
        return "jpg";
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
