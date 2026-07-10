# HRMS-后端-审批中心

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | - |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 8 部分——审批中心。当前公司审批依赖微信/邮件等碎片化渠道，缺乏统一的审批工作台，审批进度不透明、委托审批无系统支持、审批超时无自动处理。本模块旨在建立统一的审批中心，整合入转调离、请假、补卡、薪资批次等所有审批类型的待办列表、审批详情、审批操作（通过/拒绝/转交）、委托审批和审批历史追踪能力，为各业务模块提供标准化的审批流引擎。

### 相关资料

- [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述审批中心涉及的功能与场景。

本模块核心功能包括：

1. **审批工作台**：待办列表（显示当前用户需要审批的申请，按类型/时间筛选）、已办列表（历史审批记录）
2. **审批详情**：完整展示申请信息（按审批类型动态渲染不同字段）、审批历史时间线（已审批节点及意见）、审批操作（通过/拒绝需填写理由、转交选择审批人）
3. **审批流引擎**：支持多级审批、条件分支（如二审开关）、审批转交、审批超时升级/催办
4. **委托审批**：审批人可设置委托其他人代为审批，委托期间任务自动转给被委托人，委托人可随时取消，被委托人审批时记录"XXX 代 YYY 审批"
5. **审批类型汇总**：入职审批、转正审批、调岗审批、离职审批、请假审批、补卡审批、薪资批次审批、加班审批

### 功能模块树

```plain
审批中心
├── 审批工作台
│   ├── 待办列表
│   │   ├── 按审批类型筛选
│   │   ├── 按时间排序
│   │   └── 超时标记（红色高亮）
│   ├── 已办列表
│   │   └── 历史审批记录
│   └── 我发起的申请
│       └── 查看自己提交的审批进度
├── 审批详情
│   ├── 申请信息展示（按类型动态渲染）
│   ├── 审批历史时间线
│   └── 审批操作区
│       ├── 通过（可选填写意见）
│       ├── 拒绝（必填原因）
│       └── 转交（选择审批人）
├── 审批流引擎
│   ├── 审批流定义（类型+节点+条件）
│   ├── 审批流实例（运行时状态）
│   ├── 多级审批流转
│   ├── 条件分支（二审开关：非标准职位/薪资超范围）
│   ├── 转交处理
│   ├── 撤回处理（仅第一级发起人可撤回）
│   └── 超时处理（48小时自动升级/催办提醒）
└── 委托审批
    ├── 设置委托人
    ├── 取消委托
    ├── 委托自动路由
    └── 委托审批记录标记
```

## 流程图

> 对审批中心涉及的核心流程进行梳理。

### 8-1 通用审批流转流程

```plain
业务模块提交审批
     │
     ▼
创建审批流实例
  ├── 确定审批类型
  ├── 构建审批节点链（根据类型规则 + 条件分支）
  └── 当前节点 = 第一个审批节点
     │
     ▼
通知当前节点审批人（站内消息 + [邮件/短信]）
     │
     ▼
审批人操作：
     ├── 通过
     │   ├── 是否有下一节点？
     │   │   ├── 是 → 流转至下一节点
     │   │   └── 否 → 审批完成 → 回调业务模块
     │   └── 记录审批历史（通过 + 意见）
     ├── 拒绝
     │   ├── 流程终止
     │   ├── 记录审批历史（拒绝 + 原因）
     │   └── 回调业务模块（通知发起人）
     └── 转交
         ├── 变更为新审批人
         ├── 记录审批历史（XXX 转交至 YYY）
         └── 重新通知新审批人
```

### 8-2 审批超时处理流程

```plain
定时任务：每小时执行
     │
     ▼
扫描所有审批中的审批流实例
  WHERE status = 'APPROVING'
  AND current_node_create_time + 48小时 <= NOW()
     │
     ▼
对每个超时审批：
     ├── 是否有上级审批人？
     │   ├── 是 → 自动升级至上级审批人
     │   │      记录 "XXX 超时未处理，自动升级至 YYY"
     │   └── 否 → 发送催办通知（站内+邮件）
     └── 给当前审批人发送超时提醒
```

### 8-3 委托审批路由流程

```plain
审批流引擎确定当前审批人
     │
     ▼
查询当前审批人是否有生效中的委托
  SELECT * FROM approval_delegation
  WHERE delegator_id = currentApproverId
  AND start_date <= NOW() AND end_date >= NOW()
  AND status = 'ACTIVE'
     │
     ├── 有委托 → 实际审批人 = 被委托人(delegate_id)
     │           记录 "待 XXX（由 YYY 代审）"
     ├── 无委托 → 实际审批人 = currentApproverId
     │
     ▼
通知实际审批人
```

## UML 图

> 描述审批中心的核心类和依赖关系。

### 审批核心领域模型

```plain
@startuml
class ApprovalDefinition {
  - id: Long
  - approvalType: ApprovalTypeEnum   "审批类型"
  - name: String                     "审批流名称"
  - nodes: List<NodeDefinition>      "节点定义列表"
  - isActive: Boolean                "是否启用"
}

class NodeDefinition {
  - id: Long
  - definitionId: Long               "所属审批流"
  - nodeOrder: Integer               "节点顺序"
  - approverType: ApproverType       "审批人类型：指定人/角色/部门负责人/直接上级"
  - approverId: Long                 "指定审批人ID（approverType=指定人时）"
  - roleType: Integer                "角色类型（approverType=角色时）"
  - conditionField: String           "条件字段"
  - conditionValue: String           "条件值（如薪资超出范围时启用该节点）"
  - timeoutHours: Integer            "超时小时数"
}

class ApprovalInstance {
  - id: Long
  - definitionId: Long               "审批流定义ID"
  - businessType: String             "业务类型"
  - businessId: Long                 "业务单据ID"
  - status: InstanceStatus           "审批流状态"
  - currentNodeOrder: Integer        "当前审批节点序号"
  - currentNodeApproverId: Long      "当前节点审批人"
  - applicantId: Long                "发起人ID"
  - createTime: LocalDateTime
  - finishTime: LocalDateTime
}

class ApprovalRecord {
  - id: Long
  - instanceId: Long                 "审批流实例ID"
  - nodeOrder: Integer               "审批节点序号"
  - approverId: Long                 "审批人ID"
  - actualApproverId: Long           "实际审批人ID（处理转交/委托）"
  - action: ApprovalAction           "操作：通过/拒绝/转交"
  - opinion: String                  "审批意见"
  - isDelegated: Boolean             "是否委托审批"
  - delegatorId: Long                "原审批人ID（委托审批时）"
  - createTime: LocalDateTime
}

class ApprovalDelegation {
  - id: Long
  - delegatorId: Long                "委托人ID"
  - delegateId: Long                 "被委托人ID"
  - startDate: LocalDate             "委托开始日期"
  - endDate: LocalDate               "委托结束日期"
  - approvalTypes: String            "委托审批类型范围(JSON数组，空=全部)"
  - status: DelegationStatus         "状态"
  - cancelTime: LocalDateTime        "取消时间"
}

enum ApprovalTypeEnum {
  ONBOARDING       "入职审批"
  REGULARIZATION   "转正审批"
  TRANSFER         "调岗审批"
  RESIGNATION      "离职审批"
  LEAVE            "请假审批"
  MAKEUP           "补卡审批"
  SALARY_BATCH     "薪资批次审批"
  OVERTIME         "加班审批"
}

enum ApproverType {
  SPECIFIED    "指定人"
  ROLE         "角色"
  DEPT_MANAGER "部门负责人"
  DIRECT_SUPERIOR "直接上级"
}

enum InstanceStatus {
  APPROVING    "审批中"
  APPROVED     "已通过"
  REJECTED     "已拒绝"
  WITHDRAWN    "已撤回"
  CANCELLED    "已取消"
}

enum ApprovalAction {
  APPROVE  "通过"
  REJECT   "拒绝"
  TRANSFER "转交"
}

enum DelegationStatus {
  ACTIVE   "生效中"
  CANCELLED "已取消"
  EXPIRED  "已过期"
}

ApprovalDefinition "1" -- "*" NodeDefinition
ApprovalDefinition "1" -- "*" ApprovalInstance
ApprovalInstance "1" -- "*" ApprovalRecord
@enduml
```

## 时序图

### 8-1 通用审批提交时序

![通用审批提交时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-approval-submit.svg)

### 8-2 审批流转时序

![审批流转时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-approval-flow.svg)

### 8-3 委托审批时序

![委托审批时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-approval-delegate.svg)

## 数据库设计

> 审批中心模块涉及的核心数据表。

### 审批流定义表 approval_definition

```sql
CREATE TABLE IF NOT EXISTS `approval_definition` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `approval_type`       VARCHAR(32)       NOT NULL COMMENT '审批类型：ONBOARDING/REGULARIZATION/TRANSFER/RESIGNATION/LEAVE/MAKEUP/SALARY_BATCH/OVERTIME',
    `name`                VARCHAR(64)       NOT NULL COMMENT '审批流名称',
    `is_active`           TINYINT           NOT NULL DEFAULT 1 COMMENT '是否启用：0=否 1=是',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_approval_type` (`approval_type`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '审批流定义表';
```

### 审批节点定义表 approval_node_definition

```sql
CREATE TABLE IF NOT EXISTS `approval_node_definition` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `definition_id`       BIGINT UNSIGNED   NOT NULL COMMENT '所属审批流定义ID',
    `node_order`          INT               NOT NULL COMMENT '节点顺序，从1开始',
    `approver_type`       TINYINT           NOT NULL COMMENT '审批人类型：1=指定人 2=角色 3=部门负责人 4=直接上级',
    `approver_id`         BIGINT UNSIGNED            COMMENT '指定审批人ID（approver_type=1时）',
    `role_type`           VARCHAR(32)                COMMENT '角色类型（approver_type=2时）：HR/HR_MANAGER/DEPT_MANAGER/FINANCE',
    `condition_field`     VARCHAR(64)                COMMENT '条件字段名（控制该节点是否启用）',
    `condition_value`     VARCHAR(256)               COMMENT '条件值（匹配时启用该节点）',
    `timeout_hours`       INT               NOT NULL DEFAULT 48 COMMENT '审批超时小时数',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_definition_id` (`definition_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '审批节点定义表';
```

### 审批流实例表 approval_instance

```sql
CREATE TABLE IF NOT EXISTS `approval_instance` (
    `id`                        BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `definition_id`             BIGINT UNSIGNED   NOT NULL COMMENT '审批流定义ID',
    `business_type`             VARCHAR(32)       NOT NULL COMMENT '业务类型，同approval_type',
    `business_id`               BIGINT UNSIGNED   NOT NULL COMMENT '业务单据ID',
    `status`                    TINYINT           NOT NULL DEFAULT 1 COMMENT '状态：1=审批中 2=已通过 3=已拒绝 4=已撤回 5=已取消',
    `current_node_order`        INT               NOT NULL DEFAULT 1 COMMENT '当前审批节点序号',
    `current_node_approver_id`  BIGINT UNSIGNED            COMMENT '当前节点审批人ID',
    `applicant_id`              BIGINT UNSIGNED   NOT NULL COMMENT '发起人ID',
    `node_chain`                JSON              NOT NULL COMMENT '审批节点链快照（JSON，记录节点顺序及审批人）',
    `create_time`               DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（发起时间）',
    `finish_time`               DATETIME                   COMMENT '完成时间',
    `update_time`               DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_business_type_id` (`business_type`, `business_id`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_current_approver` (`current_node_approver_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '审批流实例表';
```

### 审批记录表 approval_record

```sql
CREATE TABLE IF NOT EXISTS `approval_record` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id`         BIGINT UNSIGNED   NOT NULL COMMENT '审批流实例ID',
    `node_order`          INT               NOT NULL COMMENT '审批节点序号',
    `approver_id`         BIGINT UNSIGNED   NOT NULL COMMENT '原审批人ID',
    `actual_approver_id`  BIGINT UNSIGNED   NOT NULL COMMENT '实际审批人ID（转交/委托后可能与approver_id不同）',
    `action`              TINYINT           NOT NULL COMMENT '操作：1=通过 2=拒绝 3=转交',
    `opinion`             VARCHAR(512)               COMMENT '审批意见/拒绝原因/转交说明',
    `is_delegated`        TINYINT           NOT NULL DEFAULT 0 COMMENT '是否委托审批：0=否 1=是',
    `delegator_id`        BIGINT UNSIGNED            COMMENT '原委托人ID（is_delegated=1时有值）',
    `transfer_to_id`      BIGINT UNSIGNED            COMMENT '转交目标人ID（action=3时有值）',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_approver_id` (`approver_id`),
    KEY `idx_create_time` (`create_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '审批记录表';
```

### 委托审批表 approval_delegation

```sql
CREATE TABLE IF NOT EXISTS `approval_delegation` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `delegator_id`        BIGINT UNSIGNED   NOT NULL COMMENT '委托人ID',
    `delegate_id`         BIGINT UNSIGNED   NOT NULL COMMENT '被委托人ID',
    `start_date`          DATE              NOT NULL COMMENT '委托开始日期',
    `end_date`            DATE              NOT NULL COMMENT '委托结束日期',
    `approval_types`      VARCHAR(512)               COMMENT '委托审批类型范围（JSON数组，空=全部类型）',
    `status`              TINYINT           NOT NULL DEFAULT 1 COMMENT '状态：1=生效中 2=已取消 3=已过期',
    `cancel_time`         DATETIME                   COMMENT '取消时间',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_delegator_id` (`delegator_id`),
    KEY `idx_delegate_id` (`delegate_id`),
    KEY `idx_status_date` (`status`, `end_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '委托审批表';
```

## API 设计

### 1. 审批工作台

```plain
GET    /api/v1/approvals/pending       # 我的待办列表
GET    /api/v1/approvals/processed     # 我的已办列表
GET    /api/v1/approvals/initiated     # 我发起的申请列表
```

#### 待办列表请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| approvalType | String | 否 | 审批类型过滤 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |

#### 待办列表响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 5,
    "records": [
      {
        "instanceId": 2001,
        "approvalType": "LEAVE",
        "approvalTypeDesc": "请假审批",
        "businessId": 300,
        "summary": "张三 - 年假 3天 (2024-07-15 ~ 2024-07-17)",
        "applicantId": 1001,
        "applicantName": "张三",
        "createTime": "2024-07-10T09:00:00",
        "timeoutTime": "2024-07-12T09:00:00",
        "isTimeout": false
      }
    ]
  }
}
```

---

### 2. 审批详情

```plain
GET    /api/v1/approvals/{instanceId}/detail    # 审批详情
```

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "instanceId": 2001,
    "approvalType": "LEAVE",
    "approvalTypeDesc": "请假审批",
    "status": 1,
    "statusDesc": "审批中",
    "applicantName": "张三",
    "applicantDept": "技术部",
    "businessData": {
      "leaveType": "年假",
      "startTime": "2024-07-15 09:00",
      "endTime": "2024-07-17 18:00",
      "leaveDays": 3,
      "reason": "家庭事务"
    },
    "history": [
      {
        "nodeOrder": 1,
        "nodeName": "直接上级审批",
        "approverName": "李四",
        "action": 1,
        "actionDesc": "通过",
        "opinion": "同意",
        "createTime": "2024-07-10T10:00:00"
      }
    ],
    "currentNode": {
      "nodeOrder": 2,
      "nodeName": "部门负责人审批",
      "approverId": 200,
      "approverName": "王五"
    }
  }
}
```

---

### 3. 审批操作

```plain
POST   /api/v1/approvals/{instanceId}/approve   # 通过
POST   /api/v1/approvals/{instanceId}/reject    # 拒绝
POST   /api/v1/approvals/{instanceId}/transfer  # 转交
```

#### 通过请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| opinion | String | 否 | 审批意见 |

#### 拒绝请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| reason | String | 是 | 拒绝原因 |

#### 转交请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| transferToId | Long | 是 | 转交目标审批人ID |
| reason | String | 是 | 转交原因 |

---

### 4. 撤回审批

```plain
POST   /api/v1/approvals/{instanceId}/withdraw  # 撤回（仅发起人，仅限第一级审批中）
```

---

### 5. 委托审批

```plain
POST   /api/v1/approval-delegations              # 设置委托
PUT    /api/v1/approval-delegations/{id}/cancel  # 取消委托
GET    /api/v1/approval-delegations              # 我的委托列表
GET    /api/v1/approval-delegations/delegated-to-me  # 委托给我的列表
```

#### 设置委托请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| delegateId | Long | 是 | 被委托人ID |
| startDate | Date | 是 | 委托开始日期 |
| endDate | Date | 是 | 委托结束日期 |
| approvalTypes | String[] | 否 | 委托审批类型，空=全部 |

---

### 6. 审批流定义管理（系统管理员）

```plain
GET    /api/v1/approval-definitions              # 审批流定义列表
GET    /api/v1/approval-definitions/{id}         # 审批流定义详情（含节点）
POST   /api/v1/approval-definitions              # 新建审批流定义
PUT    /api/v1/approval-definitions/{id}         # 更新审批流定义
```

---

## 关键技术设计

### 审批流引擎

审批流引擎是核心组件，提供统一的审批流转能力，与业务解耦：

```java
public interface ApprovalEngine {
    // 发起审批
    ApprovalInstance startApproval(String businessType, Long businessId,
                                    Long applicantId, Map<String, Object> context);
    // 审批通过
    void approve(Long instanceId, Long approverId, String opinion);
    // 拒绝
    void reject(Long instanceId, Long approverId, String reason);
    // 转交
    void transfer(Long instanceId, Long approverId, Long transferToId, String reason);
    // 撤回
    void withdraw(Long instanceId, Long applicantId);
    // 审批完成回调
    void onApprovalComplete(Long instanceId, boolean isApproved);
}
```

### 审批节点链快照

审批流实例创建时，将审批节点链快照保存到 `node_chain` JSON 字段：

```json
[
  { "nodeOrder": 1, "approverType": "DEPT_MANAGER", "approverId": 200, "approverName": "李四", "status": "APPROVED" },
  { "nodeOrder": 2, "approverType": "ROLE", "roleType": "HR_MANAGER", "approverId": 300, "approverName": "王HR", "status": "PENDING", "conditionField": "isNonStandard", "conditionValue": "true" }
]
```

好处：
- 审批过程中即使组织架构变更，不影响已创建的审批流
- 前端可直接读取 node_chain 渲染审批进度

### 条件节点（二审开关）

以入职审批为例，非标准职位或薪资超出职级范围时需要 HR 负责人二审：

```java
public List<ApprovalNode> buildNodeChain(ApprovalDefinition definition,
                                          Map<String, Object> context) {
    List<ApprovalNode> nodes = new ArrayList<>();
    for (NodeDefinition nodeDef : definition.getNodes()) {
        // 检查条件节点是否启用
        if (nodeDef.getConditionField() != null) {
            String conditionField = nodeDef.getConditionField();
            String expectedValue = nodeDef.getConditionValue();
            String actualValue = String.valueOf(context.get(conditionField));
            if (!expectedValue.equals(actualValue)) {
                continue; // 条件不满足，跳过该节点
            }
        }
        // 确定审批人
        Long approverId = resolveApprover(nodeDef, context);
        nodes.add(new ApprovalNode(nodeDef.getNodeOrder(), approverId, nodeDef.getTimeoutHours()));
    }
    return nodes;
}
```

### 审批回调机制

审批完成后通过事件机制通知业务模块：

```java
@Component
public class ApprovalEventPublisher {
    @Autowired
    private ApplicationEventPublisher publisher;
    
    public void publishApprovalComplete(Long instanceId, String businessType,
                                         Long businessId, boolean isApproved) {
        publisher.publishEvent(new ApprovalCompleteEvent(this, instanceId,
            businessType, businessId, isApproved));
    }
}

// 各业务模块监听
@Component
public class OnboardingApprovalListener {
    @EventListener
    public void handleApprovalComplete(ApprovalCompleteEvent event) {
        if (!"ONBOARDING".equals(event.getBusinessType())) return;
        if (event.isApproved()) {
            onboardingService.processHire(event.getBusinessId()); // 生成工号、创建账号...
        } else {
            onboardingService.markRejected(event.getBusinessId());
        }
    }
}
```

### 委托审批路由

```java
public Long resolveActualApprover(Long approverId, String approvalType) {
    ApprovalDelegation delegation = delegationMapper.findActiveByDelegator(
        approverId, LocalDate.now());
    if (delegation != null) {
        // 检查委托范围是否包含当前审批类型
        if (delegation.getApprovalTypes() == null 
            || delegation.getApprovalTypes().contains(approvalType)) {
            return delegation.getDelegateId();
        }
    }
    return approverId;
}
```

### 超时处理

```java
@Scheduled(cron = "0 0 * * * ?") // 每小时执行
public void handleTimeoutApprovals() {
    List<ApprovalInstance> timeoutInstances = instanceMapper
        .findApprovingTimeoutBefore(LocalDateTime.now().minusHours(48));
    
    for (ApprovalInstance instance : timeoutInstances) {
        // 查找当前审批人的直接上级
        Long superiorId = employeeService.getDirectSuperior(
            instance.getCurrentNodeApproverId());
        if (superiorId != null) {
            // 自动升级
            approvalEngine.escalate(instance.getId(), superiorId);
        } else {
            // 无上级，催办
            notificationService.sendUrgeNotification(
                instance.getCurrentNodeApproverId(), instance.getId());
        }
    }
}
```

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审审批类型汇总、审批流规则、委托审批需求 | 1天 |
| 技术方案 | 完成系分文档评审，确认审批引擎架构、条件分支、超时升级方案 | 2天 |
| 数据库开发 | 建表、索引优化、初始化审批流模板 | 1天 |
| 后端开发 | 审批引擎、审批工作台API、审批详情、审批操作(通过/拒绝/转交)、委托审批、超时处理、审批流定义管理 | 7天 |
| 前端开发 | 待办/已办列表、审批详情页（含类型动态渲染）、委托审批设置页 | 4天 |
| 联调测试 | 前后端联调、各类型审批流转测试、委托/转交/超时场景测试 | 3天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 2天 |

> **总预估工期**：约 20 个工作日
