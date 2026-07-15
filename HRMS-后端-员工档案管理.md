# HRMS-后端-员工档案管理

## 变更记录

| **日期**     | **版本** | **修订说明** | **作者** |
|------------| --- | --- | --- |
| 2026-07-08 | 1.0 | 初稿 | - |
| 2026-07-12 | 1.2 | 补全字段定义（四分类）、可编辑性/可见性、高级搜索、字段权限 | - |
| 2026-07-13 | 1.3 | 两表拆分、新增必填校验、锁定字段拦截、系统账号自动创建、变更日志 | - |
| 2026-07-14 | 1.4 | 同步实际代码：修复 DDL（移除 jobLevel/salaryId/hireType 列）、list 接口改用 DTO、add 接口响应简化、枚举统一至 model/enums | - |

## 项目背景

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 4 部分——员工档案管理。旨在实现员工档案的数字化统一管理，涵盖四类字段定义（基础信息/个人信息/工作信息/薪资合同）、员工查询与列表、工号自动生成、系统账号创建、字段级权限控制，为入转调离、薪资核算、考勤管理提供基础数据支撑。

### 相关资料

- [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 技术栈

| 层级 | 技术 |
|---|---|
| 框架 | Spring Boot 2.7.2 |
| ORM | MyBatis-Plus 3.5.2（逻辑删除：`@TableLogic` 注解字段 `isDeleted`） |
| 数据库 | MySQL 8.0（表列名下划线命名，`map-underscore-to-camel-case: false`） |
| 接口文档 | Knife4j |

## 功能模块

1. **员工档案字段定义**：四类字段（基础信息/个人信息/工作信息/薪资合同），每字段标注来源、必填、可编辑性、可见性
2. **工号自动生成**：格式 `年份(4位) + 部门编码(2位) + 序号(3位)`
3. **系统账号自动创建**：入职时自动创建，登录账号=手机号，初始密码随机生成
4. **员工列表查询**：分页 + 高级搜索（关键词/部门多选/职位多选/状态多选/职级多选/入职日期范围）
5. **员工详情**：四分区展示，按角色控制字段可见性
6. **员工编辑**：按字段权限控制可编辑项，锁定字段走流程
7. **字段级权限控制**：不同角色对敏感字段的可见性和可编辑性控制
8. **变更历史审计**：所有字段变更记录到变更日志表

### 功能模块树

```plain
员工档案管理
├── 档案字段定义
│   ├── 基础信息（系统生成/核心信息）
│   ├── 个人信息
│   ├── 工作信息
│   └── 薪资与合同信息
├── 工号生成规则
├── 系统账号创建
├── 员工查询与列表
│   ├── 默认列表展示
│   ├── 高级搜索
│   └── 列表操作（查看/编辑/更多）
└── 字段级权限控制
```

## 档案字段定义

### 4.1.1 基础信息（系统生成 / 核心信息）

| 字段 | 来源 | 说明 |
| --- | --- | --- |
| 工号 | 系统生成 | 格式：`年份(4) + 部门编码(2) + 序号(3)`，如 `202401005` |
| 系统账号 | 系统生成 | 登录账号=手机号，初始密码随机生成 |
| 在职状态 | 系统维护 | 1=试用期 / 2=正式 / 3=待离职 / 4=已离职 |
| 入职日期 | 入职时确认 | 实际到岗日期，影响工龄计算 |
| 创建时间 | 系统记录 | 账号创建时间 |

### 4.1.2 个人信息

| 字段 | 必填 | 可编辑 | 可见性 |
| --- | --- | --- | --- |
| 姓名 | 是 | ✓ | 所有人 |
| 性别 | 是 | ✓ | 所有人 |
| 手机号 | 是 | ✗（需申请） | HR + 部门主管 + 本人 |
| 邮箱 | 是 | ✓ | HR + 部门主管 + 本人 |
| 身份证号 | 是 | ✗ | HR + 本人（脱敏） |
| 生日 | 否 | ✓ | HR + 部门主管 + 本人 |
| 户籍地址 | 否 | ✓ | HR + 本人 |
| 现居住地址 | 否 | ✓ | HR + 部门主管 + 本人 |

### 4.1.3 工作信息

| 字段 | 必填 | 可编辑 | 可见性 |
| --- | --- | --- | --- |
| 所属部门 | 是 | ✗（需调岗流程） | 所有人 |
| 职位 | 是 | ✗（需调岗流程） | 所有人 |
| 职级 | 否 | ✗（需调岗流程） | 所有人 |
| 直接汇报人 | 否 | ✗（需调岗流程） | HR + 部门主管 + 本人 |
| 工作地点 | 否 | ✗（需调岗流程） | 所有人 |
| 入职类型 | 是 | ✗ | HR + 本人 |

### 4.1.4 薪资与合同信息

| 字段 | 必填 | 说明 | 可见性 |
| --- | --- | --- | --- |
| 合同类型 | 是 | 1=固定期限 / 2=无固定期限 / 3=劳务合同 | HR |
| 合同到期日 | 否 | 固定期限合同必填 | HR |
| 试用期待遇比例 | 是 | 80%–100%，影响薪资计算 | HR |
| 基本工资 | 是 | - | HR、财务 |
| 银行账号 | 否 | 加密存储 | HR |
| 开户行 | 否 | - | HR |

## 数据库设计

> 采用主表 + 详情表双表设计。主表存储列表查询核心字段，详情表存储敏感/大文本/补充字段，
> 一对一关联（`employee_detail.employeeId` UNIQUE KEY）。
> DB 列名使用驼峰命名，`map-underscore-to-camel-case: false`。

### 员工主表 employee

```sql
CREATE TABLE IF NOT EXISTS `employee` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employeeName`      VARCHAR(128)      NOT NULL COMMENT '员工姓名',
    `employeeNo`        VARCHAR(16)       NOT NULL COMMENT '工号，格式: 年份(4)+部门编码(2)+序号(3)',
    `account`           VARCHAR(32)       NULL COMMENT '系统账号（=手机号）',
    `userId`            BIGINT            NULL COMMENT '关联用户ID',
    `status`            TINYINT           NOT NULL DEFAULT 1 COMMENT '在职状态：1=试用期 2=正式 3=待离职 4=已离职',
    `gender`            TINYINT           NOT NULL DEFAULT 0 COMMENT '性别: 0=女 1=男',
    `phone`             VARCHAR(20)       NOT NULL COMMENT '手机号',
    `email`             VARCHAR(256)      NULL COMMENT '邮箱',
    `departmentId`      BIGINT            NULL COMMENT '部门ID',
    `positionId`        BIGINT            NULL COMMENT '职位ID',
    `hireDate`          DATETIME          NULL COMMENT '入职日期',
    `employmentType`    VARCHAR(16)       NOT NULL COMMENT '录用类型: FULL_TIME/PART_TIME/INTERN',
    `createTime`        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDeleted`         TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_no` (`employeeNo`),
    KEY `idx_department_id` (`departmentId`),
    KEY `idx_position_id` (`positionId`),
    KEY `idx_status` (`status`),
    KEY `idx_phone` (`phone`),
    KEY `idx_hire_date` (`hireDate`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工主表';
```

### 员工详情表 employee_detail

> 与 employee 一对一，存储敏感字段、大文本字段和补充信息。
> 列表查询无需 JOIN，详情查询时 LEFT JOIN 一次。

```sql
CREATE TABLE IF NOT EXISTS `employee_detail` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employeeId`            BIGINT UNSIGNED   NOT NULL COMMENT '员工ID，关联employee.id',
    `idCard`                VARCHAR(256)      NULL COMMENT '身份证号（加密存储）',
    `birthday`              DATE              NULL COMMENT '生日',
    `registeredAddress`     VARCHAR(512)      NULL COMMENT '户籍地址',
    `currentAddress`        VARCHAR(512)      NULL COMMENT '现居住地址',
    `jobLevel`              VARCHAR(8)        NULL COMMENT '职级',
    `directReportId`        BIGINT UNSIGNED   NULL COMMENT '直接汇报人ID',
    `workLocation`          VARCHAR(128)      NULL COMMENT '工作地点',
    `contractType`          TINYINT           NULL COMMENT '合同类型：1=固定期限 2=无固定期限 3=劳务合同',
    `contractExpireDate`    DATE              NULL COMMENT '合同到期日',
    `probationRatio`        DECIMAL(5,4)      NULL COMMENT '试用期待遇比例',
    `baseSalary`            DECIMAL(12,2)     NULL COMMENT '基本工资',
    `bankAccount`           VARCHAR(64)       NULL COMMENT '银行账号（加密存储）',
    `bankName`              VARCHAR(128)      NULL COMMENT '开户行',
    `emergencyContactName`  VARCHAR(128)      NULL COMMENT '紧急联系人姓名',
    `emergencyContactPhone` VARCHAR(32)       NULL COMMENT '紧急联系人电话',
    `createTime`            DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`            DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_id` (`employeeId`),
    KEY `idx_direct_report_id` (`directReportId`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工详情表';
```

### 员工档案变更日志表 employee_change_log

```sql
CREATE TABLE IF NOT EXISTS `employee_change_log` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employeeId`        BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `fieldName`         VARCHAR(64)       NOT NULL COMMENT '变更字段名',
    `oldValue`          VARCHAR(512)               COMMENT '变更前值',
    `newValue`          VARCHAR(512)               COMMENT '变更后值',
    `changeType`        VARCHAR(32)       NOT NULL COMMENT '变更类型：DIRECT_EDIT/FLOW_CHANGE/SYSTEM',
    `operatorId`        BIGINT UNSIGNED            COMMENT '操作人ID',
    `remark`            VARCHAR(256)               COMMENT '备注',
    `createTime`        DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employeeId`),
    KEY `idx_create_time` (`createTime`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工档案变更日志表';
```

## API 设计

> 基础路径：`/api`（`server.servlet.context-path=/api`）
>
> 统一响应：`{ "code": 0, "data": ..., "message": "ok" }`

---

### 1. 查询员工列表

```
GET /api/employee/list?keyword=张三&departmentIds=1,2&positionIds=3&statuses=1,2&hireDateStart=2025-01-01&hireDateEnd=2025-12-31&page=1&size=20
```

**请求参数**（Query String，绑定到 `EmployeeQueryRequest` DTO）：

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| keyword | String | 否 | 模糊搜索（姓名/工号/手机号） |
| departmentIds | Long[] | 否 | 部门ID列表（多选，逗号分隔） |
| positionIds | Long[] | 否 | 职位ID列表（多选，逗号分隔） |
| statuses | Integer[] | 否 | 在职状态：1=试用期 2=正式 3=待离职 4=已离职（多选） |
| jobLevels | String[] | 否 | 职级列表（多选） |
| hireDateStart | Date | 否 | 入职日期起始（格式 yyyy-MM-dd） |
| hireDateEnd | Date | 否 | 入职日期截止（格式 yyyy-MM-dd） |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |

**响应格式**：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 156,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 101,
        "employeeNo": "202401005",
        "employeeName": "张三",
        "departmentName": "技术部",
        "positionName": "Java开发工程师",
        "status": 2,
        "statusDesc": "正式",
        "hireDate": "2024-01-15"
      }
    ]
  }
}
```

---

### 2. 查询员工详情

```
GET /api/employee/detail?id=101
```

**响应格式**（扁平 VO 结构，按四个分区注释）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 101,
    "employeeNo": "202401005",
    "account": "138****1234",
    "status": 2,
    "statusDesc": "正式",
    "hireDate": "2024-01-15",
    "createTime": "2024-01-15T09:00:00",
    "employeeName": "张三",
    "gender": 1,
    "genderDesc": "男",
    "phone": "138****1234",
    "email": "zhangsan@example.com",
    "idCard": "3301**********1234",
    "birthday": "1995-06-15",
    "registeredAddress": "浙江省杭州市...",
    "currentAddress": "浙江省杭州市...",
    "departmentId": 10,
    "departmentName": "技术部",
    "positionId": 20,
    "positionName": "Java开发工程师",
    "directReportId": 100,
    "directReportName": "李四",
    "workLocation": "杭州",
    "employmentType": "FULL_TIME",
    "employmentTypeDesc": "全职",
    "contractType": 1,
    "contractTypeDesc": "固定期限",
    "contractExpireDate": "2027-01-14",
    "probationRatio": 0.8000,
    "baseSalary": 15000.00,
    "bankAccount": "****5678",
    "bankName": "招商银行",
    "emergencyContactName": "张父",
    "emergencyContactPhone": "139****5678"
  }
}
```

