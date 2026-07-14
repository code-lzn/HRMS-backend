

# use szml;


# create database szml1;
use szml1;
# role表进行对应的权限校验路博负责
#角色ID关联对应的role表---注意user表仅仅作为账号表进行使用不涉及个人信息
#员工ID关联对应的employee表---个人信息在employee表进行保存
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    roleId       bigint                                 null comment '角色ID',
    employeeId   bigint                                 null comment '员工ID',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除'
) comment '用户' collate = utf8mb4_unicode_ci;


# 角色表进行权限校验路径
create table if not exists role
(
    id          bigint auto_increment comment 'id' primary key,
    roleName    varchar(128)                           not null comment '角色名称',
    roleCode    varchar(128)                           not null comment '角色编码',
    description varchar(512)                           null comment '角色描述',
    dataScope  tinyint  default 5                 not null comment '默认数据范围：1=全量 2=全部员工 3=本部门及下属 4=薪资相关 5=仅本人',
    status      tinyint      default 1                 not null comment '状态：0-禁用，1-启用',
    permissions json                               null comment '权限列表（JSON数组，存储权限编码）',
    fieldPermissions JSON DEFAULT NULL COMMENT '字段权限：{"字段名":{"viewable":[角色ID],"editable":[角色ID],"mask":"脱敏规则"}}',
    createTime  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint      default 0                 not null comment '是否删除',
    unique key uk_roleCode (roleCode)
) comment '角色' collate = utf8mb4_unicode_ci;




# comment '角色表' collate = utf8mb4_unicode_ci;

#employee表
create table  if not exists employee
(
    id             bigint auto_increment comment 'id' primary key,
    employeeName   varchar(128)    not null comment '员工名称（真实姓名）',
    employeeNo     VARCHAR(16)     NOT NULL COMMENT '工号, 格式: 年份(4)+部门编码(2)+序号(3)',
    account        VARCHAR(32)     NULL COMMENT '系统账号（=手机号）',
    userId         bigint          null comment '关联用户ID',
    status         tinyint         default 1 not null comment '在职状态：1=试用期 2=正式 3=待离职 4=已离职',
    gender         TINYINT         NOT NULL DEFAULT 0 COMMENT '性别: 0=女, 1=男',
    phone          varchar(32)     null comment '手机号',
    email          varchar(256)    null comment '邮箱',
    departmentId   bigint          null comment '部门ID',
    positionId     bigint          null comment '职位ID',
    jobLevel       VARCHAR(8)      NULL COMMENT '职级，如P5、M2',
    hireDate       datetime        null comment '入职日期',
    salaryId       bigint          null comment '薪资ID',
    employmentType VARCHAR(16)     NOT NULL COMMENT '录用类型: FULL_TIME=全职, PART_TIME=兼职, INTERN=实习',
    createTime     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDeleted      tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY `uk_employee_no` (`employeeNo`),
    KEY `idx_department_id` (`departmentId`),
    KEY `idx_position_id` (`positionId`),
    KEY `idx_status` (`status`),
    KEY `idx_phone` (`phone`),
    KEY `idx_job_level` (`jobLevel`),
    KEY `idx_hire_date` (`hireDate`)
) comment '员工' collate = utf8mb4_unicode_ci;

