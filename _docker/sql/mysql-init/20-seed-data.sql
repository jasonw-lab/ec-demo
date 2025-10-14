-- Seed data for demo scenarios

-- Storage: one product with stock
USE `seata_storage`;
INSERT INTO `t_storage` (product_id, total, used, residue) VALUES (1, 100, 0, 100)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue);

-- Account: two users with balances
USE `seata_account`;
INSERT INTO `t_account` (user_id, total, used, residue) VALUES (1, 1000.00, 0.00, 1000.00)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue);
INSERT INTO `t_account` (user_id, total, used, residue) VALUES (2, 50.00, 0.00, 50.00)
  ON DUPLICATE KEY UPDATE total=VALUES(total), used=VALUES(used), residue=VALUES(residue);

-- Order has no initial data (created by API)
USE `seata_order`;
