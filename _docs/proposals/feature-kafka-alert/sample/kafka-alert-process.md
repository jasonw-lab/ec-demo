以下は `hello-mid` Kafka デモへ組み込むための「kafka-alert（PoC）」実装プロンプトです。  
目的: Kafka Streams を用いて Rule A/B/C（注文と決済の整合性崩れ）を検知し、`alerts.order_payment_inconsistency.v1` トピックへ `AlertRaised` を出力できる PoC を作成する。

--- 
1) 全体設計（最小・移植しやすい）
コンポーネント

alert-streams（既存/これから）：Rule A/B/C を検知して alerts.order_payment_inconsistency.v1 に AlertRaised を publish

alert-process-service（新規）：alerts topic を consume → ログ出力 → MySQL へ保存

なぜ分ける？

Streams側は「検知」に集中、DB/UIは「管理」に集中

ec-demo に移植しても役割が明確で崩れにくい（面接でも説明しやすい）

2) alert-process-service の責務
入力

Kafka Topic：alerts.order_payment_inconsistency.v1

Message：AlertRaised（JSON）
  ※ `libs/event-contracts` の `AlertRaisedEvent` を利用

処理

受信ログ（orderId, alertId, rule, severity）

DBへUPSERT（重複受信に強くする）

必要ならメトリクス（件数）※PoCはログだけでもOK

出力

MySQL：ec_system.sys_pay_alert に登録

UI管理画面はこのテーブルを読む想定

3) 重要な設計ポイント（実務で効く）
3.1 冪等性（重複登録防止）

Kafkaは再配信/再処理が起きるので、DB側で守ります。

おすすめキー

alert_id（UUID）を UNIQUE にする（最も簡単）

もしalert_idを持てない/信用しないなら、追加で

(order_id, rule, detected_at) などの複合ユニークも候補

3.2 例外時の扱い（PoCは簡単でOK）

JSONパース失敗：ログしてスキップ（DLQは後回し）

DBエラー：例外投げてリトライ（consumerの再試行に任せる）

ただし重複登録は UNIQUE + UPSERT で吸収

4) テーブル設計（UI表示しやすい形）
テーブル：ec_system.sys_pay_alert

最低限のUI列

id（PK, auto increment）

alert_id（UUID, unique）

order_id

rule（A/B/C）

severity（P1/P2）

detected_at（検知時刻）

status（NEW/ACK/RESOLVED）※UI運用のため最初から入れるのおすすめ

message（人が見る文）

facts_json（検知根拠をJSONで保存）

created_at / updated_at

5) DBスクリプト（MySQL）
-- database
CREATE DATABASE IF NOT EXISTS ec_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ec_system;

-- table
CREATE TABLE IF NOT EXISTS sys_pay_alert (
  id BIGINT NOT NULL AUTO_INCREMENT,
  alert_id VARCHAR(36) NOT NULL COMMENT 'UUID from AlertRaised.alertId',
  order_id VARCHAR(64) NOT NULL COMMENT 'Business order identifier',
  rule CHAR(1) NOT NULL COMMENT 'A/B/C',
  severity VARCHAR(2) NOT NULL COMMENT 'P1/P2',
  detected_at DATETIME(3) NOT NULL COMMENT 'Alert detected time',

  status VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/ACK/RESOLVED',
  message VARCHAR(255) NOT NULL COMMENT 'Human readable summary',
  facts_json JSON NULL COMMENT 'Evidence/facts for UI/debug',

  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),

  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_pay_alert_alert_id (alert_id),
  KEY idx_sys_pay_alert_order_id (order_id),
  KEY idx_sys_pay_alert_detected_at (detected_at),
  KEY idx_sys_pay_alert_status_detected (status, detected_at)
) ENGINE=InnoDB;

-- (optional) sample enum guard via CHECK (MySQL 8.0.16+)
ALTER TABLE sys_pay_alert
  ADD CONSTRAINT chk_sys_pay_alert_rule CHECK (rule IN ('A','B','C')),
  ADD CONSTRAINT chk_sys_pay_alert_severity CHECK (severity IN ('P1','P2')),
  ADD CONSTRAINT chk_sys_pay_alert_status CHECK (status IN ('NEW','ACK','RESOLVED'));


facts_json は MySQL JSON 型。UIで詳細を展開できて便利です。

6) alert-process-service 実装案（Spring Boot）
パッケージ例
alert-process-service/
  api/            (将来 UI操作 API を作るなら)
  consumer/       (KafkaListener)
  service/        (保存ロジック)
  repository/     (JDBC/JPA)
  model/          (AlertRaised DTO / Entity)
  config/

推奨：JDBC（シンプル・確実）

JdbcTemplate or NamedParameterJdbcTemplate

SQLは INSERT ... ON DUPLICATE KEY UPDATE を使う

UPSERT SQL例
INSERT INTO sys_pay_alert
(alert_id, order_id, rule, severity, detected_at, status, message, facts_json)
VALUES (?, ?, ?, ?, ?, 'NEW', ?, ?)
ON DUPLICATE KEY UPDATE
  updated_at = CURRENT_TIMESTAMP(3);

Kafka consumer（要点）

@KafkaListener(topics="alerts.order_payment_inconsistency.v1", groupId="alert-process-service")

受信→DTO化→DB保存（detectedAt は ISO-8601 文字列なので Instant.parse で扱える）

ログは必ず orderId/alertId/rule/severity

7) 次に決め打ちして進める前提（こちらで仮定してOK）

DBは MySQL 8.x

alert-streams の AlertRaised に alertId, rule, severity, orderId, detectedAt, facts が入っている

UIはまず一覧表示（NEW/ACK/RESOLVED）を想定
