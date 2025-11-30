-- Migration: Add personal information columns to t_user table
-- This script adds columns for personal information (last_name, first_name, etc.)
-- Run this if the database already exists and you need to add these columns

USE `seata_account`;

-- Add personal information columns to t_user table
ALTER TABLE `t_user`
  ADD COLUMN `last_name` VARCHAR(255) DEFAULT NULL COMMENT '姓 (全角)' AFTER `provider_id`,
  ADD COLUMN `first_name` VARCHAR(255) DEFAULT NULL COMMENT '名 (全角)' AFTER `last_name`,
  ADD COLUMN `last_name_kana` VARCHAR(255) DEFAULT NULL COMMENT '姓カナ (全角)' AFTER `first_name`,
  ADD COLUMN `first_name_kana` VARCHAR(255) DEFAULT NULL COMMENT '名カナ (全角)' AFTER `last_name_kana`,
  ADD COLUMN `birth_date` VARCHAR(10) DEFAULT NULL COMMENT '生年月日 (yyyy/mm/dd)' AFTER `first_name_kana`,
  ADD COLUMN `gender` VARCHAR(20) DEFAULT NULL COMMENT '性別' AFTER `birth_date`;

