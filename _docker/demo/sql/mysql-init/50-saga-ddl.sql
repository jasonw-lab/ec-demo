-- ====================================================================================
-- Seata Saga 用 テーブル: MySQL 8.0 版
-- 文字コード: utf8mb4 / InnoDB
-- ====================================================================================

-- Seata server meta tables (for AT/TCC)
USE `seata`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1) SEATA_STATE_INST
--    各ステップ（状態）の実行履歴
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `SEATA_STATE_INST`;
CREATE TABLE `SEATA_STATE_INST` (
                                    `ID`                         varchar(128)  NOT NULL COMMENT 'id',
                                    `MACHINE_INST_ID`            varchar(128)  NOT NULL COMMENT 'state machine instance id',
                                    `NAME`                       varchar(128)  NOT NULL COMMENT 'state name',
                                    `TYPE`                       varchar(32)            COMMENT 'state type',
                                    `SERVICE_NAME`               varchar(128)           COMMENT 'service name',
                                    `SERVICE_METHOD`             varchar(128)           COMMENT 'method name',
                                    `SERVICE_TYPE`               varchar(32)            COMMENT 'service type',
                                    `BUSINESS_KEY`               varchar(128)           COMMENT 'business key',
                                    `STATE_ID_COMPENSATED_FOR`   varchar(128)           COMMENT 'state compensated for',
                                    `STATE_ID_RETRIED_FOR`       varchar(128)           COMMENT 'state retried for',
                                    `GMT_STARTED`                timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'start time',
                                    `IS_FOR_UPDATE`              tinyint(1)             COMMENT 'is service for update',
                                    `INPUT_PARAMS`               longtext                COMMENT 'input parameters',
                                    `OUTPUT_PARAMS`              longtext                COMMENT 'output parameters',
                                    `STATUS`                     varchar(16)   NOT NULL COMMENT 'status(SU|FA|UN|SK|RU)',
                                    `EXCEP`                      blob                   COMMENT 'exception',
                                    `GMT_UPDATED`                timestamp              NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
                                    `GMT_END`                    timestamp              NULL DEFAULT NULL COMMENT 'end time',
                                    PRIMARY KEY (`ID`),
                                    KEY `idx_state_inst_machine_inst_id` (`MACHINE_INST_ID`),
                                    KEY `idx_state_inst_gmt_started` (`GMT_STARTED`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- 2) SEATA_STATE_MACHINE_DEF
--    ステートマシン定義（JSON 本体やバージョン）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `SEATA_STATE_MACHINE_DEF`;
CREATE TABLE `SEATA_STATE_MACHINE_DEF` (
                                           `ID`                varchar(128)  NOT NULL COMMENT 'id',
                                           `NAME`              varchar(128)  NOT NULL COMMENT 'name',
                                           `TENANT_ID`         varchar(64)   NOT NULL COMMENT 'tenant id',
                                           `APP_NAME`          varchar(128)  NOT NULL COMMENT 'application name',
                                           `TYPE`              varchar(32)            COMMENT 'state language type',
                                           `COMMENT_`          varchar(255)           COMMENT 'comment',
                                           `VER`               varchar(32)   NOT NULL COMMENT 'version',
                                           `GMT_CREATE`        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
                                           `STATUS`            varchar(8)    NOT NULL COMMENT 'status(AC:active|IN:inactive)',
                                           `CONTENT`           longtext               COMMENT 'content',
                                           `RECOVER_STRATEGY`  varchar(32)            COMMENT 'transaction recover strategy(compensate|retry)',
                                           PRIMARY KEY (`ID`),
                                           KEY `idx_sm_def_name_ver` (`NAME`, `VER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------------------------
-- 3) SEATA_STATE_MACHINE_INST
--    ステートマシン実行インスタンス
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `SEATA_STATE_MACHINE_INST`;
CREATE TABLE `SEATA_STATE_MACHINE_INST` (
                                            `ID`                     varchar(128)  NOT NULL COMMENT 'id',
                                            `MACHINE_ID`             varchar(128)  NOT NULL COMMENT 'state machine definition id',
                                            `TENANT_ID`              varchar(64)   NOT NULL COMMENT 'tenant id',
                                            `PARENT_ID`              varchar(128)           COMMENT 'parent id',
                                            `GMT_STARTED`            timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'start time',
                                            `BUSINESS_KEY`           varchar(128)           COMMENT 'business key',
                                            `START_PARAMS`           longtext               COMMENT 'start parameters',
                                            `GMT_END`                timestamp              NULL DEFAULT NULL COMMENT 'end time',
                                            `EXCEP`                  blob                   COMMENT 'exception',
                                            `END_PARAMS`             longtext               COMMENT 'end parameters',
                                            `STATUS`                 varchar(16)            COMMENT 'status(SU|FA|UN|SK|RU)',
                                            `COMPENSATION_STATUS`    varchar(16)            COMMENT 'compensation status(SU|FA|UN|SK|RU)',
                                            `IS_RUNNING`             tinyint(1)             COMMENT 'is running(0 no|1 yes)',
                                            `GMT_UPDATED`            timestamp              NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
                                            PRIMARY KEY (`ID`),
                                            KEY `idx_sm_inst_machine_id` (`MACHINE_ID`),
                                            KEY `idx_sm_inst_tenant_id` (`TENANT_ID`),
                                            KEY `idx_sm_inst_gmt_started` (`GMT_STARTED`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
