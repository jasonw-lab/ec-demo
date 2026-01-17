package com.demo.ec.es.service;

import com.demo.ec.es.config.EsServiceProperties;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ThumbnailService {
    private final ExecutorService executor;
    private final EsServiceProperties properties;

    public ThumbnailService(EsServiceProperties properties) {
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(properties.getThumbnail().getThreads());
    }

    public byte[] createThumbnail(InputStream inputStream) throws Exception {
        Future<byte[]> future = executor.submit(() -> {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Thumbnails.of(inputStream)
                        .size(properties.getThumbnail().getWidth(), properties.getThumbnail().getHeight())
                        .outputFormat("jpg")
                        .outputQuality(0.85)
                        .toOutputStream(out);
                return out.toByteArray();
            }
        });
        return future.get();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
