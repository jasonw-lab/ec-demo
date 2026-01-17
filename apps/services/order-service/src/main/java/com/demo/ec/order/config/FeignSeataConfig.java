package com.demo.ec.order.config;

import feign.RequestInterceptor;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds Seata XID header propagation for all OpenFeign requests from order-service.
 * This ensures downstream services (account/storage) execute within the same Global Transaction
 * and can register their TCC branches so that confirm/cancel callbacks are invoked.
 */
@Configuration
public class FeignSeataConfig {
    private static final Logger log = LoggerFactory.getLogger(FeignSeataConfig.class);

    @Bean
    public RequestInterceptor seataFeignRequestInterceptor() {
        return template -> {
            try {
                String xid = RootContext.getXID();
                if (xid != null && !xid.isEmpty()) {
                    template.header(RootContext.KEY_XID, xid);
                    if (log.isDebugEnabled()) {
                        log.debug("Propagated Seata XID via Feign header: {}", xid);
                    }
                }
            } catch (Throwable ignore) {
                // no-op: keep Feign call working even if Seata is not on classpath
            }
        };
    }
}