---

### 3. 新增员工

```
POST /api/employee/add
Content-Type: application/json
```

**请求体**：

```json
{
  "employeeName": "张三",
  "gender": 1,
  "phone": "13800001234",
  "email": "zhangsan@example.com",
  "idCard": "330106199506151234",
  "birthday": "1995-06-15",
  "registeredAddress": "浙江省杭州市...",
  "currentAddress": "浙江省杭州市...",
  "departmentId": 10,
  "positionId": 20,
  "jobLevel": "P5",
  "directReportId": 100,
  "workLocation": "杭州",
  "hireDate": "2024-01-15",
  "employmentType": "FULL_TIME",
  "contractType": 1,
  "contractExpireDate": "2027-01-14",
  "probationRatio": 0.8000,
  "baseSalary": 15000.00,
  "bankAccount": "6222****5678",
  "bankName": "招商银行",
  "emergencyContactName": "张父",
  "emergencyContactPhone": "13900005678"
}
```

| **参数** | **类型** | **必填** | **说明** |
| --- | --- | --- | --- |
| employeeName | String | 是 | 姓名 |
| gender | Integer | 是 | 性别：0=女 1=男 |
| phone | String | 是 | 手机号（同时作为系统账号） |
| email | String | 是 | 邮箱 |
| idCard | String | 是 | 身份证号 |
| birthday | Date | 否 | 生日 |
| registeredAddress | String | 否 | 户籍地址 |
| currentAddress | String | 否 | 现居住地址 |
| departmentId | Long | 是 | 所属部门ID，必须逻辑存在 |
| positionId | Long | 是 | 职位ID，必须逻辑存在 |
| directReportId | Long | 否 | 直接汇报人ID |
| workLocation | String | 否 | 工作地点 |
| hireDate | Date | 是 | 入职日期 |
| employmentType | String | 是 | 录用类型：FULL_TIME/PART_TIME/INTERN |
| contractType | Integer | 是 | 合同类型：1=固定期限 2=无固定期限 3=劳务合同 |
| contractExpireDate | Date | 否 | 合同到期日（固定期限必填） |
| probationRatio | BigDecimal | 是 | 试用期待遇比例 0.8~1.0 |
| baseSalary | BigDecimal | 是 | 基本工资 |
| bankAccount | String | 否 | 银行账号 |
| bankName | String | 否 | 开户行 |
| emergencyContactName | String | 否 | 紧急联系人姓名 |
| emergencyContactPhone | String | 否 | 紧急联系人电话 |

