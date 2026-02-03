package com.demo.ec.bff;

import com.demo.ec.bff.config.AuthSessionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties({AuthSessionProperties.class})
@EnableScheduling
public class EcBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcBackendApplication.class, args);
    }

    /**
     * CORS configuration for development environment.
     * 
     * SECURITY NOTE: For production deployments, configure allowed origins
     * via environment variables or application properties. Never use "*" 
     * wildcard in production as it allows any origin to access the API.
     * 
     * Example production configuration:
     * - allowedOrigins("https://yourdomain.com", "https://www.yourdomain.com")
     * - Consider using environment-based configuration for flexibility
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
