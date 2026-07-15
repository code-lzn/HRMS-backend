-- ============================================================
-- HRMS 人力资源管理平台 - 全部建表语句（13张）
-- 数据库：szml
-- 日期：2026-07-14
-- 说明：弃用 post / post_thumb / post_favour（社区帖子遗留表）
--       新增 department / position / employee / attendance_record / approval_flow
-- ============================================================

USE szml;


-- ============================================================
-- 一、基础模块
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户表（user）
-- 核心：登录认证 + 角色权限，全平台所有人员（员工/HR/主管/财务/管理员）均为 user
-- employee_id / create_by / operator_id / approver_id 全部关联本表 id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`            BIGINT        NOT NULL COMMENT '主键，雪花算法',
    `user_account`  VARCHAR(256)  NOT NULL COMMENT '登录账号（工号）',
    `user_password` VARCHAR(512)  NOT NULL COMMENT '密码，MD5 加盐',
    `union_id`      VARCHAR(256)  DEFAULT NULL COMMENT '微信开放平台 UnionID（预留）',
    `mp_open_id`    VARCHAR(256)  DEFAULT NULL COMMENT '公众号 OpenID（预留）',
    `user_name`     VARCHAR(256)  DEFAULT NULL COMMENT '用户昵称 / 员工姓名',
    `user_avatar`   VARCHAR(1024) DEFAULT NULL COMMENT '头像 URL',
    `user_profile`  VARCHAR(512)  DEFAULT NULL COMMENT '简介',
    `user_role`     VARCHAR(256)  NOT NULL DEFAULT 'employee' COMMENT '角色：sys_admin=系统管理员, hr=HR专员, dept_manager=部门主管, finance=财务专员, employee=普通员工',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常, 1=删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_account` (`user_account`),
    INDEX `idx_union_id` (`union_id`),
    INDEX `idx_mp_open_id` (`mp_open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表：登录认证 + 角色，全平台人员基础表';


-- ------------------------------------------------------------
-- 2. 部门表（department）
-- 树形结构，parent_id 自关联
-- manager_id → user.id（部门主管）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
    `id`          BIGINT       NOT NULL COMMENT '主键，雪花算法',
    `parent_id`   BIGINT       DEFAULT NULL COMMENT '上级部门 ID，NULL 表示一级部门',
    `name`        VARCHAR(128) NOT NULL COMMENT '部门名称',
    `manager_id`  BIGINT       DEFAULT NULL COMMENT '部门主管 ID，关联 user.id',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_dept_parent` (`parent_id`),
    INDEX `idx_dept_manager` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表：组织架构树，部门主管关联 user';


-- ------------------------------------------------------------
-- 3. 职位表（position）
-- 独立字典表，被 employee 和 salary_account 引用
-- level 兼任"职级"概念，减少一张表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `position`;
CREATE TABLE `position` (
    `id`          BIGINT       NOT NULL COMMENT '主键，雪花算法',
    `name`        VARCHAR(128) NOT NULL COMMENT '职位名称，如"Java开发工程师""产品经理"',
    `level`       INT          DEFAULT 1 COMMENT '职级：1=初级, 2=中级, 3=高级, 4=资深, 5=专家/经理',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_position_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位表：职位 + 职级合一，字典表';


-- ------------------------------------------------------------
-- 4. 员工档案表（employee）
-- 关系：user_id → user.id（1:1），department_id → department.id，position_id → position.id
-- 入转调离：入职看 hire_date，转正看 probation_end_date，调岗改 department_id/position_id，
--           离职改 employment_status + resign_date，异动轨迹看 approval_flow
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
    `id`                  BIGINT       NOT NULL COMMENT '主键，雪花算法',
    `user_id`             BIGINT       NOT NULL COMMENT '用户 ID，关联 user.id，1:1 唯一',
    `employee_no`         VARCHAR(32)  NOT NULL COMMENT '工号，全局唯一',
    `real_name`           VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    `gender`              TINYINT      DEFAULT 0 COMMENT '性别：0=女, 1=男',
    `phone`               VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `id_card`             VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
    `bank_card`           VARCHAR(32)  DEFAULT NULL COMMENT '银行卡号',
    `bank_name`           VARCHAR(64)  DEFAULT NULL COMMENT '开户银行',
    `emergency_contact`   VARCHAR(64)  DEFAULT NULL COMMENT '紧急联系人姓名',
    `emergency_phone`     VARCHAR(20)  DEFAULT NULL COMMENT '紧急联系人电话',
    `department_id`       BIGINT       DEFAULT NULL COMMENT '所属部门 ID，关联 department.id',
    `position_id`         BIGINT       DEFAULT NULL COMMENT '职位 ID，关联 position.id',
    `employment_status`   TINYINT      DEFAULT 1 COMMENT '在职状态：1=试用期, 2=正式, 3=待离职, 4=已离职',
    `hire_date`           DATE         DEFAULT NULL COMMENT '入职日期',
    `probation_end_date`  DATE         DEFAULT NULL COMMENT '转正日期（试用期结束日）',
    `resign_date`         DATE         DEFAULT NULL COMMENT '离职日期',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_employee_user` (`user_id`),
    UNIQUE INDEX `uk_employee_no` (`employee_no`),
    INDEX `idx_employee_dept` (`department_id`),
    INDEX `idx_employee_position` (`position_id`),
    INDEX `idx_employee_status` (`employment_status`),
    INDEX `idx_employee_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工档案表：1:1扩展 user，HR 管理核心，包含个人信息+组织归属+在职状态';


-- ============================================================
-- 二、考勤模块
-- ============================================================

-- ------------------------------------------------------------
-- 5. 考勤记录表（attendance_record）
-- 设计：考勤打卡/请假/加班/外出 四种记录合一，按 record_type 区分
--       类型不同，使用的字段子集不同（见字段注释）
-- 关系：employee_id → user.id，department_id → department.id（冗余，方便部门查询）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `attendance_record`;
CREATE TABLE `attendance_record` (
    `id`              BIGINT        NOT NULL COMMENT '主键，雪花算法',
    `employee_id`     BIGINT        NOT NULL COMMENT '员工 ID，关联 user.id',
    `department_id`   BIGINT        DEFAULT NULL COMMENT '部门 ID（冗余，便于部门维度查询考勤），关联 department.id',
    `record_date`     DATE          NOT NULL COMMENT '考勤日期 / 请假开始日期',

    -- 类型
    `record_type`     TINYINT       NOT NULL COMMENT '记录类型：1=考勤打卡, 2=请假, 3=加班, 4=外出',

    -- 打卡专用字段（record_type=1）
    `check_in_time`   DATETIME      DEFAULT NULL COMMENT '上班打卡时间',
    `check_out_time`  DATETIME      DEFAULT NULL COMMENT '下班打卡时间',
    `check_status`    TINYINT       DEFAULT NULL COMMENT '打卡状态：1=正常, 2=迟到, 3=早退, 4=缺卡',
    `late_minutes`    INT           DEFAULT 0 COMMENT '迟到分钟数',
    `early_minutes`   INT           DEFAULT 0 COMMENT '早退分钟数',

    -- 请假/加班/外出专用字段（record_type=2,3,4）
    `leave_type`      TINYINT       DEFAULT NULL COMMENT '请假类型：1=事假, 2=病假, 3=年假, 4=婚假, 5=产假, 6=调休',
    `start_time`      DATETIME      DEFAULT NULL COMMENT '开始时间（请假/加班/外出）',
    `end_time`        DATETIME      DEFAULT NULL COMMENT '结束时间（请假/加班/外出）',
    `duration_days`   DECIMAL(4,1)  DEFAULT NULL COMMENT '请假天数，如 0.5=半天, 1=一天',
    `duration_hours`  DECIMAL(4,1)  DEFAULT NULL COMMENT '加班小时数',

    -- 通用字段
    `reason`          VARCHAR(512)  DEFAULT NULL COMMENT '原因说明（请假原因/加班原因/外出事由）',
    `status`          TINYINT       DEFAULT 0 COMMENT '审批状态（打卡默认通过）：0=待审批, 1=已通过, 2=已驳回, 3=已撤回',
    `approver_id`     BIGINT        DEFAULT NULL COMMENT '审批人 ID，关联 user.id',
    `approve_time`    DATETIME      DEFAULT NULL COMMENT '审批时间',
    `approve_comment` VARCHAR(256)  DEFAULT NULL COMMENT '审批意见',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_att_employee` (`employee_id`),
    INDEX `idx_att_dept` (`department_id`),
    INDEX `idx_att_date` (`record_date`),
    INDEX `idx_att_type` (`record_type`),
    INDEX `idx_att_status` (`status`),
    INDEX `idx_att_employee_date` (`employee_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤记录表：打卡+请假+加班+外出四合一，按 record_type 区分类型';


-- ============================================================
-- 三、审批模块
-- ============================================================

-- ------------------------------------------------------------
-- 6. 审批流表（approval_flow）
-- 设计：通用审批引擎，business_type + business_id 关联任意业务
--       approval_chain 定义审批链，approval_log 记录每步审批结果
-- 支持的 business_type：
--   1=请假   → business_id → attendance_record.id
--   2=加班   → business_id → attendance_record.id
--   3=转正   → business_id → employee.user_id
--   4=调岗   → business_id → employee.user_id
--   5=离职   → business_id → employee.user_id
--   6=薪资审批 → business_id → salary_batch.id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `approval_flow`;
CREATE TABLE `approval_flow` (
    `id`              BIGINT       NOT NULL COMMENT '主键，雪花算法',
    `applicant_id`    BIGINT       NOT NULL COMMENT '申请人 ID，关联 user.id',
    `department_id`   BIGINT       DEFAULT NULL COMMENT '申请人所在部门 ID（冗余），关联 department.id',
    `business_type`   TINYINT      NOT NULL COMMENT '业务类型：1=请假, 2=加班, 3=转正, 4=调岗, 5=离职, 6=薪资审批',
    `business_id`     BIGINT       NOT NULL COMMENT '关联业务表的主键 ID',
    `title`           VARCHAR(256) NOT NULL COMMENT '审批标题，如"张三-事假申请-3天"',
    `status`          TINYINT      DEFAULT 0 COMMENT '审批状态：0=审批中, 1=已通过, 2=已驳回, 3=已撤回',
    `current_step`    INT          DEFAULT 1 COMMENT '当前审批步骤（1-based）',

    -- 审批链 JSON
    -- 示例：[{"step":1,"approver_id":101,"role":"部门主管"},{"step":2,"approver_id":102,"role":"HR"}]
    `approval_chain`  TEXT         NOT NULL COMMENT '审批链 JSON，定义每一步的审批人',

    -- 审批记录 JSON
    -- 示例：[{"step":1,"approver_id":101,"action":"approve","comment":"同意","time":"2026-07-14 10:00:00"}]
    -- action: approve=同意, reject=驳回
    `approval_log`    TEXT         DEFAULT NULL COMMENT '审批记录 JSON，每一步的审批结果',

    `submit_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `finish_time`     DATETIME     DEFAULT NULL COMMENT '审批完成时间（通过/驳回）',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_approval_applicant` (`applicant_id`),
    INDEX `idx_approval_dept` (`department_id`),
    INDEX `idx_approval_business` (`business_type`, `business_id`),
    INDEX `idx_approval_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批流表：通用审批引擎，支持请假/加班/转正/调岗/离职/薪资六类审批';


-- ============================================================
-- 四、薪资模块（已有表，保持原结构）
-- ============================================================

-- ------------------------------------------------------------
-- 7. 薪资账套表（salary_account）
-- 关系：1 : N → salary_item，1 : N → employee_salary
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_account`;
CREATE TABLE `salary_account` (
    `id`             BIGINT       NOT NULL COMMENT '主键，雪花算法',
    `name`           VARCHAR(128) NOT NULL COMMENT '账套名称，如"标准职员工资"',
    `scope_type`     INT          DEFAULT 1 NOT NULL COMMENT '适用范围类型：1=部门, 2=职位, 3=职级',
    `scope_ids`      VARCHAR(512) DEFAULT NULL COMMENT '适用范围 ID 列表，逗号分隔；NULL 表示全员适用',
    `effective_date` DATE         DEFAULT NULL COMMENT '生效日期',
    `is_deleted`     TINYINT      DEFAULT 0 NOT NULL COMMENT '逻辑删除：0=正常, 1=删除',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_account_name` (`name`),
    INDEX `idx_account_scope` (`scope_type`, `scope_ids`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资账套表：定义薪酬模板，一个账套包含多个工资项目';


-- ------------------------------------------------------------
-- 8. 工资项目表（salary_item）
-- 关系：account_id → salary_account.id（N:1）
-- item_type → 决定使用的计算器
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_item`;
CREATE TABLE `salary_item` (
    `id`          BIGINT       NOT NULL COMMENT '主键，雪花算法',
    `account_id`  BIGINT       NOT NULL COMMENT '所属账套 ID，关联 salary_account.id',
    `name`        VARCHAR(128) NOT NULL COMMENT '项目名称，如"基本工资""绩效奖金""养老保险""个税"',
    `item_type`   INT          NOT NULL COMMENT '项目类型：1=固定收入, 2=变动收入, 3=考勤扣款, 4=社保扣除, 5=公积金扣除, 6=个税',
    `formula`     VARCHAR(256) DEFAULT NULL COMMENT '计算公式，如"base*0.08"，NULL 表示由 Calculator 内部计算',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序号（工资条展示顺序）',
    `is_taxable`  TINYINT      DEFAULT 1 COMMENT '是否计入应纳税所得额：1=是, 0=否',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_item_account` (`account_id`),
    INDEX `idx_item_type` (`item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工资项目表：定义账套下的工资构成项目及计算规则';


-- ------------------------------------------------------------
-- 9. 员工薪资档案表（employee_salary）
-- 关系：employee_id → user.id，account_id → salary_account.id
-- 每员工一个有效档案（以 effective_date 最新为准）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `employee_salary`;
CREATE TABLE `employee_salary` (
    `id`                   BIGINT         NOT NULL COMMENT '主键，雪花算法',
    `employee_id`          BIGINT         NOT NULL COMMENT '员工 ID，关联 user.id',
    `account_id`           BIGINT         NOT NULL COMMENT '薪资账套 ID，关联 salary_account.id',
    `base_salary`          DECIMAL(12,2)  DEFAULT 0.00 COMMENT '月基本工资',
    `allowance_base`       DECIMAL(12,2)  DEFAULT 0.00 COMMENT '津贴补贴基数',
    `social_security_base` DECIMAL(12,2)  DEFAULT 0.00 COMMENT '社保缴费基数',
    `housing_fund_base`    DECIMAL(12,2)  DEFAULT 0.00 COMMENT '公积金缴费基数',
    `performance_base`     DECIMAL(12,2)  DEFAULT 0.00 COMMENT '绩效工资基数',
    `effective_date`       DATE           NOT NULL COMMENT '档案生效日期',
    `is_deleted`           TINYINT        DEFAULT 0 NOT NULL COMMENT '逻辑删除：0=正常, 1=删除',
    `create_time`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_es_employee` (`employee_id`),
    INDEX `idx_es_account` (`account_id`),
    INDEX `idx_es_effective` (`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工薪资档案表：关联员工与账套，定义各项薪资基数，HR 定薪核心表';


-- ------------------------------------------------------------
-- 10. 薪资核算批次表（salary_batch）
-- 关系：create_by → user.id，1 : N → salary_detail
-- 状态：0=草稿, 1=计算中, 2=待确认, 3=审批中, 4=已通过, 5=已发放, 6=已驳回
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_batch`;
CREATE TABLE `salary_batch` (
    `id`              BIGINT        NOT NULL COMMENT '主键，雪花算法',
    `batch_no`        VARCHAR(32)   NOT NULL COMMENT '批次号，如"SAL202607"',
    `salary_month`    VARCHAR(7)    NOT NULL COMMENT '薪资月份，格式 yyyy-MM',
    `status`          INT           DEFAULT 0 NOT NULL COMMENT '状态：0=草稿, 1=计算中, 2=待确认, 3=审批中, 4=已通过, 5=已发放, 6=已驳回',
    `total_employees` INT           DEFAULT 0 COMMENT '参与核算员工数',
    `total_gross_pay` DECIMAL(14,2) DEFAULT 0.00 COMMENT '应发工资合计',
    `total_net_pay`   DECIMAL(14,2) DEFAULT 0.00 COMMENT '实发工资合计',
    `total_tax`       DECIMAL(14,2) DEFAULT 0.00 COMMENT '个税合计',
    `create_by`       BIGINT        DEFAULT NULL COMMENT '创建人 ID（HR），关联 user.id',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_batch_no` (`batch_no`),
    INDEX `idx_batch_month` (`salary_month`),
    INDEX `idx_batch_status` (`status`),
    INDEX `idx_batch_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资核算批次表：月度核算主表，控制核算流程和状态流转';


-- ------------------------------------------------------------
-- 11. 薪资明细表 / 工资条（salary_detail）
-- 关系：batch_id → salary_batch.id，employee_id → user.id
-- salary_items 存 JSON，避免过多关联
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_detail`;
CREATE TABLE `salary_detail` (
    `id`                BIGINT         NOT NULL COMMENT '主键，雪花算法',
    `batch_id`          BIGINT         NOT NULL COMMENT '批次 ID，关联 salary_batch.id',
    `employee_id`       BIGINT         NOT NULL COMMENT '员工 ID，关联 user.id',
    `salary_items`      TEXT           DEFAULT NULL COMMENT '工资项目明细 JSON：[{"name":"基本工资","amount":8000,"type":1},...]',
    `gross_pay`         DECIMAL(12,2)  DEFAULT 0.00 COMMENT '应发工资 = 各项收入之和',
    `social_security`   DECIMAL(10,2)  DEFAULT 0.00 COMMENT '社保个人部分',
    `housing_fund`      DECIMAL(10,2)  DEFAULT 0.00 COMMENT '公积金个人部分',
    `income_tax`        DECIMAL(10,2)  DEFAULT 0.00 COMMENT '个人所得税',
    `total_deductions`  DECIMAL(10,2)  DEFAULT 0.00 COMMENT '扣款合计 = 社保+公积金+个税+考勤扣款',
    `net_pay`           DECIMAL(12,2)  DEFAULT 0.00 COMMENT '实发工资 = 应发 - 扣款合计 + 手动调整',
    `is_abnormal`       TINYINT        DEFAULT 0 COMMENT '异常标记：0=正常, 1=黄色预警, 2=红色预警, 3=红色阻断',
    `abnormal_reason`   VARCHAR(512)   DEFAULT NULL COMMENT '异常原因',
    `manual_adjustment` DECIMAL(10,2)  DEFAULT 0.00 COMMENT '手动调整金额（正=补发, 负=扣回）',
    `adjustment_reason` VARCHAR(256)   DEFAULT NULL COMMENT '手动调整原因',
    `payslip_viewed`    TINYINT        DEFAULT 0 COMMENT '工资条查看状态：0=未查看, 1=已验证, 2=已查看',
    `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_sd_batch` (`batch_id`),
    INDEX `idx_sd_employee` (`employee_id`),
    INDEX `idx_sd_batch_employee` (`batch_id`, `employee_id`),
    INDEX `idx_sd_abnormal` (`batch_id`, `is_abnormal`),
    INDEX `idx_sd_payslip` (`employee_id`, `payslip_viewed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资明细表（工资条）：每员工每月一条，完整工资条数据';


-- ------------------------------------------------------------
-- 12. 调薪历史表（salary_change_history）
-- 关系：employee_id → user.id，operator_id → user.id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_change_history`;
CREATE TABLE `salary_change_history` (
    `id`             BIGINT       NOT NULL COMMENT '主键，雪花算法',
    `employee_id`    BIGINT       NOT NULL COMMENT '员工 ID，关联 user.id',
    `change_type`    INT          NOT NULL COMMENT '变更类型：1=调薪, 2=账套变更, 3=基数调整, 4=转正调薪, 5=调岗调薪',
    `old_value`      VARCHAR(512) DEFAULT NULL COMMENT '变更前 JSON',
    `new_value`      VARCHAR(512) DEFAULT NULL COMMENT '变更后 JSON',
    `effective_date` DATE         NOT NULL COMMENT '生效日期',
    `operator_id`    BIGINT       NOT NULL COMMENT '操作人 ID（HR），关联 user.id',
    `remark`         VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_sch_employee` (`employee_id`),
    INDEX `idx_sch_operator` (`operator_id`),
    INDEX `idx_sch_effective` (`effective_date`),
    INDEX `idx_sch_type` (`change_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调薪历史表：记录每次薪资档案变更，支持审计追溯';


-- ------------------------------------------------------------
-- 13. 个税累计表（income_tax_cumulative）
-- 关系：employee_id → user.id
-- 累计预扣法：本月个税 = 累计应纳税额 - 累计已缴税额
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `income_tax_cumulative`;
CREATE TABLE `income_tax_cumulative` (
    `id`                           BIGINT        NOT NULL COMMENT '主键，雪花算法',
    `employee_id`                  BIGINT        NOT NULL COMMENT '员工 ID，关联 user.id',
    `tax_year`                     INT           NOT NULL COMMENT '纳税年度，如 2026',
    `tax_month`                    INT           NOT NULL COMMENT '纳税月份，1~12',
    `cumulative_gross_pay`         DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计应发工资',
    `cumulative_threshold`         DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计起征点 = 5000 × 月数',
    `cumulative_social_security`   DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计社保个人部分',
    `cumulative_housing_fund`      DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计公积金个人部分',
    `cumulative_special_deduction` DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计专项附加扣除',
    `cumulative_taxable_income`    DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计应纳税所得额',
    `tax_rate`                     DECIMAL(5,4)  DEFAULT 0.0000 COMMENT '适用税率',
    `quick_deduction`              DECIMAL(10,2) DEFAULT 0.00 COMMENT '速算扣除数',
    `cumulative_tax_payable`       DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计应纳税额',
    `cumulative_tax_paid`          DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计已缴税额',
    `current_month_tax`            DECIMAL(10,2) DEFAULT 0.00 COMMENT '本月应缴个税 = 累计应纳税额 - 累计已缴税额',
    `create_time`                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_tax_employee_month` (`employee_id`, `tax_year`, `tax_month`),
    INDEX `idx_itc_employee_year` (`employee_id`, `tax_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个税累计表：支持累计预扣法，每员工每月一条，用于计算当月个税';


-- ============================================================
-- 附：全部表关系一览（13张表）
-- ============================================================
-- user         ──1:1── employee          员工档案扩展
-- user         ──1:N── employee_salary   薪资档案
-- user         ──1:N── salary_detail     工资条
-- user         ──1:N── salary_batch      创建核算批次
-- user         ──1:N── salary_change_history  操作调薪
-- user         ──1:N── income_tax_cumulative   个税累计
-- user         ──1:N── attendance_record 考勤记录
-- user         ──1:N── approval_flow     申请审批
-- department   ──1:N── employee          部门员工
-- department   ──1:N── attendance_record 部门考勤（冗余）
-- department   ──1:N── approval_flow     部门审批（冗余）
-- department   ──自关联── department     树形上级
-- position     ──1:N── employee          职位员工
-- salary_account ──1:N── salary_item     账套项目
-- salary_account ──1:N── employee_salary 账套下的员工
-- salary_batch ──1:N── salary_detail     批次明细
-- salary_batch ──1:1── approval_flow     薪资审批（business_type=6）
-- attendance_record ──1:1── approval_flow  请假/加班审批（business_type=1,2）
