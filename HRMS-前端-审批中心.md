# HRMS-前端-审批中心

# 1. 需求背景

> 建立统一的审批工作台，整合入转调离、请假、补卡、薪资批次等所有审批类型的待办列表、审批详情、审批操作（通过/拒绝/转交）、委托审批和审批历史追踪。

## 1.1 项目成员

| **角色** | **成员** | **备注** |
| --- | --- | --- |
| 业务方 | - | 全公司 |
| 产品经理（PD） | - | |
| 后端技术 | - | |
| UED（设计师） | - | |
| 前端 | - | |
| 质量 | - | |

## 1.2 项目文档

PRD 文档：[HRMS.md](./HRMS.md) 第8章

后端系分：[HRMS-后端-审批中心.md](./HRMS-后端-审批中心.md)

# 2. 详细设计

## 2.1 前端迭代目标

1. 审批工作台：待办列表（含委托审批项）
2. 审批详情页：按审批类型动态渲染申请信息 + 审批历史时间线 + 操作区（通过/拒绝/转交）
3. 委托审批：委托设置页（查看/新建/取消委托）

## 2.2 迭代具体描述

### 2.2.1 审批工作台 — 待办列表

##### UI&交互

- 顶部标题："待审批"
- 列表展示待审批项，包含直接审批和委托审批（委托审批标记"代 xxx 审批"）
- 点击某条待办 → 进入审批详情页
- 列表按申请时间倒序

##### 待办列表展示字段

| 字段名称 | 说明 | 来源字段 |
| --- | --- | --- |
| 审批类型 | Tag + 颜色：入职(蓝)/转正(绿)/调岗(橙)/离职(红)/请假(青)/补卡(紫)/薪资(金) | businessTypeText |
| 申请人 | 姓名 | applicantName |
| 当前节点 | 审批流当前所处节点名称 | currentNodeName |
| 发起时间 | YYYY-MM-DD HH:mm | applyTime |
| 操作 | 查看详情按钮 | - |

##### 所需API

| 接口 | 说明 |
| --- | --- |
| GET /approval/pending | 获取待办列表 |

**响应数据结构** (`BaseResponse<List<ApprovalPendingVO>>`):

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

> **注意**: `detailId` 用于后续审批操作（通过/拒绝/转交时传给后端），`businessId` 用于查询业务详情。

---

### 2.2.2 审批详情页（核心页面）

##### 路由

```
/approval/detail/:recordId?detailId=xxx
```

##### UI 布局

