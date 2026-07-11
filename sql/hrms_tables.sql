-- ============================================================
-- HRMS 数据库表设计
-- 基于 HRMS.md 产品规格文档 (v1.0, 2026-07-07)
-- 生成日期: 2026-07-11
-- ============================================================

-- ============================================================
-- 1. 系统基础模块 (System)
-- ============================================================

-- 1.1 系统用户表（登录账号）
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    username        VARCHAR(20)     NOT NULL                 COMMENT '登录账号（手机号）',
    password        VARCHAR(128)    NOT NULL                 COMMENT '加密密码',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '账号状态: 1=启用, 0=禁用',
    pwd_reset_flag  TINYINT         NOT NULL DEFAULT 1       COMMENT '首次登录强制改密: 1=需要, 0=已修改',
    pwd_updated_at  DATETIME        DEFAULT NULL             COMMENT '密码最近修改时间（90天强制更换）',
    last_login_at   DATETIME        DEFAULT NULL             COMMENT '最近登录时间',
    last_login_ip   VARCHAR(64)     DEFAULT NULL             COMMENT '最近登录IP',
    session_timeout INT             NOT NULL DEFAULT 30      COMMENT '无操作超时(分钟), 默认30',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 1.2 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    role_code       VARCHAR(32)     NOT NULL                 COMMENT '角色编码',
    role_name       VARCHAR(64)     NOT NULL                 COMMENT '角色名称',
    description     VARCHAR(256)    DEFAULT NULL             COMMENT '角色描述',
    sort_order      INT             NOT NULL DEFAULT 0       COMMENT '排序序号',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 1.3 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT          NOT NULL                 COMMENT '用户ID',
    role_id         BIGINT          NOT NULL                 COMMENT '角色ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 1.4 权限表（菜单/按钮/字段级）
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    perm_code       VARCHAR(64)     NOT NULL                 COMMENT '权限编码',
    perm_name       VARCHAR(64)     NOT NULL                 COMMENT '权限名称',
    perm_type       VARCHAR(16)     NOT NULL DEFAULT 'MENU'  COMMENT '权限类型: MENU=菜单, BUTTON=按钮, FIELD=字段',
    parent_id       BIGINT          DEFAULT NULL             COMMENT '父权限ID（菜单树）',
    resource_path   VARCHAR(256)    DEFAULT NULL             COMMENT '资源路径/接口URL',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 1.5 角色权限关联表
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    role_id         BIGINT          NOT NULL                 COMMENT '角色ID',
    permission_id   BIGINT          NOT NULL                 COMMENT '权限ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 1.6 系统配置表
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    config_key      VARCHAR(64)     NOT NULL                 COMMENT '配置键',
    config_value    TEXT            NOT NULL                 COMMENT '配置值',
    description     VARCHAR(256)    DEFAULT NULL             COMMENT '配置说明',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 1.7 操作日志表（审计）
DROP TABLE IF EXISTS sys_oper_log;
CREATE TABLE sys_oper_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT          DEFAULT NULL             COMMENT '操作用户ID',
    username        VARCHAR(32)     DEFAULT NULL             COMMENT '操作用户名',
    module          VARCHAR(64)     DEFAULT NULL             COMMENT '操作模块',
    operation       VARCHAR(64)     DEFAULT NULL             COMMENT '操作类型',
    request_uri     VARCHAR(256)    DEFAULT NULL             COMMENT '请求URI',
    request_method  VARCHAR(16)     DEFAULT NULL             COMMENT '请求方法 GET/POST/PUT/DELETE',
    request_params  TEXT            DEFAULT NULL             COMMENT '请求参数（敏感字段脱敏）',
    ip              VARCHAR(64)     DEFAULT NULL             COMMENT '请求IP',
    duration        INT             DEFAULT NULL             COMMENT '执行耗时(毫秒)',
    result          TINYINT         NOT NULL DEFAULT 1       COMMENT '执行结果: 1=成功, 0=失败',
    error_msg       TEXT            DEFAULT NULL             COMMENT '错误信息',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at),
    KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 1.8 登录日志表
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT          DEFAULT NULL             COMMENT '用户ID',
    username        VARCHAR(32)     DEFAULT NULL             COMMENT '登录账号',
    ip              VARCHAR(64)     DEFAULT NULL             COMMENT '登录IP',
    user_agent      VARCHAR(256)    DEFAULT NULL             COMMENT '浏览器UA',
    result          TINYINT         NOT NULL DEFAULT 1       COMMENT '登录结果: 1=成功, 0=失败',
    fail_reason     VARCHAR(128)    DEFAULT NULL             COMMENT '失败原因',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';


-- ============================================================
-- 2. 组织架构模块 (Organization)
-- ============================================================

