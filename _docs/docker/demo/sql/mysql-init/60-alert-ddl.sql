-- Alert Service DDL
-- アラート管理テーブル: sys_pay_alert
-- 決済と注文の不整合を検知したアラート情報を格納

CREATE DATABASE IF NOT EXISTS ec_system;
USE ec_system;

CREATE TABLE IF NOT EXISTS sys_pay_alert (
    alert_id VARCHAR(64) PRIMARY KEY COMMENT 'アラートID (UUID)',
    order_id VARCHAR(64) NOT NULL COMMENT '注文ID',
    rule VARCHAR(10) NOT NULL COMMENT 'ルール種別 (A/B/C)',
    severity VARCHAR(10) COMMENT '重大度 (P1/P2)',
    detected_at TIMESTAMP(3) COMMENT '検知日時',
    status VARCHAR(20) DEFAULT 'NEW' COMMENT 'ステータス (NEW/ACKNOWLEDGED/RESOLVED)',
    message TEXT COMMENT 'アラートメッセージ',
    facts_json JSON COMMENT '検知時の詳細情報 (JSON)',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '作成日時',
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新日時',
    INDEX idx_order_id (order_id),
    INDEX idx_detected_at (detected_at),
    INDEX idx_rule_severity (rule, severity),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='決済注文不整合アラート';

-- テーブル作成完了
SELECT 'Alert DDL executed successfully' as status;
