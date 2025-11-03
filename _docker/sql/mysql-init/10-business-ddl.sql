-- Business schemas and tables for AT demos

-- Order DB
USE `seata_order`;

DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(128) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `count` INT NOT NULL,
  `amount` DECIMAL(18,2) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `payment_status` VARCHAR(32) DEFAULT NULL,
  `payment_url` VARCHAR(512) DEFAULT NULL,
  `payment_requested_at` DATETIME DEFAULT NULL,
  `payment_expires_at` DATETIME DEFAULT NULL,
  `payment_completed_at` DATETIME DEFAULT NULL,
  `payment_channel_token` VARCHAR(128) DEFAULT NULL,
  `payment_channel_expires_at` DATETIME DEFAULT NULL,
  `payment_last_event_id` VARCHAR(128) DEFAULT NULL,
  `fail_code` VARCHAR(64) DEFAULT NULL,
  `fail_message` VARCHAR(255) DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `failed_at` DATETIME DEFAULT NULL,
  `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_payment_channel_token` (`payment_channel_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AT mode requires undo_log in each business DB
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
  `branch_id` BIGINT NOT NULL,
  `xid` VARCHAR(100) NOT NULL,
  `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL,
  `log_status` INT NOT NULL,
  `log_created` DATETIME NOT NULL,
  `log_modified` DATETIME NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Storage DB
USE `seata_storage`;

DROP TABLE IF EXISTS `t_storage`;
CREATE TABLE `t_storage` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `total` INT NOT NULL,
  `used` INT NOT NULL DEFAULT 0,
  `residue` INT NOT NULL,
  `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `tx_step_log`;
CREATE TABLE `tx_step_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(128) NOT NULL,
  `step` VARCHAR(64) NOT NULL,
  `status` VARCHAR(16) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_order_step` (`order_no`, `step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
  `branch_id` BIGINT NOT NULL,
  `xid` VARCHAR(100) NOT NULL,
  `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL,
  `log_status` INT NOT NULL,
  `log_created` DATETIME NOT NULL,
  `log_modified` DATETIME NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Account DB
USE `seata_account`;

DROP TABLE IF EXISTS `t_account`;
CREATE TABLE `t_account` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `total` DECIMAL(18,2) NOT NULL,
  `used` DECIMAL(18,2) NOT NULL DEFAULT 0,
  `residue` DECIMAL(18,2) NOT NULL,
  `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
  `branch_id` BIGINT NOT NULL,
  `xid` VARCHAR(100) NOT NULL,
  `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL,
  `log_status` INT NOT NULL,
  `log_created` DATETIME NOT NULL,
  `log_modified` DATETIME NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
