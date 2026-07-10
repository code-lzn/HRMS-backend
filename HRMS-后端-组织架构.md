# HRMS-后端-组织架构管理

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | - |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 3 部分——组织架构管理。当前公司内部组织架构依赖线下维护，缺乏统一的部门树和职位体系管理。本模块旨在建立公司多级组织架构的数字化管理能力，涵盖部门增删改查与人员统计、职位序列与职级体系定义，为员工档案、入转调离、考勤薪资等模块提供组织和职位基础数据支撑。

### 相关资料

- [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述组织架构管理涉及的功能与场景。

本模块核心功能包括：

1. **部门管理**：支持多级部门树增删改查，部门编码用于工号生成，部门负责人决定数据权限范围
2. **部门层级约束**：最大层级深度 5 级，部门合并时强制员工转移
3. **部门人数统计**：实时统计该部门及下属部门在职员工数（试用期 + 正式）
4. **职位管理**：定义职位名称、职位序列（M/P/S）、职级范围、默认试用期
5. **职位序列职级对照**：管理序列 M1-M5、专业序列 P1-P10、支持序列 S1-S5

### 功能模块树

```plain
组织架构管理
├── 部门管理
│   ├── 部门树查询
│   ├── 部门新增
│   ├── 部门编辑
│   ├── 部门删除（校验：无下属部门 + 无在职员工）
│   ├── 部门合并（含员工批量转移）
│   └── 部门人数统计（含递归汇总）
└── 职位管理
    ├── 职位列表查询
    ├── 职位新增
    ├── 职位编辑
    ├── 职位删除
    └── 职位序列职级对照查询
```

## 流程图

> 对组织架构管理涉及的核心流程进行梳理。

### 3-1 部门新增流程

```plain
管理员进入部门管理页
     │
     ▼
点击"新增部门"
     │
     ▼
填写部门信息（名称、编码、上级部门、负责人、排序、描述）
     │
     ▼
系统校验
     ├── 部门编码是否唯一？→ 否 → 提示"编码已存在"
     ├── 上级部门层级是否已达5级？→ 是 → 提示"已达到最大层级深度"
     ├── 部门名称同级是否重名？→ 是 → 提示"同级部门名称重复"
     │
     ▼
创建部门记录
     │
     ▼
初始化工号序列表（employee_no_sequence，为该部门编码创建当年记录）
     │
     ▼
部门创建完成
```

### 3-2 部门删除流程

```plain
管理员点击删除部门
     │
     ▼
系统校验
     ├── 是否有子部门？→ 是 → 提示"请先删除子部门"
     ├── 是否有在职员工？→ 是 → 提示"请先转移员工至其他部门"
     │
     ▼
软删除部门记录（is_deleted = 1）
     │
     ▼
删除完成
```

### 3-3 部门合并流程

```plain
管理员选择"合并部门"
     │
     ▼
选择源部门（被合并）和目标部门（保留）
     │
     ▼
系统校验
     ├── 源部门 = 目标部门？→ 是 → 提示"不能合并到自身"
     ├── 目标部门是否为源部门的子部门？→ 是 → 提示"不能合并到子部门"
     │
     ▼
批量更新：源部门所有在职员工 → 目标部门
     │
     ▼
批量更新：源部门所有子部门 → 目标部门子部门
     │
     ▼
软删除源部门
     │
     ▼
记录合并日志
     │
     ▼
合并完成
```

### 3-4 职位新增流程

```plain
管理员点击"新增职位"
     │
     ▼
填写职位信息（名称、序列、所属部门、职级范围、默认试用期、描述）
     │
     ▼
系统校验
     ├── 职位名称 + 序列 + 部门 是否唯一？→ 否 → 提示"已存在相同职位"
     ├── 职级范围是否在该序列允许范围内？→ 否 → 提示"职级超出序列范围"
     │
     ▼
创建职位记录
     │
     ▼
职位创建完成
```

## UML 图

> 描述组织架构管理的核心类和依赖关系。

### 组织架构核心领域模型

```plain
@startuml
class Department {
  - id: Long
  - name: String                "部门名称"
  - code: String                "部门编码，用于工号生成"
  - parentId: Long              "上级部门ID"
  - managerId: Long             "部门负责人ID"
  - sortOrder: Integer          "排序序号"
  - description: String         "部门描述"
  - employeeCount: Integer      "部门人数（仅VO层，不持久化）"
  - isDeleted: Integer          "逻辑删除"
  - createTime: LocalDateTime
  - updateTime: LocalDateTime
  --
  + getChildDepartments(): List<Department>
  + getRecursiveEmployeeCount(): Integer
}

class Position {
  - id: Long
  - name: String                "职位名称"
  - sequence: PositionSequence  "职位序列 M/P/S"
  - departmentId: Long          "所属部门ID，null=全公司通用"
  - levelMin: String            "职级下限"
  - levelMax: String            "职级上限"
  - defaultProbationMonths: Int "默认试用期(月)"
  - description: String         "职位描述"
  - isDeleted: Integer          "逻辑删除"
  - createTime: LocalDateTime
  - updateTime: LocalDateTime
  --
  + getLevelRange(): String
}

enum PositionSequence {
  MANAGEMENT   "管理序列 M"
  PROFESSIONAL "专业序列 P"
  SUPPORT      "支持序列 S"
}

Department "1" -- "*" Department : parent > children
Department "1" -- "*" Position : has
Position "*" -- "1" PositionSequence

note right of Department
  部门树通过 parentId 自关联，
  人数统计实时 SQL 递归计算
end note
@enduml
```

[PlantUML在线绘制](https://www.processon.com/plantuml)

## 时序图

> 描述组织架构管理核心交互的调用时序。

### 3-1 部门树查询时序

![部门树查询时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-dept-tree.svg)

### 3-2 部门合并时序

![部门合并时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-dept-merge.svg)

### 3-3 部门人数统计时序

![部门人数统计时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-dept-count.svg)

## 数据库设计

> 组织架构管理模块涉及的核心数据表。

### 部门表 department

```sql
CREATE TABLE IF NOT EXISTS `department` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`              VARCHAR(64)       NOT NULL COMMENT '部门名称',
    `code`              VARCHAR(4)        NOT NULL COMMENT '部门编码，2位，用于工号生成',
    `parent_id`         BIGINT UNSIGNED            COMMENT '上级部门ID，空表示根部门',
    `manager_id`        BIGINT UNSIGNED            COMMENT '部门负责人ID，关联employee.id',
    `sort_order`        INT               NOT NULL DEFAULT 0 COMMENT '排序序号，越小越靠前',
    `description`       VARCHAR(256)               COMMENT '部门描述',
    `is_deleted`        TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    `create_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_manager_id` (`manager_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '部门表';
```

### 职位表 position

```sql
CREATE TABLE IF NOT EXISTS `position` (
    `id`                        BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                      VARCHAR(64)       NOT NULL COMMENT '职位名称，如Java开发工程师',
    `sequence`                  TINYINT           NOT NULL COMMENT '职位序列：1=M管理 2=P专业 3=S支持',
    `department_id`             BIGINT UNSIGNED            COMMENT '所属部门ID，空表示全公司通用',
    `level_min`                 VARCHAR(8)        NOT NULL COMMENT '职级下限，如P1',
    `level_max`                 VARCHAR(8)        NOT NULL COMMENT '职级上限，如P10',
    `default_probation_months`  INT               NOT NULL DEFAULT 3 COMMENT '默认试用期月数',
    `description`               VARCHAR(256)               COMMENT '职位描述/岗位职责',
    `is_deleted`                TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    `create_time`               DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`               DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_sequence` (`sequence`),
    KEY `idx_department_id` (`department_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '职位表';
```

### 部门合并日志表 department_merge_log

```sql
CREATE TABLE IF NOT EXISTS `department_merge_log` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_dept_id`      BIGINT UNSIGNED   NOT NULL COMMENT '源部门ID（被合并）',
    `source_dept_name`    VARCHAR(64)       NOT NULL COMMENT '源部门名称（快照）',
    `target_dept_id`      BIGINT UNSIGNED   NOT NULL COMMENT '目标部门ID（保留）',
    `target_dept_name`    VARCHAR(64)       NOT NULL COMMENT '目标部门名称（快照）',
    `transferred_employees` INT             NOT NULL DEFAULT 0 COMMENT '转移员工数',
    `operator_id`         BIGINT UNSIGNED   NOT NULL COMMENT '操作人ID',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_source_dept_id` (`source_dept_id`),
    KEY `idx_target_dept_id` (`target_dept_id`),
    KEY `idx_create_time` (`create_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '部门合并日志表';
```

## API 设计

### 1. 获取部门树

```plain
GET /api/v1/departments/tree
```

#### 请求参数

无

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "总公司",
      "code": "00",
      "managerId": 100,
      "managerName": "王总",
      "sortOrder": 0,
      "employeeCount": 156,
      "children": [
        {
          "id": 10,
          "name": "技术部",
          "code": "01",
          "managerId": 200,
          "managerName": "李四",
          "sortOrder": 1,
          "employeeCount": 42,
          "children": []
        }
      ]
    }
  ]
}
```

> **说明**：返回完整部门树，`employeeCount` 为含子部门的递归汇总人数。

---

### 2. 新增部门

```plain
POST /api/v1/departments
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| name | String | 是 | 部门名称 |
| code | String | 是 | 部门编码（2位） |
| parentId | Long | 否 | 上级部门ID，空表示根部门 |
| managerId | Long | 否 | 部门负责人ID |
| sortOrder | Integer | 是 | 排序序号 |
| description | String | 否 | 部门描述 |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { "id": 15 }
}
```

---

### 3. 更新部门

```plain
PUT /api/v1/departments/{id}
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 部门ID（路径参数） |
| name | String | 否 | 部门名称 |
| managerId | Long | 否 | 部门负责人ID |
| sortOrder | Integer | 否 | 排序序号 |
| description | String | 否 | 部门描述 |

