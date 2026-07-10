# HRMS-后端-薪资管理

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | - |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 7 部分——薪资管理。当前公司薪资核算依赖 Excel 手工计算，存在公式易出错、数据来源分散（考勤/请假/调薪分散在多个表格）、核算周期长、员工无法自助查看工资条等问题。本模块旨在建立统一的薪资管理体系，涵盖薪资账套配置、员工薪资档案、月度薪资核算全流程（数据准备→自动计算→审核→审批→发放）、工资条员工自助查看，为财务审核和成本分析提供数据支撑。

### 相关资料

- [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述薪资管理涉及的功能与场景。

本模块核心功能包括：

1. **薪资账套管理**：定义工资模板（名称/适用范围/工资项目），支持固定收入、变动收入、考勤扣款、社保扣除、公积金扣除、个税六类工资项目
2. **员工薪资档案**：每个员工独立维护薪资档案（适用账套/基本工资/各项津贴基数/社保公积金基数/绩效基数），试用期薪资按比例计算，调薪历史记录可追溯
3. **月度薪资核算**：核算批次管理（草稿→计算中→待确认→审批中→已通过→已发放→已驳回），自动拉取考勤/请假/加班/薪资档案数据，逐项计算（基本+津贴+绩效+加班-考勤扣款-社保-公积金-个税），异常检测预警（请假>15天/加班>50h/变动>30%/新员工无档案）
4. **薪资审批**：HR提交 → 财务审核 → [老板审批]
5. **工资条**：审批通过后员工可查看，首次查看需二次验证（短信/密码），按工资项展开明细
6. **AntV 可视化**：薪资成本趋势折线图、部门薪资分布柱状图、薪资构成占比饼图、社保公积金对比分组柱状图、薪资变动分布直方图

### 功能模块树

```plain
薪资管理
├── 薪资账套管理
│   ├── 账套CRUD
│   ├── 工资项目配置（固定收入/变动收入/考勤扣款/社保扣除/公积金扣除/个税）
│   ├── 适用范围设置（按部门/职位/职级）
│   └── 账套模板复制
├── 员工薪资档案
│   ├── 薪资档案查询
│   ├── 薪资档案编辑（适用账套/基本工资/津贴基数/社保基数/公积金基数/绩效基数）
│   ├── 试用期薪资自动计算（按试用期比例）
│   └── 调薪历史记录
├── 月度薪资核算
│   ├── 核算批次管理
│   │   ├── 新建批次（选择月份）
│   │   ├── 数据准备（锁定考勤/拉取请假/拉取加班/拉取薪资档案）
│   │   ├── 执行计算
│   │   ├── 预览确认
│   │   └── 手动调整
│   ├── 薪资计算引擎
│   │   ├── 固定收入计算
│   │   ├── 变动收入计算（绩效系数×基数、加班费=小时工资×倍数×时长）
│   │   ├── 考勤扣款计算（迟到50元/次、请假=日工资×天数）
│   │   ├── 社保公积金计算（基数×比例）
│   │   └── 个税计算（累计预扣法）
│   ├── 异常检测
│   │   ├── 请假>15天 → 黄色预警
│   │   ├── 加班>50h → 黄色预警
│   │   ├── 变动>30% → 红色预警
│   │   └── 新员工无薪资档案 → 红色阻断
│   └── AntV 可视化
│       ├── 薪资成本月度趋势折线图
│       ├── 部门薪资分布柱状图
│       ├── 薪资构成占比饼图
│       ├── 社保公积金对比分组柱状图
│       └── 薪资变动分布直方图
├── 薪资审批
│   ├── 提交审批（HR → 财务 → [老板]）
│   ├── 审批详情查看
│   └── 驳回重算
└── 工资条
    ├── 员工工资条列表（按月）
    ├── 工资条详情（二次验证）
    └── 个人薪资趋势图（AntV折线图）
```

## 流程图

> 对薪资管理涉及的核心流程进行梳理。

### 7-1 月度薪资核算流程

```plain
新建核算批次（选择月份）
         │
         ▼
┌──────────────────────────────────────┐
│ 数据准备（自动拉取）                  │
│ - 锁定当月考勤数据                    │
│ - 拉取请假记录（已通过）              │
│ - 拉取加班审批记录（已通过）          │
│ - 拉取员工薪资档案                    │
│ - 验证新员工薪资档案完整性            │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│ 逐员工自动计算                        │
│ For each employee:                   │
│ 1. 取薪资档案 → 基本工资 + 津贴       │
│ 2. 取绩效数据 → 基数 × 绩效系数       │
│ 3. 取加班记录 → 小时工资 × 倍数 × 时长 │
│ 4. 取考勤汇总 → 迟到扣款 + 请假扣款   │
│ 5. 计算社保：基数 × 比例（养老8%+医保2%+失业0.5%）│
│ 6. 计算公积金：基数 × 12%             │
│ 7. 应发 = 1+2+3-4                    │
│ 8. 个税 = 累计预扣法(年初至今累计)    │
│ 9. 实发 = 应发 - 5 - 6 - 8           │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│ 异常检测                              │
│ - 请假>15天 → 黄色预警                │
│ - 加班>50h → 黄色预警                 │
│ - 变动>30% → 红色预警                 │
│ - 新员工无薪资档案 → 红色阻断          │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│ 核算预览                              │
│ - 表格展示全员薪资                    │
│ - 异常数据标红/标黄提示               │
│ - 支持手动调整（记录调整原因）         │
└──────────────────┬───────────────────┘
                   │
              ┌────┴────┐
              │         │
         确认无误     需要调整
              │         │
              ▼         ▼
         提交审批    添加调整项
         给财务      重新计算
              │
              ▼
┌──────────────────────────────────────┐
│ 审批流程                              │
│ HR提交 → 财务审核 → [老板审批]        │
└──────────────────┬───────────────────┘
                   │
                   ▼
              审批通过
                   │
                   ▼
         工资条对员工可见
                   │
                   ▼
         实际发放后标记已发放
```

### 7-2 个税累计预扣法计算流程

```plain
获取员工当年累计数据：
  - 累计应发工资（1月至当前月）
  - 累计社保公积金扣除
  - 累计专项附加扣除
  - 累计已缴个税
     │
     ▼
累计应纳税所得额 = 累计应发 - 累计起征点(5000×月数) - 累计社保公积金 - 累计专项附加扣除
     │
     ▼
查表确定税率和速算扣除数：
  区间0-36000：3%，速算0
  区间36000-144000：10%，速算2520
  区间144000-300000：20%，速算16920
  ...（共7级累进税率）
     │
     ▼
累计应缴个税 = 累计应纳税所得额 × 税率 - 速算扣除数
     │
     ▼
当月应缴个税 = 累计应缴个税 - 累计已缴个税
```

## UML 图

> 描述薪资管理的核心类和依赖关系。

### 薪资核心领域模型

```plain
@startuml
class SalaryAccount {
  - id: Long
  - name: String                "账套名称"
  - scopeType: Integer          "适用范围类型"
  - scopeIds: String            "适用范围ID集合(JSON)"
  - effectiveDate: Date         "生效日期"
  - items: List<SalaryItem>     "工资项目列表"
  - isDeleted: Integer
}

class SalaryItem {
  - id: Long
  - accountId: Long             "所属账套"
  - name: String                "项目名称"
  - itemType: SalaryItemType    "项目类型"
  - formula: String             "计算公式/规则"
  - sortOrder: Integer          "排序"
}

class EmployeeSalary {
  - id: Long
  - employeeId: Long
  - accountId: Long             "适用账套"
  - baseSalary: BigDecimal      "基本工资"
  - allowanceBase: BigDecimal   "津贴基数"
  - socialSecurityBase: BigDecimal "社保基数"
  - housingFundBase: BigDecimal  "公积金基数"
  - performanceBase: BigDecimal  "绩效基数"
  - effectiveDate: Date         "生效日期"
}

class SalaryBatch {
  - id: Long
  - month: String               "核算月份 yyyy-MM"
  - status: BatchStatus         "批次状态"
  - totalEmployees: Integer     "核算人数"
  - totalPayable: BigDecimal    "应发合计"
  - totalNetPay: BigDecimal     "实发合计"
  - createBy: Long
  - createTime: LocalDateTime
}

class SalaryDetail {
  - id: Long
  - batchId: Long               "所属批次"
  - employeeId: Long
  - items: String               "工资项明细(JSON)"
  - grossPay: BigDecimal        "应发合计"
  - deductions: BigDecimal      "扣除合计"
  - netPay: BigDecimal          "实发合计"
  - isAbnormal: Boolean         "是否异常"
  - abnormalReason: String      "异常原因"
  - manualAdjustment: BigDecimal "手动调整额"
  - adjustmentReason: String    "调整原因"
}

class SalaryHistory {
  - id: Long
  - employeeId: Long
  - changeType: Integer         "变更类型：调薪/账套变更/基数调整"
  - oldValue: String            "变更前值(JSON)"
  - newValue: String            "变更后值(JSON)"
  - effectiveDate: Date
  - operatorId: Long
  - createTime: LocalDateTime
}

enum SalaryItemType {
  FIXED_INCOME     "固定收入"
  VARIABLE_INCOME  "变动收入"
  ATTENDANCE_DEDUCT "考勤扣款"
  SOCIAL_SECURITY  "社保扣除"
  HOUSING_FUND     "公积金扣除"
  INCOME_TAX       "个税"
}

enum BatchStatus {
  DRAFT       "草稿"
  CALCULATING "计算中"
  CONFIRMING  "待确认"
  APPROVING   "审批中"
  APPROVED    "已通过"
  PAID        "已发放"
  REJECTED    "已驳回"
}

SalaryAccount "1" -- "*" SalaryItem
SalaryAccount "1" -- "*" EmployeeSalary
SalaryBatch "1" -- "*" SalaryDetail
EmployeeSalary "1" -- "*" SalaryHistory
@enduml
```

## 时序图

### 7-1 薪资核算时序

![薪资核算时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-salary-calc.svg)

### 7-2 工资条查看时序

![工资条查看时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-payslip.svg)

## 数据库设计

> 薪资管理模块涉及的核心数据表。

### 薪资账套表 salary_account

```sql
CREATE TABLE IF NOT EXISTS `salary_account` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                VARCHAR(64)       NOT NULL COMMENT '账套名称',
    `scope_type`          TINYINT           NOT NULL COMMENT '适用范围类型：1=部门 2=职位 3=职级',
    `scope_ids`           VARCHAR(512)               COMMENT '适用范围ID集合（JSON数组）',
    `effective_date`      DATE              NOT NULL COMMENT '生效日期',
    `is_deleted`          TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资账套表';
```

### 工资项目表 salary_item

```sql
CREATE TABLE IF NOT EXISTS `salary_item` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `account_id`          BIGINT UNSIGNED   NOT NULL COMMENT '所属账套ID',
    `name`                VARCHAR(64)       NOT NULL COMMENT '项目名称，如基本工资、绩效奖金',
    `item_type`           TINYINT           NOT NULL COMMENT '项目类型：1=固定收入 2=变动收入 3=考勤扣款 4=社保扣除 5=公积金扣除 6=个税',
    `formula`             VARCHAR(512)               COMMENT '计算公式/规则描述',
    `sort_order`          INT               NOT NULL DEFAULT 0 COMMENT '排序序号',
    `is_taxable`          TINYINT           NOT NULL DEFAULT 1 COMMENT '是否计入个税：0=否 1=是',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_account_id` (`account_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工资项目表';
```

### 员工薪资档案表 employee_salary

```sql
CREATE TABLE IF NOT EXISTS `employee_salary` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `account_id`              BIGINT UNSIGNED   NOT NULL COMMENT '适用账套ID',
    `base_salary`             DECIMAL(12,2)     NOT NULL COMMENT '基本工资',
    `allowance_base`          DECIMAL(12,2)     NOT NULL DEFAULT 0 COMMENT '津贴基数',
    `social_security_base`    DECIMAL(12,2)     NOT NULL COMMENT '社保基数',
    `housing_fund_base`       DECIMAL(12,2)     NOT NULL COMMENT '公积金基数',
    `performance_base`        DECIMAL(12,2)              COMMENT '绩效基数',
    `effective_date`          DATE              NOT NULL COMMENT '生效日期',
    `is_deleted`              TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_effective_date` (`effective_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工薪资档案表';
```

### 调薪历史表 salary_change_history

```sql
CREATE TABLE IF NOT EXISTS `salary_change_history` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `change_type`         TINYINT           NOT NULL COMMENT '变更类型：1=调薪 2=账套变更 3=基数调整 4=转正调薪 5=调岗调薪',
    `old_value`           JSON                       COMMENT '变更前薪资档案快照',
    `new_value`           JSON                       COMMENT '变更后薪资档案快照',
    `effective_date`      DATE              NOT NULL COMMENT '生效日期',
    `operator_id`         BIGINT UNSIGNED   NOT NULL COMMENT '操作人ID',
    `remark`              VARCHAR(256)               COMMENT '备注',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_effective_date` (`effective_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '调薪历史表';
```

### 薪资核算批次表 salary_batch

```sql
CREATE TABLE IF NOT EXISTS `salary_batch` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `batch_no`            VARCHAR(32)       NOT NULL COMMENT '批次号',
    `salary_month`        VARCHAR(7)        NOT NULL COMMENT '核算月份，格式yyyy-MM',
    `status`              TINYINT           NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=计算中 2=待确认 3=审批中 4=已通过 5=已发放 6=已驳回',
    `total_employees`     INT               NOT NULL DEFAULT 0 COMMENT '核算人数',
    `total_gross_pay`     DECIMAL(16,2)              COMMENT '应发合计',
    `total_net_pay`       DECIMAL(16,2)              COMMENT '实发合计',
    `total_tax`           DECIMAL(16,2)              COMMENT '个税合计',
    `create_by`           BIGINT UNSIGNED   NOT NULL COMMENT '创建人ID（HR）',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_no` (`batch_no`),
    UNIQUE KEY `uk_salary_month` (`salary_month`),
    KEY `idx_status` (`status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资核算批次表';
```

### 薪资明细表 salary_detail

```sql
CREATE TABLE IF NOT EXISTS `salary_detail` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `batch_id`              BIGINT UNSIGNED   NOT NULL COMMENT '所属批次ID',
    `employee_id`           BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `salary_items`          JSON              NOT NULL COMMENT '工资项明细(JSON)',
    `gross_pay`             DECIMAL(12,2)     NOT NULL COMMENT '应发合计',
    `social_security`       DECIMAL(12,2)     NOT NULL DEFAULT 0 COMMENT '社保扣除合计',
    `housing_fund`          DECIMAL(12,2)     NOT NULL DEFAULT 0 COMMENT '公积金扣除',
    `income_tax`            DECIMAL(12,2)     NOT NULL DEFAULT 0 COMMENT '个人所得税',
    `total_deductions`      DECIMAL(12,2)     NOT NULL DEFAULT 0 COMMENT '扣除合计',
    `net_pay`               DECIMAL(12,2)     NOT NULL COMMENT '实发合计',
    `is_abnormal`           TINYINT           NOT NULL DEFAULT 0 COMMENT '是否异常：0=正常 1=黄色预警 2=红色预警',
    `abnormal_reason`       VARCHAR(256)               COMMENT '异常原因',
    `manual_adjustment`     DECIMAL(12,2)    NOT NULL DEFAULT 0 COMMENT '手动调整额',
    `adjustment_reason`     VARCHAR(256)               COMMENT '调整原因',
    `payslip_viewed`        TINYINT           NOT NULL DEFAULT 0 COMMENT '工资条是否已查看：0=否 1=是',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_employee` (`batch_id`, `employee_id`),
    KEY `idx_employee_id` (`employee_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '薪资明细表';
```

### 个税累计表 income_tax_cumulative

```sql
CREATE TABLE IF NOT EXISTS `income_tax_cumulative` (
    `id`                          BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`                 BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `tax_year`                    SMALLINT          NOT NULL COMMENT '纳税年度',
    `tax_month`                   TINYINT           NOT NULL COMMENT '纳税月份',
    `cumulative_gross_pay`        DECIMAL(16,2)     NOT NULL COMMENT '累计应发工资',
    `cumulative_threshold`        DECIMAL(12,2)     NOT NULL COMMENT '累计起征点（5000×月数）',
    `cumulative_social_security`  DECIMAL(12,2)     NOT NULL COMMENT '累计社保扣除',
    `cumulative_housing_fund`     DECIMAL(12,2)     NOT NULL COMMENT '累计公积金扣除',
    `cumulative_special_deduction` DECIMAL(12,2)    NOT NULL DEFAULT 0 COMMENT '累计专项附加扣除',
    `cumulative_taxable_income`   DECIMAL(16,2)     NOT NULL COMMENT '累计应纳税所得额',
    `tax_rate`                    DECIMAL(5,4)      NOT NULL COMMENT '适用税率',
    `quick_deduction`             DECIMAL(12,2)     NOT NULL COMMENT '速算扣除数',
    `cumulative_tax_payable`      DECIMAL(16,2)     NOT NULL COMMENT '累计应缴个税',
    `cumulative_tax_paid`         DECIMAL(16,2)     NOT NULL COMMENT '累计已缴个税',
    `current_month_tax`           DECIMAL(12,2)     NOT NULL COMMENT '当月应缴个税',
    `create_time`                 DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_year_month` (`employee_id`, `tax_year`, `tax_month`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '个税累计表';
```

## API 设计

### 1. 薪资账套 CRUD

```plain
GET    /api/v1/salary-accounts          # 账套列表
POST   /api/v1/salary-accounts          # 新建账套（含工资项目）
PUT    /api/v1/salary-accounts/{id}     # 编辑账套
DELETE /api/v1/salary-accounts/{id}     # 删除账套
GET    /api/v1/salary-accounts/{id}     # 账套详情（含工资项目列表）
```

### 2. 工资项目管理

```plain
POST   /api/v1/salary-accounts/{id}/items       # 添加工资项目
PUT    /api/v1/salary-items/{id}                # 编辑工资项目
DELETE /api/v1/salary-items/{id}                # 删除工资项目
PUT    /api/v1/salary-accounts/{id}/items/sort  # 调整项目排序
```

### 3. 员工薪资档案

```plain
GET    /api/v1/employee-salaries/{employeeId}   # 查询员工薪资档案
PUT    /api/v1/employee-salaries/{employeeId}   # 更新员工薪资档案
GET    /api/v1/employee-salaries/{employeeId}/history  # 调薪历史
```

### 4. 薪资核算批次

```plain
POST   /api/v1/salary-batches              # 新建核算批次
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| salaryMonth | String | 是 | 核算月份，格式 yyyy-MM |

---

```plain
POST   /api/v1/salary-batches/{id}/calculate   # 执行计算
GET    /api/v1/salary-batches/{id}/preview     # 预览核算结果（分页）
GET    /api/v1/salary-batches/{id}/anomalies   # 查看异常项
PUT    /api/v1/salary-batches/{id}/adjust      # 手动调整单条薪资
POST   /api/v1/salary-batches/{id}/submit      # 提交审批
```

#### 预览响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "batch": {
      "id": 100,
      "batchNo": "SAL202407001",
      "salaryMonth": "2024-07",
      "status": 2,
      "totalEmployees": 500,
      "totalGrossPay": 6250000.00,
      "totalNetPay": 4980000.00,
      "totalTax": 480000.00
    },
    "records": [
      {
        "employeeId": 1001,
        "employeeNo": "202401005",
        "name": "张三",
        "departmentName": "技术部",
        "salaryItems": [
          { "name": "基本工资", "type": 1, "amount": 10000.00 },
          { "name": "岗位津贴", "type": 1, "amount": 2000.00 },
          { "name": "绩效奖金", "type": 2, "amount": 3600.00 },
          { "name": "事假扣款", "type": 3, "amount": -500.00 }
        ],
        "grossPay": 15100.00,
        "socialSecurity": 840.00,
        "housingFund": 960.00,
        "incomeTax": 320.00,
        "netPay": 12980.00,
        "isAbnormal": false
      }
    ]
  }
}
```

### 5. 薪资审批

```plain
POST   /api/v1/salary-batches/{id}/approve      # 审批通过
POST   /api/v1/salary-batches/{id}/reject       # 驳回（含原因）
```

#### 驳回请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| reason | String | 是 | 驳回原因 |

### 6. 发放确认

```plain
POST   /api/v1/salary-batches/{id}/mark-paid    # 标记已发放
```

### 7. 工资条

```plain
GET    /api/v1/payslips                       # 我的工资条列表（按月）
GET    /api/v1/payslips/{batchId}             # 工资条详情（需二次验证）
POST   /api/v1/payslips/{batchId}/verify      # 二次验证（密码/短信验证码）
```

### 8. 薪资统计数据（AntV 图表数据源）

```plain
GET    /api/v1/salary-statistics/cost-trend?months=6        # 薪资成本月度趋势折线图
GET    /api/v1/salary-statistics/dept-distribution?month=   # 部门薪资分布柱状图
GET    /api/v1/salary-statistics/composition?month=         # 薪资构成占比饼图
GET    /api/v1/salary-statistics/social-comparison?month=   # 社保公积金对比分组柱状图
GET    /api/v1/salary-statistics/variation-distribution?month= # 薪资变动分布直方图
```

## 关键技术设计

### 薪资计算引擎

薪资计算采用策略模式，每类工资项目独立计算：

```java
public interface SalaryItemCalculator {
    BigDecimal calculate(EmployeeSalary salary, AttendanceSummary attendance,
                         LeaveSummary leave, OvertimeSummary overtime, int month);
}

@Component
public class FixedIncomeCalculator implements SalaryItemCalculator {
    public BigDecimal calculate(...) {
        BigDecimal base = salary.getBaseSalary();
        // 试用期薪资调整
        if (employee.isProbation()) {
            base = base.multiply(employee.getProbationRatio());
        }
        return base.add(salary.getAllowanceBase());
    }
}

@Component
public class AttendanceDeductionCalculator implements SalaryItemCalculator {
    public BigDecimal calculate(...) {
        int lateCount = attendance.getLateCount();
        BigDecimal dailySalary = salary.getBaseSalary()
            .divide(BigDecimal.valueOf(21.75), 2, RoundingMode.HALF_UP);
        BigDecimal leaveDays = leave.getTotalLeaveDays();
        return BigDecimal.valueOf(-50).multiply(BigDecimal.valueOf(lateCount))
            .add(dailySalary.multiply(leaveDays).negate());
    }
}
```

### 个税累计预扣法

```java
public BigDecimal calculateMonthlyTax(Long employeeId, int taxYear, int taxMonth,
                                       BigDecimal currentGrossPay, BigDecimal currentSS,
                                       BigDecimal currentHF) {
    // 查询当年累计数据
    IncomeTaxCumulative lastMonth = taxCumulativeMapper
        .findByEmployeeAndYearAndMonth(employeeId, taxYear, taxMonth - 1);
    
    BigDecimal cumulativeGross = lastMonth.getCumulativeGrossPay().add(currentGrossPay);
    BigDecimal cumulativeThreshold = BigDecimal.valueOf(5000 * taxMonth);
    BigDecimal cumulativeSS = lastMonth.getCumulativeSocialSecurity().add(currentSS);
    BigDecimal cumulativeHF = lastMonth.getCumulativeHousingFund().add(currentHF);
    
    BigDecimal taxableIncome = cumulativeGross.subtract(cumulativeThreshold)
        .subtract(cumulativeSS).subtract(cumulativeHF);
    
    // 7级累进税率表
    TaxBracket bracket = findTaxBracket(taxableIncome);
    BigDecimal cumulativeTax = taxableIncome.multiply(bracket.getRate())
        .subtract(bracket.getQuickDeduction());
    
    return cumulativeTax.subtract(lastMonth.getCumulativeTaxPaid()).max(BigDecimal.ZERO);
}
```

### 异常检测规则引擎

```java
public List<Anomaly> detectAnomalies(SalaryDetail detail, SalaryDetail lastMonth) {
    List<Anomaly> anomalies = new ArrayList<>();
    
    // 黄色预警：请假 > 15天
    if (leaveSummary.getTotalDays() > 15) {
        anomalies.add(new Anomaly(AnomalyLevel.WARNING, "当月请假超过15天"));
    }
    // 黄色预警：加班 > 50h
    if (overtimeSummary.getTotalHours() > 50) {
        anomalies.add(new Anomaly(AnomalyLevel.WARNING, "当月加班超过50小时"));
    }
    // 红色预警：变动 > 30%
    if (lastMonth != null) {
        BigDecimal change = detail.getNetPay().subtract(lastMonth.getNetPay())
            .divide(lastMonth.getNetPay(), 2, RoundingMode.HALF_UP).abs();
        if (change.compareTo(BigDecimal.valueOf(0.3)) > 0) {
            anomalies.add(new Anomaly(AnomalyLevel.ERROR, "薪资较上月变动超过30%"));
        }
    }
    return anomalies;
}
```

### 工资条二次验证

```java
// 首次查看需二次验证
public boolean verifyBeforeView(Long employeeId, Long batchId, String verifyCode) {
    // 方式1：短信验证码（优先）
    // 方式2：登录密码（备用）
    String cachedCode = redisTemplate.opsForValue().get("payslip:verify:" + employeeId);
    if (verifyCode.equals(cachedCode)) {
        redisTemplate.delete("payslip:verify:" + employeeId);
        return true;
    }
    // 密码验证
    Employee employee = employeeMapper.selectById(employeeId);
    return passwordEncoder.matches(verifyCode, employee.getPassword());
}
```

### 大数据量核算优化

- 薪资核算（500人）：采用分批计算 + 异步执行
- 每批 100 人，批次间通过消息队列传递，结果写入 salary_detail 表
- 前端轮询批次状态，计算完成后展示预览
- 使用 Redis 缓存计算结果，避免重复计算

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审薪资账套、核算流程、个税规则、异常检测规则 | 1.5天 |
| 技术方案 | 完成系分文档评审，确认计算引擎架构、个税累计预扣方案 | 2天 |
| 数据库开发 | 建表、索引优化、初始化默认账套数据 | 1天 |
| 后端开发 | 账套CRUD、薪资档案CRUD、计算引擎、核算批次管理、审批流、工资条、统计接口 | 10天 |
| 前端开发 | 账套配置页、薪资档案页、核算批次管理、预览/调整页、工资条页、统计页(含AntV) | 6天 |
| 联调测试 | 前后端联调、计算准确性测试、个税计算验证、边界场景测试 | 4天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 2天 |

> **总预估工期**：约 26.5 个工作日
