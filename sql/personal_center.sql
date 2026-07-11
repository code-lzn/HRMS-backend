-- 个人中心模块数据库表
-- 执行前请确认已选择正确的数据库

-- 登录日志表
CREATE TABLE IF NOT EXISTS `login_log` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID（关联user.id）',
    `login_time`          DATETIME          NOT NULL COMMENT '登录时间',
    `ip`                  VARCHAR(45)       NOT NULL COMMENT '登录IP',
    `device`              VARCHAR(256)               COMMENT '设备信息（User-Agent）',
    `login_type`          TINYINT           NOT NULL COMMENT '登录方式：1=密码登录 2=短信验证码登录',
    `is_success`          TINYINT           NOT NULL DEFAULT 1 COMMENT '是否成功：0=失败 1=成功',
    `fail_reason`         VARCHAR(128)               COMMENT '失败原因',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_login_time` (`login_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '登录日志表';

-- 密码历史表
CREATE TABLE IF NOT EXISTS `password_history` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID（关联user.id）',
    `password_hash`       VARCHAR(128)      NOT NULL COMMENT '历史密码哈希',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id_time` (`employee_id`, `create_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '密码历史表，每个员工仅保留最近3次';

-- 工资条验证记录表
CREATE TABLE IF NOT EXISTS `payslip_verification` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID（关联user.id）',
    `batch_id`            BIGINT UNSIGNED   NOT NULL COMMENT '薪资批次ID',
    `verify_type`         TINYINT           NOT NULL COMMENT '验证方式：1=短信验证码 2=密码',
    `is_success`          TINYINT           NOT NULL DEFAULT 1 COMMENT '是否成功：0=失败 1=成功',
    `locked_until`        DATETIME                   COMMENT '锁定截止时间（连续3次失败后锁定30分钟）',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_batch` (`employee_id`, `batch_id`),
    KEY `idx_locked_until` (`locked_until`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工资条查看验证记录表';

-- 考勤记录表（个人中心依赖的基础表，后续由考勤管理模块完善）
CREATE TABLE IF NOT EXISTS `attendance_record` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID（关联user.id）',
    `record_date`         DATE              NOT NULL COMMENT '考勤日期',
    `clock_in_time`       TIME                       COMMENT '上班打卡时间',
    `clock_out_time`      TIME                       COMMENT '下班打卡时间',
    `status`              TINYINT           NOT NULL DEFAULT 1 COMMENT '状态：1=正常 2=迟到 3=早退 4=旷工 5=缺卡 6=请假',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_date` (`employee_id`, `record_date`),
    KEY `idx_record_date` (`record_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤记录表';

-- 请假申请表（个人中心依赖的基础表，后续由审批中心模块完善）
CREATE TABLE IF NOT EXISTS `leave_application` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID（关联user.id）',
    `leave_type`          TINYINT           NOT NULL COMMENT '请假类型：1=年假 2=事假 3=病假 4=调休 5=婚假 6=产假 7=陪产假 8=丧假',
    `start_time`          DATETIME          NOT NULL COMMENT '开始时间',
    `end_time`            DATETIME          NOT NULL COMMENT '结束时间',
    `leave_days`          DECIMAL(5,1)      NOT NULL COMMENT '请假天数',
    `reason`              VARCHAR(512)               COMMENT '请假原因',
    `status`              TINYINT           NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=审批中 2=已通过 3=已拒绝 4=已取消',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`          TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '请假申请表';

-- 请假审批记录表
CREATE TABLE IF NOT EXISTS `leave_approval_log` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `application_id`      BIGINT UNSIGNED   NOT NULL COMMENT '请假申请ID',
    `node_name`           VARCHAR(64)       NOT NULL COMMENT '审批节点名称',
    `approver_id`         BIGINT UNSIGNED            COMMENT '审批人ID',
    `approver_name`       VARCHAR(64)                COMMENT '审批人姓名',
    `status`              TINYINT           NOT NULL DEFAULT 0 COMMENT '状态：0=待审批 1=已通过 2=已拒绝',
    `opinion`             VARCHAR(256)               COMMENT '审批意见',
    `operate_time`        DATETIME                   COMMENT '操作时间',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '请假审批记录表';

-- 薪资批次表（个人中心依赖的基础表，后续由薪资管理模块完善）
CREATE TABLE IF NOT EXISTS `salary_batch` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `batch_name`          VARCHAR(128)      NOT NULL COMMENT '批次名称',
    `salary_month`        VARCHAR(7)        NOT NULL COMMENT '薪资月份（yyyy-MM）',
    `status`              TINYINT           NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=审批中 2=已通过 3=已发放 4=已驳回',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`          TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    KEY `idx_salary_month` (`salary_month`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资批次表';

-- 薪资明细表
CREATE TABLE IF NOT EXISTS `salary_item` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `batch_id`            BIGINT UNSIGNED   NOT NULL COMMENT '薪资批次ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID（关联user.id）',
    `item_name`           VARCHAR(64)       NOT NULL COMMENT '工资项目名称',
    `item_type`           TINYINT           NOT NULL COMMENT '类型：1=收入项 2=扣除项',
    `amount`              DECIMAL(12,2)     NOT NULL COMMENT '金额',
    `sort_order`          INT               NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_batch_employee` (`batch_id`, `employee_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资明细表';
