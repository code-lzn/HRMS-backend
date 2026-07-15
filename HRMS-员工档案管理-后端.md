# HRMS-员工档案管理-后端

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-08 | 1.0 | 初稿 | 凯贾 |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 4 部分——员工档案管理。当前公司内部员工档案依赖 Excel 与纸质管理，存在数据分散、更新不及时、权限控制粗放等问题。本模块旨在实现员工档案的数字化统一管理，涵盖字段定义、查询检索、列表展示、权限控制等核心能力，为后续入转调离、薪资核算、考勤管理等模块提供基础数据支撑。

### 相关资料

*   [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)
    

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述员工档案管理涉及的功能与场景。

本模块核心功能包括：

1.  **员工档案字段管理**：定义基础信息、个人信息、工作信息、薪资与合同信息四类字段，支持必填校验与可编辑性控制
    
2.  **工号自动生成**：按"年份(4位)+部门编码(2位)+序号(3位)"规则系统自动生成
    
3.  **系统账号自动创建**：入职审批通过后自动创建账号（登录账号=手机号，初始密码随机生成）
    
4.  **员工查询与列表**：支持默认字段展示、高级搜索（关键词/部门/职位/在职状态/职级/入职日期范围）、列表操作（查看详情/编辑/调岗/离职）
    
5.  **字段级权限控制**：不同角色对敏感字段（身份证号、薪资信息、紧急联系人、银行卡号）的可见性控制
    

### 功能模块树

```plain
员工档案管理
├── 创建员工档案（由入转调离模块在入职审批通过后调用）
├── 编辑员工档案（按字段权限控制可编辑项）
├── 员工详情查询（按角色脱敏敏感字段）
├── 员工列表查询
│   ├── 默认列表展示
│   ├── 高级搜索（关键词/部门/职位/状态/职级/入职日期）
│   └── 列表操作（查看详情 / 编辑 / 调岗 / 离职）
└── 字段级权限控制（按角色过滤可见/可编辑/需走流程字段）

```

## 流程图

> 对员工档案管理涉及的核心流程进行梳理。

### 4-1 员工档案创建流程

```plain
入职审批通过
     │
     ▼
系统自动生成工号（年份+部门编码+序号）
     │
     ▼
创建系统账号（登录账号=手机号，初始密码随机）
     │
     ▼
写入员工档案（基础信息+个人信息+工作信息+薪资合同信息）
     │
     ▼
发送欢迎邮件给员工
     │
     ▼
发送确认通知给 HR 和部门负责人
     │
     ▼
员工档案创建完成

```

### 4-2 员工查询流程

```plain
用户进入员工列表页
     │
     ▼
系统加载默认列表（按权限过滤可见字段和记录范围）
     │
     ├── 根据角色过滤数据范围（全量/本部门/仅自己）
     │
     ├── 根据角色过滤字段可见性
     │
     ▼
用户选择操作
     ├── 输入搜索条件 → 高级搜索 → 返回结果
     ├── 点击"查看详情" → 打开档案完整页
     ├── 点击"编辑" → 打开编辑页（按字段权限控制可编辑项）
     └── 点击"更多操作" → 调岗/离职（根据在职状态判断可用操作）

```

### 4-3 字段编辑权限控制流程

```plain
用户点击"编辑"员工档案
     │
     ▼
前端调用 GET /api/v1/employees/field-permissions
     │
     ▼
获取当前角色可编辑字段列表（editableFields）
     │
     ├── 系统管理员 → 所有允许编辑字段
     ├── HR 专员 → 所有允许编辑字段
     ├── 部门主管 → 部分字段（无薪资/合同编辑权限）
     └── 普通员工 → 仅个人信息中可编辑字段
     │
     ▼
渲染编辑表单
     ├── editableFields 中的字段 → 展示输入框，可编辑
     └── 其余字段 → 置灰锁定，提示"如需修改请联系HR"
     │
     ▼
用户提交 PUT /api/v1/employees/{id}
传入平铺的可选字段（如 { email: "...", currentAddress: "..." }）
     │
     ▼
后端逐字段校验：该字段是否在当前角色的 editableFields 中
     │
     ├── 在 editableFields 中 → 直接更新对应子表 → 加入 updatedFields
     └── 不在 editableFields 中 → 拒绝保存 → 加入 flowRequiredFields
     │
     ▼
返回 { updatedFields: [...], flowRequiredFields: [...] }
     │
     ├── updatedFields → 前端提示"已更新：邮箱、现居住地址"
     └── flowRequiredFields → 前端提示"以下字段需走流程：手机号"，引导用户发起对应申请

```

