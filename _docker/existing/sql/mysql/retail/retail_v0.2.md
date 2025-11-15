# 📦 無人小売店舗向け 在庫・売上管理 DB設計（MySQL 8）

## 🏪 1. 店舗テーブル `retail_store`
```sql
CREATE TABLE retail_store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '店舗ID',
    store_code VARCHAR(20) UNIQUE NOT NULL COMMENT '店舗コード',
    store_name VARCHAR(100) NOT NULL COMMENT '店舗名',
    address VARCHAR(255) COMMENT '住所',
    phone VARCHAR(20) COMMENT '電話番号',
    manager VARCHAR(50) COMMENT '店長名',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状態（active, inactive）',
    opening_hours VARCHAR(100) COMMENT '営業時間',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ'
);
```

## 📦 2. 商品テーブル `retail_product`
```sql
CREATE TABLE retail_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    product_code VARCHAR(30) UNIQUE NOT NULL COMMENT '商品コード',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名',
    barcode VARCHAR(50) COMMENT 'バーコード',
    category_id BIGINT COMMENT 'カテゴリID',
    category_name VARCHAR(50) COMMENT 'カテゴリ名',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '販売価格',
    cost_price DECIMAL(10,2) COMMENT '原価',
    unit VARCHAR(20) COMMENT '単位（個、kg等）',
    shelf_life_days INT COMMENT '賞味期限（日数）',
    supplier_id BIGINT COMMENT '仕入先ID',
    supplier_name VARCHAR(100) COMMENT '仕入先名',
    description VARCHAR(500) COMMENT '商品説明',
    image_url VARCHAR(255) COMMENT '商品画像URL',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状態（active, inactive）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ'
);
```

## 📊 3. 在庫テーブル（ロット対応）`retail_inventory`
```sql
CREATE TABLE retail_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '在庫ID',
    store_id BIGINT NOT NULL COMMENT '店舗ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    lot_number VARCHAR(50) NOT NULL COMMENT 'ロット番号',
    quantity INT DEFAULT 0 COMMENT '在庫数量',
    min_stock INT DEFAULT 0 COMMENT '最小在庫数',
    max_stock INT DEFAULT 0 COMMENT '最大在庫数',
    expiry_date DATE COMMENT '賞味期限',
    location VARCHAR(50) COMMENT '保管場所',
    status VARCHAR(20) DEFAULT 'normal' COMMENT '在庫状態（low, normal, high, expired）',
    last_count_date DATETIME COMMENT '最終棚卸日',
    remarks VARCHAR(500) COMMENT '備考',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ',
    UNIQUE KEY `uk_store_product_lot` (`store_id`, `product_id`, `lot_number`),
    FOREIGN KEY (store_id) REFERENCES retail_store(id),
    FOREIGN KEY (product_id) REFERENCES retail_product(id)
);
```

## 🔄 4. 入出庫履歴テーブル `retail_inventory_transaction`
```sql
CREATE TABLE retail_inventory_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '入出庫ID',
    store_id BIGINT NOT NULL COMMENT '店舗ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    lot_number VARCHAR(50) NOT NULL COMMENT 'ロット番号',
    transaction_type ENUM('IN', 'OUT', 'SALE', 'ADJUST') NOT NULL COMMENT '操作タイプ',
    quantity INT NOT NULL CHECK (quantity > 0) COMMENT '数量',
    transaction_date DATETIME NOT NULL COMMENT '操作日時',
    expiry_date DATE COMMENT '賞味期限',
    status VARCHAR(20) NOT NULL COMMENT '状態（処理中, 完了）',
    reason VARCHAR(50) COMMENT '理由（仕入れ, 返品, 廃棄, 販売, 移動等）',
    reference_no VARCHAR(50) COMMENT '参照番号（発注番号, 販売番号等）',
    source_dest VARCHAR(100) COMMENT '移動元/移動先',
    operator VARCHAR(50) COMMENT '担当者',
    remarks VARCHAR(500) COMMENT '備考',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ',
    FOREIGN KEY (store_id) REFERENCES retail_store(id),
    FOREIGN KEY (product_id) REFERENCES retail_product(id)
);
```

## ⚠️ 5. アラートテーブル `retail_alert`
```sql
CREATE TABLE retail_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'アラートID',
    store_id BIGINT NOT NULL COMMENT '店舗ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    lot_number VARCHAR(50) NOT NULL COMMENT 'ロット番号',
    alert_type ENUM('LOW_STOCK', 'EXPIRED') NOT NULL COMMENT 'アラートタイプ',
    alert_message VARCHAR(255) COMMENT 'アラートメッセージ',
    alert_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'アラート日時',
    resolved BOOLEAN DEFAULT FALSE COMMENT '解決フラグ',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ',
    FOREIGN KEY (store_id) REFERENCES retail_store(id),
    FOREIGN KEY (product_id) REFERENCES retail_product(id)
);
```

## 💳 6. 売上ヘッダ（決済含む）テーブル `retail_sales`
```sql
CREATE TABLE retail_sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '売上ID',
    store_id BIGINT NOT NULL COMMENT '店舗ID',
    order_number VARCHAR(50) UNIQUE NOT NULL COMMENT '注文番号',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '合計金額',
    payment_method ENUM('CASH','CARD','QR','OTHER') NOT NULL COMMENT '支払方法',
    payment_provider VARCHAR(50) COMMENT '決済プロバイダ',
    payment_reference_id VARCHAR(100) COMMENT '決済参照ID',
    sale_timestamp DATETIME NOT NULL COMMENT '売上日時',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ',
    FOREIGN KEY (store_id) REFERENCES retail_store(id)
);
```

## 📄 7. 売上明細テーブル `retail_sales_detail`
```sql
CREATE TABLE retail_sales_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '売上明細ID',
    sales_id BIGINT NOT NULL COMMENT '売上ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    lot_number VARCHAR(50) NOT NULL COMMENT 'ロット番号',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '単価',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小計',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '作成時間',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    create_by BIGINT COMMENT '作成者',
    update_by BIGINT COMMENT '更新者',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '削除フラグ',
    FOREIGN KEY (sales_id) REFERENCES retail_sales(id),
    FOREIGN KEY (product_id) REFERENCES retail_product(id)
);
```

## ✅ ポイントまとめ
- **ロット番号単位で在庫・売上を管理**。
- **賞味期限・在庫不足などのアラートに対応**。
- **売上に決済情報・外部IDも記録可能**。
- **トランザクション処理で `retail_inventory` と `retail_inventory_transaction` を更新**。
- **標準のDB項目（create_time, update_time, create_by, update_by, is_deleted）を追加**。
- **テーブル名にプレフィックス `retail_` を付与**。
- **コメントを日本語で詳細に記述**。 