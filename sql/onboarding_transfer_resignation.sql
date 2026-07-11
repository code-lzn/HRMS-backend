-- ============================================================
-- HRMS 入转调离模块 - 数据库建表脚本
-- ============================================================

-- 入职申请表
CREATE TABLE IF NOT EXISTS `onboarding_application` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                  VARCHAR(32)       NOT NULL COMMENT '姓名',
    `gender`                TINYINT           NOT NULL COMMENT '性别：1=男 2=女',
    `phone`                 VARCHAR(20)       NOT NULL COMMENT '手机号',
    `email`                 VARCHAR(64)       NOT NULL COMMENT '邮箱',
    `id_card`               VARCHAR(64)       NOT NULL COMMENT '身份证号（加密存储）',
    `expected_hire_date`    DATE              NOT NULL COMMENT '预计入职日期',
    `department_id`         BIGINT UNSIGNED   NOT NULL COMMENT '所属部门ID',
    `position_id`           BIGINT UNSIGNED   NOT NULL COMMENT '职位ID',
    `hire_type`             TINYINT           NOT NULL COMMENT '录用类型：1=全职 2=兼职 3=实习',
    `probation_months`      INT               NOT NULL DEFAULT 3 COMMENT '试用期月数',
    `probation_ratio`       DECIMAL(5,4)      NOT NULL DEFAULT 0.8000 COMMENT '试用期待遇比例',
    `direct_report_id`      BIGINT UNSIGNED            COMMENT '直接汇报人ID',
    `status`                TINYINT           NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=审批中 2=已批准 3=已拒绝 4=已入职 5=已放弃',
    `reject_reason`         VARCHAR(512)               COMMENT '拒绝原因',
    `employee_id`           BIGINT UNSIGNED            COMMENT '入职成功后关联的员工ID',
    `actual_hire_date`      DATE                       COMMENT '实际入职日期（HR确认时填写）',
    `create_by`             BIGINT UNSIGNED   NOT NULL COMMENT '创建人ID（HR）',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_expected_hire_date` (`expected_hire_date`),
    KEY `idx_create_by` (`create_by`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '入职申请表';

-- 转正申请表
CREATE TABLE IF NOT EXISTS `regularization_application` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `probation_start_date`    DATE              NOT NULL COMMENT '试用期开始日期',
    `probation_end_date`      DATE              NOT NULL COMMENT '试用期结束日期',
    `performance_review`      VARCHAR(1024)     NOT NULL COMMENT '试用期表现评价',
    `salary_adjustment`       DECIMAL(12,2)              COMMENT '转正后薪资调整金额',
    `result`                  TINYINT                    COMMENT '审批结果：1=通过 2=延长试用 3=不通过',
    `extended_probation_date` DATE                       COMMENT '延长试用至日期',
    `status`                  TINYINT           NOT NULL DEFAULT 1 COMMENT '审批状态：1=审批中 2=已通过 3=已拒绝',
    `create_by`               BIGINT UNSIGNED   NOT NULL COMMENT '发起人ID（HR）',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '转正申请表';

-- 转正提醒记录表
CREATE TABLE IF NOT EXISTS `regularization_reminder` (
    `id`              BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`     BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `remind_date`     DATE              NOT NULL COMMENT '提醒日期',
    `is_processed`    TINYINT           NOT NULL DEFAULT 0 COMMENT '是否已处理：0=未处理 1=HR已发起 2=已忽略',
    `create_time`     DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_remind_date` (`employee_id`, `remind_date`),
    KEY `idx_remind_date` (`remind_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '转正提醒记录表';

-- 调岗申请表
CREATE TABLE IF NOT EXISTS `transfer_application` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`           BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `new_department_id`     BIGINT UNSIGNED   NOT NULL COMMENT '新部门ID',
    `new_position_id`       BIGINT UNSIGNED            COMMENT '新职位ID（可选）',
    `new_job_level`         VARCHAR(8)                 COMMENT '新职级（可选）',
    `new_direct_report_id`  BIGINT UNSIGNED            COMMENT '新直接汇报人ID（可选）',
    `salary_adjustment`     DECIMAL(12,2)              COMMENT '薪资调整金额（可选，不为空需额外审批）',
    `reason`                VARCHAR(512)      NOT NULL COMMENT '调岗原因',
    `status`                TINYINT           NOT NULL DEFAULT 1 COMMENT '审批状态：1=原部门审批中 2=新部门审批中 3=HR备案中 4=已通过 5=已拒绝',
    `create_by`             BIGINT UNSIGNED   NOT NULL COMMENT '发起人ID（HR）',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '调岗申请表';

-- 调岗历史表
CREATE TABLE IF NOT EXISTS `transfer_history` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `application_id`      BIGINT UNSIGNED   NOT NULL COMMENT '调岗申请ID',
    `old_department_id`   BIGINT UNSIGNED   NOT NULL COMMENT '原部门ID',
    `new_department_id`   BIGINT UNSIGNED   NOT NULL COMMENT '新部门ID',
    `old_position_id`     BIGINT UNSIGNED            COMMENT '原职位ID',
    `new_position_id`     BIGINT UNSIGNED            COMMENT '新职位ID',
    `old_job_level`       VARCHAR(8)                 COMMENT '原职级',
    `new_job_level`       VARCHAR(8)                 COMMENT '新职级',
    `old_direct_report_id` BIGINT UNSIGNED           COMMENT '原汇报人ID',
    `new_direct_report_id` BIGINT UNSIGNED           COMMENT '新汇报人ID',
    `transfer_date`       DATE              NOT NULL COMMENT '调岗生效日期',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_transfer_date` (`transfer_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '调岗历史表';

-- 离职申请表
CREATE TABLE IF NOT EXISTS `resignation_application` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `leave_date`              DATE              NOT NULL COMMENT '离职日期',
    `leave_reason`            VARCHAR(32)       NOT NULL COMMENT '离职原因',
    `leave_reason_detail`     VARCHAR(512)               COMMENT '详细说明',
    `leave_type`              TINYINT           NOT NULL COMMENT '离职类型：1=辞职 2=辞退 3=合同到期不续签 4=其他',
    `handover_person_id`      BIGINT UNSIGNED   NOT NULL COMMENT '工作交接人ID',
    `status`                  TINYINT           NOT NULL DEFAULT 1 COMMENT '审批状态：1=审批中 2=已通过待离职 3=已拒绝 4=已离职',
    `create_by`               BIGINT UNSIGNED   NOT NULL COMMENT '发起人ID（HR）',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`),
    KEY `idx_leave_date` (`leave_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '离职申请表';