> **说明**：部门编码和上级部门不支持直接修改，需走合并/迁移流程。

---

### 4. 删除部门

```plain
DELETE /api/v1/departments/{id}
```

> **说明**：删除前校验无子部门且无在职员工，否则返回业务错误码。

---

### 5. 合并部门

```plain
POST /api/v1/departments/merge
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| sourceDeptId | Long | 是 | 源部门ID（被合并） |
| targetDeptId | Long | 是 | 目标部门ID（保留） |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "transferredEmployees": 12,
    "transferredChildDepts": 2
  }
}
```

---

### 6. 获取职位列表

```plain
GET /api/v1/positions
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| sequence | Integer | 否 | 职位序列过滤：1=M 2=P 3=S |
| departmentId | Long | 否 | 部门ID过滤，不传返回所有 |

---

### 7. 新增职位

```plain
POST /api/v1/positions
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| name | String | 是 | 职位名称 |
| sequence | Integer | 是 | 职位序列：1=M 2=P 3=S |
| departmentId | Long | 否 | 所属部门ID |
| levelMin | String | 是 | 职级下限 |
| levelMax | String | 是 | 职级上限 |
| defaultProbationMonths | Integer | 是 | 默认试用期月数 |
| description | String | 否 | 职位描述 |

---

### 8. 更新职位