**校验规则**：
- 姓名、性别、手机号、录用类型、部门、职位 必填
- 手机号格式校验（1[3-9] 开头 11 位）
- 邮箱格式非必填，填则校验
- `departmentId` 对应的部门必须逻辑存在
- `positionId` 对应的职位必须逻辑存在
- 工号系统自动生成；系统账号=手机号，自动创建
- 试用期待遇比例需在 0.8~1.0 之间
- 固定期限合同 `contractExpireDate` 必填

**成功响应**：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 101
  }
}
```

---

### 4. 更新员工

```
PUT /api/employee/update
Content-Type: application/json
```

**请求体**：

```json
{
  "id": 101,
  "email": "newemail@example.com",
  "currentAddress": "新地址"
}
```

> 只传需要变更的字段；锁定字段（手机号、身份证号、部门、职位等）前端置灰，后端拒绝直接修改，需走对应流程。

**字段可编辑性规则**：

| 字段类型 | 可编辑性 | 说明 |
| --- | --- | --- |
| 姓名、性别、邮箱、生日、户籍地址、现居住地址 | ✓ 可编辑 | 直接保存 |
| 手机号、身份证号 | ✗ 锁定 | 需申请流程 |
| 部门、职位、直接汇报人、工作地点、职级 | ✗ 锁定 | 需调岗流程 |
| 合同类型、合同到期日、试用期比例、基本工资、银行账号、开户行 | ✗ 锁定 | 仅 HR 可编辑 |
| 录用类型 | ✗ 锁定 | 入职后不可变更 |

---

### 5. 删除员工

```
POST /api/employee/delete
Content-Type: application/json
```

**请求体**：`{ "id": 101 }`

> 软删除；校验无待处理的审批流等关联数据。

---

### 6. 员工变更历史

```
GET /api/employee/change-logs?employeeId=101&page=1&size=20
```

**响应格式**：

```json
{
  "code": 0,
  "data": {
    "total": 5,
    "records": [
      {
        "id": 201,
        "fieldName": "email",
        "fieldDesc": "邮箱",
        "oldValue": "old@example.com",
        "newValue": "new@example.com",
        "changeType": "DIRECT_EDIT",
        "operatorName": "HR管理员",
        "createTime": "2025-03-15T14:30:00"
      }
    ]
  }
}
```

---

## API 汇总

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 1 | GET | `/api/employee/list` | 员工列表（分页 + 高级搜索） |
| 2 | GET | `/api/employee/detail` | 员工详情（四分区，敏感字段脱敏） |
| 3 | POST | `/api/employee/add` | 新增员工 + 自动创建系统账号 |
| 4 | PUT | `/api/employee/update` | 更新员工（锁定字段拦截 + 变更日志） |
| 5 | POST | `/api/employee/delete` | 删除员工（软删除） |
| 6 | GET | `/api/employee/change-logs` | 员工变更历史 |

> `generate-employee-no` 和 `field-permissions` 已改为 Service 内部方法，不对外暴露。

## 关键技术设计

### 工号生成

规则：`年份(4位) + 部门编码(2位) + 序号(3位)`。同一部门同一年份序号递增，`employee_no` 唯一索引防并发重复。

### 系统账号创建

新增员工时自动创建：
1. 登录账号 = 手机号
2. 初始密码随机生成（大小写字母+数字，8位）
3. 写入 `user` 表，`userId` 回填到 `employee.userId`

### 字段权限控制

字段权限在 Service 层通过 `getFieldPermissions()` 方法返回，前端可选调用：

| 字段 | HR | 部门主管 | 普通员工 | 财务 |
| --- | --- | --- | --- | --- |
| 姓名/性别 | 查看 | 查看 | 查看 | - |
| 手机号/邮箱 | 查看 | 查看（本部门） | 查看（仅己） | - |
| 身份证号 | 查看 | 不可见 | 查看（仅己） | - |
| 薪资信息 | 查看 | 不可见 | 查看（仅己） | 查看 |
| 银行账号 | 查看 | 不可见 | 不可见 | - |

### 变更历史审计

所有字段变更记录到 `employee_change_log`：
- `DIRECT_EDIT`：直接编辑
- `FLOW_CHANGE`：流程变更（调岗、离职）
- `SYSTEM`：系统自动变更

### 敏感数据脱敏

- 身份证号：显示前4后4，中间 `*`
- 手机号：显示前3后4
- 银行账号：显示后4位

## 排期

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 确认字段定义与权限规则 | 1天 |
| 技术方案 | 完成系分文档评审 | 1天 |
| 数据库开发 | 建表、索引、初始化数据 | 0.5天 |
| 后端开发 | CRUD + 高级搜索 + 工号 + 账号创建 + 权限 + 审计 | 5天 |
| 前端开发 | 列表 + 详情（四分区）+ 编辑（权限表单）+ 变更历史 | 5天 |
| 联调测试 | 前后端联调、权限场景、并发工号测试 | 3天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 2天 |

> **总预估工期**：约 17.5 个工作日
