package com.demo.ec.es.application;

import com.demo.ec.es.config.EsServiceProperties;
import com.demo.ec.es.domain.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service for managing file storage operations with MinIO.
 * Handles bucket creation, file uploads, and URL generation.
 */
@Service
public class MinioStorageService {
    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final EsServiceProperties properties;

    public MinioStorageService(MinioClient minioClient, EsServiceProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    /**
     * Ensures the configured bucket exists, creating it if necessary.
     *
     * @throws StorageException if bucket creation fails
     */
    public void ensureBucket() {
        String bucketName = properties.getMinio().getBucket();
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            
            if (!exists) {
                log.info("Creating MinIO bucket: {}", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket created successfully: {}", bucketName);
            } else {
                log.debug("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception ex) {
            log.error("Failed to ensure MinIO bucket: {}", bucketName, ex);
            throw new StorageException("Failed to ensure MinIO bucket: " + bucketName, ex);
        }
    }

    /**
     * Uploads byte array to MinIO.
     *
     * @param objectName  the object key in MinIO
     * @param bytes       the content to upload
     * @param contentType MIME type
     * @throws StorageException if upload fails
     */
    public void uploadBytes(String objectName, byte[] bytes, String contentType) {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getMinio().getBucket())
                            .object(objectName)
                            .stream(in, bytes.length, -1)
                            .contentType(contentType)
                            .build());
            
            log.debug("Uploaded bytes to MinIO: {} ({} bytes)", objectName, bytes.length);
        } catch (Exception ex) {
            log.error("Failed to upload bytes to MinIO: {}", objectName, ex);
            throw new StorageException("Failed to upload bytes to MinIO: " + objectName, ex);
        }
    }

    /**
     * Uploads file from local filesystem to MinIO.
     *
     * @param objectName  the object key in MinIO
     * @param filePath    local file path
     * @param contentType MIME type
     * @throws StorageException if upload fails
     */
    public void uploadFile(String objectName, String filePath, String contentType) {
        try (InputStream in = new FileInputStream(filePath)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getMinio().getBucket())
                            .object(objectName)
                            .stream(in, -1, 10 * 1024 * 1024)
                            .contentType(contentType)
                            .build());
            
            log.debug("Uploaded file to MinIO: {} from {}", objectName, filePath);
        } catch (IOException ex) {
            log.error("Failed to read file: {}", filePath, ex);
            throw new StorageException("Failed to read file: " + filePath, ex);
        } catch (Exception ex) {
            log.error("Failed to upload file to MinIO: {}", objectName, ex);
            throw new StorageException("Failed to upload file to MinIO: " + objectName, ex);
        }
    }

    /**
     * Builds public URL for accessing MinIO object.
     *
     * @param objectPath the object path (with leading slash)
     * @return full public URL
     */
    public String buildPublicUrl(String objectPath) {
        String base = properties.getMinio().getPublicBaseUrl();
        String bucket = properties.getMinio().getBucket();
        if (base == null || base.isBlank()) {
            throw new StorageException("MinIO public base URL is not configured");
        }
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedObjectPath = objectPath.startsWith("/") ? objectPath : "/" + objectPath;

        try {
            java.net.URI uri = java.net.URI.create(normalizedBase);
            String host = uri.getHost();
            String path = uri.getPath() == null ? "" : uri.getPath();
            boolean hostHasBucket = bucket != null && host != null && host.startsWith(bucket + ".");
            boolean pathHasBucket = bucket != null
                    && (path.equals("/" + bucket) || path.startsWith("/" + bucket + "/"));
            if (!hostHasBucket && !pathHasBucket && bucket != null && !bucket.isBlank()) {
                normalizedBase = normalizedBase + "/" + bucket;
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid MinIO public base URL: {}", normalizedBase);
        }

        return normalizedBase + normalizedObjectPath;
    }
}