## UML 图

> 描述员工档案管理的核心类和依赖关系。

### 员工档案核心领域模型

```plain
@startuml
class Employee {
  - id: Long
  - employeeNo: String          "工号，系统生成"
  - account: String             "系统账号=手机号"
  - status: EmployeeStatus      "在职状态"
  - hireDate: LocalDate         "入职日期"
  - createTime: LocalDateTime   "创建时间"
  --
  + getPersonalInfo(): PersonalInfo
  + getWorkInfo(): WorkInfo
  + getSalaryInfo(): SalaryInfo
}

class PersonalInfo {
  - name: String               "姓名"
  - gender: Gender             "性别"
  - phone: String              "手机号"
  - email: String              "邮箱"
  - idCard: String             "身份证号"
  - birthday: LocalDate        "生日"
  - registeredAddress: String  "户籍地址"
  - currentAddress: String     "现居住地址"
}

class WorkInfo {
  - department: Department     "所属部门"
  - position: Position         "职位"
  - jobLevel: String           "职级"
  - directReport: Employee     "直接汇报人"
  - workLocation: String       "工作地点"
  - hireType: HireType         "入职类型"
}

class SalaryInfo {
  - contractType: ContractType "合同类型"
  - contractExpireDate: LocalDate "合同到期日"
  - probationRatio: BigDecimal "试用期待遇比例 0.8~1.0"
  - salaryAccount: SalaryAccount "薪资账套"
  - baseSalary: BigDecimal     "基本工资"
  - bankAccount: String        "银行账号"
  - bankName: String           "开户行"
}

class Department {
  - id: Long
  - name: String
  - code: String               "部门编码，用于工号生成"
  - parent: Department
  - manager: Employee
  - sortOrder: Integer
  - description: String
}

class Position {
  - id: Long
  - name: String
  - sequence: String           "职位序列 M/P/S"
  - department: Department
  - levelRange: String         "职级范围"
  - defaultProbation: Integer  "默认试用期月数"
}

enum EmployeeStatus {
  PROBATION    "试用期"
  REGULAR      "正式"
  PENDING_LEAVE "待离职"
  RESIGNED     "已离职"
}

enum HireType {
  FULL_TIME    "全职"
  PART_TIME    "兼职"
  INTERNSHIP   "实习"
}

enum ContractType {
  FIXED_TERM         "固定期限"
  INDEFINITE_TERM    "无固定期限"
  LABOR              "劳务合同"
}

Employee *-- PersonalInfo
Employee *-- WorkInfo
Employee *-- SalaryInfo
WorkInfo *-- Department
WorkInfo *-- Position
Employee --> EmployeeStatus
WorkInfo --> HireType
SalaryInfo --> ContractType
@enduml

```

