# 审批中心 — 后端设计文档

**日期：** 2026-07-15
**版本：** 2.0
**作者：** 郭文峰

---

## 1. 概述

审批中心是 HRMS 系统的通用审批引擎模块，为入转调离、考勤、薪资等业务提供统一的审批流能力。

### 1.1 已实现功能

| 能力 | 状态 |
|------|:---:|
| 创建审批实例 + 节点链 | ✅ |
| 审批通过（流转/回调） | ✅ |
| 审批拒绝（终止/回调） | ✅ |
| 审批转交（记录原审批人） | ✅ |
| 撤回申请 | ✅ |
| 待办列表（含委托路由） | ✅ |
| 已办列表 | ✅ |
| 审批详情（含节点时间线） | ✅ |
| 委托审批（设置/取消/查询/路由/审计） | ✅ |
| 审批超时处理（48h @Scheduled 扫描） | ✅ |
| 待办红点数量（`getPendingCount`） | ✅ |
| HR 节点配置开关 | ✅ |

### 1.2 已接入业务（7 种）

| 业务 | 申请者 | 审批链 | Builder |
|------|--------|--------|---------|
| 入职审批 | HR | 部门负责人 → [HR负责人] | `OnboardingNodeBuilder` |
| 转正审批 | HR | 部门负责人 → [HR负责人] | `ProbationNodeBuilder` |
| 调岗审批 | HR | 原部门负责人 → 新部门负责人 → [HR负责人] | `TransferNodeBuilder` |
| 离职审批 | HR | 部门负责人 → [HR负责人] | `ResignationNodeBuilder` |
| 请假审批 | 员工 | 按 PRD 6.3.4 动态路由 | `LeaveNodeBuilder` |
| 补卡审批 | 员工 | 直接上级 | `SupplementCardNodeBuilder` |
| 薪资批次审批 | HR | 财务专员 → [老板] | `SalaryBatchNodeBuilder` |

> `[HR负责人]` 可配置开关：`approval.hr-node.enabled`，默认 `true`
> `[老板]` 可选，暂无数据模型支撑，PRD 标记为可选

---

## 2. 架构设计

### 2.1 核心思路

策略模式 + 回调接口：

- **ApprovalNodeBuilder（策略接口）**：不同业务类型对应不同 Builder，负责构建审批节点链
- **ApprovalCallback（回调接口）**：各业务模块实现，审批通过/拒绝后由引擎回调，解耦审批流转与业务处理
- **ApprovalNodeBuilderFactory（工厂）**：按 `ApprovalBizType` 路由到对应 Builder
- **ApproverResolver（审批人解析器）**：统一查询各类审批人（HR负责人、财务专员），消除 Builder 间重复代码
- **ApprovalDelegateService（委托服务）**：管理委托关系 + 审批路由转发

### 2.2 核心流程

```
业务模块提交申请
      │
      ▼
┌─────────────────┐
│ 1. 创建审批实例  │  createInstance(bizType, bizId, applicantId)
│    + 节点链      │  → Builder.build() → 生成 ApprovalNode 列表 → 保存
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. 审批人操作    │  approve/reject/transfer
│    节点流转      │  → 校验权限（含委托路由）→ 校验状态（含超时）→ 更新节点
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
  通过       拒绝
    │         │
    ▼         ▼
┌──────┐  ┌──────┐
│下一节点│  │终止流 │
│或回调 │  │+回调  │
└──────┘  └──────┘
```

### 2.3 关键接口

**ApprovalFlowService（审批引擎）：**

```java
public interface ApprovalFlowService {
    ApprovalInstance createInstance(ApprovalBizType bizType, Long bizId, Long applicantId);
    void approve(Long nodeId, Long employeeId, String comment);
    void reject(Long nodeId, Long employeeId, String comment);
    void transfer(Long nodeId, Long fromEmployeeId, Long toEmployeeId);
    void cancel(Long instanceId, Long operatorId);
    Page<PendingItemVO> getPendingList(Long employeeId, ApprovalQuery query);
    Page<ProcessedItemVO> getProcessedList(Long employeeId, ApprovalQuery query);
    ApprovalInstanceVO getDetail(Long instanceId);
    long getPendingCount(Long employeeId);
}
```

**ApprovalCallback（业务回调，业务模块实现）：**

```java
public interface ApprovalCallback {
    void onApproved(ApprovalBizType bizType, Long bizId);
    void onRejected(ApprovalBizType bizType, Long bizId);
}
```

