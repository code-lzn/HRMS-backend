-- ============================================================
-- 入职申请表 + 审批流初始化
-- 数据库: szml1
-- 使用前务必执行以下两条 SET 语句：
-- ============================================================
use szml1;

-- ============================================================
-- 1. 建表
-- ============================================================
DROP TABLE IF EXISTS emp_onboarding;
CREATE TABLE emp_onboarding (
    id                    BIGINT          NOT NULL AUTO_INCREMENT,
    businessNo            VARCHAR(32)     DEFAULT NULL,
    flowId                BIGINT          DEFAULT NULL,
    recordId              BIGINT          DEFAULT NULL,
    deptId                BIGINT          NOT NULL,
    positionId            BIGINT          NOT NULL,
    hireDate              DATE            NOT NULL,
    probationMonth        INT             NOT NULL DEFAULT 3,
    employmentType        VARCHAR(16)     NOT NULL,
    contractType          TINYINT         DEFAULT NULL,
    contractExpireDate    DATE            DEFAULT NULL,
    baseSalary            DECIMAL(12,2)   DEFAULT NULL,
    socialInsuranceBase   DECIMAL(12,2)   DEFAULT NULL,
    housingFundBase       DECIMAL(12,2)   DEFAULT NULL,
    bankAccount           VARCHAR(64)     DEFAULT NULL,
    bankName              VARCHAR(128)    DEFAULT NULL,
    candidateName         VARCHAR(64)     NOT NULL,
    phone                 VARCHAR(20)     NOT NULL,
    idCard                VARCHAR(256)    DEFAULT NULL,
    email                 VARCHAR(128)    DEFAULT NULL,
    emergencyContactName  VARCHAR(64)     DEFAULT NULL,
    emergencyContactPhone VARCHAR(20)     DEFAULT NULL,
    employeeId            BIGINT          DEFAULT NULL,
    operatorId            BIGINT          NOT NULL,
    approverId            BIGINT          DEFAULT NULL COMMENT '审批人ID（部门负责人，关联employee.id）',
    remark                VARCHAR(512)    DEFAULT NULL,
    createTime            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDeleted             TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. ONBOARDING 审批流（缺了就插入）
-- ============================================================
INSERT IGNORE INTO approval_flow (id, businessType, flowName, description, status)
VALUES (1, 'ONBOARDING', '入职审批', 'HR发起→部门负责人→[HR负责人]', 1);

-- 删掉旧节点重建（避免列不匹配）
DELETE FROM approval_flow_node WHERE flowId = 1;

INSERT INTO approval_flow_node (flowId, nodeName, nodeOrder, approverType, approverId, isOptional, createTime)
VALUES
(1, '部门负责人', 1, 'DEPT_MANAGER', NULL, 0, NOW()),
(1, 'HR负责人',   2, 'HR_MANAGER',   NULL, 1, NOW());

-- ============================================================
-- 3. 关键：给所有部门设置负责人（否则 DEPT_MANAGER 节点找不到审批人）
--    挑一个在职员工做各部门负责人
-- ============================================================
UPDATE department SET managerId = (
    SELECT id FROM employee WHERE departmentId = department.id AND status IN (1,2) LIMIT 1
)
WHERE managerId IS NULL AND id IN (SELECT DISTINCT departmentId FROM employee WHERE status IN (1,2));

-- 如果上面更新后还有 NULL，随便挑个在职员工当兜底
UPDATE department SET managerId = (
    SELECT id FROM employee WHERE status IN (1,2) LIMIT 1
)
WHERE managerId IS NULL;

-- ============================================================
-- 4. 已有数据库升级：添加审批人字段
-- ============================================================
ALTER TABLE emp_onboarding ADD COLUMN IF NOT EXISTS approverId BIGINT DEFAULT NULL COMMENT '审批人ID（部门负责人，关联employee.id）' AFTER operatorId;