[PlantUML在线绘制\_PlantUML在线编辑器官网\_PlantUML在线编译器免下载 - ProcessOn](https://www.processon.com/plantuml?editor_content=5ddvpbrtHkrS4I0GIUxF1jOVqnxBJOiPlcaqjSotSEhZm6HVTNaVa0dRRGRwYPOdpRSl922jPmjbGx5bm2JNWWvJBvJMuvTHmkpPMf4NEpAFCmbOtT68PkysHB4nvGwxXrMPVOoZIg8LZ07Faz1HHJAAiz4MzcF7mHRtEvk397CmGhMU6TlSeVtVTyczZ3B841h63xbTy4UlO6Y54YC8sE6mhacqJhvPvah03kP9q3wZLVAhGsRb01YeyO0GrluekZi9mzgiu7Lhsn1y00wwoKQKxBcTJjhUOSUczc6Sd1t2k8w0A6eCAyuAA84ryx3MZRAJALRnn5ce95NVQG3irrmB23kIHtzng93CdzRCFCbKxlrn0OWVkQ6LVuzjaqe8EBRHTPeaD3ydHpIb0Gvd61e2UOUOFtlCeEYY7THGJVPfnjaQVeT6ENSP6McehFEFZSfRC0zD0yNRxYGvj1BarK2YEvosKep9PashHkwSgkyL72JMXo7FKxZSymu2GQHHqwmOvSYMUxskLqPfp8QzH5mH56Opt02H6wu94U09qwBO5uoJtXWv4HkhGGkoInVd8uXdogppSeMOGXZMrJnPsC1QCo5Y0kQAkJrJdOEpPd0gUrFyhPMAd997JuWjwJP2c1FIMTfQ1l3aLf0nrpcdCFC7OLwcpJm6W3QZZ8lmN0xAokk7283IOs8nWPwVvKCXCtAhM389BhcbpMJ4uEF1aKa2w04Vqa6mxrkCcnuJT5DjZJB8IR6qVbXMymzEC7MhnWfXGBPMkjL6gNTclXTUYPkm1JQJWPbI5mGuRzR8AlenzJKJPVuFrTyrtbwxKGNOObic4zmoIax9neEYmd3Pt4bWPclwaMat4p1pnUzQ0iVppuhu6ZYF6g2w1gUNmj3gYXdugz2vxP44xLgrFy6nqARLN2dRrR1bWyXVojEGXWWUAprxGNrPEkTQVFyu72GIVVqieCMpnhEteAUVNi1htpDTGahfMwXD4QLPxVLAvMZ4pPugDv22HsaOXE1EJLYeaq5XCAwezCUuBQn82rjnImLFEyrDzOPvZoKsKGpYxOvoU5TUhczrIfi7aTvM8shegx9GZLsez60u1G36Wp2nTVWMx58xdyXbgcnJrrdaxTkFkoI6gIZyxmCfWn9664fn6NixK0mJuAqPgAMfuYyHpdjh2Ght1MliIxAfXBtA6YdfDXZ0tbIbqqzxlLqFcU64z04SH95nqvl4Qju1L0gAFT41dKbg7NqQ547odCgFAofMqBf2GDXO9A1LGGCNpxlgRHd3TlxcL4TZeX4Ua5hYOAPD3hVeJ61w81FUOoQjGrSjDWamjmsmegAd8swcpHJEPjGDhRYm6HXwXZsDhF7uV2fO6skN7J45iccydQH5ezeMx764a23HSVWl8ueU8cNqqDMbPPVEtnIiiY497M8ydGD5Hi7QEI7zbaLMy5telyQpsi)

## 时序图

> 描述员工档案管理核心交互的调用时序。

### 4-1 创建员工档案时序

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/mPdnpEbNgeyz6qw9/img/2ab30ae0-5fcd-440b-b523-479c00b8c232.svg)

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/mPdnpEbNgeyz6qw9/img/32f4be2a-f491-468d-9cbd-9ca11dd57cf8.svg)

### 4-2 员工查询列表时序

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/mPdnpEbNgeyz6qw9/img/ca8fda22-bfc9-4090-be85-695406c366f6.svg)

### 4-3 编辑员工档案时序

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/mPdnpEbNgeyz6qw9/img/a577edbf-caba-4dd5-bd85-2507e5d4f6a5.svg)

## 数据库设计

> 员工档案管理模块涉及的核心数据表。

### 员工主表 

### employee

