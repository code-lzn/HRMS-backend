# HRMS-后端-审批中心

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | - |
| 2026-07-12 | 1.1 | 根据实际代码实现更新：修正表结构、API路径、实体字段 | - |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 8 部分——审批中心。当前公司审批依赖微信/邮件等碎片化渠道，缺乏统一的审批工作台，审批进度不透明、委托审批无系统支持。本模块旨在建立统一的审批中心，整合入转调离、请假、补卡、薪资批次等所有审批类型的待办列表、审批详情、审批操作（通过/拒绝/转交）、委托审批和审批历史追踪能力，为各业务模块提供标准化的审批流引擎。

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

1. **审批工作台**：待办列表（显示当前用户需要审批的申请，包含直接审批和委托审批）
2. **审批详情**：完整展示申请信息、审批历史时间线（已审批节点及意见）
3. **审批操作**：通过（可选意见）、拒绝（填写原因）、转交（选择审批人+原因）
4. **审批流引擎**：支持多级审批流转，审批人类型包括部门负责人/直接上级/HR负责人/财务专员/老板/指定人
5. **委托审批**：审批人可设置委托其他人代为审批（按业务类型+日期范围），委托期间任务自动转给被委托人，被委托人审批时记录代审批标记

### 功能模块树

```plain
审批中心
├── 审批工作台
│   └── 待办列表
│       ├── 直接审批：当前人是审批人且状态为PENDING
│       └── 委托审批：当前人被委托且委托在有效期内
├── 审批详情
│   ├── 申请信息展示（按业务类型携带businessId）
│   ├── 审批历史时间线
│   └── 审批操作区
│       ├── 通过（可选填写意见）
│       ├── 拒绝（填写原因）
│       └── 转交（选择审批人+原因）
├── 审批流引擎
│   ├── 审批流定义（businessType + flowName + 节点列表）
│   ├── 审批流实例（运行时状态：APPROVING/APPROVED/REJECTED/WITHDRAWN）
│   ├── 多级审批流转（按节点顺序逐级推进）
│   └── 审批人解析（DEPT_MANAGER/HR_MANAGER/DIRECT_SUPERIOR/FINANCE/BOSS/SPECIFIED）
└── 委托审批
    ├── 设置委托人
    ├── 取消委托（仅委托人本人）
    ├── 委托自动路由（根据业务类型范围过滤）
    └── 委托审批记录标记（isDelegated + delegatedBy）
```

## 流程图

> 对审批中心涉及的核心流程进行梳理。

### 8-1 通用审批流转流程

```plain
业务模块调用 startApproval()
     │
     ▼
创建审批流实例 (approval_record)
  ├── 匹配审批流定义 (approval_flow, 按 businessType + status=1)
  ├── 构建审批节点链 (approval_flow_node, 按 nodeOrder 排序)
  ├── resolveApprover() 解析每节点审批人
  └── 创建审批明细 (approval_detail, 全部初始为 PENDING)
     │
     ▼
审批人操作（通过 detailId 定位明细）:
     ├── 通过 (POST /approval/approve)
     │   ├── 校验权限（审批人本人或有效被委托人）
     │   ├── 标记 action=APPROVE
     │   ├── advanceApproval(): currentStep++
     │   └── 若 currentStep > totalSteps → status=APPROVED
     ├── 拒绝 (POST /approval/reject)
     │   ├── 标记 action=REJECT
     │   └── record.status=REJECTED, finishedAt=now
     └── 转交 (POST /approval/transfer)
         ├── 原审批人标记 action=TRANSFER
         └── 创建新 detail 给目标审批人（同节点、同步骤）
```

### 8-2 委托审批路由流程

