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
-- 4. 员工主表（列表查询核心字段）
-- ============================================================
CREATE TABLE IF NOT EXISTS `employee` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employeeName`      VARCHAR(128)      NOT NULL COMMENT '员工姓名',
    `employeeNo`        VARCHAR(16)       NOT NULL COMMENT '工号，格式: 年份(4)+部门编码(2)+序号(3)',
    `account`           VARCHAR(32)       NULL COMMENT '系统账号（=手机号）',
    `userId`            BIGINT            NULL COMMENT '关联用户ID',
    `status`            TINYINT           NOT NULL DEFAULT 1 COMMENT '在职状态：1=试用期 2=正式 3=待离职 4=已离职',
    `gender`            TINYINT           NOT NULL DEFAULT 0 COMMENT '性别: 0=女 1=男',
    `phone`             VARCHAR(20)       NOT NULL COMMENT '手机号',
    `email`             VARCHAR(256)      NULL COMMENT '邮箱',
    `departmentId`      BIGINT            NULL COMMENT '部门ID',
    `positionId`        BIGINT            NULL COMMENT '职位ID',
    `jobLevel`          VARCHAR(8)        NULL COMMENT '职级，如P5、M2',
    `hireDate`          DATETIME          NULL COMMENT '入职日期',
    `hireType`          TINYINT           NULL COMMENT '入职类型',
    `salaryId`          BIGINT            NULL COMMENT '薪资ID',
    `employmentType`    VARCHAR(16)       NOT NULL COMMENT '录用类型: FULL_TIME/PART_TIME/INTERN',
    `createTime`        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDeleted`         TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_no` (`employeeNo`),
    KEY `idx_department_id` (`departmentId`),
    KEY `idx_position_id` (`positionId`),
    KEY `idx_status` (`status`),
    KEY `idx_phone` (`phone`),
    KEY `idx_job_level` (`jobLevel`),
    KEY `idx_hire_date` (`hireDate`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工主表';

-- ============================================================
-- 4.1 员工详情表（补充信息 + 敏感字段，与 employee 一对一）
-- ============================================================
CREATE TABLE IF NOT EXISTS `employee_detail` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employeeId`            BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `account`               VARCHAR(32)       NULL COMMENT '系统账号（=手机号）',
    `idCard`                VARCHAR(256)      NULL COMMENT '身份证号（加密存储）',
    `birthday`              DATE              NULL COMMENT '生日',
    `registeredAddress`     VARCHAR(512)      NULL COMMENT '户籍地址',
    `currentAddress`        VARCHAR(512)      NULL COMMENT '现居住地址',
    `jobLevel`              VARCHAR(8)        NULL COMMENT '职级',
    `directReportId`        BIGINT UNSIGNED   NULL COMMENT '直接汇报人ID',
    `workLocation`          VARCHAR(128)      NULL COMMENT '工作地点',
    `contractType`          TINYINT           NULL COMMENT '合同类型：1=固定期限 2=无固定期限 3=劳务合同',
    `contractExpireDate`    DATE              NULL COMMENT '合同到期日',
    `probationRatio`        DECIMAL(5,4)      NULL COMMENT '试用期待遇比例',
    `baseSalary`            DECIMAL(12,2)     NULL COMMENT '基本工资',
    `bankAccount`           VARCHAR(64)       NULL COMMENT '银行账号（加密存储）',
    `bankName`              VARCHAR(128)      NULL COMMENT '开户行',
    `emergencyContactName`  VARCHAR(128)      NULL COMMENT '紧急联系人姓名',
    `emergencyContactPhone` VARCHAR(32)       NULL COMMENT '紧急联系人电话',
    `createTime`            DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`            DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_id` (`employeeId`),
    KEY `idx_direct_report_id` (`directReportId`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工详情表';

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