```sql
CREATE TABLE IF NOT EXISTS `employee` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_no`       VARCHAR(12)       NOT NULL COMMENT '工号，格式：年份(4位)+部门编码(2位)+序号(3位)，如202401005',
    `account`           VARCHAR(32)       NOT NULL COMMENT '系统账号（登录账号=手机号）',
    `password`          VARCHAR(128)      NOT NULL COMMENT '加密后的密码',
    `status`            TINYINT           NOT NULL DEFAULT 1 COMMENT '在职状态：1=试用期 2=正式 3=待离职 4=已离职',
    `hire_date`         DATE              NOT NULL COMMENT '入职日期',
    `hire_type`         TINYINT           NOT NULL COMMENT '入职类型：1=全职 2=兼职 3=实习',
    `create_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_no` (`employee_no`),
    UNIQUE KEY `uk_account` (`account`),
    KEY `idx_status` (`status`),
    KEY `idx_hire_date` (`hire_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工主表';

```

### 员工个人信息表 

### employee\_personal\_info

```sql
CREATE TABLE IF NOT EXISTS `employee_personal_info` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`           BIGINT UNSIGNED   NOT NULL COMMENT '员工ID，关联employee.id',
    `name`                  VARCHAR(32)       NOT NULL COMMENT '姓名',
    `gender`                TINYINT           NOT NULL COMMENT '性别：1=男 2=女',
    `phone`                 VARCHAR(20)       NOT NULL COMMENT '手机号',
    `email`                 VARCHAR(64)       NOT NULL COMMENT '邮箱',
    `id_card`               VARCHAR(64)       NOT NULL COMMENT '身份证号（加密存储）',
    `birthday`              DATE                       COMMENT '生日',
    `registered_address`    VARCHAR(256)               COMMENT '户籍地址',
    `current_address`       VARCHAR(256)               COMMENT '现居住地址',
    `emergency_contact_name`  VARCHAR(32)               COMMENT '紧急联系人姓名',
    `emergency_contact_phone` VARCHAR(20)               COMMENT '紧急联系人电话',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_id` (`employee_id`),
    KEY `idx_name` (`name`),
    KEY `idx_phone` (`phone`),
    KEY `idx_id_card` (`id_card`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工个人信息表';

```

### 员工工作信息表 

### employee\_work\_info

```sql
CREATE TABLE IF NOT EXISTS `employee_work_info` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`           BIGINT UNSIGNED   NOT NULL COMMENT '员工ID，关联employee.id',
    `department_id`         BIGINT UNSIGNED   NOT NULL COMMENT '所属部门ID，关联department.id',
    `position_id`           BIGINT UNSIGNED   NOT NULL COMMENT '职位ID，关联position.id',
    `job_level`             VARCHAR(8)                 COMMENT '职级，如P5、M2',
    `direct_report_id`      BIGINT UNSIGNED            COMMENT '直接汇报人ID，关联employee.id',
    `work_location`         VARCHAR(128)               COMMENT '工作地点',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_id` (`employee_id`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_position_id` (`position_id`),
    KEY `idx_direct_report_id` (`direct_report_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工工作信息表';

```

### 员工薪资与合同信息表 

### employee\_salary\_info

```sql
CREATE TABLE IF NOT EXISTS `employee_salary_info` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID，关联employee.id',
    `contract_type`           TINYINT           NOT NULL COMMENT '合同类型：1=固定期限 2=无固定期限 3=劳务合同',
    `contract_expire_date`    DATE                       COMMENT '合同到期日，固定期限合同必填',
    `probation_ratio`         DECIMAL(5,4)      NOT NULL DEFAULT 0.8000 COMMENT '试用期待遇比例，范围0.8~1.0',
    `salary_account_id`       BIGINT UNSIGNED   NOT NULL COMMENT '薪资账套ID，关联salary_account.id',
    `base_salary`             DECIMAL(12,2)     NOT NULL COMMENT '基本工资',
    `bank_account`            VARCHAR(64)                COMMENT '银行账号（加密存储）',
    `bank_name`               VARCHAR(128)               COMMENT '开户行',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_id` (`employee_id`),
    KEY `idx_salary_account_id` (`salary_account_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工薪资与合同信息表';

```

### 工号序列表 

### employee\_no\_sequence

```sql
CREATE TABLE IF NOT EXISTS `employee_no_sequence` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `year`              SMALLINT          NOT NULL COMMENT '年份，如2024',
    `dept_code`         VARCHAR(4)        NOT NULL COMMENT '部门编码，2位',
    `current_seq`       INT               NOT NULL DEFAULT 0 COMMENT '当前序号，自增',
    `create_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_year_dept` (`year`, `dept_code`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工号序列表，用于工号自增生成';

```

### 员工档案变更历史表 

### employee\_change\_log

```sql
CREATE TABLE IF NOT EXISTS `employee_change_log` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`       BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `field_name`        VARCHAR(64)       NOT NULL COMMENT '变更字段名',
    `old_value`         VARCHAR(512)               COMMENT '变更前值',
    `new_value`         VARCHAR(512)               COMMENT '变更后值',
    `change_type`       VARCHAR(32)       NOT NULL COMMENT '变更类型：DIRECT_EDIT/FLOW_CHANGE/SYSTEM',
    `operator_id`       BIGINT UNSIGNED            COMMENT '操作人ID',
    `remark`            VARCHAR(256)               COMMENT '备注',
    `create_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_create_time` (`create_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工档案变更历史表';

```

## API 设计

### 1. 查询员工列表

```plain
GET /api/v1/employees

```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| keyword | String | 否 | 模糊搜索关键词（姓名/工号/手机号） |
| departmentIds | Long\[\] | 否 | 部门ID列表（部门树多选） |
| positionIds | Long\[\] | 否 | 职位ID列表（下拉多选） |
| statuses | Integer\[\] | 否 | 在职状态列表：1=试用期 2=正式 3=待离职 4=已离职 |
| jobLevels | String\[\] | 否 | 职级列表，如P5、M2 |
| hireDateStart | Date | 否 | 入职日期起始 |
| hireDateEnd | Date | 否 | 入职日期截止 |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页条数，默认20 |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 156,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 1001,
        "employeeNo": "202401005",
        "name": "张三",
        "departmentName": "技术部",
        "positionName": "Java开发工程师",
        "jobLevel": "P5",
        "status": 2,
        "statusDesc": "正式",
        "hireDate": "2024-01-15"
      }
    ]
  }
}

