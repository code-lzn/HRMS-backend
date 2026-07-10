# HRMS-后端-入转调离流程

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | - |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 5 部分——入转调离流程。当前公司员工入职、转正、调岗、离职四大异动流程依赖线下纸质审批，存在审批周期长、数据同步不及时、历史追溯困难等问题。本模块旨在实现入转调离全流程线上化，涵盖状态流转、多级审批、自动处理（工号生成、账号创建、邮件通知）等核心能力，与员工档案、组织架构、审批中心模块深度联动。

### 相关资料

- [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述入转调离涉及的功能与场景。

本模块核心功能包括：

1. **入职流程**：候选人信息录入（草稿/提交）、多级审批（部门负责人 → HR负责人可选二审）、审批通过后自动生成工号+创建账号+发送邮件
2. **转正流程**：系统自动提醒（到期前7天）、HR发起转正评估、审批流转（部门负责人 → HR负责人）、支持延长试用/不通过
3. **调岗流程**：HR发起调岗申请、三方审批（原部门负责人知情 → 新部门负责人接收 → HR负责人备案）、支持薪资调整附审
4. **离职流程**：HR发起离职申请、审批流转（部门负责人确认交接 → HR负责人确认）、到达离职日期自动禁用账号、工号标记释放

### 功能模块树

```plain
入转调离流程
├── 入职管理
│   ├── 候选人信息录入（入职申请表单）
│   ├── 保存草稿 / 编辑草稿
│   ├── 提交审批
│   ├── 审批流转（一级：部门负责人 / 二级：HR负责人可选）
│   ├── 审批结果处理（通过→自动处理 / 拒绝→打回 / 转交）
│   ├── 入职确认（HR确认实际到岗）
│   └── 放弃入职标记
├── 转正管理
│   ├── 转正提醒（到期前7天自动提醒HR）
│   ├── 转正申请发起（HR填写评估）
│   ├── 审批流转（部门负责人 → HR负责人）
│   ├── 审批结果处理（通过→状态变正式 / 延长试用 / 不通过→辞退）
│   └── 转正后薪资调整（可选审批）
├── 调岗管理
│   ├── 调岗申请发起（HR填写变更信息）
│   ├── 审批流转（原部门负责人 → 新部门负责人 → HR负责人）
│   ├── 调岗生效处理（更新档案、记录调岗历史）
│   └── 薪资调整附审（可选）
└── 离职管理
    ├── 离职申请发起（HR填写离职信息）
    ├── 审批流转（部门负责人 → HR负责人）
    ├── 待离职状态管理（等待最后工作日）
    └── 离职生效处理（状态变更、账号禁用、工号释放、考勤移除）
```

## 流程图

> 对入转调离涉及的核心流程进行梳理。

### 5-1 入职审批通过后自动处理流程

```plain
入职审批通过
     │
     ▼
开启事务
     │
     ├── 1. 调用工号生成服务
     │      year = 当前年份
     │      deptCode = 部门编码
     │      seq = SELECT ... FOR UPDATE + UPDATE +1
     │      employeeNo = year + deptCode + format(seq, "03d")
     │
     ├── 2. 创建系统账号
     │      account = 手机号
     │      password = 随机生成(8位+大小写+数字)
     │      密码加密存储
     │
     ├── 3. 写入员工档案
     │      INSERT INTO employee (employeeNo, account, password, status, hireDate, hireType, ...)
     │      INSERT INTO employee_personal_info (...)
     │      INSERT INTO employee_work_info (...)
     │      INSERT INTO employee_salary_info (...)
     │
     ├── 4. 发送通知（异步）
     │      发送欢迎邮件给候选人
     │      发送确认通知给 HR 和部门负责人
     │
     └── 5. 提交事务
     │
     ▼
入职自动处理完成
```

### 5-2 转正触发流程

```plain
定时任务：每天 08:00 执行
     │
     ▼
扫描员工表
  SELECT * FROM employee
  WHERE status = 1 (试用期)
  AND hire_date + 试用期月数 - 7天 <= 今天
     │
     ▼
对每个符合条件的员工
     │
     ▼
检查是否已发送提醒（防止重复提醒）
  → 已提醒则跳过
  → 未提醒则创建提醒记录 + 发送通知给HR
     │
     ▼
HR收到通知后 → 进入转正管理页发起评估
```

### 5-3 离职生效处理流程

```plain
定时任务：每天 00:05 执行
     │
     ▼
扫描员工表
  SELECT * FROM employee
  WHERE status = 3 (待离职)
  AND leave_date <= 今天
     │
     ▼
对每个到期员工执行：
     ├── 更新员工状态 status = 4 (已离职)
     ├── 禁用系统账号 (is_disabled = 1)
     ├── 标记工号序列可复用
     ├── 从考勤组移除
     ├── 计算薪资至离职日期（通知薪资模块）
     ├── 脱敏敏感字段（身份证、银行卡号）
     └── 记录操作日志
```

## 流程图（简写 ASCII）

### 5-4 入职状态流转

```plain
新建候选人
     │ 填写信息
     ▼
┌──────────┐    保存为草稿      ┌──────────┐
│ 信息录入  │ ─────────────────> │   草稿   │
└─────┬────┘                     └────┬─────┘
      │                                │
      │ 填写完整，提交审批              │ 编辑后提交
      ▼                                ▼
┌──────────────────────────────────────────────────┐
│                   审批中                          │
│  审批流：部门负责人 → [HR负责人]（可选）         │
└─────────────────────────┬────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
   审批通过           审批拒绝           转交他人
        │                 │                 │
        ▼                 ▼                 ▜────> 新审批人
  ┌──────────┐       ┌──────────┐
  │ 已批准待  │       │  已拒绝   │
  │   入职   │       │ (可重新)  │
  └────┬─────┘       └──────────┘
       │
       │ 实际到岗，HR确认
       ▼
  ┌──────────┐
  │   已入职  │  ← 进入员工档案，开始计算考勤/薪资
  └──────────┘
```

## UML 图

> 描述入转调离的核心类和依赖关系。

### 入转调离核心领域模型

```plain
@startuml
class OnboardingApplication {
  - id: Long
  - name: String               "姓名"
  - gender: Integer            "性别"
  - phone: String              "手机号"
  - email: String              "邮箱"
  - idCard: String             "身份证号"
  - expectedHireDate: Date     "预计入职日期"
  - departmentId: Long         "所属部门"
  - positionId: Long           "职位"
  - hireType: Integer          "录用类型：1=全职 2=兼职 3=实习"
  - probationMonths: Int       "试用期(月)"
  - probationRatio: BigDecimal "试用期薪资比例"
  - directReportId: Long       "直接汇报人"
  - status: OnboardingStatus   "业务状态"
  - createTime: LocalDateTime
  --
  + submit(): void
  + approve(): void
  + reject(): void
}

class RegularizationApplication {
  - id: Long
  - employeeId: Long
  - probationStartDate: Date   "试用期开始日期"
  - probationEndDate: Date     "试用期结束日期"
  - performanceReview: String  "试用期表现评价"
  - salaryAdjustment: BigDecimal "转正后薪资调整"
  - result: RegularizationResult "审批结果"
  - status: ApprovalStatus
}

class TransferApplication {
  - id: Long
  - employeeId: Long
  - newDeptId: Long            "新部门ID"
  - newPositionId: Long        "新职位ID（可选）"
  - newJobLevel: String        "新职级（可选）"
  - newDirectReportId: Long    "新汇报人（可选）"
  - salaryAdjustment: BigDecimal "薪资调整（可选）"
  - reason: String             "调岗原因"
  - status: ApprovalStatus
}

class ResignationApplication {
  - id: Long
  - employeeId: Long
  - leaveDate: Date            "离职日期"
  - leaveReason: String        "离职原因分类"
  - leaveReasonDetail: String  "详细说明"
  - leaveType: Integer         "离职类型：1=辞职 2=辞退 3=合同到期 4=其他"
  - handoverPersonId: Long    "工作交接人"
  - status: ApprovalStatus
}

enum OnboardingStatus {
  DRAFT       "草稿"
  PENDING     "审批中"
  APPROVED    "已批准待入职"
  REJECTED    "已拒绝"
  ONBOARDED   "已入职"
  ABANDONED   "已放弃"
}

enum RegularizationResult {
  PASS        "通过"
  EXTEND      "延长试用"
  FAIL        "不通过"
}

enum ApprovalStatus {
  PENDING     "审批中"
  APPROVED    "已通过"
  REJECTED    "已拒绝"
}

OnboardingApplication --> OnboardingStatus
RegularizationApplication --> RegularizationResult
TransferApplication --> ApprovalStatus
ResignationApplication --> ApprovalStatus
@enduml
```

## 时序图

### 5-1 入职提交审批时序

![入职提交审批时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-onboarding.svg)

### 5-2 转正自动提醒时序

![转正自动提醒时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-regularization.svg)

### 5-3 调岗审批流转时序

![调岗审批流转时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-transfer.svg)

## 数据库设计

> 入转调离模块涉及的核心数据表。

### 入职申请表 onboarding_application

```sql
CREATE TABLE IF NOT EXISTS `onboarding_application` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                  VARCHAR(32)       NOT NULL COMMENT '姓名',
    `gender`                TINYINT           NOT NULL COMMENT '性别：1=男 2=女',
    `phone`                 VARCHAR(20)       NOT NULL COMMENT '手机号',
    `email`                 VARCHAR(64)       NOT NULL COMMENT '邮箱',
    `id_card`               VARCHAR(64)       NOT NULL COMMENT '身份证号（加密存储）',
    `expected_hire_date`    DATE              NOT NULL COMMENT '预计入职日期',
    `department_id`         BIGINT UNSIGNED   NOT NULL COMMENT '所属部门ID',
    `position_id`           BIGINT UNSIGNED   NOT NULL COMMENT '职位ID',
    `hire_type`             TINYINT           NOT NULL COMMENT '录用类型：1=全职 2=兼职 3=实习',
    `probation_months`      INT               NOT NULL DEFAULT 3 COMMENT '试用期月数',
    `probation_ratio`       DECIMAL(5,4)      NOT NULL DEFAULT 0.8000 COMMENT '试用期待遇比例',
    `direct_report_id`      BIGINT UNSIGNED            COMMENT '直接汇报人ID',
    `status`                TINYINT           NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=审批中 2=已批准 3=已拒绝 4=已入职 5=已放弃',
    `reject_reason`         VARCHAR(512)               COMMENT '拒绝原因',
    `employee_id`           BIGINT UNSIGNED            COMMENT '入职成功后关联的员工ID',
    `actual_hire_date`      DATE                       COMMENT '实际入职日期（HR确认时填写）',
    `create_by`             BIGINT UNSIGNED   NOT NULL COMMENT '创建人ID（HR）',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_expected_hire_date` (`expected_hire_date`),
    KEY `idx_create_by` (`create_by`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '入职申请表';
```

### 转正申请表 regularization_application

```sql
CREATE TABLE IF NOT EXISTS `regularization_application` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `probation_start_date`    DATE              NOT NULL COMMENT '试用期开始日期',
    `probation_end_date`      DATE              NOT NULL COMMENT '试用期结束日期',
    `performance_review`      VARCHAR(1024)     NOT NULL COMMENT '试用期表现评价',
    `salary_adjustment`       DECIMAL(12,2)              COMMENT '转正后薪资调整金额',
    `result`                  TINYINT                    COMMENT '审批结果：1=通过 2=延长试用 3=不通过',
    `extended_probation_date` DATE                       COMMENT '延长试用至日期',
    `status`                  TINYINT           NOT NULL DEFAULT 1 COMMENT '审批状态：1=审批中 2=已通过 3=已拒绝',
    `create_by`               BIGINT UNSIGNED   NOT NULL COMMENT '发起人ID（HR）',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '转正申请表';
```

### 转正提醒记录表 regularization_reminder

```sql
CREATE TABLE IF NOT EXISTS `regularization_reminder` (
    `id`              BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`     BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `remind_date`     DATE              NOT NULL COMMENT '提醒日期',
    `is_processed`    TINYINT           NOT NULL DEFAULT 0 COMMENT '是否已处理：0=未处理 1=HR已发起 2=已忽略',
    `create_time`     DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_remind_date` (`employee_id`, `remind_date`),
    KEY `idx_remind_date` (`remind_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '转正提醒记录表';
```

### 调岗申请表 transfer_application

```sql
CREATE TABLE IF NOT EXISTS `transfer_application` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`           BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `new_department_id`     BIGINT UNSIGNED   NOT NULL COMMENT '新部门ID',
    `new_position_id`       BIGINT UNSIGNED            COMMENT '新职位ID（可选）',
    `new_job_level`         VARCHAR(8)                 COMMENT '新职级（可选）',
    `new_direct_report_id`  BIGINT UNSIGNED            COMMENT '新直接汇报人ID（可选）',
    `salary_adjustment`     DECIMAL(12,2)              COMMENT '薪资调整金额（可选，不为空需额外审批）',
    `reason`                VARCHAR(512)      NOT NULL COMMENT '调岗原因',
    `status`                TINYINT           NOT NULL DEFAULT 1 COMMENT '审批状态：1=原部门审批中 2=新部门审批中 3=HR备案中 4=已通过 5=已拒绝',
    `create_by`             BIGINT UNSIGNED   NOT NULL COMMENT '发起人ID（HR）',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '调岗申请表';
```

### 调岗历史表 transfer_history

```sql
CREATE TABLE IF NOT EXISTS `transfer_history` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `application_id`      BIGINT UNSIGNED   NOT NULL COMMENT '调岗申请ID',
    `old_department_id`   BIGINT UNSIGNED   NOT NULL COMMENT '原部门ID',
    `new_department_id`   BIGINT UNSIGNED   NOT NULL COMMENT '新部门ID',
    `old_position_id`     BIGINT UNSIGNED            COMMENT '原职位ID',
    `new_position_id`     BIGINT UNSIGNED            COMMENT '新职位ID',
    `old_job_level`       VARCHAR(8)                 COMMENT '原职级',
    `new_job_level`       VARCHAR(8)                 COMMENT '新职级',
    `old_direct_report_id` BIGINT UNSIGNED           COMMENT '原汇报人ID',
    `new_direct_report_id` BIGINT UNSIGNED           COMMENT '新汇报人ID',
    `transfer_date`       DATE              NOT NULL COMMENT '调岗生效日期',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_transfer_date` (`transfer_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '调岗历史表';
```

### 离职申请表 resignation_application

```sql
CREATE TABLE IF NOT EXISTS `resignation_application` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `leave_date`              DATE              NOT NULL COMMENT '离职日期',
    `leave_reason`            VARCHAR(32)       NOT NULL COMMENT '离职原因：1=主动 2=被动 3=协商',
    `leave_reason_detail`     VARCHAR(512)               COMMENT '详细说明',
    `leave_type`              TINYINT           NOT NULL COMMENT '离职类型：1=辞职 2=辞退 3=合同到期不续签 4=其他',
    `handover_person_id`      BIGINT UNSIGNED   NOT NULL COMMENT '工作交接人ID',
    `status`                  TINYINT           NOT NULL DEFAULT 1 COMMENT '审批状态：1=审批中 2=已通过待离职 3=已拒绝 4=已离职',
    `create_by`               BIGINT UNSIGNED   NOT NULL COMMENT '发起人ID（HR）',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`),
    KEY `idx_leave_date` (`leave_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '离职申请表';
```

## API 设计

### 1. 创建入职申请（草稿/提交）

```plain
POST /api/v1/onboarding-applications
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| name | String | 是 | 姓名 |
| gender | Integer | 是 | 性别：1=男 2=女 |
| phone | String | 是 | 手机号 |
| email | String | 是 | 邮箱 |
| idCard | String | 是 | 身份证号 |
| expectedHireDate | Date | 是 | 预计入职日期 |
| departmentId | Long | 是 | 所属部门ID |
| positionId | Long | 是 | 职位ID |
| hireType | Integer | 是 | 录用类型：1=全职 2=兼职 3=实习 |
| probationMonths | Integer | 是 | 试用期月数 |
| probationRatio | BigDecimal | 是 | 试用期待遇比例 |
| directReportId | Long | 否 | 直接汇报人ID |
| submit | Boolean | 否 | 是否提交审批，默认false=保存草稿 |

---

### 2. 提交入职审批

```plain
POST /api/v1/onboarding-applications/{id}/submit
```

> **说明**：将草稿状态的入职申请提交审批。系统创建审批流（部门负责人 → [HR负责人]）。

---

### 3. 入职审批通过后确认到岗

```plain
PUT /api/v1/onboarding-applications/{id}/confirm-hire
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| actualHireDate | Date | 是 | 实际入职日期 |

> **说明**：执行自动处理流程（生成工号 → 创建账号 → 写入档案 → 发送通知）。

---

### 4. 标记放弃入职

```plain
PUT /api/v1/onboarding-applications/{id}/abandon
```

---

### 5. 查询入职申请列表

```plain
GET /api/v1/onboarding-applications
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| status | Integer | 否 | 状态过滤 |
| keyword | String | 否 | 模糊搜索姓名/手机号 |
| page | Integer | 否 | 页码 |
| size | Integer | 否 | 每页条数 |

---

### 6. 创建转正申请

```plain
POST /api/v1/regularization-applications
```

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| employeeId | Long | 是 | 员工ID |
| probationStartDate | Date | 是 | 试用期开始日期 |
| probationEndDate | Date | 是 | 试用期结束日期 |
| performanceReview | String | 是 | 试用期表现评价 |
| salaryAdjustment | BigDecimal | 否 | 转正后薪资调整 |
| result | Integer | 否 | 审批结果，提交时可不填 |

---

### 7. 获取待转正员工列表

```plain
GET /api/v1/regularization-applications/pending
```

> **说明**：返回试用期即将到期（7天内）或已到期但未发起转正的员工列表，供HR发起转正评估。

---

### 8. 创建调岗申请

```plain
POST /api/v1/transfer-applications
```

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| employeeId | Long | 是 | 员工ID |
| newDepartmentId | Long | 是 | 新部门ID |
| newPositionId | Long | 否 | 新职位ID |
| newJobLevel | String | 否 | 新职级 |
| newDirectReportId | Long | 否 | 新直接汇报人ID |
| salaryAdjustment | BigDecimal | 否 | 薪资调整 |
| reason | String | 是 | 调岗原因 |

---

### 9. 创建离职申请

```plain
POST /api/v1/resignation-applications
```

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| employeeId | Long | 是 | 员工ID |
| leaveDate | Date | 是 | 离职日期（>=今天） |
| leaveReason | String | 是 | 离职原因 |
| leaveReasonDetail | String | 否 | 详细说明 |
| leaveType | Integer | 是 | 离职类型：1=辞职 2=辞退 3=合同到期 4=其他 |
| handoverPersonId | Long | 是 | 工作交接人ID |

---

### 10. 查询异动历史

```plain
GET /api/v1/employees/{id}/change-history
```

> **说明**：查询员工的入转调离历史记录（入职/转正/调岗/离职），按时间倒序。

---

## 关键技术设计

### 工号生成并发安全方案

（与员工档案模块共用，详见 HRMS-后端.md）

### 入职自动处理事务

入职审批通过后的自动处理必须在同一个事务中完成：

1. `SELECT ... FOR UPDATE` 锁定工号序列表行
2. 递增序号，生成工号
3. INSERT employee（uk_employee_no 唯一索引兜底）
4. INSERT employee_personal_info / work_info / salary_info
5. INSERT employee_change_log（记录入职事件）
6. 提交事务
7. 事务提交后异步发送邮件和通知（避免事务内调用外部服务）

### 离职日期定时任务

使用 Spring Scheduler 每天凌晨执行：

```java
@Scheduled(cron = "0 5 0 * * ?")
public void processResignation() {
    List<Employee> pendingLeave = employeeService.findByStatusAndLeaveDateLe(
        EmployeeStatusEnum.PENDING_LEAVE, LocalDate.now());
    for (Employee emp : pendingLeave) {
        employeeService.processResignation(emp.getId());
    }
}
```

### 转正提醒幂等性

- `regularization_reminder` 表用 `uk_employee_remind_date (employee_id, remind_date)` 唯一索引
- INSERT ... ON DUPLICATE KEY UPDATE 实现幂等，避免重复提醒
- 每个员工每轮转正周期只提醒一次

### 调岗审批流状态机

```
PENDING_OLD_DEPT (1) → PENDING_NEW_DEPT (2) → PENDING_HR(3) → APPROVED(4)
          ↓                    ↓                    ↓
      REJECTED(5)         REJECTED(5)          REJECTED(5)
```

每个审批节点按顺序流转，拒绝则终止流程；支持审批人转交。

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审入转调离流程规则，确认审批流与状态机 | 1天 |
| 技术方案 | 完成系分文档评审，确认数据库设计与状态流转方案 | 2天 |
| 数据库开发 | 建表、索引优化、初始化审批流模板数据 | 1天 |
| 后端开发 | 入职/转正/调岗/离职CRUD、状态机流转、自动处理、定时任务 | 8天 |
| 前端开发 | 入职表单/列表、转正评估、调岗表单、离职表单 | 5天 |
| 联调测试 | 前后端联调、状态流转测试、并发工号测试、定时任务测试 | 3天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 2天 |

> **总预估工期**：约 22 个工作日
