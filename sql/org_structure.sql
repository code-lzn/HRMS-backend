-- 组织架构模块建表脚本
-- 执行前请确保已选择正确的数据库

use szml;

-- ============================================================
-- 1. 部门表
-- ============================================================
CREATE TABLE IF NOT EXISTS `department` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`              VARCHAR(64)       NOT NULL COMMENT '部门名称',
    `code`              VARCHAR(4)        NOT NULL COMMENT '部门编码，2位，用于工号生成',
    `parent_id`         BIGINT UNSIGNED            COMMENT '上级部门ID，空表示根部门',
    `manager_id`        BIGINT UNSIGNED            COMMENT '部门负责人ID，关联employee.id',
    `sort_order`        INT               NOT NULL DEFAULT 0 COMMENT '排序序号，越小越靠前',
    `description`       VARCHAR(256)               COMMENT '部门描述',
    `is_deleted`        TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    `create_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_manager_id` (`manager_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '部门表';

-- ============================================================
-- 2. 职位表
-- ============================================================
CREATE TABLE IF NOT EXISTS `position` (
    `id`                        BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                      VARCHAR(64)       NOT NULL COMMENT '职位名称，如Java开发工程师',
    `sequence`                  TINYINT           NOT NULL COMMENT '职位序列：1=M管理 2=P专业 3=S支持',
    `department_id`             BIGINT UNSIGNED            COMMENT '所属部门ID，空表示全公司通用',
    `level_min`                 VARCHAR(8)        NOT NULL COMMENT '职级下限，如P1',
    `level_max`                 VARCHAR(8)        NOT NULL COMMENT '职级上限，如P10',
    `default_probation_months`  INT               NOT NULL DEFAULT 3 COMMENT '默认试用期月数',
    `description`               VARCHAR(256)               COMMENT '职位描述/岗位职责',
    `is_deleted`                TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    `create_time`               DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`               DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sequence` (`sequence`),
    KEY `idx_department_id` (`department_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '职位表';

-- ============================================================
-- 3. 部门合并日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `department_merge_log` (
    `id`                   BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_dept_id`       BIGINT UNSIGNED   NOT NULL COMMENT '源部门ID（被合并）',
    `source_dept_name`     VARCHAR(64)       NOT NULL COMMENT '源部门名称（快照）',
    `target_dept_id`       BIGINT UNSIGNED   NOT NULL COMMENT '目标部门ID（保留）',
    `target_dept_name`     VARCHAR(64)       NOT NULL COMMENT '目标部门名称（快照）',
    `transferred_employees` INT              NOT NULL DEFAULT 0 COMMENT '转移员工数',
    `operator_id`          BIGINT UNSIGNED   NOT NULL COMMENT '操作人ID',
    `create_time`          DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_source_dept_id` (`source_dept_id`),
    KEY `idx_target_dept_id` (`target_dept_id`),
    KEY `idx_create_time` (`create_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '部门合并日志表';

-- ============================================================
-- 4. 员工表（组织架构模块依赖的最小结构，后续员工模块可扩展）
-- ============================================================
CREATE TABLE IF NOT EXISTS `employee` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`              VARCHAR(64)       NOT NULL COMMENT '员工姓名',
    `department_id`     BIGINT UNSIGNED            COMMENT '所属部门ID，关联department.id',
    `position_id`       BIGINT UNSIGNED            COMMENT '职位ID，关联position.id',
    `status`            TINYINT           NOT NULL DEFAULT 1 COMMENT '员工状态：1=试用期 2=正式 3=离职',
    `is_deleted`        TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    `create_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_position_id` (`position_id`),
    KEY `idx_status` (`status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工表';

-- ============================================================
-- 初始化数据：默认根部门
-- ============================================================
INSERT IGNORE INTO `department` (`id`, `name`, `code`, `parent_id`, `sort_order`, `description`) VALUES
(1, '总公司', '00', NULL, 0, '公司根部门');

-- ============================================================
-- 初始化数据：默认职位
-- ============================================================
INSERT IGNORE INTO `position` (`id`, `name`, `sequence`, `department_id`, `level_min`, `level_max`, `default_probation_months`, `description`) VALUES
(1, '总经理',         1, NULL, 'M3', 'M5', 3, '公司总经理，管理序列'),
(2, '部门经理',       1, NULL, 'M1', 'M3', 3, '部门经理，管理序列'),
(3, '高级开发工程师', 2, NULL, 'P6', 'P8', 3, '高级开发，专业序列'),
(4, '开发工程师',     2, NULL, 'P3', 'P6', 3, '初中级开发，专业序列'),
(5, '行政专员',       3, NULL, 'S1', 'S3', 3, '行政支持，支持序列');