```plain
PUT /api/v1/positions/{id}
```

---

### 9. 删除职位

```plain
DELETE /api/v1/positions/{id}
```

> **说明**：删除前校验无员工引用该职位。

---

### 10. 获取序列职级对照

```plain
GET /api/v1/positions/sequences
```

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "sequence": 1,
      "sequenceName": "管理序列",
      "sequenceCode": "M",
      "levels": ["M1", "M2", "M3", "M4", "M5"]
    },
    {
      "sequence": 2,
      "sequenceName": "专业序列",
      "sequenceCode": "P",
      "levels": ["P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10"]
    },
    {
      "sequence": 3,
      "sequenceName": "支持序列",
      "sequenceCode": "S",
      "levels": ["S1", "S2", "S3", "S4", "S5"]
    }
  ]
}
```

## 关键技术设计

### 部门树查询方案

采用一次查询加载全部部门，内存中构建树结构：

1. `SELECT * FROM department WHERE is_deleted = 0 ORDER BY parent_id, sort_order`
2. 内存中按 `parentId` 分组，递归构建树
3. 部门人数通过子查询递归汇总：`SELECT COUNT(*) FROM employee WHERE department_id IN (部门及子部门ID集合) AND status IN (1, 2)`

对于大数据量场景（部门数 > 500），可考虑使用 MySQL 递归 CTE（MySQL 8.0+）：

```sql
WITH RECURSIVE dept_tree AS (
    SELECT id, parent_id, 0 AS depth FROM department WHERE id = ?
    UNION ALL
    SELECT d.id, d.parent_id, dt.depth + 1
    FROM department d INNER JOIN dept_tree dt ON d.parent_id = dt.id
)
SELECT * FROM dept_tree;
```

### 部门层级深度校验

新增部门时校验层级深度，避免超过 5 级：

1. 获取目标父部门的层级深度（从根递归计数）
2. 若 `depth + 1 > 5`，拒绝创建
3. 可使用路径字段（如 `path` 字段存储 `1/10/15`）优化层级查询性能

### 部门合并事务保障

部门合并涉及多表操作，需在事务中执行：

1. 开启事务
2. 更新源部门所有在职员工的 `department_id` → 目标部门
3. 更新源部门所有子部门的 `parent_id` → 目标部门
4. 软删除源部门
5. 写入 `department_merge_log`
6. 提交事务

若步骤 2/3 涉及大量数据，可分批更新（每批 500 条），防止长事务锁表。

### 部门编码唯一性

- `department.code` 设置 `UNIQUE KEY`
- 工号生成依赖部门编码，变更部门编码会导致历史工号溯源困难，因此编码创建后不允许修改
- 部门删除后编码保留不回收，防止历史工号冲突

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审产品规格，确认部门层级限制与职位序列定义 | 0.5天 |
| 技术方案 | 完成系分文档评审，确认数据库设计与接口方案 | 1天 |
| 数据库开发 | 建表、索引优化、初始化默认部门/职位数据脚本 | 0.5天 |
| 后端开发 | 部门CRUD、部门树查询、部门合并、人数统计、职位CRUD、序列对照 | 3天 |
| 前端开发 | 部门树管理页、职位管理页 | 2天 |
| 联调测试 | 前后端联调、合并场景测试、并发安全测试 | 1.5天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 1天 |

> **总预估工期**：约 9.5 个工作日
