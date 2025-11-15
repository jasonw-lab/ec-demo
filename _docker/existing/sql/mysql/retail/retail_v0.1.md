# 📦 無人小売店舗向け 在庫・売上管理 DB設計（MySQL 8）

## 🏪 1. 店舗テーブル `store`
```sql
CREATE TABLE store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_code VARCHAR(20) UNIQUE NOT NULL,
    store_name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 📦 2. 商品テーブル `product`
```sql
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(30) UNIQUE NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    unit_price DECIMAL(10,2),
    cost_price DECIMAL(10,2),
    shelf_life_days INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 📊 3. 在庫テーブル（ロット対応）`inventory`
```sql
CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    lot_number VARCHAR(50) NOT NULL,
    quantity INT DEFAULT 0,
    expiry_date DATE,
    received_date DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (store_id, product_id, lot_number),
    FOREIGN KEY (store_id) REFERENCES store(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);
```

## 🔄 4. 入出庫履歴テーブル `inventory_transaction`
```sql
CREATE TABLE inventory_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    lot_number VARCHAR(50) NOT NULL,
    transaction_type ENUM('IN', 'OUT', 'SALE', 'ADJUST') NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    transaction_date DATETIME NOT NULL,
    expiry_date DATE,
    remarks VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (store_id) REFERENCES store(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);
```

## ⚠️ 5. アラートテーブル `alert`
```sql
CREATE TABLE alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    lot_number VARCHAR(50) NOT NULL,
    alert_type ENUM('LOW_STOCK', 'EXPIRED') NOT NULL,
    alert_message VARCHAR(255),
    alert_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (store_id) REFERENCES store(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);
```

## 💳 6. 売上ヘッダ（決済含む）テーブル `sales`
```sql
CREATE TABLE sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM('CASH','CARD','QR','OTHER') NOT NULL,
    payment_provider VARCHAR(50),
    payment_reference_id VARCHAR(100),
    sale_timestamp DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (store_id) REFERENCES store(id)
);
```

## 📄 7. 売上明細テーブル `sales_detail`
```sql
CREATE TABLE sales_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sales_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    lot_number VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (sales_id) REFERENCES sales(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);
```

## ✅ ポイントまとめ
- **ロット番号単位で在庫・売上を管理**。
- **賞味期限・在庫不足などのアラートに対応**。
- **売上に決済情報・外部IDも記録可能**。
- **トランザクション処理で `inventory` と `inventory_transaction` を更新**。
