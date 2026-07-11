-- ============================================================
-- HRMS 人力资源管理平台 - 完整建表语句
-- 数据库：szml
-- ============================================================

-- ------------------------------------------------------------
-- 使用数据库
-- ------------------------------------------------------------
USE szml;

-- ============================================================
-- 一、基础功能表
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户表（user）
-- 用途：系统用户基础表，登录认证、权限管理、员工身份均依赖此表
-- 与薪资模块的关系：employee_id / create_by / operator_id 都关联本表 id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE IF NOT EXISTS `user`
(
    `id`           BIGINT        NOT NULL COMMENT '主键，雪花算法生成',
    `user_account` VARCHAR(256)  NOT NULL COMMENT '登录账号，员工工号或自定义账号',
    `user_password` VARCHAR(512) NOT NULL COMMENT '登录密码，MD5 加盐加密存储（SALT + 原密码取 MD5）',
    `union_id`     VARCHAR(256)  DEFAULT NULL COMMENT '微信开放平台 UnionID，用于微信扫码登录（预留）',
    `mp_open_id`   VARCHAR(256)  DEFAULT NULL COMMENT '微信公众号 OpenID，用于公众号内登录（预留）',
    `user_name`    VARCHAR(256)  DEFAULT NULL COMMENT '用户昵称 / 员工姓名',
    `user_avatar`  VARCHAR(1024) DEFAULT NULL COMMENT '用户头像 URL，通常为 COS 地址',
    `user_profile` VARCHAR(512)  DEFAULT NULL COMMENT '用户简介 / 个人说明',
    `user_role`    VARCHAR(256)  DEFAULT 'user' NOT NULL COMMENT '用户角色：user=普通员工, admin=管理员/HR, ban=已封禁',
    `create_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间（注册时间）',
    `update_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间',
    `is_delete`    TINYINT       DEFAULT 0 NOT NULL COMMENT '逻辑删除标记：0=正常, 1=已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_account` (`user_account`) COMMENT '账号索引，加速登录查询',
    INDEX `idx_union_id` (`union_id`) COMMENT '微信 UnionID 索引，加速微信登录查询',
    INDEX `idx_mp_open_id` (`mp_open_id`) COMMENT '公众号 OpenID 索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表：存储系统所有用户的账号、角色、基本信息';


-- ============================================================
-- 二、帖子/社区功能表（模板遗留，当前薪资模块未使用）
-- ============================================================

-- ------------------------------------------------------------
-- 2. 帖子表（post）
-- 用途：社区帖子发布（模板自带功能，薪资模块不依赖此表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `post`;
CREATE TABLE IF NOT EXISTS `post`
(
    `id`          BIGINT       NOT NULL COMMENT '主键，雪花算法生成',
    `title`       VARCHAR(256) DEFAULT NULL COMMENT '帖子标题',
    `content`     TEXT         DEFAULT NULL COMMENT '帖子正文内容（富文本/Markdown）',
    `tags`        VARCHAR(256) DEFAULT NULL COMMENT '标签列表，JSON 格式存储，如 ["java","spring"]',
    `thumb_num`   INT          DEFAULT 0 NOT NULL COMMENT '点赞数（冗余聚合，避免 JOIN）',
    `favour_num`  INT          DEFAULT 0 NOT NULL COMMENT '收藏数（冗余聚合，避免 JOIN）',
    `user_id`     BIGINT       NOT NULL COMMENT '发帖人 ID，关联 user.id',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '发帖时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后编辑时间',
    `is_delete`   TINYINT      DEFAULT 0 NOT NULL COMMENT '逻辑删除标记：0=正常, 1=已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_post_user_id` (`user_id`) COMMENT '用户维度查询帖子列表',
    INDEX `idx_post_create_time` (`create_time`) COMMENT '按时间排序查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表：社区功能，当前薪资模块不依赖此表';


-- ------------------------------------------------------------
-- 3. 帖子点赞表（post_thumb）
-- 用途：记录用户对帖子的点赞行为
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `post_thumb`;
CREATE TABLE IF NOT EXISTS `post_thumb`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `post_id`     BIGINT   NOT NULL COMMENT '被点赞的帖子 ID，关联 post.id',
    `user_id`     BIGINT   NOT NULL COMMENT '点赞用户 ID，关联 user.id',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '点赞时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_post_user` (`post_id`, `user_id`) COMMENT '唯一索引：同一用户对同一帖子只能点赞一次',
    INDEX `idx_post_thumb_user_id` (`user_id`) COMMENT '用户维度查询点赞列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表：记录用户对帖子的点赞，当前薪资模块不依赖此表';


-- ------------------------------------------------------------
-- 4. 帖子收藏表（post_favour）
-- 用途：记录用户对帖子的收藏行为
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `post_favour`;
CREATE TABLE IF NOT EXISTS `post_favour`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `post_id`     BIGINT   NOT NULL COMMENT '被收藏的帖子 ID，关联 post.id',
    `user_id`     BIGINT   NOT NULL COMMENT '收藏用户 ID，关联 user.id',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '收藏时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_post_user` (`post_id`, `user_id`) COMMENT '唯一索引：同一用户对同一帖子只能收藏一次',
    INDEX `idx_post_favour_user_id` (`user_id`) COMMENT '用户维度查询收藏列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子收藏表：记录用户对帖子的收藏，当前薪资模块不依赖此表';


-- ============================================================
-- 三、薪资管理核心表
-- ============================================================

-- ------------------------------------------------------------
-- 5. 薪资账套表（salary_account）
-- 用途：定义薪酬模板，一个账套 = 一套薪资发放规则
-- 关系：1 个账套包含 N 个工资项目（salary_item）
--       多个员工（employee_salary）可共用同一个账套
-- 业务场景：HR 先创建账套并配置工资项目，再将员工挂到该账套下
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_account`;
CREATE TABLE IF NOT EXISTS `salary_account`
(
    `id`             BIGINT       NOT NULL COMMENT '主键，雪花算法生成',
    `name`           VARCHAR(128) NOT NULL COMMENT '账套名称，如"研发岗标准薪酬模板""销售岗提成制模板""管理岗年薪制模板"',
    `scope_type`     INT          DEFAULT 1 NOT NULL COMMENT '适用范围类型：1=部门, 2=职位, 3=职级（见 ScopeTypeEnum）',
    `scope_ids`      VARCHAR(512) DEFAULT NULL COMMENT '适用范围 ID 列表，逗号分隔，如"研发部,产品部"对应部门的 id；NULL 表示全员适用',
    `effective_date` DATE         DEFAULT NULL COMMENT '账套生效日期，可用于未来生效的薪酬方案',
    `is_deleted`     TINYINT      DEFAULT 0 NOT NULL COMMENT '逻辑删除标记：0=正常, 1=已删除',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_account_name` (`name`) COMMENT '按账套名称查询',
    INDEX `idx_account_scope` (`scope_type`, `scope_ids`) COMMENT '按适用范围筛选'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资账套表：定义薪酬模板，包含适用范围、生效日期等信息，一个账套包含多个工资项目';


-- ------------------------------------------------------------
-- 6. 工资项目表（salary_item）
-- 用途：定义薪资账套下的具体工资构成项目
-- 关系：account_id → salary_account.id（多对一）
-- 每种项目类型由不同的计算器处理：
--   FIXED_INCOME(1)    → FixedIncomeCalculator      固定收入（基本工资、岗位津贴）
--   VARIABLE_INCOME(2) → VariableIncomeCalculator    变动收入（绩效奖金、提成）
--   ATTENDANCE_DEDUCT(3)→AttendanceDeductionCalculator 考勤扣款（迟到、请假）
--   SOCIAL_SECURITY(4) → SocialSecurityCalculator     社保个人部分扣除
--   HOUSING_FUND(5)    → HousingFundCalculator         公积金个人部分扣除
--   INCOME_TAX(6)      → IncomeTaxCalculator           个税计算
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_item`;
CREATE TABLE IF NOT EXISTS `salary_item`
(
    `id`          BIGINT       NOT NULL COMMENT '主键，雪花算法生成',
    `account_id`  BIGINT       NOT NULL COMMENT '所属账套 ID，关联 salary_account.id',
    `name`        VARCHAR(128) NOT NULL COMMENT '工资项目名称，如"基本工资""绩效奖金""交通补贴""社保个人部分""公积金个人部分""考勤扣款""个税"',
    `item_type`   INT          NOT NULL COMMENT '项目类型：1=固定收入, 2=变动收入, 3=考勤扣款, 4=社保扣除, 5=公积金扣除, 6=个税（见 SalaryItemTypeEnum）——不同 type 走不同计算器',
    `formula`     VARCHAR(256) DEFAULT NULL COMMENT '计算公式/规则，如"base*0.08"表示按基本工资×8%；"fixed:300"表示固定 300 元；NULL 表示由对应 Calculator 内部逻辑计算',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序号，数字越小越靠前，用于工资条展示顺序',
    `is_taxable`  TINYINT      DEFAULT 1 COMMENT '是否计入应纳税所得额：1=是（如基本工资、绩效）, 0=否（如社保公积金扣除项本身不参与计税）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_item_account_id` (`account_id`) COMMENT '按账套查询所有工资项目',
    INDEX `idx_item_type` (`item_type`) COMMENT '按项目类型筛选'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工资项目表：定义每个账套下的工资构成项目，控制工资条显示内容和计算规则';


-- ------------------------------------------------------------
-- 7. 员工薪资档案表（employee_salary）
-- 用途：记录每个员工的薪资档案——"这个员工用哪个薪酬模板，每项薪资基数是多少"
-- 关系：employee_id → user.id（员工 = 系统用户）
--        account_id  → salary_account.id（使用哪个薪酬模板）
-- 业务：HR 为每个员工建立薪资档案，修改时会自动插入 salary_change_history 记录
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `employee_salary`;
CREATE TABLE IF NOT EXISTS `employee_salary`
(
    `id`                  BIGINT        NOT NULL COMMENT '主键，雪花算法生成',
    `employee_id`         BIGINT        NOT NULL COMMENT '员工 ID，关联 user.id（员工即系统注册用户）',
    `account_id`          BIGINT        NOT NULL COMMENT '薪资账套 ID，关联 salary_account.id，决定该员工适用的工资项目结构',
    `base_salary`         DECIMAL(12,2) DEFAULT 0.00 COMMENT '月基本工资（元），通常是合同约定的固定工资，是整个薪资计算的基础',
    `allowance_base`      DECIMAL(12,2) DEFAULT 0.00 COMMENT '补贴基数（元），如餐补、交通补贴的计算基数',
    `social_security_base` DECIMAL(12,2) DEFAULT 0.00 COMMENT '社保缴费基数（元），按当地社保政策确定，公司和个人按此基数各自承担比例',
    `housing_fund_base`   DECIMAL(12,2) DEFAULT 0.00 COMMENT '公积金缴费基数（元），按当地公积金政策确定',
    `performance_base`    DECIMAL(12,2) DEFAULT 0.00 COMMENT '绩效工资基数（元），绩效评分的满分值，实际绩效=基数×绩效系数',
    `effective_date`      DATE          NOT NULL COMMENT '档案生效日期，最新一条生效档案为当前有效档案',
    `is_deleted`          TINYINT       DEFAULT 0 NOT NULL COMMENT '逻辑删除标记：0=正常, 1=已删除',
    `create_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `update_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_es_employee_id` (`employee_id`) COMMENT '按员工查询薪资档案',
    INDEX `idx_es_account_id` (`account_id`) COMMENT '按账套查询关联员工',
    INDEX `idx_es_effective_date` (`effective_date`) COMMENT '按生效日期查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工薪资档案表：记录每个员工的薪资账套关联和各薪资基数，是 HR 定薪和月度核算的关键表';


-- ------------------------------------------------------------
-- 8. 薪资核算批次表（salary_batch）
-- 用途：每月一次薪资核算的批次记录，完整走完"草稿→计算→提交→审批→发放"流程
-- 关系：id → salary_detail.batch_id（1:N）
--        create_by → user.id
-- 状态流转：
--   草稿(0) → 计算中(1) → 待确认(2)
--   待确认(2) → 审批中(3) → 已通过(4) → 已发放(5)
--   审批中(3) → 已驳回(6) → 可重新提交至审批中(3)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_batch`;
CREATE TABLE IF NOT EXISTS `salary_batch`
(
    `id`              BIGINT        NOT NULL COMMENT '主键，雪花算法生成',
    `batch_no`        VARCHAR(32)   NOT NULL COMMENT '批次号，如"SAL202607"表示 2026 年 7 月薪资批次，全局唯一',
    `salary_month`    VARCHAR(7)    NOT NULL COMMENT '薪资所属月份，格式 yyyy-MM，如"2026-07"',
    `status`          INT           DEFAULT 0 NOT NULL COMMENT '批次状态：0=草稿, 1=计算中, 2=待确认, 3=审批中, 4=已通过, 5=已发放, 6=已驳回（见 BatchStatusEnum）',
    `total_employees` INT           DEFAULT 0 COMMENT '参与本次核算的员工总数（冗余汇总）',
    `total_gross_pay` DECIMAL(14,2) DEFAULT 0.00 COMMENT '应发工资合计（元），本批次所有员工 gross_pay 求和',
    `total_net_pay`   DECIMAL(14,2) DEFAULT 0.00 COMMENT '实发工资合计（元），本批次所有员工 net_pay 求和',
    `total_tax`       DECIMAL(14,2) DEFAULT 0.00 COMMENT '个税合计（元），本批次所有员工 income_tax 求和',
    `create_by`       BIGINT        DEFAULT NULL COMMENT '创建人 ID（HR），关联 user.id',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间（状态变更时更新）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_batch_no` (`batch_no`) COMMENT '批次号唯一索引：同一月份同一批次号不重复',
    INDEX `idx_batch_salary_month` (`salary_month`) COMMENT '按薪资月份查询',
    INDEX `idx_batch_status` (`status`) COMMENT '按状态筛选（如查询"待审批"的批次）',
    INDEX `idx_batch_create_by` (`create_by`) COMMENT '按创建人查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资核算批次表：每月薪资核算的批次主表，统一管理核算流程和汇总数据，一个批次包含多条薪资明细';


-- ------------------------------------------------------------
-- 9. 薪资明细表（salary_detail）
-- 用途：批次核算后每个员工生成一条明细 = 工资条，是员工最关心的数据
-- 关系：batch_id    → salary_batch.id（多对一）
--        employee_id → user.id
-- 字段说明：
--   salary_items：JSON 存储各工资项目的金额明细，避免关联太多
--   示例：[{"name":"基本工资","amount":8000},{"name":"绩效奖金","amount":2000},{"name":"社保","amount":-840}]
-- 工资计算公式：
--   应发工资(gross) = 各项收入之和
--   扣款合计(deductions) = 社保 + 公积金 + 个税
--   实发工资(net) = 应发工资 - 扣款合计 + 手动调整
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_detail`;
CREATE TABLE IF NOT EXISTS `salary_detail`
(
    `id`                BIGINT        NOT NULL COMMENT '主键，雪花算法生成',
    `batch_id`          BIGINT        NOT NULL COMMENT '所属核算批次 ID，关联 salary_batch.id',
    `employee_id`       BIGINT        NOT NULL COMMENT '员工 ID，关联 user.id',
    `salary_items`      TEXT          DEFAULT NULL COMMENT '工资项目明细 JSON，格式：[{"name":"基本工资","amount":8000,"type":1},{"name":"绩效奖金","amount":2000,"type":2},...]，存储每个工资金额项的详细组成',
    `gross_pay`         DECIMAL(12,2) DEFAULT 0.00 COMMENT '应发工资（元）= 固定收入 + 变动收入，即所有正向收入项目之和',
    `social_security`   DECIMAL(10,2) DEFAULT 0.00 COMMENT '社保个人缴纳金额（元），根据社保基数 × 个人比例计算',
    `housing_fund`      DECIMAL(10,2) DEFAULT 0.00 COMMENT '公积金个人缴纳金额（元），根据公积金基数 × 个人比例计算',
    `income_tax`        DECIMAL(10,2) DEFAULT 0.00 COMMENT '个人所得税（元），采用累计预扣法按月计算，本月个税 = 累计应纳税额 - 累计已缴税额',
    `total_deductions`  DECIMAL(10,2) DEFAULT 0.00 COMMENT '扣款合计（元）= 社保 + 公积金 + 个税 + 考勤扣款等',
    `net_pay`           DECIMAL(12,2) DEFAULT 0.00 COMMENT '实发工资（元）= 应发工资 - 扣款合计 + 手动调整，即员工实际到手的金额',
    `is_abnormal`       TINYINT       DEFAULT 0 COMMENT '是否异常：0=正常, 1=黄色预警, 2=红色预警, 3=红色阻断（见 AbnormalLevelEnum），异常项会计入 anomaly 列表供 HR 处理',
    `abnormal_reason`   VARCHAR(512)  DEFAULT NULL COMMENT '异常原因说明，如"社保基数与上月不一致""应发工资降幅超过 50%"',
    `manual_adjustment` DECIMAL(10,2) DEFAULT 0.00 COMMENT '手动调整金额（元），正数=补发, 负数=扣回，由 HR 手动输入',
    `adjustment_reason` VARCHAR(256)  DEFAULT NULL COMMENT '手动调整原因，如"上月漏发餐补 200 元补发"',
    `payslip_viewed`    TINYINT       DEFAULT 0 COMMENT '工资条是否已查看：0=未查看, 1=已验证（二次确认通过）, 2=已查看详情',
    `create_time`       DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间（核算完成时间）',
    PRIMARY KEY (`id`),
    INDEX `idx_sd_batch_id` (`batch_id`) COMMENT '按批次查询所有明细',
    INDEX `idx_sd_employee_id` (`employee_id`) COMMENT '按员工查询个人工资条历史',
    INDEX `idx_sd_batch_employee` (`batch_id`, `employee_id`) COMMENT '联合查询：某批次下某员工的工资条（查详情用）',
    INDEX `idx_sd_abnormal` (`batch_id`, `is_abnormal`) COMMENT '查询某批次下的异常明细',
    INDEX `idx_sd_payslip_viewed` (`employee_id`, `payslip_viewed`) COMMENT '查询员工工资条查看状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='薪资明细表（工资条）：每月核算后每个员工生成一条记录，存储工资组成、扣款、实发金额等完整工资条信息，是员工自助查询的核心数据源';


-- ------------------------------------------------------------
-- 10. 调薪历史表（salary_change_history）
-- 用途：记录员工每一次薪资档案变更的历史，用于审计和追溯
-- 触发时机：当调用 EmployeeSalaryController.updateEmployeeSalary() 时自动插入
-- 关系：employee_id → user.id
--        operator_id → user.id（操作人 HR）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `salary_change_history`;
CREATE TABLE IF NOT EXISTS `salary_change_history`
(
    `id`             BIGINT        NOT NULL COMMENT '主键，雪花算法生成',
    `employee_id`    BIGINT        NOT NULL COMMENT '被调薪的员工 ID，关联 user.id',
    `change_type`    INT           NOT NULL COMMENT '变更类型：1=调薪（基本工资变动）, 2=账套变更（换薪酬模板）, 3=基数调整（社保/公积金基数变动）, 4=转正调薪, 5=调岗调薪（见 ChangeTypeEnum）',
    `old_value`      VARCHAR(512)  DEFAULT NULL COMMENT '变更前的值，JSON 格式记录变更前的完整字段信息',
    `new_value`      VARCHAR(512)  DEFAULT NULL COMMENT '变更后的值，JSON 格式记录变更后的完整字段信息',
    `effective_date` DATE          NOT NULL COMMENT '调薪生效日期',
    `operator_id`    BIGINT        NOT NULL COMMENT '操作人 ID（HR），关联 user.id',
    `remark`         VARCHAR(512)  DEFAULT NULL COMMENT '调薪备注/原因说明，如"年度调薪，基本工资上调 10%"',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '记录创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_sch_employee_id` (`employee_id`) COMMENT '按员工查询调薪历史（员工调薪轨迹）',
    INDEX `idx_sch_operator_id` (`operator_id`) COMMENT '按操作人查询（HR 操作记录）',
    INDEX `idx_sch_effective_date` (`effective_date`) COMMENT '按生效日期查询',
    INDEX `idx_sch_change_type` (`change_type`) COMMENT '按变更类型筛选'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调薪历史表：记录员工每次薪资档案变更的完整历史，支持审计追溯和调薪轨迹查询';


-- ------------------------------------------------------------
-- 11. 个税累计表（income_tax_cumulative）
-- 用途：支持中国个税"累计预扣法"计算，每月核算时查询本年累计数据用于计算当月个税
-- 计算逻辑（中国个税累计预扣法）：
--   ① 累计应纳税所得额 = 累计收入 - 累计起征点(5000×N) - 累计社保 - 累计公积金 - 累计专项附加扣除
--   ② 累计应纳税额 = 累计应纳税所得额 × 对应税率 - 速算扣除数
--   ③ 本月个税 = 累计应纳税额 - 累计已缴税额
-- 每个员工每年最多 12 条记录（1月~12月）
-- 税率表（综合所得税率）：
--   不超过 36,000 元：     3%    速算扣除数 0
--   36,000 ~ 144,000 元： 10%    速算扣除数 2,520
--   144,000 ~ 300,000 元： 20%    速算扣除数 16,920
--   300,000 ~ 420,000 元： 25%    速算扣除数 31,920
--   420,000 ~ 660,000 元： 30%    速算扣除数 52,920
--   660,000 ~ 960,000 元： 35%    速算扣除数 85,920
--   超过 960,000 元：      45%    速算扣除数 181,920
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `income_tax_cumulative`;
CREATE TABLE IF NOT EXISTS `income_tax_cumulative`
(
    `id`                           BIGINT        NOT NULL COMMENT '主键，雪花算法生成',
    `employee_id`                  BIGINT        NOT NULL COMMENT '员工 ID，关联 user.id',
    `tax_year`                     INT           NOT NULL COMMENT '纳税年度，如 2026',
    `tax_month`                    INT           NOT NULL COMMENT '纳税月份，1~12',
    `cumulative_gross_pay`         DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计应发工资（元），年初到当前月的应发工资合计',
    `cumulative_threshold`         DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计起征点（元）= 5000 × 当前月数，如 5 月 = 25000',
    `cumulative_social_security`   DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计社保个人部分（元），年初到当前月合计',
    `cumulative_housing_fund`      DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计公积金个人部分（元），年初到当前月合计',
    `cumulative_special_deduction` DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计专项附加扣除（元）：子女教育、继续教育、大病医疗、住房贷款利息、住房租金、赡养老人 6 项的年度累计',
    `cumulative_taxable_income`    DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计应纳税所得额（元）= 累计收入 - 起征点 - 社保 - 公积金 - 专项附加扣除，这是确定税率的基础',
    `tax_rate`                     DECIMAL(5,4)  DEFAULT 0.0000 COMMENT '适用税率，根据累计应纳税所得额查表，如 0.03, 0.10, 0.20...',
    `quick_deduction`              DECIMAL(10,2) DEFAULT 0.00 COMMENT '速算扣除数（元），根据累计应纳税所得额所在档位查表确定',
    `cumulative_tax_payable`       DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计应纳税额（元）= 累计应纳税所得额 × 税率 - 速算扣除数',
    `cumulative_tax_paid`          DECIMAL(14,2) DEFAULT 0.00 COMMENT '累计已缴税额（元），年初到上月已缴纳的个税合计',
    `current_month_tax`            DECIMAL(10,2) DEFAULT 0.00 COMMENT '【关键】本月应缴个税（元）= 累计应纳税额 - 累计已缴税额',
    `create_time`                  DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间（当月核算完成时插入/更新）',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_employee_year_month` (`employee_id`, `tax_year`, `tax_month`) COMMENT '唯一索引：每个员工每年每月仅一条累计记录',
    INDEX `idx_itc_employee_year` (`employee_id`, `tax_year`) COMMENT '查询员工某年全年的累计数据'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='个税累计表：支持中国个税累计预扣法计算，每月核算时查询本表获取累计数据计算当月个税，每个员工每年最多 12 条记录';


-- ============================================================
-- 附：表关系速查
-- ============================================================
-- user (1) ──── (N) post              发帖
-- user (1) ──── (N) post_thumb         点赞
-- user (1) ──── (N) post_favour        收藏
-- user (1) ──── (N) employee_salary    员工薪资档案
-- user (1) ──── (N) salary_batch       创建核算批次
-- user (1) ──── (N) salary_change_history  操作调薪
-- user (1) ──── (N) salary_detail      工资条
-- user (1) ──── (N) income_tax_cumulative   个税累计
-- salary_account (1) ──── (N) salary_item     账套包含工资项目
-- salary_account (1) ──── (N) employee_salary  员工挂靠账套
-- salary_batch (1) ──── (N) salary_detail      批次包含明细
