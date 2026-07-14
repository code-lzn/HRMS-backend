# 审批中心 V1 — 5种业务类型扩展

> **For agentic workers:** 按 Task 顺序执行。每个 Task 编译通过后再做下一个。

**Goal:** 在已完成的入职、转正基础上，补充调岗、离职、请假、补卡、薪资批次 5 种审批类型的 Builder 及支撑代码。

**Architecture:** 沿用已有策略模式，每种业务类型一个 Builder 实现类 + 对应 Entity/Mapper。

## Global Constraints
- Java 8，包路径 com.limou.hrms
- 实体 @TableId(type = IdType.AUTO)，@TableLogic 用于 is_deleted
- 编译命令：`./mvnw compile -q`

---

### Task A: 扩展枚举 + 创建 5 个业务实体和 Mapper

#### 1. 扩展 ApprovalBizType — 在 ONBOARDING 和 PROBATION 后添加：
```java
TRANSFER("TRANSFER", "调岗审批"),
RESIGNATION("RESIGNATION", "离职审批"),
LEAVE("LEAVE", "请假审批"),
CARD_REPLENISH("CARD_REPLENISH", "补卡审批"),
SALARY_BATCH("SALARY_BATCH", "薪资批次审批");
```

#### 2. 创建 TransferApplication 实体（参照 表结构.md）

```sql
CREATE TABLE IF NOT EXISTS transfer_application (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    employee_id BIGINT UNSIGNED NOT NULL,
    from_department_id BIGINT UNSIGNED NOT NULL,
    to_department_id BIGINT UNSIGNED NOT NULL,
    from_position_id BIGINT UNSIGNED NOT NULL,
    to_position_id BIGINT UNSIGNED,
    from_job_level VARCHAR(8),
    to_job_level VARCHAR(8),
    from_direct_report_id BIGINT UNSIGNED,
    to_direct_report_id BIGINT UNSIGNED,
    salary_adjustment DECIMAL(12,2),
    reason VARCHAR(512) NOT NULL,
    status TINYINT DEFAULT 1,  -- 1=草稿 2=审批中 3=已生效 4=已拒绝
    approval_instance_id BIGINT UNSIGNED,
    applicant_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);
```

#### 3. 创建 ResignationApplication 实体

```sql
CREATE TABLE IF NOT EXISTS resignation_application (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    employee_id BIGINT UNSIGNED NOT NULL,
    resignation_date DATE NOT NULL,
    resignation_type TINYINT NOT NULL,  -- 1=辞职 2=辞退 3=合同到期 4=其他
    reason VARCHAR(512) NOT NULL,
    handover_to_id BIGINT UNSIGNED NOT NULL,
    status TINYINT DEFAULT 1,  -- 1=草稿 2=审批中 3=审批通过待离职 4=已离职 5=已拒绝
    approval_instance_id BIGINT UNSIGNED,
    actual_resignation_date DATE,
    applicant_id BIGINT UNSIGNED NOT NULL,
    remark VARCHAR(512),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);
```

#### 4. 创建 LeaveRequest 实体

```sql
CREATE TABLE IF NOT EXISTS leave_request (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    employee_id BIGINT UNSIGNED NOT NULL,
    leave_type TINYINT NOT NULL,  -- 1=年假 2=病假 3=事假 4=婚假 5=产假 6=丧假 7=调休
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    leave_days DECIMAL(4,1) NOT NULL,
    reason VARCHAR(256) NOT NULL,
    handover_employee_id BIGINT UNSIGNED,
    attachment_url VARCHAR(512),
    status TINYINT DEFAULT 1,  -- 1=草稿 2=审批中 3=已通过 4=已拒绝 5=已取消
    approval_instance_id BIGINT UNSIGNED,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);
```

#### 5. 创建 SupplementCardRequest 实体

```sql
CREATE TABLE IF NOT EXISTS supplement_card_request (
    id BIGINT UNSIGNED AUTO_INCREMENT,
    employee_id BIGINT UNSIGNED NOT NULL,
    attendance_date DATE NOT NULL,
    card_type TINYINT NOT NULL,  -- 1=上班卡 2=下班卡
    reason VARCHAR(256) NOT NULL,
    status TINYINT DEFAULT 1,  -- 1=草稿 2=审批中 3=已通过 4=已拒绝
    approval_instance_id BIGINT UNSIGNED,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);
```

#### 6. 创建 SalaryBatch 实体

```sql
CREATE TABLE IF NOT EXISTS salary_batch (
    id BIGINT UNSIGNED NOT NULL,  -- 雪花算法
    batch_no VARCHAR(32) NOT NULL,
    salary_month VARCHAR(7) NOT NULL,
    status INT DEFAULT 0,  -- 0=草稿 1=计算中 2=待确认 3=审批中 4=已通过 5=已发放 6=已驳回
    total_employees INT DEFAULT 0,
    total_gross_pay DECIMAL(14,2) DEFAULT 0.00,
    total_net_pay DECIMAL(14,2) DEFAULT 0.00,
    total_tax DECIMAL(14,2) DEFAULT 0.00,
    create_by BIGINT UNSIGNED,
    approval_instance_id BIGINT UNSIGNED,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
```

#### 7. 创建 5 个 Mapper 接口

```java
TransferApplicationMapper extends BaseMapper<TransferApplication>
ResignationApplicationMapper extends BaseMapper<ResignationApplication>
LeaveRequestMapper extends BaseMapper<LeaveRequest>
SupplementCardRequestMapper extends BaseMapper<SupplementCardRequest>
SalaryBatchMapper extends BaseMapper<SalaryBatch>
```

**注意：**SalaryBatch 的主键是雪花算法（BIGINT NOT NULL，非自增），用 @TableId(type = IdType.ASSIGN_ID)。

---

### Task B: 创建 5 个 Builder 实现类

#### 1. TransferNodeBuilder: 原部门负责人 → 新部门负责人 → HR负责人

```java
@Component
public class TransferNodeBuilder implements ApprovalNodeBuilder {
    @Resource private TransferApplicationMapper transferApplicationMapper;
    @Resource private DepartmentMapper departmentMapper;
    @Resource private EmployeeMapper employeeMapper;

    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        TransferApplication app = transferApplicationMapper.selectById(bizId);
        // Node1: 原部门负责人
        Department fromDept = departmentMapper.selectById(app.getFromDepartmentId());
        // Node2: 新部门负责人
        Department toDept = departmentMapper.selectById(app.getToDepartmentId());
        // Node3: HR负责人 (resolveHrApprover())
        // ... same pattern
    }
    public ApprovalBizType supportedBizType() { return ApprovalBizType.TRANSFER; }
}
```

#### 2. ResignationNodeBuilder: 部门负责人 → HR负责人

从 resignation_application.employee_id → employee_work_info.department_id → department.manager_id → HR负责人.

#### 3. LeaveNodeBuilder: 直接上级（>3天加部门负责人）

从 leave_request.employee_id → employee_work_info:
- Node1: 直接上级 (direct_report_id)
- Node2 (if leave_days > 3): 部门负责人 (department_id → manager_id)

#### 4. SupplementCardNodeBuilder: 直接上级

从 supplement_card_request.employee_id → employee_work_info.direct_report_id

#### 5. SalaryBatchNodeBuilder: 财务专员

查 user.user_role = 'finance' → employee.user_id 映射到 employee.id，取第一个作为审批人。

---

### Task C: 编译验证

`./mvnw compile -q` → BUILD SUCCESS
`./mvnw test -q` → 除已有 COS 配置问题外无新失败