-- 2.1 部门表（支持5级树形结构）
DROP TABLE IF EXISTS org_department;
CREATE TABLE org_department (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    dept_name       VARCHAR(64)     NOT NULL                 COMMENT '部门名称',
    dept_code       VARCHAR(16)     NOT NULL                 COMMENT '部门编码（2位，用于工号生成）',
    parent_id       BIGINT          DEFAULT NULL             COMMENT '上级部门ID，NULL表示根部门',
    manager_id      BIGINT          DEFAULT NULL             COMMENT '部门负责人ID（关联员工表）',
    sort_order      INT             NOT NULL DEFAULT 0       COMMENT '排序序号（越小越靠前）',
    description     VARCHAR(256)    DEFAULT NULL             COMMENT '部门描述',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 1=启用, 0=禁用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (dept_code),
    KEY idx_parent_id (parent_id),
    KEY idx_manager_id (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 2.2 职级表（M/P/S序列）
DROP TABLE IF EXISTS org_rank;
CREATE TABLE org_rank (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    rank_seq        VARCHAR(16)     NOT NULL                 COMMENT '所属序列: M=管理, P=专业, S=支持',
    rank_code       VARCHAR(8)      NOT NULL                 COMMENT '职级编码, 如P1/P2/M1/M2',
    rank_name       VARCHAR(32)     NOT NULL                 COMMENT '职级名称, 如高级工程师',
    rank_level      INT             NOT NULL                 COMMENT '职级数值级别（用于大小比较）',
    sort_order      INT             NOT NULL DEFAULT 0       COMMENT '排序序号',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rank_code (rank_code),
    KEY idx_rank_seq (rank_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职级表';

-- 2.3 职位表
DROP TABLE IF EXISTS org_position;
CREATE TABLE org_position (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    position_name           VARCHAR(64)     NOT NULL                 COMMENT '职位名称',
    rank_seq                VARCHAR(16)     NOT NULL                 COMMENT '所属序列: M/P/S',
    dept_id                 BIGINT          DEFAULT NULL             COMMENT '所属部门ID，NULL表示全公司通用',
    rank_range_start        VARCHAR(8)      NOT NULL                 COMMENT '职级范围起, 如P1',
    rank_range_end          VARCHAR(8)      NOT NULL                 COMMENT '职级范围止, 如P10',
    default_probation_months INT            NOT NULL DEFAULT 3       COMMENT '默认试用期(月)',
    description             VARCHAR(256)    DEFAULT NULL             COMMENT '职位描述',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dept_id (dept_id),
    KEY idx_rank_seq (rank_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位表';


-- ============================================================
-- 3. 员工管理模块 (Employee)
-- ============================================================

-- 3.1 员工档案表
DROP TABLE IF EXISTS emp_employee;
CREATE TABLE emp_employee (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id             BIGINT          DEFAULT NULL             COMMENT '关联系统账号ID',
    employee_no         VARCHAR(16)     NOT NULL                 COMMENT '工号, 格式: 年份(4)+部门编码(2)+序号(3)',
    name                VARCHAR(64)     NOT NULL                 COMMENT '姓名',
    gender              TINYINT         NOT NULL DEFAULT 0       COMMENT '性别: 0=女, 1=男',
    mobile              VARCHAR(20)     NOT NULL                 COMMENT '手机号',
    email               VARCHAR(128)    DEFAULT NULL             COMMENT '邮箱',
    id_card             VARCHAR(256)    DEFAULT NULL             COMMENT '身份证号（加密存储）',
    birthday            DATE            DEFAULT NULL             COMMENT '生日',
    household_address   VARCHAR(256)    DEFAULT NULL             COMMENT '户籍地址',
    current_address     VARCHAR(256)    DEFAULT NULL             COMMENT '现居住地址',
    emergency_contact   VARCHAR(64)     DEFAULT NULL             COMMENT '紧急联系人',
    emergency_phone     VARCHAR(20)     DEFAULT NULL             COMMENT '紧急联系人电话',
    dept_id             BIGINT          NOT NULL                 COMMENT '所属部门ID',
    position_id         BIGINT          NOT NULL                 COMMENT '职位ID',
    rank_code           VARCHAR(8)      DEFAULT NULL             COMMENT '职级编码',
    reporter_id         BIGINT          DEFAULT NULL             COMMENT '直接汇报人ID（员工ID）',
    work_location       VARCHAR(128)    DEFAULT NULL             COMMENT '工作地点',
    employment_type     VARCHAR(16)     NOT NULL                 COMMENT '录用类型: FULL_TIME=全职, PART_TIME=兼职, INTERN=实习',
    employment_status   VARCHAR(16)     NOT NULL DEFAULT 'PROBATION' COMMENT '在职状态: PROBATION=试用期, REGULAR=正式, PENDING_RESIGN=待离职, RESIGNED=已离职',
    entry_date          DATE            NOT NULL                 COMMENT '入职日期',
    resign_date         DATE            DEFAULT NULL             COMMENT '离职日期',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_no (employee_no),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_dept_id (dept_id),
    KEY idx_position_id (position_id),
    KEY idx_employment_status (employment_status),
    KEY idx_name (name),
    KEY idx_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工档案表';

-- 3.2 合同信息表
DROP TABLE IF EXISTS emp_contract;
CREATE TABLE emp_contract (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id     BIGINT          NOT NULL                 COMMENT '员工ID',
    contract_type   VARCHAR(32)     NOT NULL                 COMMENT '合同类型: FIXED_TERM=固定期限, OPEN_ENDED=无固定期限, LABOR_DISPATCH=劳务合同',
    contract_start  DATE            NOT NULL                 COMMENT '合同开始日期',
    contract_end    DATE            DEFAULT NULL             COMMENT '合同到期日（无固定期限为空）',
    contract_file   VARCHAR(256)    DEFAULT NULL             COMMENT '合同文件URL',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_contract_end (contract_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同信息表';

-- 3.3 员工薪资档案表
DROP TABLE IF EXISTS emp_salary_profile;
CREATE TABLE emp_salary_profile (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id             BIGINT          NOT NULL                 COMMENT '员工ID',
    account_set_id          BIGINT          DEFAULT NULL             COMMENT '适用薪资账套ID',
    base_salary             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '基本工资',
    allowance_base          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '岗位津贴基数',
    performance_base        DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '绩效奖金基数',
    social_insurance_base   DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '社保缴纳基数',
    housing_fund_base       DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '公积金缴纳基数',
    probation_salary_ratio  DECIMAL(4,2)    NOT NULL DEFAULT 1.00    COMMENT '试用期薪资比例 (0.80~1.00)',
    bank_account            VARCHAR(256)    DEFAULT NULL             COMMENT '银行账号（加密存储）',
    bank_name               VARCHAR(128)    DEFAULT NULL             COMMENT '开户行名称',
    effective_date          DATE            NOT NULL                 COMMENT '生效日期',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_id (employee_id),
    KEY idx_account_set_id (account_set_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工薪资档案表';

-- 3.4 调薪记录表
DROP TABLE IF EXISTS emp_salary_adjustment;
CREATE TABLE emp_salary_adjustment (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id     BIGINT          NOT NULL                 COMMENT '员工ID',
    adjust_type     VARCHAR(32)     NOT NULL                 COMMENT '调整类型: TRANSFER=调岗调薪, REGULARIZATION=转正调薪, MANUAL=手动调整',
    field_name      VARCHAR(32)     NOT NULL                 COMMENT '调整字段名',
    before_value    VARCHAR(128)    DEFAULT NULL             COMMENT '调整前值',
    after_value     VARCHAR(128)    NOT NULL                 COMMENT '调整后值',
    reason          VARCHAR(256)    DEFAULT NULL             COMMENT '调整原因',
    related_id      BIGINT          DEFAULT NULL             COMMENT '关联异动单据ID（调岗/转正）',
    adjust_date     DATE            NOT NULL                 COMMENT '调整日期',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_adjust_date (adjust_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调薪记录表';


-- ============================================================
-- 4. 人事异动模块 (HR Changes - 入转调离)
-- ============================================================

-- 4.1 入职申请表
DROP TABLE IF EXISTS hr_onboarding;
CREATE TABLE hr_onboarding (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name                    VARCHAR(64)     NOT NULL                 COMMENT '候选人姓名',
    gender                  TINYINT         NOT NULL DEFAULT 0       COMMENT '性别: 0=女, 1=男',
    mobile                  VARCHAR(20)     NOT NULL                 COMMENT '手机号',
    email                   VARCHAR(128)    DEFAULT NULL             COMMENT '邮箱',
    id_card                 VARCHAR(256)    DEFAULT NULL             COMMENT '身份证号',
    expected_entry_date     DATE            NOT NULL                 COMMENT '预计入职日期',
    dept_id                 BIGINT          NOT NULL                 COMMENT '入职部门ID',
    position_id             BIGINT          NOT NULL                 COMMENT '入职职位ID',
    employment_type         VARCHAR(16)     NOT NULL                 COMMENT '录用类型: FULL_TIME=全职, PART_TIME=兼职, INTERN=实习',
    probation_months        INT             NOT NULL DEFAULT 3       COMMENT '试用期(月)',
    probation_salary_ratio  DECIMAL(4,2)    NOT NULL DEFAULT 0.80    COMMENT '试用期薪资比例',
    reporter_id             BIGINT          DEFAULT NULL             COMMENT '直接汇报人ID',
    status                  VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, APPROVING=审批中, APPROVED=已批准待入职, REJECTED=已拒绝, ONBOARDED=已入职',
    created_by              BIGINT          NOT NULL                 COMMENT '创建人ID（HR）',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dept_id (dept_id),
    KEY idx_position_id (position_id),
    KEY idx_status (status),
    KEY idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入职申请表';

-- 4.2 转正申请表
DROP TABLE IF EXISTS hr_regularization;
CREATE TABLE hr_regularization (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id             BIGINT          NOT NULL                 COMMENT '员工ID',
    probation_start_date    DATE            NOT NULL                 COMMENT '试用期开始日期',
    probation_end_date      DATE            NOT NULL                 COMMENT '试用期结束日期',
    evaluation              TEXT            NOT NULL                 COMMENT '试用期表现评价',
    salary_adjustment       DECIMAL(12,2)   DEFAULT NULL             COMMENT '转正后薪资调整金额',
    adjust_remark           VARCHAR(256)    DEFAULT NULL             COMMENT '薪资调整说明',
    result                  VARCHAR(16)     DEFAULT NULL             COMMENT '审批结果: PASS=通过, EXTEND=延长试用, REJECT=不通过',
    extended_months         INT             DEFAULT NULL             COMMENT '延长试用月数',
    status                  VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=待发起, APPROVING=审批中, PASS=已转正, EXTENDED=已延长, REJECTED=不通过',
    created_by              BIGINT          NOT NULL                 COMMENT '发起人ID（HR）',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转正申请表';

-- 4.3 调岗申请表
DROP TABLE IF EXISTS hr_transfer;
CREATE TABLE hr_transfer (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id         BIGINT          NOT NULL                 COMMENT '员工ID',
    from_dept_id        BIGINT          NOT NULL                 COMMENT '原部门ID',
    to_dept_id          BIGINT          NOT NULL                 COMMENT '新部门ID',
    to_position_id      BIGINT          DEFAULT NULL             COMMENT '新职位ID（可选）',
    to_rank_code        VARCHAR(8)      DEFAULT NULL             COMMENT '新职级编码（可选）',
    to_reporter_id      BIGINT          DEFAULT NULL             COMMENT '新直接汇报人ID（可选）',
    salary_adjustment   DECIMAL(12,2)   DEFAULT NULL             COMMENT '薪资调整金额（可选）',
    reason              VARCHAR(256)    NOT NULL                 COMMENT '调岗原因',
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, APPROVING=审批中, APPROVED=已通过, REJECTED=已拒绝, EFFECTIVE=已生效',
    created_by          BIGINT          NOT NULL                 COMMENT '发起人ID（HR）',
    effective_date      DATE            DEFAULT NULL             COMMENT '调岗生效日期',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调岗申请表';

-- 4.4 离职申请表
DROP TABLE IF EXISTS hr_resignation;
CREATE TABLE hr_resignation (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id         BIGINT          NOT NULL                 COMMENT '员工ID',
    resign_date         DATE            NOT NULL                 COMMENT '离职日期（最后工作日）',
    resign_reason_type  VARCHAR(16)     NOT NULL                 COMMENT '原因大类: VOLUNTARY=主动, INVOLUNTARY=被动, NEGOTIATED=协商',
    resign_type         VARCHAR(32)     NOT NULL                 COMMENT '离职类型: RESIGN=辞职, DISMISS=辞退, CONTRACT_EXPIRE=合同到期不续签, OTHER=其他',
    detail_reason       TEXT            NOT NULL                 COMMENT '详细说明',
    handover_person_id  BIGINT          NOT NULL                 COMMENT '工作交接人ID',
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, APPROVING=审批中, PENDING_RESIGN=待离职(已批准), RESIGNED=已离职, REJECTED=已拒绝',
    created_by          BIGINT          NOT NULL                 COMMENT '发起人ID（HR）',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离职申请表';


-- ============================================================
-- 5. 审批中心模块 (Approval)
-- ============================================================

-- 5.1 审批流定义表
DROP TABLE IF EXISTS apr_flow_definition;
CREATE TABLE apr_flow_definition (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    business_type   VARCHAR(32)     NOT NULL                 COMMENT '业务类型: ONBOARDING=入职, REGULARIZATION=转正, TRANSFER=调岗, RESIGNATION=离职, LEAVE=请假, PATCH_CLOCK=补卡, SALARY_BATCH=薪资批次',
    flow_name       VARCHAR(64)     NOT NULL                 COMMENT '审批流名称',
    description     VARCHAR(256)    DEFAULT NULL             COMMENT '说明',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 1=启用, 0=禁用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_type (business_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流定义表';

-- 5.2 审批节点表
DROP TABLE IF EXISTS apr_flow_node;
CREATE TABLE apr_flow_node (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    flow_id         BIGINT          NOT NULL                 COMMENT '审批流ID',
    node_name       VARCHAR(64)     NOT NULL                 COMMENT '节点名称, 如"部门负责人审批"',
    node_order      INT             NOT NULL                 COMMENT '节点顺序, 从1开始',
    approver_type   VARCHAR(16)     NOT NULL                 COMMENT '审批人类型: SPECIFIED=指定人, DEPT_MANAGER=部门负责人, HR_MANAGER=HR负责人, DIRECT_SUPERIOR=直接上级, FINANCE=财务专员, ROLE=按角色',
    approver_id     BIGINT          DEFAULT NULL             COMMENT '指定审批人ID（approver_type=SPECIFIED）',
    approver_role_id BIGINT         DEFAULT NULL             COMMENT '审批角色ID（approver_type=ROLE）',
    timeout_hours   INT             DEFAULT 48               COMMENT '审批时效(小时), 默认48',
    is_optional     TINYINT         NOT NULL DEFAULT 0       COMMENT '是否可选节点: 0=必选, 1=可按条件跳过',
    skip_condition  VARCHAR(256)    DEFAULT NULL             COMMENT '跳过条件表达式',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_flow_id (flow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点表';

-- 5.3 审批实例表（一次业务申请的审批记录）
DROP TABLE IF EXISTS apr_approval_record;
CREATE TABLE apr_approval_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    flow_id         BIGINT          NOT NULL                 COMMENT '审批流定义ID',
    business_type   VARCHAR(32)     NOT NULL                 COMMENT '业务类型',
    business_id     BIGINT          NOT NULL                 COMMENT '关联业务表记录ID',
    current_node_id BIGINT          DEFAULT NULL             COMMENT '当前审批节点ID',
    applicant_id    BIGINT          NOT NULL                 COMMENT '申请人ID',
    status          VARCHAR(16)     NOT NULL DEFAULT 'APPROVING' COMMENT '审批状态: APPROVING=审批中, APPROVED=已通过, REJECTED=已拒绝, WITHDRAWN=已撤回',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_business (business_type, business_id),
    KEY idx_applicant_id (applicant_id),
    KEY idx_status (status),
    KEY idx_current_node (current_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批实例表';

-- 5.4 审批明细表（每个节点的审批动作记录）
DROP TABLE IF EXISTS apr_approval_detail;
CREATE TABLE apr_approval_detail (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    record_id       BIGINT          NOT NULL                 COMMENT '审批实例ID',
    node_id         BIGINT          NOT NULL                 COMMENT '审批节点ID',
    approver_id     BIGINT          NOT NULL                 COMMENT '审批人ID',
    action          VARCHAR(16)     NOT NULL                 COMMENT '审批动作: APPROVE=通过, REJECT=拒绝, TRANSFER=转交',
    comment         TEXT            DEFAULT NULL             COMMENT '审批意见',
    is_delegated    TINYINT         NOT NULL DEFAULT 0       COMMENT '是否代审批: 0=否, 1=是',
    delegated_by    BIGINT          DEFAULT NULL             COMMENT '委托人ID（代审批时记录）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    PRIMARY KEY (id),
    KEY idx_record_id (record_id),
    KEY idx_approver_id (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批明细表';

-- 5.5 审批委托表
DROP TABLE IF EXISTS apr_approval_delegation;
CREATE TABLE apr_approval_delegation (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    delegator_id    BIGINT          NOT NULL                 COMMENT '委托人ID',
    delegate_id     BIGINT          NOT NULL                 COMMENT '被委托人ID',
    start_date      DATE            NOT NULL                 COMMENT '委托开始日期',
    end_date        DATE            NOT NULL                 COMMENT '委托结束日期',
    business_types  VARCHAR(256)    DEFAULT NULL             COMMENT '委托业务类型（逗号分隔，NULL=全部）',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 1=有效, 0=已取消',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_delegator_id (delegator_id),
    KEY idx_delegate_id (delegate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批委托表';


-- ============================================================
-- 6. 考勤管理模块 (Attendance)
-- ============================================================

-- 6.1 考勤组表
DROP TABLE IF EXISTS att_group;
CREATE TABLE att_group (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    group_name          VARCHAR(64)     NOT NULL                 COMMENT '考勤组名称',
    shift_type          VARCHAR(16)     NOT NULL                 COMMENT '班次类型: FIXED=固定班, FLEXIBLE=弹性班, SCHEDULING=排班制',
    work_start_time     TIME            NOT NULL                 COMMENT '上班时间',
    work_end_time       TIME            NOT NULL                 COMMENT '下班时间',
    lunch_start_time    TIME            DEFAULT '12:00:00'       COMMENT '午休开始时间',
    lunch_end_time      TIME            DEFAULT '13:00:00'       COMMENT '午休结束时间',
    flex_start_time     TIME            DEFAULT NULL             COMMENT '弹性班最早打卡时间',
    flex_end_time       TIME            DEFAULT NULL             COMMENT '弹性班最晚打卡时间',
    late_threshold      INT             NOT NULL DEFAULT 15      COMMENT '迟到阈值(分钟)',
    early_threshold     INT             NOT NULL DEFAULT 15      COMMENT '早退阈值(分钟)',
    status              TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 1=启用, 0=禁用',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组表';

-- 6.2 考勤组成员表
DROP TABLE IF EXISTS att_group_member;
CREATE TABLE att_group_member (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    group_id        BIGINT          NOT NULL                 COMMENT '考勤组ID',
    employee_id     BIGINT          NOT NULL                 COMMENT '员工ID',
    effective_date  DATE            NOT NULL                 COMMENT '生效日期',
    expire_date     DATE            DEFAULT NULL             COMMENT '失效日期（NULL=长期有效）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_employee (group_id, employee_id, effective_date),
    KEY idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组成员表';

-- 6.3 打卡记录表
DROP TABLE IF EXISTS att_clock_record;
CREATE TABLE att_clock_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id     BIGINT          NOT NULL                 COMMENT '员工ID',
    clock_date      DATE            NOT NULL                 COMMENT '打卡日期',
    clock_in_time   DATETIME        DEFAULT NULL             COMMENT '上班打卡时间',
    clock_out_time  DATETIME        DEFAULT NULL             COMMENT '下班打卡时间',
    clock_in_type   VARCHAR(16)     DEFAULT NULL             COMMENT '上班打卡: NORMAL=正常, LATE=迟到, MISSING=缺卡',
    clock_out_type  VARCHAR(16)     DEFAULT NULL             COMMENT '下班打卡: NORMAL=正常, EARLY=早退, MISSING=缺卡',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '当日状态: NORMAL=正常, LATE=迟到, EARLY=早退, ABSENT=旷工, MISSING=缺勤, PENDING=待确认',
    clock_in_ip     VARCHAR(64)     DEFAULT NULL             COMMENT '上班打卡IP',
    clock_in_location VARCHAR(256)  DEFAULT NULL             COMMENT '上班打卡GPS位置',
    clock_out_ip    VARCHAR(64)     DEFAULT NULL             COMMENT '下班打卡IP',
    clock_out_location VARCHAR(256) DEFAULT NULL             COMMENT '下班打卡GPS位置',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_date (employee_id, clock_date),
    KEY idx_clock_date (clock_date),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表';

-- 6.4 请假申请表
DROP TABLE IF EXISTS att_leave_application;
CREATE TABLE att_leave_application (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id         BIGINT          NOT NULL                 COMMENT '申请人ID',
    leave_type          VARCHAR(16)     NOT NULL                 COMMENT '请假类型: ANNUAL=年假, SICK=病假, PERSONAL=事假, MARRIAGE=婚假, MATERNITY=产假, FUNERAL=丧假, COMPENSATORY=调休',
    start_time          DATETIME        NOT NULL                 COMMENT '开始时间',
    end_time            DATETIME        NOT NULL                 COMMENT '结束时间',
    leave_days          DECIMAL(4,1)    NOT NULL                 COMMENT '请假天数（支持0.5天）',
    reason              VARCHAR(256)    NOT NULL                 COMMENT '请假事由',
    handover_person_id  BIGINT          DEFAULT NULL             COMMENT '工作交接人ID',
    attachment_url      VARCHAR(256)    DEFAULT NULL             COMMENT '证明材料附件URL',
    status              VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, APPROVING=审批中, APPROVED=已通过, REJECTED=已拒绝, CANCELLED=已取消',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_status (status),
    KEY idx_leave_type (leave_type),
    KEY idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';

-- 6.5 假期余额表
DROP TABLE IF EXISTS att_leave_balance;
CREATE TABLE att_leave_balance (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id     BIGINT          NOT NULL                 COMMENT '员工ID',
    year            INT             NOT NULL                 COMMENT '年份',
    leave_type      VARCHAR(16)     NOT NULL                 COMMENT '假期类型: ANNUAL=年假, COMPENSATORY=调休',
    total_days      DECIMAL(5,1)    NOT NULL DEFAULT 0.0     COMMENT '总额度(天)',
    used_days       DECIMAL(5,1)    NOT NULL DEFAULT 0.0     COMMENT '已使用(天)',
    remaining_days  DECIMAL(5,1)    NOT NULL DEFAULT 0.0     COMMENT '剩余(天)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_year_type (employee_id, year, leave_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='假期余额表';

-- 6.6 节假日配置表
DROP TABLE IF EXISTS att_holiday_config;
CREATE TABLE att_holiday_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    holiday_date    DATE            NOT NULL                 COMMENT '日期',
    holiday_name    VARCHAR(64)     NOT NULL                 COMMENT '节假日名称',
    is_workday      TINYINT         NOT NULL DEFAULT 0       COMMENT '是否工作日: 0=休息日(节假日/周末), 1=工作日(调休补班)',
    year            INT             NOT NULL                 COMMENT '年份',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_holiday_date (holiday_date),
    KEY idx_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节假日配置表';

-- 6.7 加班记录表
DROP TABLE IF EXISTS att_overtime_record;
CREATE TABLE att_overtime_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id     BIGINT          NOT NULL                 COMMENT '员工ID',
    overtime_date   DATE            NOT NULL                 COMMENT '加班日期',
    start_time      DATETIME        NOT NULL                 COMMENT '开始时间',
    end_time        DATETIME        NOT NULL                 COMMENT '结束时间',
    overtime_hours  DECIMAL(4,1)    NOT NULL                 COMMENT '加班时长(小时)',
    reason          VARCHAR(256)    NOT NULL                 COMMENT '加班原因',
    status          VARCHAR(16)     NOT NULL DEFAULT 'APPROVED' COMMENT '状态: DRAFT=草稿, APPROVING=审批中, APPROVED=已通过, REJECTED=已拒绝, COMPENSATED=已调休',
    compensatory_expire_date DATE   DEFAULT NULL             COMMENT '调休有效期（加班当月+次月）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_overtime_date (overtime_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班记录表';

-- 6.8 补卡申请表
DROP TABLE IF EXISTS att_patch_clock;
CREATE TABLE att_patch_clock (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employee_id     BIGINT          NOT NULL                 COMMENT '申请人ID',
    clock_date      DATE            NOT NULL                 COMMENT '补卡日期',
    patch_type      VARCHAR(16)     NOT NULL                 COMMENT '补卡类型: CLOCK_IN=上班卡, CLOCK_OUT=下班卡, BOTH=双卡',
    patch_time      DATETIME        NOT NULL                 COMMENT '补卡时间',
    reason          VARCHAR(256)    NOT NULL                 COMMENT '补卡原因',
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, APPROVING=审批中, APPROVED=已通过, REJECTED=已拒绝',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_employee_id (employee_id),
    KEY idx_clock_date (clock_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';


-- ============================================================
-- 7. 薪资管理模块 (Salary)
-- ============================================================

-- 7.1 薪资账套表（工资模板）
DROP TABLE IF EXISTS sal_account_set;
CREATE TABLE sal_account_set (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name            VARCHAR(64)     NOT NULL                 COMMENT '账套名称, 如"标准职员工资"',
    description     VARCHAR(256)    DEFAULT NULL             COMMENT '账套说明',
    effective_date  DATE            NOT NULL                 COMMENT '启用日期',
    expire_date     DATE            DEFAULT NULL             COMMENT '失效日期（NULL=长期有效）',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 1=启用, 0=停用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资账套表';

-- 7.2 工资项目定义表
DROP TABLE IF EXISTS sal_account_item;
CREATE TABLE sal_account_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    item_code       VARCHAR(32)     NOT NULL                 COMMENT '项目编码',
    item_name       VARCHAR(64)     NOT NULL                 COMMENT '项目名称, 如"基本工资"',
    item_type       VARCHAR(16)     NOT NULL                 COMMENT '项目类型: FIXED_INCOME=固定收入, VARIABLE_INCOME=变动收入, ATTENDANCE_DEDUCTION=考勤扣款, SOCIAL_INSURANCE=社保扣除, HOUSING_FUND=公积金扣除, INCOME_TAX=个税',
    calc_formula    VARCHAR(256)    DEFAULT NULL             COMMENT '默认计算公式',
    default_value   DECIMAL(12,2)   DEFAULT NULL             COMMENT '默认值',
    sort_order      INT             NOT NULL DEFAULT 0       COMMENT '排序序号',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_item_code (item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资项目定义表';

-- 7.3 账套-工资项目关联表
DROP TABLE IF EXISTS sal_account_set_item;
CREATE TABLE sal_account_set_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    set_id          BIGINT          NOT NULL                 COMMENT '账套ID',
    item_id         BIGINT          NOT NULL                 COMMENT '工资项目ID',
    calc_rule       TEXT            DEFAULT NULL             COMMENT '该账套下此项目的具体计算规则',
    sort_order      INT             NOT NULL DEFAULT 0       COMMENT '排序序号',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_set_item (set_id, item_id),
    KEY idx_set_id (set_id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账套项目关联表';

-- 7.4 薪资核算批次表
DROP TABLE IF EXISTS sal_batch;
CREATE TABLE sal_batch (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batch_no                VARCHAR(32)     NOT NULL                 COMMENT '批次号',
    salary_month            VARCHAR(7)      NOT NULL                 COMMENT '薪资月份: YYYY-MM',
    status                  VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, CALCULATING=计算中, PENDING_CONFIRM=待确认, APPROVING=审批中, APPROVED=已通过, PAID=已发放, REJECTED=已驳回',
    total_employee_count    INT             NOT NULL DEFAULT 0       COMMENT '核算员工总数',
    total_gross             DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '应发工资总额',
    total_deduction         DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '扣除总额',
    total_net               DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '实发工资总额',
    created_by              BIGINT          NOT NULL                 COMMENT '创建人ID（HR）',
    approved_by             BIGINT          DEFAULT NULL             COMMENT '最终审批人ID',
    paid_at                 DATETIME        DEFAULT NULL             COMMENT '实际发放时间',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    UNIQUE KEY uk_salary_month (salary_month),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算批次表';

-- 7.5 薪资核算明细表（每位员工一条记录）
DROP TABLE IF EXISTS sal_batch_detail;
CREATE TABLE sal_batch_detail (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batch_id                BIGINT          NOT NULL                 COMMENT '批次ID',
    employee_id             BIGINT          NOT NULL                 COMMENT '员工ID',
    base_salary             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '基本工资',
    allowance               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '岗位津贴',
    performance_bonus       DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '绩效奖金',
    overtime_pay            DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '加班费',
    late_deduction          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '迟到扣款',
    leave_deduction         DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '请假扣款',
    social_pension          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '养老保险',
    social_medical          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '医疗保险',
    social_unemployment     DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '失业保险',
    housing_fund            DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '住房公积金',
    income_tax              DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '个人所得税',
    gross_salary            DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '应发工资',
    total_deduction         DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '应扣合计',
    net_salary              DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '实发工资',
    has_anomaly             TINYINT         NOT NULL DEFAULT 0       COMMENT '是否有异常: 0=正常, 1=预警, 2=阻断',
    anomaly_reason          VARCHAR(256)    DEFAULT NULL             COMMENT '异常说明',
    manual_adjust           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '手动调整金额',
    adjust_reason           VARCHAR(256)    DEFAULT NULL             COMMENT '手动调整原因',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_employee (batch_id, employee_id),
    KEY idx_employee_id (employee_id),
    KEY idx_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算明细表';

-- 7.6 工资条查看记录表
DROP TABLE IF EXISTS sal_payslip_view_log;
CREATE TABLE sal_payslip_view_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batch_detail_id BIGINT          NOT NULL                 COMMENT '薪资明细ID',
    employee_id     BIGINT          NOT NULL                 COMMENT '查看员工ID',
    verify_code     VARCHAR(6)      DEFAULT NULL             COMMENT '二次验证码（短信验证）',
    verified_at     DATETIME        DEFAULT NULL             COMMENT '验证通过时间',
    viewed_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查看时间',
    view_ip         VARCHAR(64)     DEFAULT NULL             COMMENT '查看IP',
    PRIMARY KEY (id),
    KEY idx_batch_detail_id (batch_detail_id),
    KEY idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资条查看记录表';


-- ============================================================
-- 8. 通知消息模块 (Notification)
-- ============================================================

-- 8.1 通知消息表
DROP TABLE IF EXISTS ntf_notification;
CREATE TABLE ntf_notification (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    recipient_id    BIGINT          NOT NULL                 COMMENT '接收人ID',
    title           VARCHAR(128)    NOT NULL                 COMMENT '通知标题',
    content         TEXT            NOT NULL                 COMMENT '通知内容',
    notice_type     VARCHAR(32)     NOT NULL                 COMMENT '通知类型: APPROVAL=审批通知, SYSTEM=系统通知, REMINDER=提醒通知',
    business_type   VARCHAR(32)     DEFAULT NULL             COMMENT '关联业务类型',
    business_id     BIGINT          DEFAULT NULL             COMMENT '关联业务ID',
    is_read         TINYINT         NOT NULL DEFAULT 0       COMMENT '是否已读: 0=未读, 1=已读',
    read_at         DATETIME        DEFAULT NULL             COMMENT '已读时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_recipient_id (recipient_id),
    KEY idx_is_read (is_read),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';

-- 8.2 邮件发送记录表
DROP TABLE IF EXISTS ntf_email_log;
CREATE TABLE ntf_email_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    recipient_email VARCHAR(128)    NOT NULL                 COMMENT '收件邮箱',
    recipient_name  VARCHAR(64)     DEFAULT NULL             COMMENT '收件人姓名',
    subject         VARCHAR(256)    NOT NULL                 COMMENT '邮件主题',
    content         TEXT            NOT NULL                 COMMENT '邮件内容',
    send_result     TINYINT         NOT NULL DEFAULT 1       COMMENT '发送结果: 1=成功, 0=失败',
    fail_reason     VARCHAR(256)    DEFAULT NULL             COMMENT '失败原因',
    business_type   VARCHAR(32)     DEFAULT NULL             COMMENT '触发业务类型',
    business_id     BIGINT          DEFAULT NULL             COMMENT '触发业务ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_recipient_email (recipient_email),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发送记录表';