**ApprovalDelegateService（委托审批服务）：**

```java
public interface ApprovalDelegateService {
    ApprovalDelegate createDelegate(Long delegatorId, Long delegateId, LocalDateTime startTime, LocalDateTime endTime);
    void cancelDelegate(Long delegateId, Long delegatorId);
    Map<String, List<ApprovalDelegate>> getMyDelegates(Long userId);
    Long resolveApprover(Long originalApproverId);
}
```

**ApproverResolver（审批人解析器 — 统一查询）：**

```java
@Component
public class ApproverResolver {
    public Long resolveHrApprover();      // 查 user_role='hr' → employee
    public Long resolveFinanceApprover(); // 查 user_role='finance' → employee
    public String getEmployeeName(Long employeeId);
}
```

---

## 3. 审批节点链构建

### 3.1 Builder 策略

```java
public interface ApprovalNodeBuilder {
    List<ApprovalNode> build(Long bizId, Long applicantId);
    ApprovalBizType supportedBizType();
}
```

| 业务类型 | Builder | 节点链 | 审批人来源 |
|---------|---------|--------|-----------|
| 入职审批 | `OnboardingNodeBuilder` | 部门负责人 → [HR负责人] | `dept.manager_id`, `user.user_role='hr'` |
| 转正审批 | `ProbationNodeBuilder` | 部门负责人 → [HR负责人] | `dept.manager_id`, `user.user_role='hr'` |
| 调岗审批 | `TransferNodeBuilder` | 原部门负责人 → 新部门负责人 → [HR负责人] | 原/新 `dept.manager_id`, HR |
| 离职审批 | `ResignationNodeBuilder` | 部门负责人 → [HR负责人] | `dept.manager_id`, HR |
| 请假审批 | `LeaveNodeBuilder` | 直接上级 → (条件)部门负责人/HR备案 | `work_info.direct_report_id`, 按类型+天数动态 |
| 补卡审批 | `SupplementCardNodeBuilder` | 直接上级 | `work_info.direct_report_id` |
| 薪资批次 | `SalaryBatchNodeBuilder` | 财务专员 | `user.user_role='finance'` |

### 3.2 审批人确定机制

| 节点角色 | 查询方式 | 使用场景 |
|---------|---------|---------|
| 部门负责人 | `department.manager_id` | 入转调离、请假(大额) |
| HR 负责人 | `user.user_role='hr'` → `employee.user_id` | 入转调离、婚产丧备案 |
| 直接上级 | `employee_work_info.direct_report_id` | 请假、补卡 |
| 财务专员 | `user.user_role='finance'` → `employee.user_id` | 薪资批次 |
| 指定审批人 | 业务显式传入 | 转交场景 |

### 3.3 请假审批动态路由（PRD 6.3.4）

| 请假类型 | 条件 | 审批链 |
|---------|------|--------|
| 年假/调休 | ≤ 3 天 | 直接上级 |
| 年假/调休 | > 3 天 | 直接上级 → 部门负责人 |
| 病假/事假 | ≤ 1 天 | 直接上级 |
| 病假/事假 | > 1 天 | 直接上级 → 部门负责人 |
| 婚假/产假/丧假 | 任意 | 直接上级 → HR备案 |

### 3.4 工厂注册

```java
@Component
public class ApprovalNodeBuilderFactory {
    private final Map<ApprovalBizType, ApprovalNodeBuilder> builders;
    // @PostConstruct: Spring 自动注入所有 Builder，按 supportedBizType() 建立映射
    public ApprovalNodeBuilder getBuilder(ApprovalBizType bizType) { ... }
}
```

加新业务类型只需新增 Builder 实现类 + Entity/Mapper，引擎零改动。

### 3.5 HR节点配置开关

`ApprovalConfig` 读取 `approval.hr-node.enabled`（默认 `true`），4 个含 HR 节点的 Builder 注入此配置：

```java
if (approvalConfig.getHrNode().isEnabled()) {
    Long hrId = approverResolver.resolveHrApprover();
    if (hrId != null) {
        // 添加 HR 负责人节点
    }
}
```

双重降级：① 配置关闭 → 跳过 ② 系统无 HR 用户 → 自动跳过

---

## 4. 数据库

