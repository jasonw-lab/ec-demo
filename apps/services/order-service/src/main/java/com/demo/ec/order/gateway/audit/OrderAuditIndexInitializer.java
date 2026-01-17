package com.demo.ec.order.gateway.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.util.List;

@Configuration
public class OrderAuditIndexInitializer {
    private static final Logger log = LoggerFactory.getLogger(OrderAuditIndexInitializer.class);

    @Bean
    public ApplicationRunner orderAuditIndexRunner(MongoTemplate mongoTemplate) {
        return args -> {
            IndexOperations indexOps = mongoTemplate.indexOps(OrderAuditDocument.class);
            
            // 既存のインデックスをチェック
            List<IndexInfo> existingIndexes = indexOps.getIndexInfo();
            boolean historyEventIdIndexExists = existingIndexes.stream()
                .anyMatch(indexInfo -> {
                    // フィールド名に "history.eventId" が含まれているかチェック
                    return indexInfo.getIndexFields().stream()
                        .anyMatch(field -> "history.eventId".equals(field.getKey()));
                });
            
            if (historyEventIdIndexExists) {
                log.info("[OrderAudit] index on history.eventId already exists, skipping creation");
            } else {
                indexOps.ensureIndex(new Index().on("history.eventId", org.springframework.data.domain.Sort.Direction.ASC));
                log.info("[OrderAudit] created index on order_audit.history.eventId");
            }
        };
    }
}
