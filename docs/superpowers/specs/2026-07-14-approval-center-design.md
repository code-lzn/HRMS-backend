# 审批中心 — 后端设计文档

**日期：** 2026-07-14
**版本：** 1.0
**作者：** 郭文峰

---

## 1. 概述

审批中心是 HRMS 系统的通用审批引擎模块，为入转调离、考勤、薪资等业务提供统一的审批流能力。本次为第一版，聚焦核心主链路：创建审批实例 → 节点流转（通过/拒绝/转交） → 业务回调。

### 1.1 第一版范围

| 能力 | 是否包含 |
|------|:---:|
| 创建审批实例 + 节点链 | ✅ |
| 审批通过（流转/回调） | ✅ |
| 审批拒绝（终止/回调） | ✅ |
| 审批转交 | ✅ |
| 撤回申请 | ✅ |
| 待办列表 | ✅ |
| 已办列表 | ✅ |
| 审批详情（含节点时间线） | ✅ |
| 委托审批 | ❌ 后续迭代 |
| 审批超时处理 | ❌ 后续迭代 |
| 待办红点数量 | ❌ 后续迭代 |

### 1.2 第一批接入业务

- **入职审批**：部门负责人 → HR负责人（固定2节点）
- **转正审批**：部门负责人 → HR负责人（固定2节点）

---

## 2. 架构设计

### 2.1 核心思路

策略模式 + 回调接口，沿用系分文档设计：

- **ApprovalNodeBuilder（策略接口）**：不同业务类型对应不同 Builder，负责构建审批节点链
- **ApprovalCallback（回调接口）**：各业务模块实现，审批通过/拒绝后由引擎回调，解耦审批流转与业务处理
- **ApprovalNodeBuilderFactory（工厂）**：按 `ApprovalBizType` 路由到对应 Builder

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
│ 2. 审批人操作    │  approve(nodeId) / reject(nodeId) / transfer(nodeId, toUserId)
│    节点流转      │  → 校验权限 → 更新节点 → 判断是否最后节点
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
    void approve(Long nodeId, String comment);
    void reject(Long nodeId, String comment);
    void transfer(Long nodeId, Long toApproverId);
    void cancel(Long instanceId);
    Page<ApprovalInstanceVO> getPendingList(ApprovalQuery query);
    Page<ApprovalInstanceVO> getProcessedList(ApprovalQuery query);
    ApprovalInstanceVO getDetail(Long instanceId);
}
```

**ApprovalCallback（业务回调，业务模块实现）：**

```java
public interface ApprovalCallback {
    void onApproved(ApprovalBizType bizType, Long bizId);
    void onRejected(ApprovalBizType bizType, Long bizId);
}
```

---

## 3. 审批节点链构建

### 3.1 Builder 策略

```java
public interface ApprovalNodeBuilder {
    List<ApprovalNode> build(Long bizId, Long applicantId);
}
```

| 业务类型 | Builder | 节点链 | 审批人来源 |
|---------|---------|--------|-----------|
| 入职审批 | `OnboardingNodeBuilder` | Node1:部门负责人 → Node2:HR负责人 | `department.manager_id`, `user.user_role='hr'` |
| 转正审批 | `ProbationNodeBuilder` | Node1:部门负责人 → Node2:HR负责人 | `department.manager_id`, `user.user_role='hr'` |

### 3.2 审批人确定机制

| 节点角色 | 查询方式 |
|---------|---------|
| 部门负责人 | `SELECT manager_id FROM department WHERE id = ?` |
| HR负责人 | 查 `user` 表中 `user_role = 'hr'` 的用户列表（任一审批即可） |

### 3.3 工厂注册

```java
@Component
public class ApprovalNodeBuilderFactory {
    private final Map<ApprovalBizType, ApprovalNodeBuilder> builders;
    // Spring 自动注入所有 ApprovalNodeBuilder 实现
    public ApprovalNodeBuilder getBuilder(ApprovalBizType bizType) { ... }
}
```

加新业务类型只需新增 Builder 实现类，引擎代码零改动。

---

## 4. 数据库

使用 `表结构.md` 中已定义的三张表，与系分文档一致：

### 4.1 approval_instance

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| biz_type | VARCHAR(32) | 业务类型：ONBOARDING/PROBATION/... |
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
| original_approver_id | BIGINT | 原审批人ID（转交场景记录） |
| status | TINYINT | 1=待审批 2=已通过 3=已拒绝 4=已转交 |
| comment | VARCHAR(512) | 审批意见 |
| operate_time | DATETIME | 操作时间 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

核心索引：`idx_approver_id_status (approver_id, status)` — 待办列表查询

### 4.3 approval_delegate

预留表，第一版仅建表，不做业务功能。

### 4.4 ORM

使用 MyBatis-Plus，实体类 `@TableName` 注解映射，Mapper 继承 `BaseMapper<T>`。

---

## 5. 业务表关联

各业务申请表均已预留 `approval_instance_id` 字段：

| 业务表 | 关键字段 |
|--------|---------|
| `onboarding_application` | `approval_instance_id`, `status`（1=草稿/2=审批中/3=已批准待入职/4=已入职/5=已拒绝） |
| `probation_application` | `approval_instance_id`, `status`（1=草稿/2=审批中/3=已完成/4=已拒绝） |

---

## 6. API 设计

### 6.1 接口列表

| 序号 | 接口 | 方法 | 路径 |
|------|------|------|------|
| 1 | 查询待办列表 | GET | `/api/approvals/pending` |
| 2 | 查询已办列表 | GET | `/api/approvals/processed` |
| 3 | 获取审批详情 | GET | `/api/approvals/{instanceId}` |
| 4 | 审批通过 | POST | `/api/approvals/{nodeId}/approve` |
| 5 | 审批拒绝 | POST | `/api/approvals/{nodeId}/reject` |
| 6 | 审批转交 | POST | `/api/approvals/{nodeId}/transfer` |
| 7 | 撤回申请 | POST | `/api/approvals/{instanceId}/cancel` |

### 6.2 关键接口详情

#### 审批通过 POST /api/approvals/{nodeId}/approve

前置条件：当前用户 = 该节点审批人，节点状态 = 待审批

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
    "instanceStatusDesc": "已通过",
    "callbackResult": { "summary": "入职处理完成，工号202407001" }
  }
}
```

