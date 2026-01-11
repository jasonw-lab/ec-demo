// MongoDB order_audit バリデーションルール更新スクリプト
// 使用方法: mongosh ec_demo < update-validator.js

print("🔄 Updating validator for order_audit collection...");

const result = db.runCommand({
  collMod: "order_audit",
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["orderId", "currentStatus", "processedEventIds", "history", "createdAt", "updatedAt"],
      properties: {
        orderId: {
          bsonType: "string",
          description: "注文ID（主キー）"
        },
        currentStatus: {
          bsonType: "string",
          enum: ["CREATED", "PENDING", "PAYMENT_PENDING", "PROCESSING", "PAID", "CANCELLED", "COMPLETED"],
          description: "現在のステータス"
        },
        processedEventIds: {
          bsonType: "array",
          items: { bsonType: "string" },
          description: "処理済みeventID配列（冪等性保証用）"
        },
        history: {
          bsonType: "array",
          description: "ステータス変更履歴",
          items: {
            bsonType: "object",
            required: ["status", "at", "by", "eventId"],
            properties: {
              status: { bsonType: "string" },
              reason: { bsonType: "string" },
              at: { bsonType: "date" },
              by: { bsonType: "string" },
              eventId: { bsonType: "string" },
              metadata: { bsonType: "object" }
            }
          }
        },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" }
      }
    }
  }
});

if (result.ok === 1) {
  print("✅ Validator updated successfully");
  print("   - Added PAYMENT_PENDING to enum");
} else {
  print("❌ Failed to update validator");
  printjson(result);
}
