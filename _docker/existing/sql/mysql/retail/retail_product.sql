
use youlai_boot;

-- 商品テーブル: retail_product
CREATE TABLE IF NOT EXISTS retail_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(100) NOT NULL COMMENT '商品名',
    code VARCHAR(50) NOT NULL COMMENT '商品コード',
    price DECIMAL(10,2) NOT NULL COMMENT '価格',
    stock INT NOT NULL COMMENT '在庫数',
    description VARCHAR(255) COMMENT '商品説明',
    category_id BIGINT NOT NULL COMMENT 'カテゴリID',
    category_name VARCHAR(100) COMMENT 'カテゴリ名',
    sales INT DEFAULT 0 COMMENT '販売数',
    image_url VARCHAR(255) COMMENT '商品画像URL',
    expiry_date VARCHAR(20) COMMENT '賞味期限',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成日時',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新日時',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ'
) COMMENT='商品マスタ';

-- 初期データ投入
INSERT INTO retail_product (id, name, code, price, stock, description, category_id, category_name, sales, image_url, expiry_date, create_time, update_time, create_by, update_by, is_deleted) VALUES
(1, 'プレミアムコーヒー', 'P0001', 980, 100, '高品質なコーヒー豆を使用したプレミアムコーヒー', 1, '飲料', 50, 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=200&h=200&fit=crop', '2024-12-31', NOW(), NOW(), NULL, NULL, 0),
(2, '石鹸', 'P0002', 280, 200, '肌に優しい天然成分配合の石鹸', 2, '日用品', 30, 'https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=200&h=200&fit=crop', '2025-06-30', NOW(), NOW(), NULL, NULL, 0),
(3, 'シャンプー', 'P0003', 680, 150, '髪の毛に潤いを与える高級シャンプー', 2, '日用品', 20, 'https://images.unsplash.com/photo-1625772452859-1c03d5bf1137?w=200&h=200&fit=crop', '2025-03-31', NOW(), NOW(), NULL, NULL, 0),
(4, '緑茶', 'P0004', 380, 150, '香り高い日本の緑茶です。', 2, '飲料', 30, 'https://images.unsplash.com/photo-1582793988951-9aed5509eb97?w=200&h=200&fit=crop', '2024-11-30', NOW(), NOW(), NULL, NULL, 0),
(5, 'オーガニックティー', 'P0005', 780, 50, '有機栽培された茶葉を使用したオーガニックティー', 1, '飲料', 75, 'https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9?w=200&h=200&fit=crop', '2025-06-30', NOW(), NOW(), NULL, NULL, 0),
(6, '天然水', 'P0006', 120, 200, '天然のミネラルを豊富に含む天然水', 2, '食品', 250, 'https://images.unsplash.com/photo-1605540436563-5bca919ae766?w=200&h=200&fit=crop', '2025-03-31', NOW(), NOW(), NULL, NULL, 0),
(7, 'チョコレート', 'P0007', 280, 150, 'カカオ70%の高品質チョコレート', 3, '食品', 180, 'https://images.unsplash.com/photo-1575377427642-087cf684f29d?w=200&h=200&fit=crop', '2024-12-31', NOW(), NOW(), NULL, NULL, 0),
(8, 'クッキー', 'P0008', 380, 80, 'バターの風味が豊かなクッキー', 3, '食品', 90, 'https://images.unsplash.com/photo-1558964122-2e32e1612f2d?w=200&h=200&fit=crop', '2024-09-30', NOW(), NOW(), NULL, NULL, 0),
(9, '紅茶', 'P0009', 680, 120, '芳醇な香りのセイロン紅茶', 1, '飲料', 150, 'https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9?w=200&h=200&fit=crop', '2025-06-30', NOW(), NOW(), NULL, NULL, 0),
(10, 'ポテトチップス', 'P0010', 180, 200, 'サクサク食感のポテトチップス', 2, '食品', 90, 'https://images.unsplash.com/photo-1565402170291-8491f14678db?w=200&h=200&fit=crop', '2025-03-31', NOW(), NOW(), NULL, NULL, 0),
(11, 'キャンディー', 'P0011', 150, 300, 'フルーツ味のキャンディー', 2, '食品', 365, 'https://images.unsplash.com/photo-1575224300306-1b8da36134ec?w=200&h=200&fit=crop', '2025-06-30', NOW(), NOW(), NULL, NULL, 0),
(12, 'ハンドソープ', 'P0012', 380, 150, '保湿成分配合のハンドソープ', 3, '日用品', 180, 'https://images.unsplash.com/photo-1606813907290-d86a9c56a3b4?w=200&h=200&fit=crop', '2025-06-30', NOW(), NOW(), NULL, NULL, 0),
(13, '歯ブラシ', 'P0013', 320, 200, '使いやすい柔らかめの歯ブラシです。', 3, '日用品', 60, 'https://images.unsplash.com/photo-1607613009820-a29f7bb81c04?w=200&h=200&fit=crop', '2026-12-31', NOW(), NOW(), NULL, NULL, 0),
(14, 'ノート', 'P0014', 280, 300, '書きやすい上質な紙のノートです。', 4, '雑貨', 100, 'https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=200&h=200&fit=crop', '2026-12-31', NOW(), NOW(), NULL, NULL, 0),
(15, 'ハーブティー', 'P0015', 580, 90, 'リラックス効果のあるハーブティー', 1, '飲料', 180, 'https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9?w=200&h=200&fit=crop', '2025-12-31', NOW(), NOW(), NULL, NULL, 0),
(16, 'スポンジ', 'P0016', 280, 200, 'キッチン用スポンジ', 3, '日用品', 365, 'https://images.unsplash.com/photo-1606813907290-d86a9c56a3b4?w=200&h=200&fit=crop', '2025-06-30', NOW(), NOW(), NULL, NULL, 0),
(17, '収納ボックス', 'P0017', 1280, 50, 'スタッキング可能な収納ボックス', 4, '日用品', 1825, 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=200&h=200&fit=crop', '2025-12-31', NOW(), NOW(), NULL, NULL, 0),
(18, 'フォトフレーム', 'P0018', 980, 80, '木製フォトフレーム', 4, '日用品', 1825, 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=200&h=200&fit=crop', '2025-06-30', NOW(), NOW(), NULL, NULL, 0); 