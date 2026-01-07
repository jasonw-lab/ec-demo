package com.demo.ec.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.demo.ec.es.config.EsServiceProperties;
import com.demo.ec.es.model.ProductDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class ImageUploadService {
    private static final Logger log = LoggerFactory.getLogger(ImageUploadService.class);

    private final ElasticsearchClient client;
    private final EsServiceProperties properties;
    private final MinioStorageService minioStorageService;
    private final ThumbnailService thumbnailService;

    public ImageUploadService(ElasticsearchClient client,
                              EsServiceProperties properties,
                              MinioStorageService minioStorageService,
                              ThumbnailService thumbnailService) {
        this.client = client;
        this.properties = properties;
        this.minioStorageService = minioStorageService;
        this.thumbnailService = thumbnailService;
    }

    public String uploadAndUpdate(Long productId, MultipartFile file) throws Exception {
        minioStorageService.ensureBucket();

        String filename = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = "jpg";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            ext = filename.substring(dot + 1).toLowerCase();
        }

        String origObject = "products/" + productId + "/orig." + ext;
        String thumbObject = "products/" + productId + "/thumb.jpg";

        minioStorageService.uploadBytes(origObject, file.getBytes(), contentTypeForExt(ext));
        byte[] thumb;
        try (var in = file.getInputStream()) {
            thumb = thumbnailService.createThumbnail(in);
        }
        minioStorageService.uploadBytes(thumbObject, thumb, "image/jpeg");

        String thumbnailUrl = minioStorageService.buildPublicUrl("/" + thumbObject);

        try {
            client.update(u -> u
                    .index(properties.getIndex().getAlias())
                    .id(productId.toString())
                    .doc(Map.of("thumbnailUrl", thumbnailUrl))
                    .docAsUpsert(false),
                    ProductDocument.class);
        } catch (Exception ex) {
            log.warn("Failed to update thumbnail in ES productId={} error={}", productId, ex.getMessage());
        }

        return thumbnailUrl;
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
