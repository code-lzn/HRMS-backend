# 入职管理模块 - 后端设计文档

## 变更记录

| 日期 | 版本 | 修订说明 | 作者 |
|------|------|---------|------|
| 2026-07-16 | 1.0 | 初稿 | 郭文峰 |

---

## 1. 模块背景

本模块属于 HRMS 入转调离的第 1 个子模块。入职申请由 HR 创建，经审批中心流转（部门负责人 → [HR负责人]）通过后，自动生成工号并写入员工档案三表（employee / personal_info / work_info），薪资信息由 HR 在薪资模块单独设置。

**相关文档**：
- PRD：人资管理系统-PRD.md §5.1
- 系分：入转调离-后端系分.md
- 审批中心：审批中心-后端系分.md

---

## 2. 技术选型

沿用项目现有技术栈：Spring Boot + MyBatis-Plus + MySQL + Redis，无新增依赖。

---

## 3. 包结构

```
com.limou.hrms
├── controller
│   └── OnboardingController          ← 新增
├── service
│   ├── OnboardingService             ← 新增
│   └── impl
│       └── OnboardingServiceImpl     ← 新增，实现 ApprovalCallback
├── model
│   ├── dto/onboarding/
│   │   ├── OnboardingCreateDTO       ← 新增
│   │   └── OnboardingUpdateDTO       ← 新增
│   ├── query/
│   │   └── OnboardingQuery           ← 新增
│   ├── vo/
│   │   ├── OnboardingListVO          ← 新增
│   │   └── OnboardingDetailVO        ← 新增
│   └── enums/
│       └── OnboardingStatus          ← 新增
├── entity/
│   └── OnboardingApplication         ← 已存在，不修改字段命名
├── mapper/
│   └── OnboardingApplicationMapper   ← 已存在
└── builder/
    └── OnboardingNodeBuilder         ← 已存在（审批节点构建器）
```

---

## 4. 状态机设计

### 4.1 入职申请状态枚举

```java
public enum OnboardingStatus {
    DRAFT(1, "草稿"),           // 可编辑/删除/提交
    PENDING(2, "审批中"),        // 可撤回（仅第一节点）
    APPROVED(3, "已批准待入职"),  // 可确认入职/放弃
    JOINED(4, "已入职"),         // 终态
    REJECTED(5, "已拒绝"),       // 终态
    ABANDONED(6, "已放弃");      // 终态（确认入职前放弃）
}
```

### 4.2 状态流转图

```
新建 ──> 草稿(1)
           │
           ├── 编辑（仍为草稿）
           ├── 删除
           └── 提交审批
                 │
                 ▼
           审批中(2)
                 │
          ┌──────┼──────┐
          │      │      │
        通过   拒绝   撤回
          │      │      │
          ▼      ▼      ▼
   已批准待入职 已拒绝   草稿(1)
    (3)      (5)
      │
   ┌──┴──┐
   │     │
 HR确认  标记放弃
   │     │
   ▼     ▼
已入职  已放弃
 (4)    (6)
```

---

## 5. 与审批中心集成

### 5.1 审批节点链

由已存在的 `OnboardingNodeBuilder` 构建：
- **Node 1**：部门负责人审批（必选）
- **Node 2**：HR负责人审批（可配置开关 `approval.hr-node.enabled`）

### 5.2 回调机制

`OnboardingServiceImpl` 实现 `ApprovalCallback` 接口：

```
ApprovalCallback
  ├── onApproved(ONBOARDING, bizId)
  │     → 生成工号 + 写入 employee/personal_info/work_info 三表
  │     → 入职申请状态 → APPROVED(已批准待入职)
  │
  └── onRejected(ONBOARDING, bizId)
        → 入职申请状态 → REJECTED(已拒绝)
```

### 5.3 提交审批

```java
// OnboardingServiceImpl.submitToApproval(id)
1. 校验申请状态 = DRAFT
2. 校验所有必填字段完整
3. 入职申请状态 → PENDING
4. approvalFlowService.createInstance(ONBOARDING, id, applicantId)
5. 关联 approvalInstanceId 到入职申请
```

