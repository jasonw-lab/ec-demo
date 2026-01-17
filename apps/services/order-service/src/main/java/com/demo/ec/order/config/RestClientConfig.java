package com.demo.ec.order.config;

import io.seata.core.context.RootContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(java.time.Duration.ofSeconds(3))
                .setReadTimeout(java.time.Duration.ofSeconds(3))
                .build();
        // Add interceptor to propagate Seata XID header for AT mode cross-service calls
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
        interceptors.add((request, body, execution) -> {
            try {
                String xid = RootContext.getXID();
                if (xid != null && !xid.isEmpty()) {
                    request.getHeaders().add(RootContext.KEY_XID, xid);
                }
            } catch (Throwable ignore) {
            }
            return execution.execute(request, body);
        });
        interceptors.add((request, body, execution) -> {
            if (!request.getHeaders().containsKey("X-Request-Id")) {
                request.getHeaders().add("X-Request-Id", UUID.randomUUID().toString());
            }
            return execution.execute(request, body);
        });
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