### 4.1 approval_instance

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| biz_type | VARCHAR(32) | 业务类型：ONBOARDING/PROBATION/TRANSFER/RESIGNATION/LEAVE/CARD_REPLENISH/SALARY_BATCH |
| biz_id | BIGINT | 业务主键ID |
| title | VARCHAR(128) | 审批标题 |
| status | TINYINT | 1=审批中 2=已通过 3=已拒绝 4=已撤回 |
| applicant_id | BIGINT | 申请人ID，关联 employee.id |
| current_node_order | INT | 当前审批节点序号 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

唯一索引：`uk_biz (biz_type, biz_id)`

### 4.2 approval_node

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| instance_id | BIGINT | 审批实例ID |
| node_name | VARCHAR(64) | 节点名称 |
| node_order | INT | 节点顺序号 |
| approver_id | BIGINT | 当前审批人ID，关联 employee.id |
| original_approver_id | BIGINT | 原审批人ID（转交/委托记录） |
| status | TINYINT | 1=待审批 2=已通过 3=已拒绝 4=已转交 5=已超时 |
| comment | VARCHAR(512) | 审批意见 |
| operate_time | DATETIME | 操作时间 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

核心索引：`idx_approver_id_status (approver_id, status)` — 待办列表查询

### 4.3 approval_delegate

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| delegator_id | BIGINT | 委托人ID |
| delegate_id | BIGINT | 被委托人ID |
| start_time | DATETIME | 委托开始时间 |
| end_time | DATETIME | 委托结束时间 |
| enabled | TINYINT | 0=已取消 1=生效中 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

核心索引：`idx_time_enabled (start_time, end_time, enabled)` — 委托路由查询

### 4.4 ORM

使用 MyBatis-Plus 3.5.2，实体类 `@TableName` 注解映射，Mapper 继承 `BaseMapper<T>`。审批表主键用 `IdType.AUTO`（自增），薪资批次用 `IdType.ASSIGN_ID`（雪花算法），业务表用 `@TableLogic` 逻辑删除。

---

## 5. 业务表关联

所有业务申请表均已预留 `approval_instance_id` 字段：

| 业务表 | 状态字段 | approval_instance_id |
|--------|---------|:---:|
| `onboarding_application` | 1=草稿/2=审批中/3=已批准待入职/4=已入职/5=已拒绝 | ✅ |
| `probation_application` | 1=草稿/2=审批中/3=已完成/4=已拒绝 | ✅ |
| `transfer_application` | 1=草稿/2=审批中/3=已生效/4=已拒绝 | ✅ |
| `resignation_application` | 1=草稿/2=审批中/3=审批通过待离职/4=已离职/5=已拒绝 | ✅ |
| `leave_request` | 1=草稿/2=审批中/3=已通过/4=已拒绝/5=已取消 | ✅ |
| `supplement_card_request` | 1=草稿/2=审批中/3=已通过/4=已拒绝 | ✅ |
| `salary_batch` | 0=草稿/1=计算中/2=待确认/3=审批中/4=已通过/5=已发放/6=已驳回 | ✅ |

---

## 6. API 设计

### 6.1 接口列表

#### 核心审批接口

| 序号 | 接口 | 方法 | 路径 |
|------|------|------|------|
| 1 | 查询待办列表 | GET | `/api/approvals/pending` |
| 2 | 查询已办列表 | GET | `/api/approvals/processed` |
| 3 | 获取审批详情 | GET | `/api/approvals/{instanceId}` |
| 4 | 审批通过 | POST | `/api/approvals/{nodeId}/approve` |
| 5 | 审批拒绝 | POST | `/api/approvals/{nodeId}/reject` |
| 6 | 审批转交 | POST | `/api/approvals/{nodeId}/transfer` |
| 7 | 撤回申请 | POST | `/api/approvals/{instanceId}/cancel` |

#### 扩展接口

| 序号 | 接口 | 方法 | 路径 |
|------|------|------|------|
| 8 | 待办红点数量 | GET | `/api/approvals/pending-count` |
| 9 | 设置委托审批 | POST | `/api/approvals/delegates` |
| 10 | 取消委托审批 | DELETE | `/api/approvals/delegates/{id}` |
| 11 | 查询我的委托 | GET | `/api/approvals/delegates/my` |

### 6.2 关键接口详情

#### 审批通过 POST /api/approvals/{nodeId}/approve

前置条件：当前用户 = 该节点审批人（含委托路由），节点状态 = 待审批（非超时）

请求：
```json
{ "comment": "同意入职" }
```

