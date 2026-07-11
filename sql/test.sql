

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
    employeeName     varchar(128)                           not null comment '员工名称（真实姓名）',
    userId         bigint                                 null comment '关联用户ID',
    employeeNo         VARCHAR(16)     NOT NULL                 COMMENT '工号, 格式: 年份(4)+部门编码(2)+序号(3)',
    status         tinyint      default 1                 not null comment '状态：0-离职，1-在职，2-试用期',
    gender              TINYINT         NOT NULL DEFAULT 0       COMMENT '性别: 0=女, 1=男',
    idCard             VARCHAR(256)    DEFAULT NULL             COMMENT '身份证号（加密存储）',
    hireDate       datetime                               null comment '入职日期',
    hireType       tinyint                                null comment '入职类型',
    phone   varchar(32)                            null comment '联系人电话',
    departmentId   bigint                                 null comment '部门ID',
    positionId     bigint                                 null comment '职位ID',
    salaryId       bigint                                 null comment '薪资ID',
    employmentType     VARCHAR(16)     NOT NULL                 COMMENT '录用类型: FULL_TIME=全职, PART_TIME=兼职, INTERN=实习',
    email                   varchar(256)                           null comment '邮箱',
    currentAddress          varchar(512)                           null comment '现居住地址',
    emergencyContactName    varchar(128)                           null comment '紧急联系人姓名',
    emergencyContactPhone   varchar(32)                            null comment '紧急联系人电话',
    createTime     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDeleted      tinyint      default 0                 not null comment '是否删除'
) comment '员工' collate = utf8mb4_unicode_ci;

ALTER TABLE employee ADD COLUMN salaryProfileId BIGINT DEFAULT NULL COMMENT '薪资ID';

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


