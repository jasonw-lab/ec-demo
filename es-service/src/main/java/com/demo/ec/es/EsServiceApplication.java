package com.demo.ec.es;

import com.demo.ec.es.config.EsServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(EsServiceProperties.class)
public class EsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EsServiceApplication.class, args);
    }
}
