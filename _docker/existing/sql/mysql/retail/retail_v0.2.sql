use youlai_boot;

-- ----------------------------
-- Table structure for retail_store
-- ----------------------------
DROP TABLE IF EXISTS `retail_store`;
CREATE TABLE `retail_store` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '店舗ID',
  `store_code` varchar(20) NOT NULL COMMENT '店舗コード',
  `store_name` varchar(100) NOT NULL COMMENT '店舗名',
  `address` varchar(255) DEFAULT NULL COMMENT '住所',
  `phone` varchar(20) DEFAULT NULL COMMENT '電話番号',
  `manager` varchar(50) DEFAULT NULL COMMENT '店長名',
  `status` varchar(20) DEFAULT 'active' COMMENT '状態（active, inactive）',
  `opening_hours` varchar(100) DEFAULT NULL COMMENT '営業時間',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  `create_by` bigint DEFAULT NULL COMMENT '作成者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '削除フラグ',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_code` (`store_code`) COMMENT '店舗コードはユニーク'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店舗マスタ';

-- ----------------------------
-- Table structure for retail_product
-- ----------------------------
DROP TABLE IF EXISTS `retail_product`;
CREATE TABLE `retail_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `product_code` varchar(30) NOT NULL COMMENT '商品コード',
  `product_name` varchar(100) NOT NULL COMMENT '商品名',
  `barcode` varchar(50) DEFAULT NULL COMMENT 'バーコード',
  `category_id` bigint DEFAULT NULL COMMENT 'カテゴリID',
  `category_name` varchar(50) DEFAULT NULL COMMENT 'カテゴリ名',
  `unit_price` decimal(10,2) NOT NULL COMMENT '販売価格',
  `cost_price` decimal(10,2) DEFAULT NULL COMMENT '原価',
  `unit` varchar(20) DEFAULT NULL COMMENT '単位（個、kg等）',
  `shelf_life_days` int DEFAULT NULL COMMENT '賞味期限（日数）',
  `supplier_id` bigint DEFAULT NULL COMMENT '仕入先ID',
  `supplier_name` varchar(100) DEFAULT NULL COMMENT '仕入先名',
  `description` varchar(500) DEFAULT NULL COMMENT '商品説明',
  `image_url` varchar(255) DEFAULT NULL COMMENT '商品画像URL',
  `status` varchar(20) DEFAULT 'active' COMMENT '状態（active, inactive）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  `create_by` bigint DEFAULT NULL COMMENT '作成者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '削除フラグ',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`) COMMENT '商品コードはユニーク',
  KEY `idx_category_id` (`category_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品マスタ';

-- ----------------------------
-- Table structure for retail_inventory
-- ----------------------------
DROP TABLE IF EXISTS `retail_inventory`;
CREATE TABLE `retail_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '在庫ID',
  `store_id` bigint NOT NULL COMMENT '店舗ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `lot_number` varchar(50) NOT NULL COMMENT 'ロット番号',
  `quantity` int NOT NULL DEFAULT '0' COMMENT '在庫数量',
  `min_stock` int DEFAULT '0' COMMENT '最小在庫数',
  `max_stock` int DEFAULT '0' COMMENT '最大在庫数',
  `expiry_date` date DEFAULT NULL COMMENT '賞味期限',
  `location` varchar(50) DEFAULT NULL COMMENT '保管場所',
  `status` varchar(20) DEFAULT 'normal' COMMENT '在庫状態（low, normal, high, expired）',
  `last_count_date` datetime DEFAULT NULL COMMENT '最終棚卸日',
  `remarks` varchar(500) DEFAULT NULL COMMENT '備考',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  `create_by` bigint DEFAULT NULL COMMENT '作成者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '削除フラグ',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_product_lot` (`store_id`,`product_id`,`lot_number`) COMMENT '店舗と商品とロット番号の組み合わせはユニーク',
  KEY `idx_store_id` (`store_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expiry_date` (`expiry_date`),
  CONSTRAINT `fk_inventory_product` FOREIGN KEY (`product_id`) REFERENCES `retail_product` (`id`),
  CONSTRAINT `fk_inventory_store` FOREIGN KEY (`store_id`) REFERENCES `retail_store` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在庫テーブル';

-- ----------------------------
-- Table structure for retail_inventory_transaction
-- ----------------------------
DROP TABLE IF EXISTS `retail_inventory_transaction`;
CREATE TABLE `retail_inventory_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '入出庫ID',
  `store_id` bigint NOT NULL COMMENT '店舗ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `lot_number` varchar(50) NOT NULL COMMENT 'ロット番号',
  `transaction_type` enum('IN','OUT','SALE','ADJUST') NOT NULL COMMENT '操作タイプ',
  `quantity` int NOT NULL COMMENT '数量',
  `transaction_date` datetime NOT NULL COMMENT '操作日時',
  `expiry_date` date DEFAULT NULL COMMENT '賞味期限',
  `status` varchar(20) NOT NULL COMMENT '状態（処理中, 完了）',
  `reason` varchar(50) DEFAULT NULL COMMENT '理由（仕入れ, 返品, 廃棄, 販売, 移動等）',
  `reference_no` varchar(50) DEFAULT NULL COMMENT '参照番号（発注番号, 販売番号等）',
  `source_dest` varchar(100) DEFAULT NULL COMMENT '移動元/移動先',
  `operator` varchar(50) DEFAULT NULL COMMENT '担当者',
  `remarks` varchar(500) DEFAULT NULL COMMENT '備考',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  `create_by` bigint DEFAULT NULL COMMENT '作成者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '削除フラグ',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_status` (`status`),
  KEY `idx_transaction_type` (`transaction_type`),
  KEY `idx_transaction_date` (`transaction_date`),
  KEY `idx_lot_number` (`lot_number`),
  CONSTRAINT `fk_transaction_product` FOREIGN KEY (`product_id`) REFERENCES `retail_product` (`id`),
  CONSTRAINT `fk_transaction_store` FOREIGN KEY (`store_id`) REFERENCES `retail_store` (`id`),
  CONSTRAINT `chk_quantity` CHECK ((`quantity` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入出庫履歴テーブル';

-- ----------------------------
-- Table structure for retail_alert
-- ----------------------------
DROP TABLE IF EXISTS `retail_alert`;
CREATE TABLE `retail_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'アラートID',
  `store_id` bigint NOT NULL COMMENT '店舗ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `lot_number` varchar(50) NOT NULL COMMENT 'ロット番号',
  `alert_type` enum('LOW_STOCK','EXPIRED') NOT NULL COMMENT 'アラートタイプ',
  `alert_message` varchar(255) DEFAULT NULL COMMENT 'アラートメッセージ',
  `alert_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'アラート日時',
  `resolved` tinyint(1) DEFAULT '0' COMMENT '解決フラグ',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  `create_by` bigint DEFAULT NULL COMMENT '作成者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '削除フラグ',
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_alert_type` (`alert_type`),
  KEY `idx_resolved` (`resolved`),
  CONSTRAINT `fk_alert_product` FOREIGN KEY (`product_id`) REFERENCES `retail_product` (`id`),
  CONSTRAINT `fk_alert_store` FOREIGN KEY (`store_id`) REFERENCES `retail_store` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='アラートテーブル';

-- ----------------------------
-- Table structure for retail_sales
-- ----------------------------
DROP TABLE IF EXISTS `retail_sales`;
CREATE TABLE `retail_sales` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '売上ID',
  `store_id` bigint NOT NULL COMMENT '店舗ID',
  `order_number` varchar(50) NOT NULL COMMENT '注文番号',
  `total_amount` decimal(10,2) NOT NULL COMMENT '合計金額',
  `payment_method` enum('CASH','CARD','QR','OTHER') NOT NULL COMMENT '支払方法',
  `payment_provider` varchar(50) DEFAULT NULL COMMENT '決済プロバイダ',
  `payment_reference_id` varchar(100) DEFAULT NULL COMMENT '決済参照ID',
  `sale_timestamp` datetime NOT NULL COMMENT '売上日時',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  `create_by` bigint DEFAULT NULL COMMENT '作成者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '削除フラグ',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_number` (`order_number`) COMMENT '注文番号はユニーク',
  KEY `idx_store_id` (`store_id`),
  KEY `idx_sale_timestamp` (`sale_timestamp`),
  CONSTRAINT `fk_sales_store` FOREIGN KEY (`store_id`) REFERENCES `retail_store` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='売上ヘッダテーブル';

-- ----------------------------
-- Table structure for retail_sales_detail
-- ----------------------------
DROP TABLE IF EXISTS `retail_sales_detail`;
CREATE TABLE `retail_sales_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '売上明細ID',
  `sales_id` bigint NOT NULL COMMENT '売上ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `lot_number` varchar(50) NOT NULL COMMENT 'ロット番号',
  `quantity` int NOT NULL COMMENT '数量',
  `unit_price` decimal(10,2) NOT NULL COMMENT '単価',
  `subtotal` decimal(10,2) NOT NULL COMMENT '小計',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  `create_by` bigint DEFAULT NULL COMMENT '作成者',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '削除フラグ',
  PRIMARY KEY (`id`),
  KEY `idx_sales_id` (`sales_id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_sales_detail_product` FOREIGN KEY (`product_id`) REFERENCES `retail_product` (`id`),
  CONSTRAINT `fk_sales_detail_sales` FOREIGN KEY (`sales_id`) REFERENCES `retail_sales` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='売上明細テーブル';

-- ----------------------------
-- テストデータ投入: retail_store
-- ----------------------------
INSERT INTO retail_store (id, store_code, store_name, address, phone, manager, status, opening_hours, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 'ST001', '東京本店', '東京都渋谷区神宮前1-1-1', '03-1234-5678', '山田太郎', 'active', '24時間', NOW(), NOW(), NULL, NULL, 0),
(2, 'ST002', '横浜駅前店', '神奈川県横浜市西区高島2-2-2', '045-123-4567', '鈴木一郎', 'active', '24時間', NOW(), NOW(), NULL, NULL, 0),
(3, 'ST003', '金沢店', '石川県金沢市香林坊3-3-3', '076-123-4567', '佐藤花子', 'active', '24時間', NOW(), NOW(), NULL, NULL, 0),
(4, 'ST004', '大阪梅田店', '大阪府大阪市北区梅田4-4-4', '06-1234-5678', '田中次郎', 'active', '24時間', NOW(), NOW(), NULL, NULL, 0),
(5, 'ST005', '福岡博多店', '福岡県福岡市博多区博多5-5-5', '092-123-4567', '中村三郎', 'active', '24時間', NOW(), NOW(), NULL, NULL, 0);

-- ----------------------------
-- テストデータ投入: retail_product
-- ----------------------------
INSERT INTO retail_product (id, product_code, product_name, barcode, category_id, category_name, unit_price, cost_price, unit, shelf_life_days, supplier_id, supplier_name, description, image_url, status, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 'P0001', 'プレミアムコーヒー', '4901234567890', 1, '飲料', 980.00, 500.00, '個', 180, 1, 'コーヒー株式会社', '高品質なコーヒー豆を使用したプレミアムコーヒー', 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(2, 'P0002', '石鹸', '4902345678901', 3, '日用品', 280.00, 150.00, '個', 365, 2, '石鹸株式会社', '肌に優しい天然成分配合の石鹸', 'https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(3, 'P0003', 'シャンプー', '4903456789012', 3, '日用品', 680.00, 350.00, '個', 365, 3, 'シャンプー株式会社', '髪の毛に潤いを与える高級シャンプー', 'https://images.unsplash.com/photo-1625772452859-1c03d5bf1137?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(4, 'P0004', '緑茶', '4904567890123', 1, '飲料', 380.00, 200.00, '個', 180, 4, 'お茶株式会社', '香り高い日本の緑茶です。', 'https://images.unsplash.com/photo-1582793988951-9aed5509eb97?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(5, 'P0005', 'オーガニックティー', '4905678901234', 1, '飲料', 780.00, 400.00, '個', 180, 5, 'ティー株式会社', '有機栽培された茶葉を使用したオーガニックティー', 'https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(6, 'P0006', '天然水', '4906789012345', 2, '食品', 120.00, 60.00, '本', 365, 6, '水株式会社', '天然のミネラルを豊富に含む天然水', 'https://images.unsplash.com/photo-1605540436563-5bca919ae766?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(7, 'P0007', 'チョコレート', '4907890123456', 2, '食品', 280.00, 140.00, '個', 180, 7, 'チョコレート株式会社', 'カカオ70%の高品質チョコレート', 'https://images.unsplash.com/photo-1575377427642-087cf684f29d?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(8, 'P0008', 'クッキー', '4908901234567', 2, '食品', 380.00, 190.00, '個', 90, 8, 'クッキー株式会社', 'バターの風味が豊かなクッキー', 'https://images.unsplash.com/photo-1558964122-2e32e1612f2d?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(9, 'P0009', '紅茶', '4909012345678', 1, '飲料', 680.00, 340.00, '個', 180, 9, '紅茶株式会社', '芳醇な香りのセイロン紅茶', 'https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(10, 'P0010', 'ポテトチップス', '4900123456789', 2, '食品', 180.00, 90.00, '個', 90, 10, 'スナック株式会社', 'サクサク食感のポテトチップス', 'https://images.unsplash.com/photo-1565402170291-8491f14678db?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(11, 'P0011', 'キャンディー', '4901234567891', 2, '食品', 150.00, 75.00, '個', 365, 11, 'キャンディー株式会社', 'フルーツ味のキャンディー', 'https://images.unsplash.com/photo-1575224300306-1b8da36134ec?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(12, 'P0012', 'ハンドソープ', '4902345678902', 3, '日用品', 380.00, 190.00, '個', 365, 12, 'ハンドソープ株式会社', '保湿成分配合のハンドソープ', 'https://images.unsplash.com/photo-1606813907290-d86a9c56a3b4?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(13, 'P0013', '歯ブラシ', '4903456789013', 3, '日用品', 320.00, 160.00, '個', 730, 13, '歯ブラシ株式会社', '使いやすい柔らかめの歯ブラシです。', 'https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(14, 'P0014', 'ノート', '4904567890124', 4, '雑貨', 280.00, 140.00, '個', 730, 14, 'ノート株式会社', '書きやすい上質な紙のノートです。', 'https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(15, 'P0015', 'ハーブティー', '4905678901235', 1, '飲料', 580.00, 290.00, '個', 180, 15, 'ハーブティー株式会社', 'リラックス効果のあるハーブティー', 'https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(16, 'P0016', 'スポンジ', '4906789012346', 3, '日用品', 280.00, 140.00, '個', 365, 16, 'スポンジ株式会社', 'キッチン用スポンジ', 'https://images.unsplash.com/photo-1606813907290-d86a9c56a3b4?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(17, 'P0017', '収納ボックス', '4907890123457', 4, '日用品', 1280.00, 640.00, '個', 730, 17, '収納ボックス株式会社', 'スタッキング可能な収納ボックス', 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0),
(18, 'P0018', 'フォトフレーム', '4908901234568', 4, '日用品', 980.00, 490.00, '個', 730, 18, 'フォトフレーム株式会社', '木製フォトフレーム', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=200&h=200&fit=crop', 'active', NOW(), NOW(), NULL, NULL, 0);

-- ----------------------------
-- テストデータ投入: retail_inventory
-- ----------------------------
INSERT INTO retail_inventory (id, store_id, product_id, lot_number, quantity, min_stock, max_stock, expiry_date, location, status, last_count_date, remarks, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 1, 1, 'LOT20240320-001', 5, 10, 100, '2024-06-20', 'A-1-1', 'low', NOW(), '在庫切れ注意', NOW(), NOW(), NULL, NULL, 0),
(2, 1, 2, 'LOT20240320-002', 15, 5, 50, '2025-06-30', 'A-1-2', 'normal', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(3, 1, 3, 'LOT20240320-003', 30, 10, 100, '2025-03-31', 'A-1-3', 'high', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(4, 2, 1, 'LOT20240320-004', 20, 10, 100, '2024-06-20', 'B-1-1', 'normal', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(5, 2, 2, 'LOT20240320-005', 25, 5, 50, '2025-06-30', 'B-1-2', 'normal', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(6, 2, 3, 'LOT20240320-006', 40, 10, 100, '2025-03-31', 'B-1-3', 'high', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(7, 3, 1, 'LOT20240320-007', 8, 10, 100, '2024-06-20', 'C-1-1', 'low', NOW(), '在庫切れ注意', NOW(), NOW(), NULL, NULL, 0),
(8, 3, 2, 'LOT20240320-008', 12, 5, 50, '2025-06-30', 'C-1-2', 'normal', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(9, 3, 3, 'LOT20240320-009', 35, 10, 100, '2025-03-31', 'C-1-3', 'high', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(10, 4, 1, 'LOT20240320-010', 15, 10, 100, '2024-06-20', 'D-1-1', 'normal', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(11, 4, 2, 'LOT20240320-011', 18, 5, 50, '2025-06-30', 'D-1-2', 'normal', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(12, 4, 3, 'LOT20240320-012', 45, 10, 100, '2025-03-31', 'D-1-3', 'high', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(13, 5, 1, 'LOT20240320-013', 7, 10, 100, '2024-06-20', 'E-1-1', 'low', NOW(), '在庫切れ注意', NOW(), NOW(), NULL, NULL, 0),
(14, 5, 2, 'LOT20240320-014', 20, 5, 50, '2025-06-30', 'E-1-2', 'normal', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0),
(15, 5, 3, 'LOT20240320-015', 50, 10, 100, '2025-03-31', 'E-1-3', 'high', NOW(), NULL, NOW(), NOW(), NULL, NULL, 0);

-- ----------------------------
-- テストデータ投入: retail_inventory_transaction
-- ----------------------------
INSERT INTO retail_inventory_transaction (id, store_id, product_id, lot_number, transaction_type, quantity, transaction_date, expiry_date, status, reason, reference_no, source_dest, operator, remarks, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 1, 1, 'LOT20240320-001', 'IN', 100, '2024-03-20 10:00:00', '2024-06-20', '完了', '仕入れ', 'PO20240320-001', 'コーヒー株式会社', '山田太郎', '通常仕入れ', NOW(), NOW(), NULL, NULL, 0),
(2, 1, 1, 'LOT20240320-001', 'OUT', 50, '2024-03-20 11:00:00', '2024-06-20', '完了', '販売', 'SO20240320-001', '店舗販売', '鈴木一郎', '通常販売', NOW(), NOW(), NULL, NULL, 0),
(3, 1, 1, 'LOT20240320-001', 'OUT', 45, '2024-03-20 12:00:00', '2024-06-20', '完了', '販売', 'SO20240320-002', '店舗販売', '佐藤花子', '通常販売', NOW(), NOW(), NULL, NULL, 0),
(4, 2, 2, 'LOT20240320-002', 'IN', 200, '2024-03-20 13:00:00', '2025-06-30', '完了', '仕入れ', 'PO20240320-002', '石鹸株式会社', '田中次郎', '通常仕入れ', NOW(), NOW(), NULL, NULL, 0),
(5, 2, 2, 'LOT20240320-002', 'OUT', 100, '2024-03-20 14:00:00', '2025-06-30', '完了', '販売', 'SO20240320-003', '店舗販売', '中村三郎', '通常販売', NOW(), NOW(), NULL, NULL, 0),
(6, 2, 2, 'LOT20240320-002', 'OUT', 75, '2024-03-20 15:00:00', '2025-06-30', '完了', '販売', 'SO20240320-004', '店舗販売', '山田太郎', '通常販売', NOW(), NOW(), NULL, NULL, 0),
(7, 3, 3, 'LOT20240320-003', 'IN', 150, '2024-03-20 16:00:00', '2025-03-31', '完了', '仕入れ', 'PO20240320-003', 'シャンプー株式会社', '鈴木一郎', '通常仕入れ', NOW(), NOW(), NULL, NULL, 0),
(8, 3, 3, 'LOT20240320-003', 'OUT', 75, '2024-03-20 17:00:00', '2025-03-31', '完了', '販売', 'SO20240320-005', '店舗販売', '佐藤花子', '通常販売', NOW(), NOW(), NULL, NULL, 0),
(9, 3, 3, 'LOT20240320-003', 'OUT', 40, '2024-03-20 18:00:00', '2025-03-31', '完了', '販売', 'SO20240320-006', '店舗販売', '田中次郎', '通常販売', NOW(), NOW(), NULL, NULL, 0),
(10, 4, 1, 'LOT20240320-004', 'IN', 100, '2024-03-20 19:00:00', '2024-06-20', '完了', '仕入れ', 'PO20240320-004', 'コーヒー株式会社', '中村三郎', '通常仕入れ', NOW(), NOW(), NULL, NULL, 0),
(11, 4, 1, 'LOT20240320-004', 'OUT', 50, '2024-03-20 20:00:00', '2024-06-20', '完了', '販売', 'SO20240320-007', '店舗販売', '山田太郎', '通常販売', NOW(), NOW(), NULL, NULL, 0),
(12, 4, 1, 'LOT20240320-004', 'OUT', 35, '2024-03-20 21:00:00', '2024-06-20', '完了', '販売', 'SO20240320-008', '店舗販売', '鈴木一郎', '通常販売', NOW(), NOW(), NULL, NULL, 0),
(13, 5, 2, 'LOT20240320-005', 'IN', 200, '2024-03-20 22:00:00', '2025-06-30', '完了', '仕入れ', 'PO20240320-005', '石鹸株式会社', '佐藤花子', '通常仕入れ', NOW(), NOW(), NULL, NULL, 0),
(14, 5, 2, 'LOT20240320-005', 'OUT', 100, '2024-03-20 23:00:00', '2025-06-30', '完了', '販売', 'SO20240320-009', '店舗販売', '田中次郎', '通常販売', NOW(), NOW(), NULL, NULL, 0),
(15, 5, 2, 'LOT20240320-005', 'OUT', 80, '2024-03-21 00:00:00', '2025-06-30', '完了', '販売', 'SO20240320-010', '店舗販売', '中村三郎', '通常販売', NOW(), NOW(), NULL, NULL, 0);

-- ----------------------------
-- テストデータ投入: retail_alert
-- ----------------------------
INSERT INTO retail_alert (id, store_id, product_id, lot_number, alert_type, alert_message, alert_date, resolved, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 1, 1, 'LOT20240320-001', 'LOW_STOCK', '在庫が少なくなっています。', '2024-03-20 10:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(2, 1, 1, 'LOT20240320-001', 'EXPIRED', '賞味期限が近づいています。', '2024-03-20 11:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(3, 2, 2, 'LOT20240320-002', 'LOW_STOCK', '在庫が少なくなっています。', '2024-03-20 12:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(4, 2, 2, 'LOT20240320-002', 'EXPIRED', '賞味期限が近づいています。', '2024-03-20 13:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(5, 3, 3, 'LOT20240320-003', 'LOW_STOCK', '在庫が少なくなっています。', '2024-03-20 14:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(6, 3, 3, 'LOT20240320-003', 'EXPIRED', '賞味期限が近づいています。', '2024-03-20 15:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(7, 4, 1, 'LOT20240320-004', 'LOW_STOCK', '在庫が少なくなっています。', '2024-03-20 16:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(8, 4, 1, 'LOT20240320-004', 'EXPIRED', '賞味期限が近づいています。', '2024-03-20 17:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(9, 5, 2, 'LOT20240320-005', 'LOW_STOCK', '在庫が少なくなっています。', '2024-03-20 18:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(10, 5, 2, 'LOT20240320-005', 'EXPIRED', '賞味期限が近づいています。', '2024-03-20 19:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(11, 1, 3, 'LOT20240320-006', 'LOW_STOCK', '在庫が少なくなっています。', '2024-03-20 20:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(12, 1, 3, 'LOT20240320-006', 'EXPIRED', '賞味期限が近づいています。', '2024-03-20 21:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(13, 2, 1, 'LOT20240320-007', 'LOW_STOCK', '在庫が少なくなっています。', '2024-03-20 22:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(14, 2, 1, 'LOT20240320-007', 'EXPIRED', '賞味期限が近づいています。', '2024-03-20 23:00:00', 0, NOW(), NOW(), NULL, NULL, 0),
(15, 3, 2, 'LOT20240320-008', 'LOW_STOCK', '在庫が少なくなっています。', '2024-03-21 00:00:00', 0, NOW(), NOW(), NULL, NULL, 0);

-- ----------------------------
-- テストデータ投入: retail_sales
-- ----------------------------
INSERT INTO retail_sales (id, store_id, order_number, total_amount, payment_method, payment_provider, payment_reference_id, sale_timestamp, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 1, 'ORD20240320-001', 9800.00, 'CARD', 'VISA', 'PAY20240320-001', '2024-03-20 10:00:00', NOW(), NOW(), NULL, NULL, 0),
(2, 1, 'ORD20240320-002', 5600.00, 'QR', 'PayPay', 'PAY20240320-002', '2024-03-20 11:00:00', NOW(), NOW(), NULL, NULL, 0),
(3, 1, 'ORD20240320-003', 13600.00, 'CASH', NULL, NULL, '2024-03-20 12:00:00', NOW(), NOW(), NULL, NULL, 0),
(4, 2, 'ORD20240320-004', 19600.00, 'CARD', 'MasterCard', 'PAY20240320-003', '2024-03-20 13:00:00', NOW(), NOW(), NULL, NULL, 0),
(5, 2, 'ORD20240320-005', 11200.00, 'QR', 'LINE Pay', 'PAY20240320-004', '2024-03-20 14:00:00', NOW(), NOW(), NULL, NULL, 0),
(6, 2, 'ORD20240320-006', 27200.00, 'CASH', NULL, NULL, '2024-03-20 15:00:00', NOW(), NOW(), NULL, NULL, 0),
(7, 3, 'ORD20240320-007', 29400.00, 'CARD', 'JCB', 'PAY20240320-005', '2024-03-20 16:00:00', NOW(), NOW(), NULL, NULL, 0),
(8, 3, 'ORD20240320-008', 16800.00, 'QR', 'PayPay', 'PAY20240320-006', '2024-03-20 17:00:00', NOW(), NOW(), NULL, NULL, 0),
(9, 3, 'ORD20240320-009', 40800.00, 'CASH', NULL, NULL, '2024-03-20 18:00:00', NOW(), NOW(), NULL, NULL, 0),
(10, 4, 'ORD20240320-010', 39200.00, 'CARD', 'VISA', 'PAY20240320-007', '2024-03-20 19:00:00', NOW(), NOW(), NULL, NULL, 0),
(11, 4, 'ORD20240320-011', 22400.00, 'QR', 'LINE Pay', 'PAY20240320-008', '2024-03-20 20:00:00', NOW(), NOW(), NULL, NULL, 0),
(12, 4, 'ORD20240320-012', 54400.00, 'CASH', NULL, NULL, '2024-03-20 21:00:00', NOW(), NOW(), NULL, NULL, 0),
(13, 5, 'ORD20240320-013', 49000.00, 'CARD', 'MasterCard', 'PAY20240320-009', '2024-03-20 22:00:00', NOW(), NOW(), NULL, NULL, 0),
(14, 5, 'ORD20240320-014', 28000.00, 'QR', 'PayPay', 'PAY20240320-010', '2024-03-20 23:00:00', NOW(), NOW(), NULL, NULL, 0),
(15, 5, 'ORD20240320-015', 68000.00, 'CASH', NULL, NULL, '2024-03-21 00:00:00', NOW(), NOW(), NULL, NULL, 0);

-- ----------------------------
-- テストデータ投入: retail_sales_detail
-- ----------------------------
INSERT INTO retail_sales_detail (id, sales_id, product_id, lot_number, quantity, unit_price, subtotal, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 1, 1, 'LOT20240320-001', 10, 980.00, 9800.00, NOW(), NOW(), NULL, NULL, 0),
(2, 2, 2, 'LOT20240320-002', 20, 280.00, 5600.00, NOW(), NOW(), NULL, NULL, 0),
(3, 3, 3, 'LOT20240320-003', 20, 680.00, 13600.00, NOW(), NOW(), NULL, NULL, 0),
(4, 4, 1, 'LOT20240320-004', 20, 980.00, 19600.00, NOW(), NOW(), NULL, NULL, 0),
(5, 5, 2, 'LOT20240320-005', 40, 280.00, 11200.00, NOW(), NOW(), NULL, NULL, 0),
(6, 6, 3, 'LOT20240320-006', 40, 680.00, 27200.00, NOW(), NOW(), NULL, NULL, 0),
(7, 7, 1, 'LOT20240320-007', 30, 980.00, 29400.00, NOW(), NOW(), NULL, NULL, 0),
(8, 8, 2, 'LOT20240320-008', 60, 280.00, 16800.00, NOW(), NOW(), NULL, NULL, 0),
(9, 9, 3, 'LOT20240320-009', 60, 680.00, 40800.00, NOW(), NOW(), NULL, NULL, 0),
(10, 10, 1, 'LOT20240320-010', 40, 980.00, 39200.00, NOW(), NOW(), NULL, NULL, 0),
(11, 11, 2, 'LOT20240320-011', 80, 280.00, 22400.00, NOW(), NOW(), NULL, NULL, 0),
(12, 12, 3, 'LOT20240320-012', 80, 680.00, 54400.00, NOW(), NOW(), NULL, NULL, 0),
(13, 13, 1, 'LOT20240320-013', 50, 980.00, 49000.00, NOW(), NOW(), NULL, NULL, 0),
(14, 14, 2, 'LOT20240320-014', 100, 280.00, 28000.00, NOW(), NOW(), NULL, NULL, 0),
(15, 15, 3, 'LOT20240320-015', 100, 680.00, 68000.00, NOW(), NOW(), NULL, NULL, 0); 