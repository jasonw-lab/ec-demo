-- TCC mode seed data

-- TCC Storage: one product with stock
USE `seata_storage`;
TRUNCATE TABLE `tcc_storage`;
INSERT INTO `tcc_storage` (xid, product_id, total, used, residue, frozen, status) VALUES ('init', 1, 100, 0, 100, 0, 1);

-- TCC Account: two users with balances
USE `seata_account`;
TRUNCATE TABLE `tcc_account`;
INSERT INTO `tcc_account` (xid, order_id, user_id, total, used, residue, frozen, status) VALUES ('init', NULL, 1, 1000.00, 0.00, 1000.00, 0.00, 'SUCCESS');
INSERT INTO `tcc_account` (xid, order_id, user_id, total, used, residue, frozen, status) VALUES (NULL, NULL, 2, 50.00, 0.00, 50.00, 0.00, 'SUCCESS');

-- TCC Order has no initial data (created by TCC operations)
USE `seata_order`;
TRUNCATE TABLE `tcc_order`;