非最后节点响应：返回下一节点信息
```json
{
  "code": 0,
  "data": {
    "nodeId": 10001,
    "nodeStatus": 2,
    "nodeStatusDesc": "已通过",
    "nextNode": { "nodeId": 10002, "nodeName": "HR负责人审批", "approverName": "王五" }
  }
}
```

最后节点响应：触发业务回调
```json
{
  "code": 0,
  "data": {
    "nodeId": 10002,
    "nodeStatus": 2,
    "instanceStatus": 2,
    "instanceStatusDesc": "已通过"
  }
}
```

#### 委托审批 — 设置委托 POST /api/approvals/delegates

校验：不能委托给自己、时间不重叠、endTime > startTime

请求：
```json
{ "delegateId": 300, "startTime": "2024-05-01T09:00:00", "endTime": "2024-05-07T18:00:00" }
```

委托路由逻辑：`approve/reject/transfer` 前调用 `resolveApprover()`，若当前用户是被委托人，自动记录 `original_approver_id = 委托人ID` 用于审计。

#### 待办红点 GET /api/approvals/pending-count

响应：
```json
{ "code": 0, "data": { "count": 8 } }
```

计算逻辑：本人待办数 + 委托给我的待办数

---

## 7. 委托审批

### 7.1 路由机制

```
resolveApprover(originalApproverId):
  1. 查 approval_delegate WHERE delegator_id = ? AND enabled = 1
     AND NOW() BETWEEN start_time AND end_time
  2. 有 → 返回 delegate_id
  3. 无 → 返回 originalApproverId（本人）
```

### 7.2 审计记录

被委托人操作时，系统在 `approval_node` 中记录：
- `approver_id` = 实际操作人（被委托人）
- `original_approver_id` = 委托人（原审批人）

前端可展示 "XXX 代 YYY 审批"。

---

## 8. 审批超时处理

### 8.1 方案

定时任务（`@Scheduled`）每整点扫描，不依赖 MQ：

```
ApprovalTimeoutJob.scanTimeoutNodes():
  SELECT * FROM approval_node
  WHERE status = 1(PENDING) AND create_time < NOW() - 48h
  → 标记 status = 5(TIMEOUT)
```

### 8.2 超时阻止

`approve/reject/transfer` 前校验节点状态：若为 TIMEOUT(5)，抛出 `APPROVAL_NODE_TIMEOUT(40011)`，阻止操作。

### 8.3 NodeStatus 扩展

| 代码 | 含义 |
|:---:|------|
| 1 | 待审批 PENDING |
| 2 | 已通过 APPROVED |
| 3 | 已拒绝 REJECTED |
| 4 | 已转交 TRANSFERRED |
| 5 | 已超时 TIMEOUT |

---

## 9. 错误码

| 错误码 | 说明 | HTTP |
|--------|------|------|
| 40001 | 审批实例不存在 | 404 |
| 40002 | 审批节点不存在 | 404 |
| 40003 | 该节点不属于当前用户 | 403 |
| 40004 | 该节点已被处理 | 400 |
| 40005 | 仅第一节点可撤回 | 400 |
| 40006 | 委托时间冲突 | 400 |
| 40007 | 不能委托给自己 | 400 |
| 40008 | 委托结束时间必须晚于开始时间 | 400 |
| 40011 | 审批已超时，无法操作 | 400 |

---

## 10. 包结构

