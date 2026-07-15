-- ============================================================
-- 入职申请表 DDL
-- 数据库: szml1
-- ============================================================
use szml1;

DROP TABLE IF EXISTS hr_onboarding;
CREATE TABLE hr_onboarding (
    id                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name                  VARCHAR(64)     NOT NULL                 COMMENT '候选人姓名',
    gender                TINYINT         NOT NULL DEFAULT 0       COMMENT '性别: 0=女, 1=男',
    mobile                VARCHAR(20)     NOT NULL                 COMMENT '手机号',
    email                 VARCHAR(128)    DEFAULT NULL             COMMENT '邮箱',
    idCard                VARCHAR(256)    DEFAULT NULL             COMMENT '身份证号',
    expectedEntryDate     DATE            NOT NULL                 COMMENT '预计入职日期',
    deptId                BIGINT          NOT NULL                 COMMENT '入职部门ID',
    positionId            BIGINT          NOT NULL                 COMMENT '入职职位ID',
    employmentType        VARCHAR(16)     NOT NULL                 COMMENT '录用类型: FULL_TIME=全职, PART_TIME=兼职, INTERN=实习',
    probationMonths       INT             NOT NULL DEFAULT 3       COMMENT '试用期(月)',
    probationSalaryRatio  DECIMAL(4,2)    NOT NULL DEFAULT 0.80    COMMENT '试用期薪资比例',
    reporterId            BIGINT          DEFAULT NULL             COMMENT '直接汇报人ID',
    status                VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, APPROVING=审批中, APPROVED=已批准待入职, REJECTED=已拒绝, ONBOARDED=已入职',
    createdBy             BIGINT          NOT NULL                 COMMENT '创建人ID（HR员工ID）',
    approvalRecordId      BIGINT          DEFAULT NULL             COMMENT '关联审批实例ID',
    actualEntryDate       DATE            DEFAULT NULL             COMMENT '实际入职日期',
    createdAt             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updatedAt             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_dept_id (deptId),
    KEY idx_created_by (createdBy)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入职申请表';