```
┌──────────────────────────────────────────┐
│  ← 返回工作台                             │
│  审批类型 Tag  +  业务标题                 │
├──────────────────────────────────────────┤
│                                          │
│  ┌── 申请信息 ─────────────────────────┐  │
│  │ （按审批类型动态渲染不同字段）        │  │
│  │  通过 businessType 判断渲染哪个组件   │  │
│  │  通过 businessId 查询业务详情         │  │
│  └──────────────────────────────────────┘  │
│                                          │
│  ┌── 审批历史 ─────────────────────────┐  │
│  │  Timeline 时间线组件                 │  │
│  │  ○ 节点1：部门负责人 — 李四          │  │
│  │  │  通过 · 2026-07-10 10:00          │  │
│  │  │  意见：同意                        │  │
│  │  │                                  │  │
│  │  ● 节点2：HR负责人 — 王HR（当前）     │  │
│  │     待审批                            │  │
│  └──────────────────────────────────────┘  │
│                                          │
│  ┌── 审批操作（仅当前审批人可见）────────┐  │
│  │  [通过]  [拒绝]  [转交]              │  │
│  └──────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

##### 审批历史时间线渲染

- Timeline 组件（Ant Design）
- 已完成节点（action != PENDING）：绿色圆点 + 通过/拒绝/转交 Tag
  - 代审批标记：`isDelegated=1` 时显示 "xxx 代 yyy 审批"
- 当前节点（action = PENDING 且 stepOrder = currentStep）：蓝色圆点 + 待审批
- 未到达节点（action = PENDING 且 stepOrder > currentStep）：灰色圆点
- 按 stepOrder 正序排列

##### 审批操作

操作按钮仅在"当前用户是当前节点的审批人"时显示。判断逻辑：从 `nodeHistory` 中找到 `stepOrder == currentStep` 且 `action == 'PENDING'` 的节点。

| 按钮 | 交互 | 必填项 | 二次确认 |
| --- | --- | --- | --- |
| 通过 | Modal 输入意见（可选） | N | Y |
| 拒绝 | Modal 输入原因 | N（建议必填） | Y |
| 转交 | Modal 选择转交人 + 原因 | targetUserId: Y | Y |

##### 审批操作请求参数

**通过 / 拒绝** (`POST /approval/approve` / `POST /approval/reject`):

```json
{
  "detailId": 201,
  "comment": "同意"
}
```

**转交** (`POST /approval/transfer`):

```json
{
  "detailId": 201,
  "targetUserId": 25,
  "comment": "我不在期间由王五处理"
}
```

##### 审批类型 → 动态渲染配置

| 审批类型 | 渲染组件 | 数据字段 |
| --- | --- | --- |
| ONBOARDING (入职审批) | 入职信息卡片 | 姓名/性别/手机/邮箱/入职日期/部门/职位等 |
| REGULARIZATION (转正审批) | 转正评估卡片 | 员工信息/试用期起止/表现评价等 |
| TRANSFER (调岗审批) | 调岗对比卡片 | 原部门→新部门/原职位→新职位/原因 |
| RESIGNATION (离职审批) | 离职信息卡片 | 员工信息/离职日期/离职类型/原因 |
| LEAVE (请假审批) | 请假信息卡片 | 请假类型/起止时间/天数/事由 (已有接口) |
| PATCH_CLOCK (补卡审批) | 补卡信息卡片 | 补卡日期/补卡类型/原因 |
| SALARY_BATCH (薪资审批) | 薪资批次卡片 | 月份/人数/应发合计/实发合计/异常统计 |

##### 所需API

| 接口 | 说明 |
| --- | --- |
| GET /approval/detail/{recordId} | 审批详情（含节点历史） |
| POST /approval/approve | 通过 |
| POST /approval/reject | 拒绝 |
| POST /approval/transfer | 转交 |

**审批详情响应** (`BaseResponse<ApprovalDetailVO>`):

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
        "action": "APPROVE",
        "actionText": "通过",
        "comment": "同意",
        "isDelegated": 0,
        "delegatedByName": null,
        "operateTime": "2026-07-10T10:00:00"
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

### 2.2.3 委托审批设置页

##### 路由

```
/approval/delegation
```

##### UI

- 当前生效委托列表（表格形式）
- 「新建委托」按钮 → 弹窗
- 每个委托记录：委托人 → 被委托人 + 委托时间范围 + 委托类型范围 + 状态 + 取消按钮
- status: 1=生效中（绿色）, 0=已取消（灰色）

##### 委托列表展示字段

| 字段名称 | 说明 |
| --- | --- |
| 被委托人 | delegateName |
| 委托类型 | businessTypes（逗号分隔展示，空则显示"全部"） |
| 开始日期 | startDate |
| 结束日期 | endDate |
| 状态 | status (1=生效中, 0=已取消) |
| 创建时间 | createTime |
| 操作 | 取消按钮（仅生效中的委托可取消） |

##### 新建委托弹窗

| 字段名称 | 说明 | 输入方式 | 是否必填 | 数据源 |
| --- | --- | --- | --- | --- |
| 被委托人 | 选择 | Select + 搜索 | Y | 员工搜索接口 |
| 开始日期 | >= 今天 | DatePicker | Y | - |
| 结束日期 | > 开始日期 | DatePicker | Y | - |
| 委托类型 | 空=全部类型 | Select multiple | N | 审批类型枚举 |

##### 所需API

| 接口 | 说明 |
| --- | --- |
| GET /approval/delegation/my | 我的委托列表 |
| POST /approval/delegation | 创建委托 |
| POST /approval/delegation/cancel/{id} | 取消委托 |

**创建委托请求** (`DelegationRequest`):

```json
{
  "delegateId": 20,
  "businessTypes": "LEAVE,PATCH_CLOCK",
  "startDate": "2026-07-12",
  "endDate": "2026-07-31"
}
```

---

## 2.3 菜单与权限变动

| 菜单路径 | 权限 | 备注 |
| --- | --- | --- |
| 审批中心 > 审批工作台 | 所有登录用户 | 每人看到自己的待办 |
| 审批中心 > 委托审批 | 所有登录用户 | |

---

## 2.4 模块划分与工作量评估

| 模块 | 细节 | 开发 | 联调 | 自测 |
| --- | --- | --- | --- | --- |
| 审批工作台 | 待办列表 + 类型筛选 | 1 | 0.5 | 0.5 |
| 审批详情页 | 7种类型动态渲染 + Timeline + 操作 | 2.5 | 0.5 | 0.5 |
| 委托审批 | 委托列表 + 新建/取消弹窗 | 1 | 0.5 | 0.5 |

\[20260711~~20260713\] 系分编写+评审
\[20260714~~20260717\] 实现审批工作台+详情页（核心）
\[20260717~~20260718\] 实现委托审批
\[20260719~~20260722\] 联调（重点测试各类型的动态渲染和状态流转）
\[20260722~~20260724\] 测试

# 3. 监控和埋点

- 审批操作埋点（通过/拒绝/转交，按类型拆分）
- 委托设置埋点

# 4. 发布计划

- 预发验证：7种审批类型详情页逐一验证 + 操作流转
- 灰度发布：先开放给HR部门
- 回滚方案：回滚前端版本

# 5. 其他

## 5.1 风险评估

- 7种审批类型的申请信息字段各不相同，需设计好动态渲染配置（用 businessType → component mapping）
- 委托审批详情页需正确标记代审批信息（isDelegated + delegatedByName）
- 转交后原审批节点会产生多条 detail（原审批人 TRANSFER + 新审批人 PENDING），时间线需按 operateTime 处理

## 5.2 变更记录

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | - |
| 2026-07-12 | 1.1 | 根据实际后端实现更新API路径、请求/响应结构、移除未实现功能 | - |