#### 审批拒绝 POST /api/approvals/{nodeId}/reject

comment 必填，该节点置为已拒绝，实例置为已拒绝，触发 `onRejected` 回调。

#### 审批转交 POST /api/approvals/{nodeId}/transfer

保存 `original_approver_id` = 原审批人，更新 `approver_id` = 新审批人，节点状态重置为待审批。

#### 撤回 POST /api/approvals/{instanceId}/cancel

前置条件：申请人为当前用户、当前节点为第一节点、实例状态=审批中。

---

## 7. 包结构

```
com.limou.hrms
├── controller/
│   └── ApprovalController
├── service/
│   ├── ApprovalFlowService              ← 审批引擎接口
│   ├── ApprovalCallback                 ← 业务回调接口
│   ├── OnboardingService                ← 入职模块（实现 ApprovalCallback）
│   ├── ProbationService                 ← 转正模块（实现 ApprovalCallback）
│   └── impl/
│       ├── ApprovalFlowServiceImpl
│       ├── OnboardingServiceImpl
│       └── ProbationServiceImpl
├── mapper/
│   ├── ApprovalInstanceMapper
│   ├── ApprovalNodeMapper
│   └── ApprovalDelegateMapper
├── model/
│   ├── entity/
│   │   ├── ApprovalInstance
│   │   ├── ApprovalNode
│   │   └── ApprovalDelegate
│   ├── dto/
│   │   └── ApprovalActionDTO
│   └── vo/
│       ├── ApprovalInstanceVO
│       └── ApprovalNodeVO
├── enums/
│   ├── ApprovalBizType
│   ├── ApprovalStatus
│   └── NodeStatus
└── builder/
    ├── ApprovalNodeBuilder              ← 构建器接口
    ├── OnboardingNodeBuilder
    ├── ProbationNodeBuilder
    └── ApprovalNodeBuilderFactory
```

风格：与当前项目保持一致，service 下直接放接口，impl 下放实现。

---

## 8. 错误码

| 错误码 | 说明 | HTTP |
|--------|------|------|
| 40001 | 审批实例不存在 | 404 |
| 40002 | 审批节点不存在 | 404 |
| 40003 | 该节点不属于当前用户 | 403 |
| 40004 | 该节点已被处理 | 400 |
| 40005 | 仅第一节点可撤回 | 400 |

---

## 9. 技术栈

| 类别 | 选型 |
|------|------|
| 框架 | Spring Boot |
| ORM | MyBatis-Plus |
| 数据库 | MySQL |
| 事务 | Spring `@Transactional` |

第一版不引入 Redis / RabbitMQ，后续迭代再加。

---

## 10. 回调事务策略

业务回调（`onApproved` / `onRejected`）在审批引擎的 `@Transactional` 事务内执行。如果业务模块的回调逻辑（如生成工号、创建账号）抛异常，整个审批操作回滚，保证一致性。

回调失败的应对：第一版通过事务回滚兜底，后续可加手动重试机制。