```
> **说明**：返回字段根据当前用户角色权限进行脱敏处理，普通员工仅返回本人信息，部门主管仅返回本部门信息。

---

### 2. 获取员工档案详情

```plain
GET /api/v1/employees/{id}

```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 员工ID（路径参数） |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1001,
    "employeeNo": "202401005",
    "account": "138****1234",
    "status": 2,
    "statusDesc": "正式",
    "hireDate": "2024-01-15",
    "hireType": 1,
    "hireTypeDesc": "全职",
    "createTime": "2024-01-15T09:00:00",
    "personalInfo": {
      "name": "张三",
      "gender": 1,
      "genderDesc": "男",
      "phone": "138****1234",
      "email": "zhangsan@example.com",
      "idCard": "3301**********1234",
      "birthday": "1995-06-15",
      "registeredAddress": "浙江省杭州市...",
      "currentAddress": "浙江省杭州市..."
    },
    "workInfo": {
      "departmentId": 10,
      "departmentName": "技术部",
      "positionId": 20,
      "positionName": "Java开发工程师",
      "jobLevel": "P5",
      "directReportId": 1000,
      "directReportName": "李四",
      "workLocation": "杭州"
    },
    "salaryInfo": null
  }
}

```
> **说明**：`salaryInfo` 字段仅 HR 专员和财务专员可见，其他角色返回 `null`。`personalInfo` 中的敏感字段（身份证号、紧急联系人等）根据角色做脱敏处理：HR 可查看完整信息，部门主管仅可见姓名/手机号/邮箱，普通员工仅可查看本人信息。

---