#员工详情表----与employee一对一
CREATE TABLE IF NOT EXISTS employee_detail (
    id                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    employeeId            BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    account               VARCHAR(32)       NULL COMMENT '系统账号（=手机号）',
    idCard                VARCHAR(256)      NULL COMMENT '身份证号（加密存储）',
    birthday              DATE              NULL COMMENT '生日',
    registeredAddress     VARCHAR(512)      NULL COMMENT '户籍地址',
    currentAddress        VARCHAR(512)      NULL COMMENT '现居住地址',
    jobLevel              VARCHAR(8)        NULL COMMENT '职级',
    directReportId        BIGINT UNSIGNED   NULL COMMENT '直接汇报人ID',
    workLocation          VARCHAR(128)      NULL COMMENT '工作地点',
    contractType          TINYINT           NULL COMMENT '合同类型：1=固定期限 2=无固定期限 3=劳务合同',
    contractExpireDate    DATE              NULL COMMENT '合同到期日',
    probationRatio        DECIMAL(5,4)      NULL COMMENT '试用期待遇比例',
    baseSalary            DECIMAL(12,2)     NULL COMMENT '基本工资',
    bankAccount           VARCHAR(64)       NULL COMMENT '银行账号（加密存储）',
    bankName              VARCHAR(128)      NULL COMMENT '开户行',
    emergencyContactName  VARCHAR(128)      NULL COMMENT '紧急联系人姓名',
    emergencyContactPhone VARCHAR(32)       NULL COMMENT '紧急联系人电话',
    createTime            DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime            DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_id (employeeId),
    KEY idx_direct_report_id (directReportId)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工详情表';

#员工档案变更日志表
CREATE TABLE IF NOT EXISTS employee_change_log (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    employeeId      BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
    fieldName       VARCHAR(64)     NOT NULL COMMENT '变更字段名',
    oldValue        VARCHAR(512)    NULL COMMENT '变更前值',
    newValue        VARCHAR(512)    NULL COMMENT '变更后值',
    changeType      VARCHAR(32)     NOT NULL COMMENT 'DIRECT_EDIT/FLOW_CHANGE/SYSTEM',
    operatorId      BIGINT UNSIGNED NULL COMMENT '操作人ID',
    remark          VARCHAR(256)    NULL COMMENT '备注',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_employee_id (employeeId),
    KEY idx_create_time (createTime)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工档案变更日志表';

#部门表----与employee进行关联
CREATE TABLE department (
                                id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
                                deptName       VARCHAR(64)     NOT NULL                 COMMENT '部门名称',
                                deptCode       VARCHAR(16)     NOT NULL                 COMMENT '部门编码（2位，用于工号生成）',
                                parentId       BIGINT          DEFAULT NULL             COMMENT '上级部门ID，NULL表示根部门',
                                managerId      BIGINT          DEFAULT NULL             COMMENT '部门负责人ID（关联员工表）',
                                sortOrder      INT             NOT NULL DEFAULT 0       COMMENT '排序序号（越小越靠前）',
                                description     VARCHAR(256)    DEFAULT NULL             COMMENT '部门描述',
                                createdTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                updatedTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                isDeleted               tinyint  default 0                 not null comment '逻辑删除：0=否 1=是',
                                PRIMARY KEY (id),
                                UNIQUE KEY uk_dept_code (deptCode),
                                KEY idx_parent_id (parentId),
                                KEY idx_manager_id (managerId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';


#职位表----与employee进行关联--先不用去管
# 与原来的一样
create table position
(
    id                       bigint unsigned auto_increment comment '主键ID'
        primary key,
    name                     varchar(64)                        not null comment '职位名称，如Java开发工程师',
    sequence                 tinyint                            not null comment '职位序列：1=M管理 2=P专业 3=S支持',
    departmentId            bigint unsigned                    null comment '所属部门ID，空表示全公司通用',
    levelMin                varchar(8)                         not null comment '职级下限，如P1',
    levelMax                varchar(8)                         not null comment '职级上限，如P10',
    defaultProbationMonths int      default 3                 not null comment '默认试用期月数',
    description              varchar(256)                       null comment '职位描述/岗位职责',

    createTime              datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime              datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDeleted               tinyint  default 0                 not null comment '逻辑删除：0=否 1=是'
)
    comment '职位表';

create index idx_department_id
    on position (departmentId);

create index idx_sequence
    on position (sequence);


-- 3.3 员工薪资档案表
DROP TABLE IF EXISTS emp_salary_profile;
CREATE TABLE emp_salary_profile (
                                    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
                                    employeeId             BIGINT          NOT NULL                 COMMENT '员工ID',
                                    accountSetId          BIGINT          DEFAULT NULL             COMMENT '适用薪资账套ID',
                                    baseSalary             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '基本工资',
                                    allowanceBase          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '岗位津贴基数',
                                    performanceBase        DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '绩效奖金基数',
                                    socialInsuranceBase   DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '社保缴纳基数',
                                    housingFundBase       DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '公积金缴纳基数',
                                    probationSalaryRatio  DECIMAL(4,2)    NOT NULL DEFAULT 1.00    COMMENT '试用期薪资比例 (0.80~1.00)',
                                    bankAccount            VARCHAR(256)    DEFAULT NULL             COMMENT '银行账号（加密存储）',
                                    bankName               VARCHAR(128)    DEFAULT NULL             COMMENT '开户行名称',
                                    effectiveDate          DATE            NOT NULL                 COMMENT '生效日期',
                                    createdTIme              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    updatedTime              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    isDeleted               tinyint  default 0                 not null comment '逻辑删除：0=否 1=是',
                                    PRIMARY KEY (id),
                                    UNIQUE KEY uk_employee_id (employeeId),
                                    KEY idx_account_set_id (accountSetId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工薪资档案表';



CREATE TABLE IF NOT EXISTS `department_merge_log` (
                                                      `id`                   BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                      `sourceDeptId`       BIGINT UNSIGNED   NOT NULL COMMENT '源部门ID（被合并）',
                                                      `sourceDeptName`     VARCHAR(64)       NOT NULL COMMENT '源部门名称（快照）',
                                                      `targetDeptId`       BIGINT UNSIGNED   NOT NULL COMMENT '目标部门ID（保留）',
                                                      `targetDeptName`     VARCHAR(64)       NOT NULL COMMENT '目标部门名称（快照）',
                                                      `transferredEmployees` INT              NOT NULL DEFAULT 0 COMMENT '转移员工数',
                                                      `operatorId`          BIGINT UNSIGNED   NOT NULL COMMENT '操作人ID',
                                                      `createTime`          DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                      PRIMARY KEY (`id`),
                                                      KEY `idx_source_dept_id` (`targetDeptId`),
                                                      KEY `idx_target_dept_id` (`targetDeptName`),
                                                      KEY `idx_create_time` (`createTime`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '部门合并日志表';


-- ============== 考勤模块 ==============

-- 考勤打卡记录表
CREATE TABLE IF NOT EXISTS attendance (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
    userId          BIGINT          NOT NULL                 COMMENT '用户ID',
    attendanceDate  DATE            NOT NULL                 COMMENT '考勤日期',
    punchInTime     DATETIME        DEFAULT NULL             COMMENT '上班打卡时间',
    punchOutTime    DATETIME        DEFAULT NULL             COMMENT '下班打卡时间',
    status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=正常 1=迟到 2=早退 3=缺卡 4=请假 5=旷工',
    punchInType     TINYINT         DEFAULT NULL             COMMENT '上班打卡方式：0=网页 1=APP',
    punchOutType    TINYINT         DEFAULT NULL             COMMENT '下班打卡方式：0=网页 1=APP',
    punchInLocation VARCHAR(256)    DEFAULT NULL             COMMENT '上班打卡位置',
    punchOutLocation VARCHAR(256)   DEFAULT NULL             COMMENT '下班打卡位置',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDeleted               tinyint  default 0                 not null comment '逻辑删除：0=否 1=是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_date (employeeId, attendanceDate),
    KEY idx_user_id (userId),
    KEY idx_attendance_date (attendanceDate),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤打卡记录表';


-- 请假申请表
CREATE TABLE IF NOT EXISTS `leave_record`(
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
    userId          BIGINT          NOT NULL                 COMMENT '用户ID',
    leaveType       TINYINT         NOT NULL                 COMMENT '请假类型：0=事假 1=病假 2=年假 3=婚假 4=产假 5=丧假 6=调休',
    startDate       DATE            NOT NULL                 COMMENT '开始日期',
    endDate         DATE            NOT NULL                 COMMENT '结束日期',
    totalDays       DECIMAL(4,1)    NOT NULL                 COMMENT '请假总天数',
    reason          VARCHAR(512)    NOT NULL                 COMMENT '请假原因',
    status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=待审批 1=已通过 2=已拒绝 3=已撤销',
    approverId      BIGINT          DEFAULT NULL             COMMENT '审批人ID',
    approveTime     DATETIME        DEFAULT NULL             COMMENT '审批时间',
    approveComment  VARCHAR(512)    DEFAULT NULL             COMMENT '审批意见',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDeleted               tinyint  default 0                 not null comment '逻辑删除：0=否 1=是',
    PRIMARY KEY (id),
    KEY idx_employee_id (employeeId),
    KEY idx_user_id (userId),
    KEY idx_status (status),
    KEY idx_start_date (startDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';


-- 补卡申请表
CREATE TABLE IF NOT EXISTS makeup_punch (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
    userId          BIGINT          NOT NULL                 COMMENT '用户ID',
    punchDate       DATE            NOT NULL                 COMMENT '补卡日期',
    punchType       TINYINT         NOT NULL                 COMMENT '补卡类型：0=上班补卡 1=下班补卡',
    punchTime       DATETIME        NOT NULL                 COMMENT '实际到岗/离岗时间',
    reason          VARCHAR(512)    NOT NULL                 COMMENT '缺卡原因',
    status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=待审批 1=已通过 2=已拒绝',
    approverId      BIGINT          DEFAULT NULL             COMMENT '审批人ID',
    approveTime     DATETIME        DEFAULT NULL             COMMENT '审批时间',
    approveComment  VARCHAR(512)    DEFAULT NULL             COMMENT '审批意见',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDeleted               tinyint  default 0                 not null comment '逻辑删除：0=否 1=是',
    PRIMARY KEY (id),
    KEY idx_employee_id (employeeId),
    KEY idx_user_id (userId),
    KEY idx_status (status),
    KEY idx_punch_date (punchDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';


-- ============== 薪资模块 ==============

-- 薪资核算批次表
CREATE TABLE IF NOT EXISTS sal_batch (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batchNo                 VARCHAR(32)     NOT NULL                 COMMENT '批次号',
    salaryMonth             VARCHAR(7)      NOT NULL                 COMMENT '薪资月份: YYYY-MM',
    status                  VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, PENDING_CONFIRM=待确认, APPROVING=审批中, APPROVED=已通过, PAID=已发放',
    totalEmployeeCount      INT             NOT NULL DEFAULT 0       COMMENT '核算员工总数',
    totalGross              DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '应发工资总额',
    totalDeduction          DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '扣除总额',
    totalNet                DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '实发工资总额',
    createdBy               BIGINT          NOT NULL                 COMMENT '创建人ID',
    approvedBy              BIGINT          DEFAULT NULL             COMMENT '审批人ID',
    paidAt                  DATETIME        DEFAULT NULL             COMMENT '实际发放时间',
    createdAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updatedAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batchNo),
    UNIQUE KEY uk_salary_month (salaryMonth),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算批次表';

-- 薪资核算明细表（工资条）
CREATE TABLE IF NOT EXISTS sal_batch_detail (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batchId                 BIGINT          NOT NULL                 COMMENT '批次ID',
    employeeId              BIGINT          NOT NULL                 COMMENT '员工ID',
    baseSalary              DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '基本工资',
    allowance               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '岗位津贴',
    performanceBonus        DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '绩效奖金',
    overtimePay             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '加班费',
    lateDeduction           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '迟到扣款',
    leaveDeduction          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '请假扣款',
    socialPension           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '养老保险',
    socialMedical           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '医疗保险',
    socialUnemployment      DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '失业保险',
    housingFund             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '住房公积金',
    incomeTax               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '个人所得税',
    grossSalary             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '应发工资',
    totalDeduction          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '应扣合计',
    netSalary               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '实发工资',
    hasAnomaly              TINYINT         NOT NULL DEFAULT 0       COMMENT '是否有异常: 0=正常, 1=预警, 2=阻断',
    anomalyReason           VARCHAR(256)    DEFAULT NULL             COMMENT '异常说明',
    manualAdjust            DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '手动调整金额',
    adjustReason            VARCHAR(256)    DEFAULT NULL             COMMENT '手动调整原因',
    createdAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updatedAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_employee (batchId, employeeId),
    KEY idx_employee_id (employeeId),
    KEY idx_batch_id (batchId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算明细表（工资条）';


-- 考勤打卡记录表
CREATE TABLE IF NOT EXISTS attendance (
                                                 id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
                                                 employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
                                                 userId          BIGINT          NOT NULL                 COMMENT '用户ID',
                                                 attendanceDate  DATE            NOT NULL                 COMMENT '考勤日期',
                                                 punchInTime     DATETIME        DEFAULT NULL             COMMENT '上班打卡时间',
                                                 punchOutTime    DATETIME        DEFAULT NULL             COMMENT '下班打卡时间',
                                                 status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=正常 1=迟到 2=早退 3=缺卡 4=请假 5=旷工',
                                                 punchInType     TINYINT         DEFAULT NULL             COMMENT '上班打卡方式：0=网页 1=APP',
                                                 punchOutType    TINYINT         DEFAULT NULL             COMMENT '下班打卡方式：0=网页 1=APP',
                                                 punchInLocation VARCHAR(256)    DEFAULT NULL             COMMENT '上班打卡位置',
                                                 punchOutLocation VARCHAR(256)   DEFAULT NULL             COMMENT '下班打卡位置',
                                                 remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
                                                 createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                 updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                 isDeleted       TINYINT         DEFAULT 0 NOT NULL       COMMENT '逻辑删除：0=否 1=是',
                                                 PRIMARY KEY (id),
                                                 UNIQUE KEY uk_employee_date (employeeId, attendanceDate),
                                                 KEY idx_user_id (userId),
                                                 KEY idx_attendance_date (attendanceDate),
                                                 KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤打卡记录表';


-- 请假申请表
CREATE TABLE IF NOT EXISTS leave_request (
                                             id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
                                             employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
                                             userId          BIGINT          NOT NULL                 COMMENT '用户ID',
                                             leaveType       TINYINT         NOT NULL                 COMMENT '请假类型：0=事假 1=病假 2=年假 3=婚假 4=产假 5=丧假 6=调休',
                                             startDate       DATE            NOT NULL                 COMMENT '开始日期',
                                             endDate         DATE            NOT NULL                 COMMENT '结束日期',
                                             totalDays       DECIMAL(4,1)    NOT NULL                 COMMENT '请假总天数',
                                             reason          VARCHAR(512)    NOT NULL                 COMMENT '请假原因',
                                             status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=待审批 1=已通过 2=已拒绝 3=已撤销',
                                             approverId      BIGINT          DEFAULT NULL             COMMENT '审批人ID',
                                             approveTime     DATETIME        DEFAULT NULL             COMMENT '审批时间',
                                             approveComment  VARCHAR(512)    DEFAULT NULL             COMMENT '审批意见',
                                             createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             isDeleted       TINYINT         DEFAULT 0 NOT NULL       COMMENT '逻辑删除：0=否 1=是',
                                             PRIMARY KEY (id),
                                             KEY idx_employee_id (employeeId),
                                             KEY idx_user_id (userId),
                                             KEY idx_status (status),
                                             KEY idx_start_date (startDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';




-- 补卡申请表
CREATE TABLE IF NOT EXISTS makeup_punch (
                                            id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
                                            employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
                                            userId          BIGINT          NOT NULL                 COMMENT '用户ID',
                                            punchDate       DATE            NOT NULL                 COMMENT '补卡日期',
                                            punchType       TINYINT         NOT NULL                 COMMENT '补卡类型：0=上班补卡 1=下班补卡',
                                            punchTime       DATETIME        NOT NULL                 COMMENT '实际到岗/离岗时间',
                                            reason          VARCHAR(512)    NOT NULL                 COMMENT '缺卡原因',
                                            status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=待审批 1=已通过 2=已拒绝',
                                            approverId      BIGINT          DEFAULT NULL             COMMENT '审批人ID',
                                            approveTime     DATETIME        DEFAULT NULL             COMMENT '审批时间',
                                            approveComment  VARCHAR(512)    DEFAULT NULL             COMMENT '审批意见',
                                            createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            isDeleted       TINYINT         DEFAULT 0 NOT NULL       COMMENT '逻辑删除：0=否 1=是',
                                            PRIMARY KEY (id),
                                            KEY idx_employee_id (employeeId),
                                            KEY idx_user_id (userId),
                                            KEY idx_status (status),
                                            KEY idx_punch_date (punchDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';


-- ============== 薪资模块 ==============

-- 薪资核算批次表
CREATE TABLE IF NOT EXISTS sal_batch (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batchNo                 VARCHAR(32)     NOT NULL                 COMMENT '批次号',
    salaryMonth             VARCHAR(7)      NOT NULL                 COMMENT '薪资月份: YYYY-MM',
    status                  VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, PENDING_CONFIRM=待确认, APPROVING=审批中, APPROVED=已通过, PAID=已发放',
    totalEmployeeCount      INT             NOT NULL DEFAULT 0       COMMENT '核算员工总数',
    totalGross              DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '应发工资总额',
    totalDeduction          DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '扣除总额',
    totalNet                DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '实发工资总额',
    createdBy               BIGINT          NOT NULL                 COMMENT '创建人ID',
    approvedBy              BIGINT          DEFAULT NULL             COMMENT '审批人ID',
    paidAt                  DATETIME        DEFAULT NULL             COMMENT '实际发放时间',
    createdAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updatedAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batchNo),
    UNIQUE KEY uk_salary_month (salaryMonth),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算批次表';

-- 薪资核算明细表（工资条）
CREATE TABLE IF NOT EXISTS sal_batch_detail (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batchId                 BIGINT          NOT NULL                 COMMENT '批次ID',
    employeeId              BIGINT          NOT NULL                 COMMENT '员工ID',
    baseSalary              DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '基本工资',
    allowance               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '岗位津贴',
    performanceBonus        DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '绩效奖金',
    overtimePay             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '加班费',
    lateDeduction           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '迟到扣款',
    leaveDeduction          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '请假扣款',
    socialPension           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '养老保险',
    socialMedical           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '医疗保险',
    socialUnemployment      DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '失业保险',
    housingFund             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '住房公积金',
    incomeTax               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '个人所得税',
    grossSalary             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '应发工资',
    totalDeduction          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '应扣合计',
    netSalary               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '实发工资',
    hasAnomaly              TINYINT         NOT NULL DEFAULT 0       COMMENT '是否有异常: 0=正常, 1=预警, 2=阻断',
    anomalyReason           VARCHAR(256)    DEFAULT NULL             COMMENT '异常说明',
    manualAdjust            DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '手动调整金额',
    adjustReason            VARCHAR(256)    DEFAULT NULL             COMMENT '手动调整原因',
    createdAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updatedAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_employee (batchId, employeeId),
    KEY idx_employee_id (employeeId),
    KEY idx_batch_id (batchId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算明细表（工资条）';


-- ============== 薪资测试数据（userId=2075829151662010370, employeeId=15） ==============

-- 2026年1月~6月薪资批次
INSERT INTO sal_batch (id, batchNo, salaryMonth, status, totalEmployeeCount, totalGross, totalDeduction, totalNet, createdBy, paidAt) VALUES
(1, 'B202601', '2026-01', 'PAID', 1, 15000.00, 2870.00, 12130.00, 1, '2026-02-10 10:00:00'),
(2, 'B202602', '2026-02', 'PAID', 1, 15200.00, 2890.00, 12310.00, 1, '2026-03-10 10:00:00'),
(3, 'B202603', '2026-03', 'PAID', 1, 15500.00, 2930.00, 12570.00, 1, '2026-04-10 10:00:00'),
(4, 'B202604', '2026-04', 'PAID', 1, 15500.00, 2930.00, 12570.00, 1, '2026-05-10 10:00:00'),
(5, 'B202605', '2026-05', 'PAID', 1, 15800.00, 2975.00, 12825.00, 1, '2026-06-10 10:00:00'),
(6, 'B202606', '2026-06', 'PAID', 1, 16000.00, 3000.00, 13000.00, 1, '2026-07-10 10:00:00');

-- 员工15的6个月工资条明细
-- 月薪约15000，社保+公积金约2870，个税按累计预扣
INSERT INTO sal_batch_detail (batchId, employeeId, baseSalary, allowance, performanceBonus, overtimePay,
                              lateDeduction, leaveDeduction, socialPension, socialMedical, socialUnemployment,
                              housingFund, incomeTax, grossSalary, totalDeduction, netSalary, hasAnomaly) VALUES
(1, 15, 10000.00, 2500.00, 2000.00, 500.00, 0.00, 0.00, 800.00, 200.00, 50.00, 1800.00, 20.00, 15000.00, 2870.00, 12130.00, 0),
(2, 15, 10000.00, 2500.00, 2200.00, 500.00, 0.00, 0.00, 800.00, 200.00, 50.00, 1800.00, 40.00, 15200.00, 2890.00, 12310.00, 0),
(3, 15, 10000.00, 2500.00, 2500.00, 500.00, 0.00, 0.00, 800.00, 200.00, 50.00, 1800.00, 80.00, 15500.00, 2930.00, 12570.00, 0),
(4, 15, 10000.00, 2500.00, 2500.00, 500.00, 50.00, 0.00, 800.00, 200.00, 50.00, 1800.00, 80.00, 15500.00, 2980.00, 12520.00, 1),
(5, 15, 10500.00, 2500.00, 2300.00, 500.00, 0.00, 0.00, 840.00, 210.00, 52.50, 1800.00, 72.50, 15800.00, 2975.00, 12825.00, 0),
(6, 15, 10500.00, 2500.00, 2500.00, 500.00, 0.00, 0.00, 840.00, 210.00, 52.50, 1800.00, 97.50, 16000.00, 3000.00, 13000.00, 0);


-- ============== 账号安全模块 ==============

-- 登录日志表
CREATE TABLE IF NOT EXISTS login_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    userId              BIGINT          NOT NULL                COMMENT '用户ID',
    loginTime           DATETIME        NOT NULL                COMMENT '登录时间',
    ip                  VARCHAR(45)     NOT NULL                COMMENT '登录IP',
    device              VARCHAR(256)    DEFAULT NULL            COMMENT '设备信息（User-Agent）',
    loginType           TINYINT         NOT NULL                COMMENT '登录方式：1=密码登录 2=短信验证码登录',
    isSuccess           TINYINT         NOT NULL DEFAULT 1      COMMENT '是否成功：0=失败 1=成功',
    failReason          VARCHAR(128)    DEFAULT NULL            COMMENT '失败原因',
    PRIMARY KEY (id),
    KEY idx_user_id (userId),
    KEY idx_login_time (loginTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- 密码历史表（每个用户仅保留最近3次旧密码）
CREATE TABLE IF NOT EXISTS password_history (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    userId              BIGINT          NOT NULL                COMMENT '用户ID',
    passwordHash        VARCHAR(128)    NOT NULL                COMMENT '历史密码哈希',
    createTime          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id_time (userId, createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码历史表';


-- ============== 账号安全测试数据（userId=2075829151662010370, employeeId=15） ==============

-- 给员工绑定手机号（employee 表已有 phone 字段）
UPDATE employee SET phone = '13800138000' WHERE id = 15;

-- 近3条密码历史
INSERT INTO password_history (userId, passwordHash, createTime) VALUES
(2075829151662010370, 'e10adc3949ba59abbe56e057f20f883e', '2025-12-15 09:00:00'),
(2075829151662010370, '5d93ceb70e7bf5f408f0a8e5c4d5e5e3', '2026-03-20 14:30:00'),
(2075829151662010370, '25d55ad283aa400af464c76d713c07ad', '2026-06-01 08:15:00');

-- 近30条登录日志（覆盖3个月）
INSERT INTO login_log (userId, loginTime, ip, device, loginType, isSuccess, failReason) VALUES
(2075829151662010370, '2026-04-01 08:55:00', '192.168.1.100', 'Mozilla/5.0 Chrome/120 Windows', 1, 1, NULL),
(2075829151662010370, '2026-04-03 09:02:00', '192.168.1.100', 'Mozilla/5.0 Chrome/120 Windows', 1, 1, NULL),
(2075829151662010370, '2026-04-07 08:48:00', '192.168.1.100', 'Mozilla/5.0 Chrome/120 Windows', 1, 1, NULL),
(2075829151662010370, '2026-04-10 09:15:00', '10.0.0.55', 'Mozilla/5.0 Safari/17 iPhone', 1, 1, NULL),
(2075829151662010370, '2026-04-12 14:20:00', '192.168.1.100', 'Mozilla/5.0 Chrome/120 Windows', 1, 0, '密码错误'),
(2075829151662010370, '2026-04-12 14:21:00', '192.168.1.100', 'Mozilla/5.0 Chrome/120 Windows', 1, 1, NULL),
(2075829151662010370, '2026-04-15 08:50:00', '192.168.1.100', 'Mozilla/5.0 Chrome/120 Windows', 1, 1, NULL),
(2075829151662010370, '2026-04-20 09:05:00', '113.87.23.45', 'Mozilla/5.0 Edge/120 Mac', 1, 1, NULL),
(2075829151662010370, '2026-04-25 08:58:00', '192.168.1.100', 'Mozilla/5.0 Chrome/121 Windows', 1, 1, NULL),
(2075829151662010370, '2026-04-28 08:45:00', '192.168.1.100', 'Mozilla/5.0 Chrome/121 Windows', 1, 1, NULL),
(2075829151662010370, '2026-05-04 09:10:00', '192.168.1.100', 'Mozilla/5.0 Chrome/121 Windows', 1, 1, NULL),
(2075829151662010370, '2026-05-08 08:52:00', '192.168.1.100', 'Mozilla/5.0 Chrome/121 Windows', 1, 1, NULL),
(2075829151662010370, '2026-05-12 09:00:00', '192.168.1.100', 'Mozilla/5.0 Chrome/121 Windows', 1, 1, NULL),
(2075829151662010370, '2026-05-15 08:47:00', '10.0.0.55', 'Mozilla/5.0 Safari/17 iPhone', 2, 0, '验证码已过期'),
(2075829151662010370, '2026-05-15 08:48:00', '10.0.0.55', 'Mozilla/5.0 Safari/17 iPhone', 2, 1, NULL),
(2075829151662010370, '2026-05-20 08:55:00', '192.168.1.100', 'Mozilla/5.0 Chrome/121 Windows', 1, 1, NULL),
(2075829151662010370, '2026-05-25 14:30:00', '114.25.66.78', 'Mozilla/5.0 Chrome/121 Android', 1, 0, '密码错误'),
(2075829151662010370, '2026-05-25 14:32:00', '114.25.66.78', 'Mozilla/5.0 Chrome/121 Android', 1, 1, NULL),
(2075829151662010370, '2026-05-28 08:50:00', '192.168.1.100', 'Mozilla/5.0 Chrome/121 Windows', 1, 1, NULL),
(2075829151662010370, '2026-06-01 09:03:00', '192.168.1.100', 'Mozilla/5.0 Chrome/122 Windows', 1, 1, NULL),
(2075829151662010370, '2026-06-05 08:56:00', '192.168.1.100', 'Mozilla/5.0 Chrome/122 Windows', 1, 1, NULL),
(2075829151662010370, '2026-06-10 09:08:00', '192.168.1.100', 'Mozilla/5.0 Chrome/122 Windows', 1, 1, NULL),
(2075829151662010370, '2026-06-15 08:50:00', '10.0.0.55', 'Mozilla/5.0 Safari/17 iPhone', 1, 1, NULL),
(2075829151662010370, '2026-06-20 08:53:00', '192.168.1.100', 'Mozilla/5.0 Chrome/122 Windows', 1, 0, '密码错误'),
(2075829151662010370, '2026-06-20 08:54:00', '192.168.1.100', 'Mozilla/5.0 Chrome/122 Windows', 1, 1, NULL),
(2075829151662010370, '2026-06-25 08:58:00', '192.168.1.100', 'Mozilla/5.0 Chrome/122 Windows', 1, 1, NULL),
(2075829151662010370, '2026-07-01 09:01:00', '192.168.1.100', 'Mozilla/5.0 Chrome/123 Windows', 1, 1, NULL),
(2075829151662010370, '2026-07-05 08:49:00', '192.168.1.100', 'Mozilla/5.0 Chrome/123 Windows', 1, 1, NULL),
(2075829151662010370, '2026-07-08 08:55:00', '10.0.0.55', 'Mozilla/5.0 Safari/17 iPhone', 1, 1, NULL),
(2075829151662010370, '2026-07-10 09:02:00', '192.168.1.100', 'Mozilla/5.0 Chrome/123 Windows', 1, 1, NULL);


-- ============== 审批中心模块 ==============

-- 审批流定义表
CREATE TABLE IF NOT EXISTS approval_flow (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    businessType    VARCHAR(32)     NOT NULL                 COMMENT '业务类型: ONBOARDING=入职, REGULARIZATION=转正, TRANSFER=调岗, RESIGNATION=离职, LEAVE=请假, PATCH_CLOCK=补卡, SALARY_BATCH=薪资批次',
    flowName        VARCHAR(64)     NOT NULL                 COMMENT '审批流名称',
    description     VARCHAR(256)    DEFAULT NULL             COMMENT '说明',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 1=启用, 0=禁用',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_type (businessType)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流定义表';

-- 审批节点定义表
CREATE TABLE IF NOT EXISTS approval_flow_node (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    flowId          BIGINT          NOT NULL                 COMMENT '审批流ID',
    nodeName        VARCHAR(64)     NOT NULL                 COMMENT '节点名称, 如"部门负责人审批"',
    nodeOrder       INT             NOT NULL                 COMMENT '节点顺序, 从1开始',
    approverType    VARCHAR(16)     NOT NULL                 COMMENT '审批人类型: DEPT_MANAGER=部门负责人, HR_MANAGER=HR负责人, DIRECT_SUPERIOR=直接上级, FINANCE=财务专员, BOSS=老板, SPECIFIED=指定人',
    approverId      BIGINT          DEFAULT NULL             COMMENT '指定审批人ID（approverType=SPECIFIED时使用）',
    isOptional      TINYINT         NOT NULL DEFAULT 0       COMMENT '是否可选: 0=必选, 1=可选',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_flow_id (flowId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点定义表';

-- 审批实例表（一次业务申请的审批记录）
CREATE TABLE IF NOT EXISTS approval_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    flowId          BIGINT          NOT NULL                 COMMENT '审批流定义ID',
    businessType    VARCHAR(32)     NOT NULL                 COMMENT '业务类型',
    businessId      BIGINT          NOT NULL                 COMMENT '关联业务表记录ID',
    applicantId     BIGINT          NOT NULL                 COMMENT '申请人ID（employeeId）',
    applicantName   VARCHAR(64)     DEFAULT NULL             COMMENT '申请人姓名（冗余，便于列表展示）',
    currentStep     INT             NOT NULL DEFAULT 1       COMMENT '当前审批步骤',
    totalSteps      INT             NOT NULL                 COMMENT '总步骤数',
    status          VARCHAR(16)     NOT NULL DEFAULT 'APPROVING' COMMENT '审批状态: APPROVING=审批中, APPROVED=已通过, REJECTED=已拒绝, WITHDRAWN=已撤回',
    finishedAt      DATETIME        DEFAULT NULL             COMMENT '审批完成时间',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
    updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_business (businessType, businessId),
    KEY idx_applicant_id (applicantId),
    KEY idx_status (status),
    KEY idx_current_step (currentStep)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批实例表';

-- 审批明细表（每个节点的审批记录）
CREATE TABLE IF NOT EXISTS approval_detail (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    recordId        BIGINT          NOT NULL                 COMMENT '审批实例ID',
    nodeId          BIGINT          NOT NULL                 COMMENT '审批节点定义ID',
    nodeName        VARCHAR(64)     NOT NULL                 COMMENT '节点名称（快照）',
    stepOrder       INT             NOT NULL                 COMMENT '步骤序号',
    approverId      BIGINT          DEFAULT NULL             COMMENT '审批人ID',
    approverName    VARCHAR(64)     DEFAULT NULL             COMMENT '审批人姓名（冗余）',
    action          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '审批动作: PENDING=待审批, APPROVE=通过, REJECT=拒绝, TRANSFER=转交',
    comment         TEXT            DEFAULT NULL             COMMENT '审批意见',
    isDelegated     TINYINT         NOT NULL DEFAULT 0       COMMENT '是否代审批: 0=否, 1=是',
    delegatedBy     BIGINT          DEFAULT NULL             COMMENT '委托人ID（代审批时记录）',
    operateTime     DATETIME        DEFAULT NULL             COMMENT '操作时间',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_record_id (recordId),
    KEY idx_approver_id (approverId),
    KEY idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批明细表';

-- 审批委托表
CREATE TABLE IF NOT EXISTS approval_delegation (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    delegatorId     BIGINT          NOT NULL                 COMMENT '委托人ID（employeeId）',
    delegatorName   VARCHAR(64)     NOT NULL                 COMMENT '委托人姓名（冗余）',
    delegateId      BIGINT          NOT NULL                 COMMENT '被委托人ID（employeeId）',
    delegateName    VARCHAR(64)     NOT NULL                 COMMENT '被委托人姓名（冗余）',
    businessTypes   VARCHAR(256)    DEFAULT NULL             COMMENT '委托业务类型（逗号分隔，NULL=全部）',
    startDate       DATE            NOT NULL                 COMMENT '委托开始日期',
    endDate         DATE            NOT NULL                 COMMENT '委托结束日期',
    status          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态: 1=有效, 0=已取消',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_delegator_id (delegatorId),
    KEY idx_delegate_id (delegateId),
    KEY idx_status_dates (status, startDate, endDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批委托表';


-- ============== 审批中心初始化数据 ==============

-- 7种审批流定义
INSERT INTO approval_flow (id, businessType, flowName, description) VALUES
(1, 'ONBOARDING',    '入职审批',   'HR发起 → 部门负责人 → HR负责人'),
(2, 'REGULARIZATION','转正审批',   'HR发起 → 部门负责人 → HR负责人'),
(3, 'TRANSFER',      '调岗审批',   'HR发起 → 原部门负责人 → 新部门负责人 → HR负责人'),
(4, 'RESIGNATION',   '离职审批',   'HR发起 → 部门负责人 → HR负责人'),
(5, 'LEAVE',         '请假审批',   '员工发起 → 直接上级'),
(6, 'PATCH_CLOCK',   '补卡审批',   '员工发起 → 直接上级'),
(7, 'SALARY_BATCH',  '薪资批次审批','HR发起 → 财务专员 → 老板');

-- 各审批流节点定义
-- 入职审批: 部门负责人 → HR负责人
INSERT INTO approval_flow_node (flowId, nodeName, nodeOrder, approverType, isOptional) VALUES
(1, '部门负责人', 1, 'DEPT_MANAGER', 0),
(1, 'HR负责人',   2, 'HR_MANAGER',   1);

-- 转正审批: 部门负责人 → HR负责人
INSERT INTO approval_flow_node (flowId, nodeName, nodeOrder, approverType, isOptional) VALUES
(2, '部门负责人', 1, 'DEPT_MANAGER', 0),
(2, 'HR负责人',   2, 'HR_MANAGER',   0);

-- 调岗审批: 原部门负责人 → 新部门负责人 → HR负责人
INSERT INTO approval_flow_node (flowId, nodeName, nodeOrder, approverType, isOptional) VALUES
(3, '原部门负责人', 1, 'DEPT_MANAGER', 0),
(3, '新部门负责人', 2, 'DEPT_MANAGER', 0),
(3, 'HR负责人',    3, 'HR_MANAGER',   0);

-- 离职审批: 部门负责人 → HR负责人
INSERT INTO approval_flow_node (flowId, nodeName, nodeOrder, approverType, isOptional) VALUES
(4, '部门负责人', 1, 'DEPT_MANAGER', 0),
(4, 'HR负责人',   2, 'HR_MANAGER',   0);

-- 请假审批: 直接上级
INSERT INTO approval_flow_node (flowId, nodeName, nodeOrder, approverType, isOptional) VALUES
(5, '直接上级', 1, 'DIRECT_SUPERIOR', 0);

-- 补卡审批: 直接上级
INSERT INTO approval_flow_node (flowId, nodeName, nodeOrder, approverType, isOptional) VALUES
(6, '直接上级', 1, 'DIRECT_SUPERIOR', 0);

-- 薪资批次审批: 财务专员 → 老板
INSERT INTO approval_flow_node (flowId, nodeName, nodeOrder, approverType, isOptional) VALUES
(7, '财务专员', 1, 'FINANCE', 0),
(7, '老板',    2, 'BOSS',    1);


-- ============== 审批中心测试数据（userId=2075829151662010370, employeeId=15） ==============

-- 假设员工15是部门负责人/直接上级，创建几条待审批的测试数据
-- 记录1: 请假审批（员工15提交的请假，待员工15的直接上级审批——此处假设员工2是直接上级）
INSERT INTO approval_record (id, flowId, businessType, businessId, applicantId, applicantName, currentStep, totalSteps, status) VALUES
(1, 5, 'LEAVE',       100, 15, '张三', 1, 1, 'APPROVING');

INSERT INTO approval_detail (recordId, nodeId, nodeName, stepOrder, approverId, approverName, action) VALUES
(1, (SELECT id FROM approval_flow_node WHERE flowId=5 AND nodeOrder=1), '直接上级', 1, 2, '李四', 'PENDING');

-- 记录2: 补卡审批（员工15提交的补卡，待审批）
INSERT INTO approval_record (id, flowId, businessType, businessId, applicantId, applicantName, currentStep, totalSteps, status) VALUES
(2, 6, 'PATCH_CLOCK', 101, 15, '张三', 1, 1, 'APPROVING');

INSERT INTO approval_detail (recordId, nodeId, nodeName, stepOrder, approverId, approverName, action) VALUES
(2, (SELECT id FROM approval_flow_node WHERE flowId=6 AND nodeOrder=1), '直接上级', 1, 2, '李四', 'PENDING');

-- 记录3: 已完成的转正审批（员工8→部门负责人2已通过→HR负责人3已通过）
INSERT INTO approval_record (id, flowId, businessType, businessId, applicantId, applicantName, currentStep, totalSteps, status, finishedAt) VALUES
(3, 2, 'REGULARIZATION', 200, 8, '王五', 2, 2, 'APPROVED', '2026-07-05 16:00:00');

INSERT INTO approval_detail (recordId, nodeId, nodeName, stepOrder, approverId, approverName, action, comment, operateTime) VALUES
(3, (SELECT id FROM approval_flow_node WHERE flowId=2 AND nodeOrder=1), '部门负责人', 1, 2, '李四', 'APPROVE', '表现优秀，同意转正', '2026-07-05 10:00:00'),
(3, (SELECT id FROM approval_flow_node WHERE flowId=2 AND nodeOrder=2), 'HR负责人',   2, 3, '赵六', 'APPROVE', '同意',             '2026-07-05 16:00:00');

-- 记录4: 已拒绝的离职审批（到HR负责人节点被拒）
INSERT INTO approval_record (id, flowId, businessType, businessId, applicantId, applicantName, currentStep, totalSteps, status, finishedAt) VALUES
(4, 4, 'RESIGNATION', 300, 10, '钱七', 2, 2, 'REJECTED', '2026-07-03 14:00:00');

INSERT INTO approval_detail (recordId, nodeId, nodeName, stepOrder, approverId, approverName, action, comment, operateTime) VALUES
(4, (SELECT id FROM approval_flow_node WHERE flowId=4 AND nodeOrder=1), '部门负责人', 1, 2, '李四', 'APPROVE', '确认',          '2026-07-03 09:00:00'),
(4, (SELECT id FROM approval_flow_node WHERE flowId=4 AND nodeOrder=2), 'HR负责人',   2, 3, '赵六', 'REJECT', '暂不批准离职', '2026-07-03 14:00:00');

-- 记录5: 审批中的薪资批次（财务专员已通过，待老板审批）
INSERT INTO approval_record (id, flowId, businessType, businessId, applicantId, applicantName, currentStep, totalSteps, status) VALUES
(5, 7, 'SALARY_BATCH', 400, 3, '赵六', 2, 2, 'APPROVING');

INSERT INTO approval_detail (recordId, nodeId, nodeName, stepOrder, approverId, approverName, action, comment, operateTime) VALUES
(5, (SELECT id FROM approval_flow_node WHERE flowId=7 AND nodeOrder=1), '财务专员', 1, 4, '孙八', 'APPROVE', '薪资核算无误', '2026-07-10 09:00:00'),
(5, (SELECT id FROM approval_flow_node WHERE flowId=7 AND nodeOrder=2), '老板',     2, 1, '周董', 'PENDING', NULL,           NULL);

-- 审批委托测试数据: 员工2（李四）委托员工15（张三）在2026-07-01~2026-07-31期间审批请假和补卡
INSERT INTO approval_delegation (delegatorId, delegatorName, delegateId, delegateName, businessTypes, startDate, endDate, status) VALUES
(2, '李四', 15, '张三', 'LEAVE,PATCH_CLOCK', '2026-07-01', '2026-07-31', 1);