### 5.4 撤回申请

```java
// OnboardingServiceImpl.cancel(id)
1. 校验申请状态 = PENDING 且当前节点 = 第一节点
2. 校验当前用户 = 申请人
3. approvalFlowService.cancel(instanceId)
4. 入职申请状态 → DRAFT（可再次编辑提交）
5. 清空 approvalInstanceId
```

---

## 6. 入职后处理（审批通过回调）

`onApproved()` 中执行三表写入事务：

```java
@Transactional(rollbackFor = Exception.class)
public void onApproved(ApprovalBizType bizType, Long bizId) {
    OnboardingApplication app = onboardingMapper.selectById(bizId);
    
    // 1. 生成工号
    String employeeNo = generateEmployeeNo(app.getDepartmentId());
    
    // 2. 写入 employee 表
    Employee employee = new Employee();
    employee.setEmployeeNo(employeeNo);
    employee.setStatus(EmployeeStatus.PROBATION.getValue());
    employee.setHireDate(app.getExpectedHireDate());
    employee.setHireType(app.getHireType());
    employeeMapper.insert(employee);
    
    // 3. 写入 personal_info 表
    EmployeePersonalInfo personalInfo = new EmployeePersonalInfo();
    personalInfo.setEmployeeId(employee.getId());
    personalInfo.setName(app.getName());
    personalInfo.setGender(app.getGender());
    personalInfo.setPhone(app.getPhone());
    personalInfo.setEmail(app.getEmail());
    personalInfo.setIdCard(aesUtil.encrypt(app.getIdCard()));
    personalInfoMapper.insert(personalInfo);
    
    // 4. 写入 work_info 表
    EmployeeWorkInfo workInfo = new EmployeeWorkInfo();
    workInfo.setEmployeeId(employee.getId());
    workInfo.setDepartmentId(app.getDepartmentId());
    workInfo.setPositionId(app.getPositionId());
    workInfo.setDirectReportId(app.getDirectReportId());
    workInfoMapper.insert(workInfo);
    
    // 5. 更新入职申请
    app.setEmployeeId(employee.getId());
    app.setStatus(OnboardingStatus.APPROVED.getCode());
    onboardingMapper.updateById(app);
    
    // 6. 事务提交后发送通知（MQ / 异步）
    // 通知HR和部门负责人、发送欢迎邮件给候选人
}
```

> **设计决策**：不调用 `EmployeeService.createEmployee()`。
> - 该方法要求必填合同/薪资字段，而 PRD 明确写"薪资由HR在薪资模块单独设置"
> - 入职回调的职责聚焦于"从申请表 → 三表"的映射，逻辑简单可控
> - 工号生成（`EmployeeNoSequenceMapper`）、加密（`AesUtil`）等底层能力直接复用

---

## 7. API 设计

### 7.1 接口列表

| 方法 | 路径 | 说明 | AuthCheck |
|------|------|------|-----------|
| GET | `/api/v1/onboarding` | 分页列表（角色路由） | 需要 |
| GET | `/api/v1/onboarding/{id}` | 详情 + 审批进度 | 需要 |
| POST | `/api/v1/onboarding` | 创建（草稿/直接提交） | HR/admin |
| PUT | `/api/v1/onboarding/{id}` | 更新草稿 | 需要（仅申请人） |
| DELETE | `/api/v1/onboarding/{id}` | 删除草稿 | 需要（仅申请人） |
| POST | `/api/v1/onboarding/{id}/submit` | 提交审批 | 需要（仅申请人） |
| POST | `/api/v1/onboarding/{id}/cancel` | 撤回 | 需要（仅申请人） |
| POST | `/api/v1/onboarding/{id}/confirm-join` | 确认入职 | HR/admin |
| POST | `/api/v1/onboarding/{id}/abandon` | 标记放弃 | HR/admin |

### 7.2 角色数据路由（list 接口）