### 3. 创建员工档案(由入转调离模块进行调用)

```plain
POST /api/v1/employees

```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| name | String | 是 | 姓名 |
| gender | Integer | 是 | 性别：1=男 2=女 |
| phone | String | 是 | 手机号 |
| email | String | 是 | 邮箱 |
| idCard | String | 是 | 身份证号 |
| hireDate | Date | 是 | 入职日期 |
| departmentId | Long | 是 | 所属部门ID |
| positionId | Long | 是 | 职位ID |
| jobLevel | String | 否 | 职级 |
| directReportId | Long | 否 | 直接汇报人ID |
| workLocation | String | 否 | 工作地点 |
| hireType | Integer | 是 | 入职类型：1=全职 2=兼职 3=实习 |
| contractType | Integer | 是 | 合同类型：1=固定期限 2=无固定期限 3=劳务合同 |
| contractExpireDate | Date | 否 | 合同到期日（固定期限合同必填） |
| probationRatio | BigDecimal | 是 | 试用期待遇比例，范围0.8~1.0 |
| salaryAccountId | Long | 是 | 薪资账套ID |
| baseSalary | BigDecimal | 是 | 基本工资 |
| bankAccount | String | 否 | 银行账号 |
| bankName | String | 否 | 开户行 |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1001,
    "employeeNo": "202401005",
    "account": "13800001234",
    "initialPassword": "Abc12345"
  }
}

```
> **说明**：通常由入职审批通过后系统自动调用，不直接对外暴露。工号由系统按规则自动生成，账号自动创建。

---

### 4. 更新员工档案

```plain
PUT /api/v1/employees/{id}

```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 员工ID（路径参数） |
| name | String | 否 | 姓名 |
| gender | Integer | 否 | 性别：1=男 2=女 |
| phone | String | 否 | 手机号（传 null 表示清空） |
| email | String | 否 | 邮箱 |
| birthday | Date | 否 | 生日（传 null 表示清空） |
| registeredAddress | String | 否 | 户籍地址（传 null 表示清空） |
| currentAddress | String | 否 | 现居住地址（传 null 表示清空） |
| emergencyContactName | String | 否 | 紧急联系人姓名（传 null 表示清空） |
| emergencyContactPhone | String | 否 | 紧急联系人电话（传 null 表示清空） |

> **字段更新规则**：不传 → 保持原值不变；传了值 → 更新；传 `null` → 清空该字段。

#### 请求示例

```json
{
  "email": "newemail@example.com",
  "currentAddress": "新地址",
  "birthday": null
}

```

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "updatedFields": ["email", "currentAddress"],
    "flowRequiredFields": []
  }
}

```
> **说明**：仅 HR 专员和系统管理员可操作。请求体为平铺的可选字段（不传保持原值、传 null 清空）。后端根据当前用户角色对应的 `editableFields` 逐字段校验，允许的直接更新并加入 `updatedFields`，不允许的拒绝并加入 `flowRequiredFields` 提示前端引导用户发起对应流程。

---

### 5. 获取字段权限

```plain
GET /api/v1/employees/field-permissions

```

#### 请求参数

无（根据当前登录用户角色自动判断）

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "viewableFields": ["name", "gender", "phone", "email", "departmentName", "positionName", "jobLevel", "status", "hireDate"],
    "editableFields": ["email", "currentAddress", "birthday"],
    "flowRequiredFields": ["departmentId", "positionId", "jobLevel", "directReportId", "phone", "workLocation"]
  }
}

```
> **说明**：接口根据当前用户角色返回可见字段、可编辑字段和需走流程字段列表，前端据此控制页面展示与交互。

---

### 6. 获取在职状态枚举(提供给查询员工列表所需的请求参数使用)

```plain
GET /api/v1/employees/statuses

```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| — | — | — | 无参数 |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {"status": 1, "statusName": "试用期"},
    {"status": 2, "statusName": "正式"},
    {"status": 3, "statusName": "待离职"},
    {"status": 4, "statusName": "已离职"}
  ]
}

```
> **说明**：供前端下拉筛选框使用，数据来源于代码枚举定义。