```plain
validateAndGetDetail(detailId, employeeId)
     │
     ▼
检查是否审批人本人: Objects.equals(detail.approverId, employeeId)
     ├── 是 → 通过权限检查
     └── 否 → 查询委托关系
          ├── approvalDelegationService.isActiveDelegate(
          │       detail.approverId, employeeId, record.businessType)
          │   检查: delegatorId=原审批人 AND delegateId=当前人
          │        AND status=1 AND 日期在有效范围内
          │        AND businessTypes 包含当前业务类型（NULL=全部）
          ├── 有委托 → 标记 detail.isDelegated=1, detail.delegatedBy=原审批人
          └── 无委托 → 抛出 APPROVAL_NO_PERMISSION
```

## 数据库设计

> 审批中心模块涉及的核心数据表（实际实现）。

### 审批流定义表 approval_flow

```sql
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
```

### 审批节点定义表 approval_flow_node

```sql
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
```

### 审批实例表 approval_record

```sql
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
```

### 审批明细表 approval_detail

```sql
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
```

### 审批委托表 approval_delegation

```sql
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
```

## 实体类设计

### ApprovalFlow（审批流定义）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long (AUTO) | 主键 |
| businessType | String | 业务类型：ONBOARDING/REGULARIZATION/TRANSFER/RESIGNATION/LEAVE/PATCH_CLOCK/SALARY_BATCH |
| flowName | String | 审批流名称 |
| description | String | 说明 |
| status | Integer | 1=启用, 0=禁用 |

### ApprovalFlowNode（审批节点定义）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long (AUTO) | 主键 |
| flowId | Long | 审批流ID |
| nodeName | String | 节点名称 |
| nodeOrder | Integer | 节点顺序 |
| approverType | String | 审批人类型：DEPT_MANAGER/HR_MANAGER/DIRECT_SUPERIOR/FINANCE/BOSS/SPECIFIED |
| approverId | Long | 指定审批人ID（SPECIFIED时使用） |
| isOptional | Integer | 是否可选：0=必选, 1=可选 |

### ApprovalRecord（审批实例）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long (AUTO) | 主键 |
| flowId | Long | 审批流定义ID |
| businessType | String | 业务类型 |
| businessId | Long | 业务单据ID |
| applicantId | Long | 申请人employeeId |
| applicantName | String | 申请人姓名（冗余） |
| currentStep | Integer | 当前步骤（从1开始） |
| totalSteps | Integer | 总步骤数 |
| status | String | APPROVING/APPROVED/REJECTED/WITHDRAWN |
| finishedAt | Date | 完成时间 |

### ApprovalDetail（审批明细）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long (AUTO) | 主键 |
| recordId | Long | 审批实例ID |
| nodeId | Long | 节点定义ID |
| nodeName | String | 节点名称（快照） |
| stepOrder | Integer | 步骤序号 |
| approverId | Long | 审批人ID |
| approverName | String | 审批人姓名（冗余） |
| action | String | PENDING/APPROVE/REJECT/TRANSFER |
| comment | String | 审批意见 |
| isDelegated | Integer | 是否代审批：0=否, 1=是 |
| delegatedBy | Long | 委托人ID（代审批时） |
| operateTime | Date | 操作时间 |

### ApprovalDelegation（审批委托）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long (AUTO) | 主键 |
| delegatorId | Long | 委托人employeeId |
| delegatorName | String | 委托人姓名（冗余） |
| delegateId | Long | 被委托人employeeId |
| delegateName | String | 被委托人姓名（冗余） |
| businessTypes | String | 委托业务类型（逗号分隔，NULL=全部） |
| startDate | Date | 委托开始日期 |
| endDate | Date | 委托结束日期 |
| status | Integer | 1=有效, 0=已取消 |

## API 设计

> 基础路径: `/approval`

### 1. 审批工作台 — 待办列表

```plain
GET /approval/pending
```

**请求参数**: 无（自动从登录态获取当前用户）

