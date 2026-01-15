-- Seed data for demo scenarios

-- Storage: products with stock from sample-products.csv
USE `seata_storage`;
INSERT INTO `t_storage` (product_id, total, used, residue) VALUES (1, 100, 0, 100)
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
