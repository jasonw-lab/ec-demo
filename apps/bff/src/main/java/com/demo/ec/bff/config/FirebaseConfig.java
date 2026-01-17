package com.demo.ec.bff.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        // Service account credentials should be provided as a JSON file on the classpath.
        // Supports both firebase-service-account.json and serviceAccountKey.json
        ClassPathResource resource = null;
        
        // Try firebase-service-account.json first
        ClassPathResource resource1 = new ClassPathResource("firebase-service-account.json");
        try {
            resource1.getInputStream().close();
            resource = resource1;
            log.info("Using firebase-service-account.json for Firebase configuration");
        } catch (IOException e) {
            // If firebase-service-account.json doesn't exist, try serviceAccountKey.json
            ClassPathResource resource2 = new ClassPathResource("serviceAccountKey.json");
            try {
                resource2.getInputStream().close();
                resource = resource2;
                log.info("Using serviceAccountKey.json for Firebase configuration");
            } catch (IOException e2) {
                log.warn("Neither firebase-service-account.json nor serviceAccountKey.json found in classpath");
                throw new IllegalStateException(
                    "Firebase service account file not found. Please create " +
                    "bff/src/main/resources/firebase-service-account.json or " +
                    "bff/src/main/resources/serviceAccountKey.json with your Firebase service account credentials.",
                    e2
                );
            }
        }
        
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                .build();

        log.info("Initializing FirebaseApp for authentication");
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public ApplicationListener<ContextRefreshedEvent> firebaseInitializationChecker() {
        return event -> {
            if (FirebaseApp.getApps().isEmpty()) {
                log.warn("Firebase service account file not found in classpath. " +
                        "Firebase authentication will not be available. " +
                        "Please create bff/src/main/resources/firebase-service-account.json or " +
                        "bff/src/main/resources/serviceAccountKey.json with your Firebase service account credentials.");
            } else {
                log.info("FirebaseApp initialized successfully");
            }
        };
    }
}