**响应格式** (`BaseResponse<List<ApprovalPendingVO>>`):

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "recordId": 100,
      "detailId": 201,
      "businessType": "LEAVE",
      "businessTypeText": "请假审批",
      "businessId": 300,
      "applicantName": "张三",
      "applyTime": "2026-07-10T09:00:00",
      "currentNodeName": "部门负责人审批"
    }
  ]
}
```

**业务逻辑**:
1. 查询直接审批项：`approval_detail WHERE approverId=当前employeeId AND action='PENDING'`
2. 查询委托审批项：查找当前人是被委托人的有效委托 → 按委托人ID查待审批 → 按 businessTypes 过滤
3. 合并去重（按 recordId），仅保留 `approval_record.status='APPROVING'` 的项

---

### 2. 审批详情

```plain
GET /approval/detail/{recordId}
```

**响应格式** (`BaseResponse<ApprovalDetailVO>`):

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": 100,
    "businessType": "LEAVE",
    "businessTypeText": "请假审批",
    "businessId": 300,
    "applicantName": "张三",
    "status": "APPROVING",
    "statusText": "审批中",
    "currentStep": 1,
    "totalSteps": 2,
    "applyTime": "2026-07-10T09:00:00",
    "finishedAt": null,
    "nodeHistory": [
      {
        "nodeName": "部门负责人审批",
        "stepOrder": 1,
        "approverName": "李四",
        "action": "PENDING",
        "actionText": "待审批",
        "comment": null,
        "isDelegated": 0,
        "delegatedByName": null,
        "operateTime": null
      },
      {
        "nodeName": "HR负责人审批",
        "stepOrder": 2,
        "approverName": "王HR",
        "action": "PENDING",
        "actionText": "待审批",
        "comment": null,
        "isDelegated": 0,
        "delegatedByName": null,
        "operateTime": null
      }
    ]
  }
}
```

---

### 3. 审批操作

#### 通过

```plain
POST /approval/approve
```

**请求参数** (`ApprovalActionRequest`):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| detailId | Long | 是 | 审批明细ID |
| comment | String | 否 | 审批意见 |

**处理流程**:
1. 校验 detail 存在且 action=PENDING
2. 校验权限（审批人本人或有效被委托人）
3. 更新 action=APPROVE, operateTime=now
4. advanceApproval: currentStep++ → 若超出 totalSteps 则 status=APPROVED

#### 拒绝

```plain
POST /approval/reject
```

**请求参数** (`ApprovalActionRequest`):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| detailId | Long | 是 | 审批明细ID |
| comment | String | 否 | 拒绝原因 |

**处理流程**: 校验同上 → 更新 action=REJECT → 整个 record 标记为 REJECTED, finishedAt=now

#### 转交

```plain
POST /approval/transfer
```

**请求参数** (`ApprovalActionRequest`):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| detailId | Long | 是 | 审批明细ID |
| targetUserId | Long | 是 | 转交目标人employeeId |
| comment | String | 否 | 转交原因 |

**处理流程**:
1. 校验 detail 和权限
2. 原审批人标记 action=TRANSFER
3. 创建新的 approval_detail（同节点、同步骤，审批人为目标人，action=PENDING）

---

### 4. 委托审批

#### 我的委托列表

```plain
GET /approval/delegation/my
```

**响应格式** (`BaseResponse<List<ApprovalDelegationVO>>`):

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "delegatorName": "李四",
      "delegateName": "张三",
      "businessTypes": "LEAVE,PATCH_CLOCK",
      "startDate": "2026-07-01",
      "endDate": "2026-07-31",
      "status": 1,
      "createTime": "2026-07-10T10:00:00"
    }
  ]
}
```

#### 创建委托

```plain
POST /approval/delegation
```

**请求参数** (`DelegationRequest`):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| delegateId | Long | 是 | 被委托人employeeId |
| businessTypes | String | 否 | 委托业务类型（逗号分隔，不传=全部） |
| startDate | Date | 是 | 委托开始日期 |
| endDate | Date | 是 | 委托结束日期 |

**校验规则**: 被委托人必须存在、不能委托给自己、开始日期不能晚于结束日期

#### 取消委托

```plain
POST /approval/delegation/cancel/{id}
```

**说明**: 仅委托人本人可取消自己的委托

---

## 关键技术设计

### Service 架构

```
ApprovalController
    ├── ApprovalService (审批流引擎)
    │   ├── getPendingList()        — 待办列表（含委托）
    │   ├── getApprovalDetail()     — 审批详情
    │   ├── approve()               — 通过
    │   ├── reject()                — 拒绝
    │   ├── transfer()              — 转交
    │   └── startApproval()         — 发起审批（供业务模块调用）
    │
    └── ApprovalDelegationService (委托管理)
        ├── createDelegation()      — 创建委托
        ├── cancelDelegation()      — 取消委托
        ├── getMyDelegations()      — 我的委托列表
        └── isActiveDelegate()      — 检查委托是否有效
