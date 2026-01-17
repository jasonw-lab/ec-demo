-- Seed data for demo scenarios

-- Storage: products with stock from sample-products.csv
USE `seata_storage`;
INSERT INTO `t_product` (product_id, category_id, name, description, image_url, price, status) VALUES
  (1, 1, 'Headphones', 'Wireless over-ear headphones', 'https://picsum.photos/seed/headphone/400/300', 9800.00, 'ACTIVE'),
  (2, 1, 'Keyboard', 'Mechanical keyboard', 'https://picsum.photos/seed/keyboard/400/300', 5980.00, 'ACTIVE'),
  (3, 2, 'T-shirt', '100% cotton', 'https://picsum.photos/seed/tshirt/400/300', 1500.00, 'ACTIVE'),
  (4, 3, 'Coffee Beans', 'Specialty 200g', 'https://picsum.photos/seed/coffee/400/300', 1200.00, 'ACTIVE'),
  (1001, NULL, 'iPhone 13 128GB Blue', 'Used, battery health 85%', '/product/1001.jpg', 59800.00, 'ACTIVE'),
  (1002, NULL, 'MacBook Air M2 13-inch', '2023 model 16GB 512GB with box', '/product/1002.jpg', 128000.00, 'ACTIVE'),
  (1003, NULL, 'AirPods Pro 2nd Gen', 'MagSafe case, used', '/product/1003.jpg', 24800.00, 'ACTIVE'),
  (1004, NULL, 'iPad Pro 11-inch 256GB', '2022 model, Apple Pencil support', '/product/1004.jpg', 79800.00, 'ACTIVE'),
  (1005, NULL, 'Apple Watch Series 8 45mm', 'GPS model, band included', '/product/1005.jpg', 38000.00, 'ACTIVE'),
  (1006, NULL, 'Sony WH-1000XM5 Black', 'Noise canceling headphones', '/product/1006.jpg', 39800.00, 'ACTIVE'),
  (1007, NULL, 'Canon EOS R6 Body', 'Mirrorless, shutter count under 5000', '/product/1007.jpg', 248000.00, 'ACTIVE'),
  (1008, NULL, 'Nintendo Switch OLED', 'White, full accessories', '/product/1008.jpg', 32800.00, 'ACTIVE'),
  (1009, NULL, 'Dyson V12 Detect Slim', 'Cordless vacuum, 2023 model', '/product/1009.jpg', 58000.00, 'ACTIVE'),
  (1010, NULL, 'BALMUDA Toaster Black', 'K05A 2022 model', '/product/1010.jpg', 22000.00, 'ACTIVE'),
  (1011, NULL, 'Sony A7 IV Body', 'Full-frame mirrorless', '/product/1011.jpg', 268000.00, 'ACTIVE'),
  (1012, NULL, 'iPad mini 6 64GB Space Gray', 'Wi-Fi, Apple Pencil support', '/product/1012.jpg', 54800.00, 'ACTIVE'),
  (1013, NULL, 'Bose QuietComfort 45', 'Noise canceling headphones, white', '/product/1013.jpg', 28000.00, 'ACTIVE'),
  (1014, NULL, 'iPhone 12 mini 128GB Red', 'Battery health 82%', '/product/1014.jpg', 42000.00, 'ACTIVE'),
  (1015, NULL, 'Dell XPS 13 9320', 'Core i7 16GB 512GB', '/product/1015.jpg', 118000.00, 'ACTIVE')
  ON DUPLICATE KEY UPDATE
    category_id=VALUES(category_id),
    name=VALUES(name),
    description=VALUES(description),
    image_url=VALUES(image_url),
    price=VALUES(price),
    status=VALUES(status);

INSERT INTO `t_storage` (product_id, total, used, residue) VALUES
  (1, 100, 0, 100),
  (2, 100, 0, 100),
  (3, 100, 0, 100),
  (4, 100, 0, 100)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue);

-- Sample products from CSV (productId: 1001-1015)
INSERT INTO `t_storage` (product_id, total, used, residue) VALUES 
  (1001, 100, 0, 100),
  (1002, 100, 0, 100),
  (1003, 100, 0, 100),
  (1004, 100, 0, 100),
  (1005, 100, 0, 100),
  (1006, 100, 0, 100),
  (1007, 100, 0, 100),
  (1008, 100, 0, 100),
  (1009, 100, 0, 100),
  (1010, 100, 0, 100),
  (1011, 100, 0, 100),
  (1012, 100, 0, 100),
  (1013, 100, 0, 100),
  (1014, 100, 0, 100),
  (1015, 100, 0, 100)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue);

-- Account: two users with balances
USE `seata_account`;
INSERT INTO `t_account` (user_id, total, used, residue) VALUES (1, 1000.00, 0.00, 1000.00)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue);
INSERT INTO `t_account` (user_id, total, used, residue) VALUES (2, 50.00, 0.00, 50.00)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue);

-- Order has no initial data (created by API)
USE `seata_order`;
