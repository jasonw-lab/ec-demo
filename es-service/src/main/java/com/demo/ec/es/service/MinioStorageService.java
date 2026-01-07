package com.demo.ec.es.service;

import com.demo.ec.es.config.EsServiceProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

@Service
public class MinioStorageService {
    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final EsServiceProperties properties;

    public MinioStorageService(MinioClient minioClient, EsServiceProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getMinio().getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getMinio().getBucket()).build());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to ensure MinIO bucket", ex);
        }
    }

    public void uploadBytes(String objectName, byte[] bytes, String contentType) throws Exception {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getMinio().getBucket())
                            .object(objectName)
                            .stream(in, bytes.length, -1)
                            .contentType(contentType)
                            .build());
        }
    }

    public void uploadFile(String objectName, String filePath, String contentType) throws Exception {
        try (InputStream in = new FileInputStream(filePath)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getMinio().getBucket())
                            .object(objectName)
                            .stream(in, -1, 10 * 1024 * 1024)
                            .contentType(contentType)
                            .build());
        }
    }

    public String buildPublicUrl(String objectPath) {
        String base = properties.getMinio().getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!objectPath.startsWith("/")) {
            objectPath = "/" + objectPath;
        }
        return base + objectPath;
    }
}
