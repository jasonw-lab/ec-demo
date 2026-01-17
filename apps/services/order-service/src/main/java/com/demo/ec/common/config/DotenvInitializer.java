package com.demo.ec.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Loads variables from .env files into System properties before the Spring context refreshes,
 * so that Spring Boot can resolve them as configuration properties.
 *
 * Priority order (first to last):
 * - File specified by ENV_FILE env var or -Denv.file=... (if present)
 * - .env (default)
 * - .env-<activeProfile> for each active Spring profile (if present)
 */
public class DotenvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        // Build candidate file list in deterministic order without duplicates
        Set<String> files = new LinkedHashSet<>();

        String explicit = System.getenv("ENV_FILE");
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getProperty("env.file");
        }
        if (explicit != null && !explicit.isBlank()) {
            files.add(explicit.trim());
        }

        files.add(".env");

        for (String profile : env.getActiveProfiles()) {
            if (profile != null && !profile.isBlank()) {
                files.add(".env-" + profile);
            }
        }

        // Load each file if present. Later files win for duplicate keys.
        for (String f : files) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .filename(f)
                        .ignoreIfMalformed()
                        .ignoreIfMissing()
                        .load();
                dotenv.entries().forEach(e -> {
                    // Put into System properties so Spring picks them up as high-priority property source
                    if (e != null && e.getKey() != null && e.getValue() != null) {
                        System.setProperty(e.getKey(), e.getValue());
                    }
                });
            } catch (Exception ex) {
                // ignore and continue
            }
        }
    }
}
