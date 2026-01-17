package com.demo.ec.es.web;

import com.demo.ec.es.application.ImageUploadService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/internal/products")
public class InternalImageController {
    private final ImageUploadService imageUploadService;

    public InternalImageController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping("/{productId}/image")
    public Map<String, String> uploadImage(@PathVariable("productId") Long productId,
                                           @RequestParam("file") MultipartFile file) throws Exception {
        String url = imageUploadService.uploadAndUpdate(productId, file);
        return Map.of("thumbnailUrl", url);
    }
}
