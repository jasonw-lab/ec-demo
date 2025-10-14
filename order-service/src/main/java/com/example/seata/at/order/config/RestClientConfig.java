package com.example.seata.at.order.config;

import io.seata.core.context.RootContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
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
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