```

### 审批人解析 (resolveApprover)

```java
private Long resolveApprover(ApprovalFlowNode node, Employee applicant) {
    switch (node.getApproverType()) {
        case "DEPT_MANAGER":
            // 查申请人所在部门，取部门负责人
            Department dept = departmentService.getById(applicant.getDepartmentId());
            return dept != null ? dept.getManagerId() : null;
        case "DIRECT_SUPERIOR":
            // 查直接上级（需 employee 表扩展 reporterId 字段）
            return null;
        case "HR_MANAGER":
        case "FINANCE":
        case "BOSS":
            // 需角色表支持，当前返回 null
            return null;
        case "SPECIFIED":
            return node.getApproverId();
        default:
            return node.getApproverId();
    }
}
```

> **注意**: DEPT_MANAGER 已实现（通过 Department.managerId），其余类型（DIRECT_SUPERIOR/HR_MANAGER/FINANCE/BOSS）需要额外数据支持，当前返回 null。

### 委托审批路由

委托审批在 `validateAndGetDetail()` 中自动处理：若操作人不是审批人本人，则查 `approval_delegation` 表寻找有效委托关系。找到后自动标记 `isDelegated=1` + `delegatedBy=原审批人ID`，前端可在审批历史中展示"XXX 代 YYY 审批"。

### 审批流推进 (advanceApproval)

```java
private void advanceApproval(Long recordId) {
    ApprovalRecord record = this.getById(recordId);
    int nextStep = record.getCurrentStep() + 1;
    if (nextStep > record.getTotalSteps()) {
        record.setStatus("APPROVED");
        record.setFinishedAt(new Date());
    } else {
        record.setCurrentStep(nextStep);
    }
    this.updateById(record);
}
```

推进仅更新 currentStep，不做节点激活/关闭。前端根据 currentStep 和 stepOrder 判断哪些节点可操作。

### 业务模块集成方式

各业务模块（请假、补卡等）在提交申请时调用 `approvalService.startApproval()`：

```java
ApprovalRecord record = approvalService.startApproval(
    "LEAVE",           // businessType
    leaveRequestId,    // businessId
    employeeId,        // applicantEmployeeId
    employeeName       // applicantName
);
```

## 错误码

| **错误码** | **常量** | **说明** |
| --- | --- | --- |
| 50022 | APPROVAL_NOT_FOUND | 审批记录不存在 |
| 50023 | APPROVAL_NOT_PENDING | 该审批节点已处理 |
| 50024 | APPROVAL_NO_PERMISSION | 无审批权限（非审批人也非有效被委托人） |
| 50025 | DELEGATION_NOT_FOUND | 委托记录不存在 |

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审审批类型汇总、审批流规则、委托审批需求 | 1天 |
| 技术方案 | 完成系分文档评审，确认审批引擎架构 | 2天 |
| 数据库开发 | 建表、索引优化、初始化审批流模板 | 1天 |
| 后端开发 | 审批引擎、审批工作台API、审批详情、审批操作(通过/拒绝/转交)、委托审批 | 5天 |
| 前端开发 | 待办列表、审批详情页（含类型动态渲染）、委托审批设置页 | 4天 |
| 联调测试 | 前后端联调、各类型审批流转测试、委托/转交场景测试 | 3天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 2天 |

> **总预估工期**：约 18 个工作日
