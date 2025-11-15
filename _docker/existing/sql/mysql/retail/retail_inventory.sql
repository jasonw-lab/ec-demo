use youlai_boot;

-- 在庫管理テーブル: retail_inventory
CREATE TABLE IF NOT EXISTS retail_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '在庫ID',
    store_id BIGINT NOT NULL COMMENT '店舗ID',
    store_name VARCHAR(100) NOT NULL COMMENT '店舗名',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名',
    stock INT NOT NULL DEFAULT 0 COMMENT '在庫数',
    expiry_date VARCHAR(20) COMMENT '賞味期限',
    status VARCHAR(20) DEFAULT 'normal' COMMENT '在庫状態（low, normal, high）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ',
    UNIQUE KEY `uk_store_product` (`store_id`, `product_id`) COMMENT '店舗と商品の組み合わせはユニーク'
) COMMENT='在庫管理テーブル';

-- 入庫管理テーブル: retail_inventory_in
CREATE TABLE IF NOT EXISTS retail_inventory_in (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '入庫ID',
    store_id BIGINT NOT NULL COMMENT '店舗ID',
    store_name VARCHAR(100) NOT NULL COMMENT '店舗名',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名',
    quantity INT NOT NULL COMMENT '入庫数量',
    lot_number VARCHAR(50) NOT NULL COMMENT 'ロット番号',
    expiry_date VARCHAR(20) COMMENT '賞味期限',
    restock_type VARCHAR(20) NOT NULL COMMENT '入庫タイプ（通常入庫, 返品入庫, 調整入庫）',
    status VARCHAR(20) DEFAULT '処理中' COMMENT '状態（処理中, 完了）',
    shipping_time DATETIME COMMENT '出荷時間',
    operator VARCHAR(50) COMMENT '担当者',
    remarks VARCHAR(255) COMMENT '備考',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ'
) COMMENT='入庫管理テーブル';

-- 出庫管理テーブル: retail_inventory_out
CREATE TABLE IF NOT EXISTS retail_inventory_out (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '出庫ID',
    store_id BIGINT NOT NULL COMMENT '店舗ID',
    store_name VARCHAR(100) NOT NULL COMMENT '店舗名',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名',
    quantity INT NOT NULL COMMENT '出庫数量',
    lot_number VARCHAR(50) NOT NULL COMMENT 'ロット番号',
    expiry_date VARCHAR(20) COMMENT '賞味期限',
    outbound_type VARCHAR(20) NOT NULL COMMENT '出庫タイプ（通常出庫, 返品出庫, 調整出庫）',
    status VARCHAR(20) DEFAULT '処理中' COMMENT '状態（処理中, 完了）',
    shipping_time DATETIME COMMENT '出荷時間',
    operator VARCHAR(50) COMMENT '担当者',
    remarks VARCHAR(255) COMMENT '備考',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ'
) COMMENT='出庫管理テーブル';

-- 在庫履歴テーブル: retail_inventory_history
CREATE TABLE IF NOT EXISTS retail_inventory_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '履歴ID',
    inventory_id BIGINT NOT NULL COMMENT '在庫ID',
    type VARCHAR(20) NOT NULL COMMENT '操作タイプ（入庫, 出庫）',
    quantity INT NOT NULL COMMENT '数量（入庫はプラス、出庫はマイナス）',
    date DATETIME NOT NULL COMMENT '操作日時',
    reason VARCHAR(100) COMMENT '理由',
    operator VARCHAR(50) COMMENT '担当者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ'
) COMMENT='在庫履歴テーブル';

-- 初期データ投入: retail_inventory
INSERT INTO retail_inventory (id, store_id, store_name, product_id, product_name, stock, expiry_date, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 1, '東京本店', 1, 'プレミアムコーヒー', 5, '2024-04-20', 'low', NOW(), NOW(), NULL, NULL, 0),
(2, 2, '横浜駅前店', 2, 'ハンバーガー', 15, '2024-04-25', 'normal', NOW(), NOW(), NULL, NULL, 0),
(3, 3, '金沢店', 3, 'フライドポテト', 30, '2024-05-01', 'high', NOW(), NOW(), NULL, NULL, 0);

-- 初期データ投入: retail_inventory_in
INSERT INTO retail_inventory_in (id, store_id, store_name, product_id, product_name, quantity, lot_number, expiry_date, restock_type, status, shipping_time, operator, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 1, '東京本店', 1, 'プレミアムコーヒー', 100, 'LOT20240320-001', '2024-06-20', '通常入庫', '完了', '2024-03-20 10:00:00', '山田太郎', '2024-03-20 10:00:00', '2024-03-20 10:00:00', NULL, NULL, 0),
(2, 2, '横浜駅前店', 2, '石鹸', 200, 'LOT20240320-002', '2025-06-30', '返品入庫', '完了', '2024-03-20 11:00:00', '鈴木一郎', '2024-03-20 11:00:00', '2024-03-20 11:00:00', NULL, NULL, 0),
(3, 3, '金沢店', 3, 'シャンプー', 150, 'LOT20240320-003', '2025-03-31', '調整入庫', '完了', '2024-03-20 12:00:00', '佐藤花子', '2024-03-20 12:00:00', '2024-03-20 12:00:00', NULL, NULL, 0);

-- 初期データ投入: retail_inventory_out
INSERT INTO retail_inventory_out (id, store_id, store_name, product_id, product_name, quantity, lot_number, expiry_date, outbound_type, status, shipping_time, operator, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 1, '東京本店', 1, 'プレミアムコーヒー', 50, 'LOT20240320-001', '2024-06-20', '通常出庫', '完了', '2024-03-20 10:00:00', '山田太郎', '2024-03-20 10:00:00', '2024-03-20 10:00:00', NULL, NULL, 0),
(2, 2, '横浜駅前店', 2, '石鹸', 100, 'LOT20240320-002', '2025-06-30', '返品出庫', '完了', '2024-03-20 11:00:00', '鈴木一郎', '2024-03-20 11:00:00', '2024-03-20 11:00:00', NULL, NULL, 0),
(3, 3, '金沢店', 3, 'シャンプー', 75, 'LOT20240320-003', '2025-03-31', '調整出庫', '完了', '2024-03-20 12:00:00', '佐藤花子', '2024-03-20 12:00:00', '2024-03-20 12:00:00', NULL, NULL, 0);

-- 初期データ投入: retail_inventory_history
INSERT INTO retail_inventory_history (id, inventory_id, type, quantity, date, reason, operator, create_time, update_time, is_deleted) VALUES
(1, 1, '入庫', 100, '2024-03-20 10:00:00', '通常入庫', '山田太郎', NOW(), NOW(), 0),
(2, 1, '出庫', -50, '2024-03-20 11:00:00', '店舗出荷', '鈴木一郎', NOW(), NOW(), 0),
(3, 2, '入庫', 75, '2024-03-19 09:00:00', '返品入庫', '佐藤花子', NOW(), NOW(), 0),
(4, 2, '出庫', -25, '2024-03-19 14:00:00', '破損品廃棄', '田中次郎', NOW(), NOW(), 0),
(5, 3, '入庫', 150, '2024-03-18 10:00:00', '通常入庫', '中村三郎', NOW(), NOW(), 0),
(6, 3, '出庫', -100, '2024-03-18 15:00:00', '店舗出荷', '小林四郎', NOW(), NOW(), 0);