```java
DataScopeEnum scope = dataScopeContext.getApprovalScope();
switch (scope) {
    case ALL  → 全平台查询
    case DEPT → 本部门员工入职申请
    case SELF → 仅自己创建的申请
}
```

### 7.3 创建入职申请 DTO

```java
public class OnboardingCreateDTO {
    @NotBlank String name;              // 姓名
    @NotNull Integer gender;            // 1=男 2=女
    @NotBlank String phone;             // 手机号
    @NotBlank @Email String email;      // 邮箱
    @NotBlank String idCard;            // 身份证号
    @NotNull LocalDate expectedHireDate;// 预计入职日期
    @NotNull Long departmentId;         // 所属部门
    @NotNull Long positionId;           // 职位
    @NotNull Integer hireType;          // 1=全职 2=兼职 3=实习
    @NotNull Integer probationMonths;   // 试用期月数
    @NotNull BigDecimal probationRatio; // 试用期薪资比例
    Long directReportId;                // 直接汇报人（默认部门负责人）
    Boolean submitDirectly;             // 是否直接提交（默认false=草稿）
}
```

### 7.4 列表 VO

```java
public class OnboardingListVO {
    Long id;
    String name;
    String phone;           // 脱敏展示
    String departmentName;
    String positionName;
    LocalDate expectedHireDate;
    Integer status;
    String statusDesc;
    String applicantName;
    LocalDateTime createTime;
}
```

### 7.5 详情 VO

```java
public class OnboardingDetailVO {
    // 基本信息（同 OnboardingApplication 所有字段 + departmentName/positionName 等名称字段）
    Long id;
    String name;
    Integer gender; String genderDesc;
    String phone;           // 身份证号非HR脱敏
    String email;
    String idCard;
    LocalDate expectedHireDate;
    Long departmentId; String departmentName;
    Long positionId; String positionName;
    Integer hireType; String hireTypeDesc;
    Integer probationMonths;
    BigDecimal probationRatio;
    Long directReportId; String directReportName;
    Integer status; String statusDesc;
    Long approvalInstanceId;
    Long employeeId;
    Long applicantId; String applicantName;
    LocalDateTime createTime;
    LocalDateTime updateTime;
    
    // 审批进度（审批中/已通过/已拒绝时返回）
    ApprovalProgressVO approvalProgress;
}
```

---

## 8. 与现有基础设施的集成

| 基础设施 | 使用方式 |
|---------|---------|
| `UserContext` | 获取当前登录用户 |
| `DataScopeContext` | 获取当前用户 employee.id + role + 数据范围 |
| `AuthCheck` | Controller 方法加注解开填充 DataScopeContext |
| `ApprovalFlowService` | 创建审批实例 / 撤回 |
| `ApprovalCallback` | 实现 onApproved / onRejected |
| `EmployeeNoSequenceMapper` | 工号序号生成（年份+部门编码+序号） |
| `AesUtil` | 身份证号加密存储 |

---

## 9. 错误码定义

| 错误码 | 说明 | HTTP |
|--------|------|------|
| 30001 | 入职申请不存在 | 404 |
| 30002 | 仅草稿状态可编辑/删除 | 400 |
| 30003 | 仅草稿状态可提交审批 | 400 |
| 30004 | 仅第一级审批节点可撤回 | 400 |
| 30005 | 仅"已批准待入职"可确认入职 | 400 |
| 30012 | 入职申请必填字段不完整 | 400 |

---

## 10. 设计决策记录

| 决策 | 选项 | 理由 |
|------|------|------|
| `probationMonths` 字段命名 | 保持现有 `defaultProbationMonths` | 不改已有实体 |
| 入职后处理 | 自己写三表写入，不调 `createEmployee()` | PRD要求薪资单独设置，不改已有 EmployeeService |
| `@AuthCheck` mustRole | 查询接口不加角色限制 | Service 层做角色路由 |
| 预览工号 / 手机查重 | 本期不做 | 核心流程优先 |