---

## 关键技术设计

### 创建档案四表事务写入方案

创建员工档案时，一次请求需向四张表写入数据，必须在同一个事务内完成，保证数据一致性：

```java
@Transactional
public CreateEmployeeVO createEmployee(CreateEmployeeDTO dto) {
    // 1. 写入 employee 主表
    Employee employee = buildEmployee(dto);
    employeeMapper.insert(employee);
    Long employeeId = employee.getId();

    // 2. 写入 personal_info 子表
    EmployeePersonalInfo personalInfo = buildPersonalInfo(employeeId, dto);
    personalInfoMapper.insert(personalInfo);

    // 3. 写入 work_info 子表
    EmployeeWorkInfo workInfo = buildWorkInfo(employeeId, dto);
    workInfoMapper.insert(workInfo);

    // 4. 写入 salary_info 子表
    EmployeeSalaryInfo salaryInfo = buildSalaryInfo(employeeId, dto);
    salaryInfoMapper.insert(salaryInfo);

    // 任意一步失败，前面已写入的数据全部回滚
    return buildVO(employee);
}
```

四张表通过 `employee_id` 关联，均设置 `uk_employee_id` 唯一索引保证一对一关系。

### 编辑字段逐字段校验方案

更新员工档案时，后端不依赖前端限制，而是在 Service 层逐字段校验权限：

```java
@Transactional
public UpdateEmployeeVO updateEmployee(Long id, UpdateEmployeeDTO dto, Long operatorId) {
    // 1. 获取当前角色的 editableFields
    Set<String> editableFields = getEditableFields(currentUser.getRole());

    List<String> updatedFields = new ArrayList<>();
    List<String> flowRequiredFields = new ArrayList<>();

    // 2. 逐字段校验
    for (FieldChange change : extractChanges(dto)) {
        if (editableFields.contains(change.getFieldName())) {
            // 允许直接编辑：记日志 → 更新
            changeLogMapper.insert(buildChangeLog(id, change, operatorId));
            updateField(id, change);
            updatedFields.add(change.getFieldName());
        } else {
            // 不允许：拒绝保存
            flowRequiredFields.add(change.getFieldName());
        }
    }

    return UpdateEmployeeVO.builder()
        .updatedFields(updatedFields)
        .flowRequiredFields(flowRequiredFields)
        .build();
}
```

- **不传** → 保持原值不变
- **传了值且在 editableFields 中** → 先记变更日志，再更新子表
- **传了值但不在 editableFields 中** → 拒绝，加入 `flowRequiredFields`

### 数据权限过滤方案

基于角色的数据范围过滤在 SQL 层统一拦截，使用 MyBatis 拦截器自动追加 WHERE 条件：

| **角色** | **数据范围** | **SQL 拦截策略** |
| --- | --- | --- |
| 系统管理员 / HR 专员 | 全量数据 | 不追加条件 |
| 部门主管 | 本部门及下属 | 递归查询部门树获取所有下级部门 ID，追加 `department_id IN (...)` |
| 普通员工 | 仅本人 | 追加 `employee_id = 当前用户ID` |
| 财务专员 | 薪资相关数据 | 按薪资维度单独控制 |

```java
// MyBatis 拦截器伪代码
@Intercepts(@Signature(type = Executor.class, method = "query", args = {...}))
public class DataScopeInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) {
        // 根据当前用户角色自动追加 SQL 条件
        String dataScope = getDataScope(currentUser);
        boundSql.setSql(boundSql.getSql() + dataScope);
        return invocation.proceed();
    }
}
```

业务代码无需在 Service 层硬编码 `if (role == xxx)` 判断。

### 字段级权限控制方案

数据权限控制"能看到多少条"，字段级权限控制"一条里能看到哪些字段"。通过接口动态返回：

