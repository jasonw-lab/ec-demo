use youlai_boot;

-- カテゴリテーブル: retail_category
CREATE TABLE IF NOT EXISTS retail_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'カテゴリID',
    name VARCHAR(100) NOT NULL COMMENT 'カテゴリ名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ'
) COMMENT='カテゴリマスタ';

-- 初期データ投入
INSERT INTO retail_category (id, name, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, '飲料', NOW(), NOW(), NULL, NULL, 0),
(2, '食品', NOW(), NOW(), NULL, NULL, 0),
(3, '日用品', NOW(), NOW(), NULL, NULL, 0),
(4, '雑貨', NOW(), NOW(), NULL, NULL, 0),
(5, 'その他', NOW(), NOW(), NULL, NULL, 0);