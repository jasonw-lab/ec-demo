package com.example.seata.at.order.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class OrderAuditIndexInitializer {
    private static final Logger log = LoggerFactory.getLogger(OrderAuditIndexInitializer.class);

    @Bean
    public ApplicationRunner orderAuditIndexRunner(MongoTemplate mongoTemplate) {
        return args -> {
            IndexOperations indexOps = mongoTemplate.indexOps(OrderAuditDocument.class);
            indexOps.ensureIndex(new Index().on("history.eventId", org.springframework.data.domain.Sort.Direction.ASC));
            log.info("[OrderAudit] ensured index on order_audit.history.eventId");
        };
    }
}