| **字段** | HR 专员 | 部门主管 | 普通员工 | 财务专员 |
| --- | --- | --- | --- | --- |
| 姓名 | 查看 | 查看 | 查看（仅自己） | - |
| 手机号 | 查看 | 查看 | 查看（仅自己） | - |
| 身份证号 | 查看 | 不可见 | 不可见 | - |
| 薪资信息 | 查看 | 不可见 | 查看（仅自己） | 查看 |
| 紧急联系人 | 查看 | 不可见 | 查看（仅自己） | - |
| 银行卡号 | 查看 | 不可见 | 不可见 | - |

三个维度的字段分类：

| **维度** | **含义** | **前端行为** |
| --- | --- | --- |
| `viewableFields` | 当前角色能看到哪些字段 | 详情页：不在列表中的字段不展示 |
| `editableFields` | 当前角色能直接修改哪些字段 | 编辑页：仅在列表中的字段渲染输入框 |
| `flowRequiredFields` | 改了要走流程的字段 | 不在 editableFields 中但用户想改时，引导发起对应审批 |

前端通过调用 `GET /api/v1/employees/field-permissions` 获取以上三个列表，权限规则集中管理在后端，修改角色权限不需要前端发版。

### 敏感数据加密与脱敏方案

**加密存储**：

身份证号、银行卡号等敏感字段采用 AES-256 加密存储，密钥通过配置中心管理，支持密钥轮换：

```java
// 写入时加密
String encryptedIdCard = aesUtil.encrypt(dto.getIdCard());
personalInfo.setIdCard(encryptedIdCard);

// 查询时解密
String idCard = aesUtil.decrypt(personalInfo.getIdCard());
```

**脱敏展示规则**：

| **字段** | **脱敏规则** | **示例** | **可看完整信息的角色** |
| --- | --- | --- | --- |
| 身份证号 | 保留前 4 后 2，中间用 `*` 填充 | `3301**********34` | HR 专员 |
| 手机号 | 保留前 3 后 4 | `138****1234` | HR 专员、部门主管 |
| 银行卡号 | 仅显示后 4 位 | `****1234` | HR 专员、财务专员 |
| 姓名 | 不脱敏 | `张三` | 全部可见 |

脱敏在 Service 层返回 VO 之前完成，确保敏感信息不会以明文形式到达前端。即使前端通过浏览器调试工具查看响应体，也只能看到脱敏后的数据。

### 变更历史审计方案

所有员工档案字段的变更均自动记录到 `employee_change_log` 表：

```java
// 写入时记录
ChangeLog log = ChangeLog.builder()
    .employeeId(id)
    .fieldName("email")
    .oldValue(oldEmail)
    .newValue(newEmail)
    .changeType("DIRECT_EDIT")  // DIRECT_EDIT / FLOW_CHANGE / SYSTEM
    .operatorId(operatorId)
    .build();
changeLogMapper.insert(log);
// 再执行实际更新
personalInfoMapper.update(...);
```

三种变更类型：

| **changeType** | **触发场景** | **示例** |
| --- | --- | --- |
| `DIRECT_EDIT` | 通过编辑接口直接修改 | HR 修改员工邮箱 |
| `FLOW_CHANGE` | 通过入转调离流程变更 | 调岗后部门、职位变更 |
| `SYSTEM` | 系统自动变更 | 转正到期自动将状态从试用期改为正式 |

变更由 Service 层自动触发，业务代码无需显式调用，保证所有写入操作均可追溯。

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审产品规格，确认字段定义与权限规则 | 1天 |
| 技术方案 | 完成系分文档评审，确认数据库设计与接口方案 | 2天 |
| 数据库开发 | 建表、索引优化、初始化数据脚本 | 1天 |
| 后端开发 | 员工档案CRUD、工号生成、权限拦截、变更审计 | 5天 |
| 前端开发 | 员工列表页、档案详情页、编辑页、高级搜索 | 5天 |
| 联调测试 | 前后端联调、权限场景测试、并发工号生成测试 | 3天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 2天 |

> **总预估工期**：约 19 个工作日