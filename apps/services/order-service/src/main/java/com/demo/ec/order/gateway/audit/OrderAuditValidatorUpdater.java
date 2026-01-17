package com.demo.ec.order.gateway.audit;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB order_audit コレクションのバリデーションルールを更新
 * REST API経由で呼ばれたときに PAYMENT_PENDING ステータスを追加
 * 
 * エンドポイント: POST /api/admin/order-audit/update-validator
 */
@RestController
@RequestMapping("/api/admin/order-audit")
public class OrderAuditValidatorUpdater {
    private static final Logger log = LoggerFactory.getLogger(OrderAuditValidatorUpdater.class);
    
    private final MongoTemplate mongoTemplate;
    
    public OrderAuditValidatorUpdater(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/update-validator")
    public ResponseEntity<Map<String, Object>> updateValidator() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            MongoDatabase database = mongoTemplate.getDb();
            
            // 新しいバリデーションルールを定義
            Document validator = createValidator();
            
            // collMod コマンドでバリデーションルールを更新
            Document command = new Document("collMod", "order_audit")
                    .append("validator", validator);
            
            Document result = database.runCommand(command);
            
            if (result.getDouble("ok") == 1.0) {
                log.info("[OrderAudit] ✅ Validator updated successfully - PAYMENT_PENDING status added");
                response.put("success", true);
                response.put("message", "Validator updated successfully - PAYMENT_PENDING status added");
                return ResponseEntity.ok(response);
            } else {
                log.warn("[OrderAudit] ⚠️ Validator update returned: {}", result.toJson());
                response.put("success", false);
                response.put("message", "Validator update returned unexpected result");
                response.put("details", result.toJson());
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("[OrderAudit] ❌ Failed to update validator: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to update validator: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private Document createValidator() {
        // JSONスキーマバリデータを作成
        List<String> statusEnum = Arrays.asList(
            "CREATED", 
            "PENDING", 
            "PAYMENT_PENDING",  // 追加
            "PROCESSING", 
            "PAID", 
            "CANCELLED", 
            "COMPLETED"
        );

        Document historyItemProperties = new Document()
                .append("status", new Document("bsonType", "string"))
                .append("reason", new Document("bsonType", "string"))
                .append("at", new Document("bsonType", "date"))
                .append("by", new Document("bsonType", "string"))
                .append("eventId", new Document("bsonType", "string"))
                .append("metadata", new Document("bsonType", "object"));

        Document historyItems = new Document()
                .append("bsonType", "object")
                .append("required", Arrays.asList("status", "at", "by", "eventId"))
                .append("properties", historyItemProperties);

        Document properties = new Document()
                .append("orderId", new Document()
                        .append("bsonType", "string")
                        .append("description", "注文ID（主キー）"))
                .append("currentStatus", new Document()
                        .append("bsonType", "string")
                        .append("enum", statusEnum)
                        .append("description", "現在のステータス"))
                .append("processedEventIds", new Document()
                        .append("bsonType", "array")
                        .append("items", new Document("bsonType", "string"))
                        .append("description", "処理済みeventID配列（冪等性保証用）"))
                .append("history", new Document()
                        .append("bsonType", "array")
                        .append("description", "ステータス変更履歴")
                        .append("items", historyItems))
                .append("createdAt", new Document("bsonType", "date"))
                .append("updatedAt", new Document("bsonType", "date"));

        Document jsonSchema = new Document()
                .append("bsonType", "object")
                .append("required", Arrays.asList("orderId", "currentStatus", "processedEventIds", "history", "createdAt", "updatedAt"))
                .append("properties", properties);

        return new Document("$jsonSchema", jsonSchema);
    }
}