```
com.limou.hrms
├── controller/
│   └── ApprovalController              ← 11个 REST 端点
├── service/
│   ├── ApprovalFlowService              ← 审批引擎接口
│   ├── ApprovalCallback                 ← 业务回调接口
│   ├── ApprovalDelegateService          ← 委托审批服务接口
│   └── impl/
│       ├── ApprovalFlowServiceImpl      ← 审批引擎实现（300+ 行）
│       └── ApprovalDelegateServiceImpl  ← 委托审批实现
├── mapper/
│   ├── ApprovalInstanceMapper           ← 审批实例
│   ├── ApprovalNodeMapper               ← 审批节点
│   ├── ApprovalDelegateMapper           ← 委托关系
│   ├── DepartmentMapper                 ← 部门（支撑查询）
│   ├── EmployeeMapper                   ← 员工（支撑查询）
│   ├── EmployeeWorkInfoMapper           ← 工作信息（支撑查询）
│   ├── OnboardingApplicationMapper      ← 入职申请
│   ├── ProbationApplicationMapper       ← 转正申请
│   ├── TransferApplicationMapper        ← 调岗申请
│   ├── ResignationApplicationMapper     ← 离职申请
│   ├── LeaveRequestMapper               ← 请假申请
│   ├── SupplementCardRequestMapper      ← 补卡申请
│   └── SalaryBatchMapper                ← 薪资批次
├── model/
│   ├── entity/                          ← 15 个实体类
│   ├── dto/
│   │   └── approval/ApprovalActionDTO
│   ├── query/ApprovalQuery
│   └── vo/                              ← ApprovalInstanceVO, ApprovalNodeVO,
│                                          PendingItemVO, ProcessedItemVO
├── enums/
│   ├── ApprovalBizType                  ← 7 种业务类型
│   ├── ApprovalStatus                   ← 4 种实例状态
│   └── NodeStatus                       ← 5 种节点状态（含 TIMEOUT）
├── builder/
│   ├── ApprovalNodeBuilder              ← 构建器接口
│   ├── ApprovalNodeBuilderFactory       ← 工厂（@PostConstruct 自动注册）
│   ├── ApproverResolver                 ← 统一审批人解析器
│   ├── OnboardingNodeBuilder            ← 入职（部门负责人 → [HR]）
│   ├── ProbationNodeBuilder             ← 转正（部门负责人 → [HR]）
│   ├── TransferNodeBuilder              ← 调岗（原部门 → 新部门 → [HR]）
│   ├── ResignationNodeBuilder           ← 离职（部门负责人 → [HR]）
│   ├── LeaveNodeBuilder                 ← 请假（PRD 6.3.4 动态路由）
│   ├── SupplementCardNodeBuilder        ← 补卡（直接上级）
│   └── SalaryBatchNodeBuilder           ← 薪资（财务专员）
├── config/
│   └── ApprovalConfig                   ← HR节点开关（@ConfigurationProperties）
└── job/
    └── ApprovalTimeoutJob               ← 超时扫描（@Scheduled 每小时）
```

---

## 11. 技术栈

| 类别 | 选型 |
|------|------|
| 框架 | Spring Boot 2.7.2 |
| ORM | MyBatis-Plus 3.5.2 |
| 数据库 | MySQL |
| 事务 | Spring `@Transactional` |
| 定时任务 | Spring `@Scheduled` |
| 工具库 | Hutool 5.8.8, commons-lang3 |

第一版不引入 Redis / RabbitMQ，委托路由直接查 DB，超时用定时任务兜底。

---

## 12. 设计决策

### 12.1 为什么用 @Scheduled 而非 RabbitMQ 延迟队列？

V1 阶段审批量不大，定时任务扫描全表性能可接受（每小时一次，索引覆盖）。引入 MQ 增加运维复杂度，后续迭代可按需切换。

### 12.2 为什么 HR 节点做成可配置开关？

PRD 中入职审批的 HR负责人标注为 `[可选]`。不同公司的 HR 审批层级不同，提供一键开关避免硬编码。双重降级（配置关闭 + 系统无 HR 用户自动跳过）保证健壮性。

### 12.3 回调事务策略

业务回调（`onApproved` / `onRejected`）在审批引擎的 `@Transactional` 事务内执行。若业务回调抛异常，整个审批操作回滚，保证一致性。第一版通过事务回滚兜底，后续可加重试机制。

### 12.4 @Autowired(required = false) for Callbacks

`ApprovalCallback` 由业务模块按需实现，引擎不强制要求任何 Bean 存在。使用 `@Autowired(required = false) List<ApprovalCallback>` 优雅处理空列表场景。

---

## 13. 变更记录

| 日期 | 版本 | 变更内容 |
|------|:---:|------|
| 2026-07-14 | 1.0 | 初稿：核心引擎 + 入职/转正 |
| 2026-07-15 | 1.1 | 扩展 5 种业务类型（调岗/离职/请假/补卡/薪资批次） |
| 2026-07-15 | 1.2 | 新增委托审批（设置/取消/查询/路由/审计） |
| 2026-07-15 | 1.3 | 新增审批超时处理（@Scheduled + NodeStatus.TIMEOUT） |
| 2026-07-15 | 1.4 | 新增待办红点（pending-count）、HR节点配置开关（ApprovalConfig） |
| 2026-07-15 | 1.5 | 提取 ApproverResolver 消除 Builder 重复代码 |
| 2026-07-15 | 1.6 | 请假审批完整实现 PRD 6.3.4 动态路由规则 |
| 2026-07-15 | 2.0 | 文档重写：合并所有变更，反映当前实际状态 |
