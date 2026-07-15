# HRMS-组织架构管理-后端

# HRMS-组织架构管理-后端

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-11 | 1.0 | 初稿 | 张浩 |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 3 部分——组织架构管理。当前公司内部组织架构依赖 Excel 维护，存在层级关系不清晰、部门人员统计滞后、职位与职级体系不规范等问题。本模块旨在建立统一的组织架构数字化管理，涵盖部门多级树形管理、职位与序列职级体系定义、部门人员实时统计等核心能力，同时为员工档案管理、考勤管理、薪资管理、审批流转等下游模块提供组织架构基础数据支撑。

### 相关资料

*   [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)
    

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述组织架构管理涉及的功能与场景。

本模块核心功能包括：

1.  **部门管理**：部门展示及操作、多级树形结构维护、层级深度限制（最大 5 级）、部门编码管理、部门负责人设置、排序管理、部门人数实时统计（含下属部门递归汇总）
    
2.  **职位管理**：职位 展示及操作、职位序列枚举（管理序列 M / 专业序列 P / 支持序列 S）、职级范围校验、所属部门关联（可为空表示全公司通用）、默认试用期配置
    

### 功能模块树

```plain
组织架构管理
├── 部门管理
│   ├── 创建部门
│   ├── 编辑部门
│   ├── 删除部门（逻辑删除）
│   ├── 部门列表查询（平铺/分页）
│   ├── 部门详情查询
│   ├── 部门树查询（嵌套结构 + 人数统计）
│   ├── 部门层级校验（最大 5 级）
│   └── 部门人数统计（实时，含子部门递归）
└── 职位管理
    ├── 创建职位
    ├── 编辑职位
    ├── 删除职位（逻辑删除）
    ├── 职位列表查询（按序列/部门筛选）
    ├── 职位详情查询
    ├── 职位序列枚举查询
    └── 职级范围校验
```

## 流程图

> 对组织架构管理涉及的核心流程进行梳理。

### 3-1 部门创建流程

```plain
HR/管理员发起创建部门
     │
     ▼
系统校验必填字段
     ├── 部门名称：必填，不可重复
     ├── 部门编码：必填，唯一
     ├── 排序序号：必填，同级不可重复
     └── 校验不通过 → 返回错误提示
     │
     ▼
校验排序序号唯一性（同级部门）
     ├── SELECT COUNT(*) FROM department WHERE parent_id = {parentId} AND sort_order = {sortOrder} AND is_deleted = 0
     ├── count > 0 → 拒绝创建，提示"同级部门中排序序号重复，请重新填写"
     └── 通过 → 继续
     │
     ▼
校验上级部门（parent_id）
     ├── 未指定上级部门 → 作为根部门
     ├── 指定上级部门 → 校验上级部门是否存在且未被删除
     │   └── 不存在/已删除 → 返回错误提示
     └── 校验层级深度
         │
         ├── 递归查询上级部门的 parent_id，计算当前深度
         ├── 深度 > 5 → 拒绝创建，提示"超过最大层级深度 5 级"
         └── 深度 ≤ 5 → 继续
         │
         ▼
校验部门负责人（manager_id，可选）
     ├── 指定负责人 → 校验员工是否存在且在职状态为"试用期"或"正式"
     └── 负责人不存在或非在职 → 返回错误提示
     │
     ▼
INSERT INTO department
     │
     ▼
返回创建结果（含部门ID）
```

### 3-2 部门删除流程

```plain
HR/管理员发起删除部门
     │
     ▼
校验部门是否存在且未删除
     ├── 不存在/已删除 → 返回错误提示
     └── 存在 → 继续
     │
     ▼
校验是否有子部门
     ├── SELECT COUNT(*) FROM department WHERE parent_id = {id} AND is_deleted = 0
     ├── 存在子部门 > 0 → 拒绝删除，提示"该部门下存在子部门，请先删除子部门"
     └── 无子部门 → 继续
     │
     ▼
校验是否有在职员工
     ├── SELECT COUNT(*) FROM employee_work_info WHERE department_id = {id} AND employee.status IN (1,2)
     ├── 员工数 > 0 → 拒绝删除，提示"该部门下有在职员工，请先转移员工"
     └── 无员工 → 继续
     │
     ▼
逻辑删除（is_deleted = 1）
     │
     ▼
返回删除成功
```

### 3-3 职位创建流程

```plain
HR/管理员发起创建职位
     │
     ▼
系统校验必填字段
     ├── 职位名称：必填
     ├── 职位序列：必填，枚举值 M/P/S
     ├── 职级范围：必填，格式校验（序列+数字-数字）
     │   ├── M序列 → M1-M5
     │   ├── P序列 → P1-P10
     │   └── S序列 → S1-S5
     ├── 默认试用期：必填，整数（月）
     └── 校验不通过 → 返回错误提示
     │
     ▼
校验所属部门（department_id，可选）
     ├── 指定部门 → 校验部门是否存在且未删除
     │   └── 不存在/已删除 → 返回错误提示
     └── 未指定 → 表示全公司通用职位
     │
     ▼
INSERT INTO position
     │
     ▼
返回创建结果（含职位ID）
```

### 3-4 部门人数统计流程

```plain
触发时机：查询部门树时实时计算
     │
     ▼
全量查询所有未删除部门
     ├── SELECT * FROM department WHERE is_deleted = 0 ORDER BY sort_order
     └── 一次性获取完整部门列表
     │
     ▼
批量统计各部门直接员工数
     ├── SELECT department_id, COUNT(*) AS cnt
     │   FROM employee
     │   WHERE status IN (1, 2) AND is_deleted = 0
     │   GROUP BY department_id
     ├── 得到 Map<部门ID, 直接员工数>
     └── 统计口径：在职状态为"试用期"或"正式"的员工
     │
     ▼
按 parent_id 分组，构建父子关系 Map
     ├── parent_id = NULL → 归入 key=0L（根部门集合）
     └── 得到 Map<父部门ID, 子部门列表>
     │
     ▼
从根部门出发，深度优先递归组装树 + 汇总人数
     ├── 对每个节点：
     │   ├── 先递归构建所有子节点（深度优先，子节点人数先确定）
     │   ├── 汇总所有子节点的 employeeCount
     │   └── 当前节点人数 = 直接员工数 + 子节点汇总
     └── 叶子节点：子节点汇总 = 0，最终人数 = 直接员工数
     │
     ▼
返回完整部门树（每个节点含 employeeCount，含下属部门汇总）
```

## UML 图

> 描述组织架构管理模块的核心类和依赖关系。

### 组织架构管理核心领域模型

```plain
@startuml

' ==================== 核心实体 ====================

class Department {
  - id: Long
  - name: String                     "部门名称"
  - code: String                     "部门编码"
  - parentId: Long                   "上级部门ID（null=根部门）"
  - managerId: Long                  "部门负责人ID"
  - sortOrder: Integer               "排序序号"
  - description: String              "部门描述"
  - isDeleted: Boolean               "逻辑删除"
  --
  + getLevel(): Integer              "计算当前层级深度"
  + getChildCount(): Integer         "子部门数量"
  + getEmployeeCount(): Integer      "本部门在职员工数"
  + getTotalEmployeeCount(): Integer "含下属部门在职员工数"
}

class Position {
  - id: Long
  - name: String                     "职位名称"
  - sequence: PositionSequence       "职位序列"
  - departmentId: Long               "所属部门ID（null=全公司通用）"
  - levelMin: Integer                "职级下限（如P序列P1）"
  - levelMax: Integer                "职级上限（如P序列P10）"
  - defaultProbationMonths: Integer  "默认试用期（月）"
  - description: String              "职位描述"
  - isDeleted: Boolean               "逻辑删除"
  --
  + getLevelRange(): String          "获取职级范围（如P1-P10）"
  + isValidLevel(Integer): Boolean   "校验职级是否在范围内"
}

' ==================== 枚举 ====================

enum PositionSequence {
  MANAGEMENT    "管理序列 M"
  PROFESSIONAL  "专业序列 P"
  SUPPORT        "支持序列 S"
}

' ==================== 关系 ====================

Department "1" -- "0..*" Department : 父子部门\n(parent_id自引用)
Department "1" -- "0..*" Position : 部门职位
Position "*" -- "1" PositionSequence : 职位序列分类

' 跨模块关联
Department "1" -- "*" EmployeeWorkInfo : 员工工作信息引用
Position "1" -- "*" EmployeeWorkInfo : 员工工作信息引用

@enduml
```

[PlantUML在线绘制_PlantUML在线编辑器官网_PlantUML在线编译器免下载 - ProcessOn](https://www.processon.com/plantuml)

## 时序图

> 描述组织架构管理核心交互的调用时序。

### 3-1 查询部门树时序

![1-1783738309660-1.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/a2QnV45v2d7kyO4X/img/4be1b5d5-3395-4e1f-8ce1-6258efafe8c9.png)

```plain
@startuml
actor 用户
participant "DepartmentController" as DC
participant "DepartmentService" as DS
participant "DepartmentMapper" as DM
participant "EmployeeMapper" as EM
database "MySQL" as DB

用户 -> DC: GET /api/v1/departments/tree
DC -> DC: 获取当前登录用户信息
DC -> DS: getDepartmentTree()

DS -> DM: 查询所有未删除部门
DM -> DB: SELECT * FROM department\nWHERE is_deleted = 0 ORDER BY sort_order
DB --> DM: 部门列表
DM --> DS: departments

DS -> EM: 批量统计各部门直接员工数
EM -> DB: SELECT department_id, COUNT(*) AS cnt\nFROM employee\nWHERE status IN (1,2) AND is_deleted = 0\nGROUP BY department_id
DB --> EM: Map<部门ID, 直接员工数>
EM --> DS: directEmployeeCounts

DS -> DS: 组装部门树 + 汇总人数\n1. 按 parent_id 分组构建父子关系 Map\n2. 从根部门出发，深度优先递归\n3. 每个节点人数 = 直接员工数 + 子节点汇总

DS --> DC: 部门树（含人数统计）
DC --> 用户: 返回树形结构
@enduml
```

### 3-2 创建部门时序

![PlantUML (1).png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/a2QnV45v2d7kyO4X/img/65b7c3a3-cbe2-4fe9-81ad-997ba6199775.png)

```plain
@startuml
actor "HR/管理员" as HR
participant "DepartmentController" as DC
participant "DepartmentService" as DS
participant "DepartmentMapper" as DM
participant "EmployeeMapper" as EM
database "MySQL" as DB

HR -> DC: POST /api/v1/departments
DC -> DC: 校验 JSR-303 注解
DC -> DS: createDepartment(dto)

DS -> DM: 校验部门名称唯一性\n(name + is_deleted)
DM -> DB: SELECT COUNT(*) FROM department\nWHERE name = ? AND is_deleted = 0
DB --> DM: count
DM --> DS: 唯一性结果

alt 名称重复
    DS --> DC: 提示"部门名称已存在"
    DC --> HR: 返回错误
else 名称唯一
    alt 指定了上级部门
        DS -> DM: 查询上级部门
        DM -> DB: SELECT * FROM department\nWHERE id = ? AND is_deleted = 0
        DB --> DM: department
        DM --> DS: department

        alt 上级部门不存在或已删除
            DS --> DC: 提示"上级部门不存在或已删除"
            DC --> HR: 返回错误
        else 上级部门存在
            DS -> DS: 递归计算层级深度\n(沿 parent_id 向上追溯)
            alt 深度 >= 5
                DS --> DC: 提示"超过最大层级深度5级"
                DC --> HR: 返回错误
            end
        end
    end

    alt 指定了部门负责人
        DS -> EM: 校验负责人是否在职
        EM -> DB: SELECT * FROM employee\nWHERE id = ? AND status IN (1,2) AND is_deleted = 0
        DB --> EM: employee
        EM --> DS: employee
        alt 负责人不存在或非在职
            DS --> DC: 提示"负责人不存在或非在职"
            DC --> HR: 返回错误
        end
    end

    DS -> DM: INSERT INTO department
    DM -> DB: INSERT
    DB --> DM: OK

    DS --> DC: 创建成功
    DC --> HR: 返回部门ID
end
@enduml
```

### 3-3 创建职位时序

![3-1783738346251-5.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/a2QnV45v2d7kyO4X/img/8e381ef9-2c31-478e-ac57-efd62e1dbdf6.png)

```plain
@startuml
actor "HR/管理员" as HR
participant "PositionController" as PC
participant "PositionService" as PS
participant "PositionMapper" as PM
participant "DepartmentMapper" as DM
database "MySQL" as DB

HR -> PC: POST /api/v1/positions
PC -> PC: 校验 JSR-303 注解
PC -> PS: createPosition(dto)

PS -> PS: 校验职位序列枚举值

PS -> PS: 校验职级范围格式\n（如M1-M5, P1-P10, S1-S5）
alt 序列与职级不匹配
    PS --> PC: 提示"职级范围超出该序列限定"
    PC --> HR: 返回错误
else 校验通过
    alt 指定了所属部门
        PS -> DM: 查询部门是否存在且未删除
        DM -> DB: SELECT * FROM department\nWHERE id = ? AND is_deleted = 0
        DB --> DM: department
        DM --> PS: department
        alt 部门不存在/已删除
            PS --> PC: 提示"所属部门不存在"
            PC --> HR: 返回错误
        end
    end

    PS -> PM: INSERT INTO position
    PM -> DB: INSERT
    DB --> PM: OK

    PS --> PC: 创建成功
    PC --> HR: 返回职位ID
end
@enduml
```

## 数据库设计

> 组织架构管理模块涉及的核心数据表。

### 部门表

#### department

```sql
CREATE TABLE IF NOT EXISTS `department` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                    VARCHAR(64)       NOT NULL COMMENT '部门名称，如"技术部"',
    `code`                    VARCHAR(16)       NOT NULL COMMENT '部门编码（纯数字），如"01"、"0101"，用于工号生成',
    `parent_id`               BIGINT UNSIGNED            COMMENT '上级部门ID，关联department.id，NULL表示根部门',
    `manager_id`              BIGINT UNSIGNED            COMMENT '部门负责人ID，关联employee.id，用于数据权限判定',
    `sort_order`              INT               NOT NULL DEFAULT 0 COMMENT '排序序号，越小越靠前',
    `description`             VARCHAR(256)               COMMENT '部门描述/职能说明',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`              TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`, `is_deleted`),
    UNIQUE KEY `uk_code` (`code`, `is_deleted`),
    UNIQUE KEY `uk_parent_sort` (`parent_id`, `sort_order`, `is_deleted`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_manager_id` (`manager_id`),
    KEY `idx_is_deleted` (`is_deleted`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '部门表，维护公司多级组织架构';
```
> **说明**：`parent_id` 采用自引用方式实现多级树形结构，NULL 表示一级根部门。`uk_name` 和 `uk_code` 唯一索引与 `is_deleted` 组合，确保未删除记录中名称和编码唯一。`uk_parent_sort` 唯一索引确保同级部门中 `sortOrder` 不重复，同时支持已删除记录的"同名重建"场景。注意：MySQL 中 `parent_id` 为 NULL 时唯一约束不生效（NULL ≠ NULL），根部门的排序唯一性由后端校验兜底。

### 职位表

#### position

```sql
CREATE TABLE IF NOT EXISTS `position` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                    VARCHAR(64)       NOT NULL COMMENT '职位名称，如"Java开发工程师"',
    `sequence`                TINYINT           NOT NULL COMMENT '职位序列：1=管理序列M 2=专业序列P 3=支持序列S',
    `department_id`           BIGINT UNSIGNED            COMMENT '所属部门ID，关联department.id，NULL表示全公司通用',
    `level_min`               INT               NOT NULL COMMENT '职级下限（数字部分），如P序列P1填1',
    `level_max`               INT               NOT NULL COMMENT '职级上限（数字部分），如P序列P10填10',
    `default_probation_months` INT              NOT NULL DEFAULT 3 COMMENT '默认试用期（月），入职时自动填充',
    `description`             VARCHAR(256)               COMMENT '职位描述/岗位职责说明',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`              TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name_dept` (`name`, `department_id`, `is_deleted`),
    KEY `idx_sequence` (`sequence`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_is_deleted` (`is_deleted`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '职位表，定义职位及所属序列职级';
```
> **说明**：`department_id` 为 NULL 时表示该职位为全公司通用职位（不限定特定部门）。`level_min` / `level_max` 仅存储序列中数字部分（如 P1-P10，存储 min=1, max=10），序列前缀由 `sequence` 字段决定。`uk_name_dept` 唯一索引确保同一部门下职位名称不重复（含 NULL 部门）。

### 职位序列枚举

#### position\_sequence（代码枚举，非数据库表）

职位序列在代码层面以枚举形式定义，不单独建表：

| **枚举值** | **序列标识** | **职级范围** | **说明** |
| --- | --- | --- | --- |
| `MANAGEMENT` (1) | M | M1-M5 | 管理序列：M1主管、M2经理、M3总监、M5VP |
| `PROFESSIONAL` (2) | P | P1-P10 | 专业序列：P3初级、P5中级、P7高级、P9专家 |
| `SUPPORT` (3) | S | S1-S5 | 支持序列：S1-S5 职能等级 |

> **说明**：若未来需要新增序列，只需在枚举中添加新值并定义职级范围，无需变更数据库结构。

## API 设计

> 组织架构管理模块核心接口。

### 部门管理

#### 查询部门树（含人数统计）

```plain
GET /api/v1/departments/tree
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| — | — | — | 无请求参数，返回完整部门树 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "总公司",
      "code": "00",
      "parentId": null,
      "managerId": 1000,
      "managerName": "王总",
      "sortOrder": 0,
      "employeeCount": 156,
      "description": "公司总部",
      "children": [
        {
          "id": 10,
          "name": "技术部",
          "code": "01",
          "parentId": 1,
          "managerId": 1001,
          "managerName": "张三",
          "sortOrder": 1,
          "employeeCount": 45,
          "description": "技术研发部门",
          "children": [
            {
              "id": 101,
              "name": "前端组",
              "code": "0101",
              "parentId": 10,
              "managerId": null,
              "managerName": null,
              "sortOrder": 1,
              "employeeCount": 15,
              "description": null,
              "children": []
            },
            {
              "id": 102,
              "name": "后端组",
              "code": "0102",
              "parentId": 10,
              "managerId": null,
              "managerName": null,
              "sortOrder": 2,
              "employeeCount": 20,
              "description": null,
              "children": []
            }
          ]
        },
        {
          "id": 20,
          "name": "产品部",
          "code": "02",
          "parentId": 1,
          "managerId": 1002,
          "managerName": "李四",
          "sortOrder": 2,
          "employeeCount": 12,
          "description": "产品设计与规划",
          "children": []
        }
      ]
    }
  ]
}
```
> **说明**：`employeeCount` 为该节点及其所有下属部门在职员工数之和（统计口径：在职状态为"试用期"或"正式"）。`children` 为空数组 `[]` 表示叶子节点。数据范围：HR 专员和系统管理员返回全量；部门主管仅返回本人管辖的部门子树（从所属部门向下递归）；普通员工返回本人所在部门信息。`sortOrder` 控制同级部门排序，越小越靠前。

---

#### 查询部门列表（平铺）

```plain
GET /api/v1/departments
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| keyword | String | 否 | 部门名称模糊搜索 |
| parentId | Long | 否 | 按上级部门筛选（与 keyword 可组合） |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 8,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 10,
        "name": "技术部",
        "code": "01",
        "parentId": 1,
        "parentName": "总公司",
        "managerId": 1001,
        "managerName": "张三",
        "sortOrder": 1,
        "level": 1,
        "employeeCount": 45,
        "childCount": 2,
        "description": "技术研发部门",
        "createTime": "2026-01-15T09:00:00"
      }
    ]
  }
}
```
> **说明**：平铺列表主要用于后台管理页的搜索和批量操作。`level` 字段表示在当前树中的层级深度（1 为根部门下的第一级）。`employeeCount` 为含子部门的汇总人数，`childCount` 为该部门直接子部门数。数据范围与部门树接口一致。

---

#### 查询部门详情

```plain
GET /api/v1/departments/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 部门ID（路径参数） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10,
    "name": "技术部",
    "code": "01",
    "parentId": 1,
    "parentName": "总公司",
    "managerId": 1001,
    "managerName": "张三",
    "sortOrder": 1,
    "level": 1,
    "employeeCount": 45,
    "childCount": 2,
    "children": [
      {"id": 101, "name": "前端组", "code": "0101", "employeeCount": 15},
      {"id": 102, "name": "后端组", "code": "0102", "employeeCount": 20}
    ],
    "description": "技术研发部门",
    "createTime": "2026-01-15T09:00:00",
    "updateTime": "2026-06-20T14:30:00"
  }
}
```
> **说明**：`children` 仅返回直接子部门的简要信息（id/name/code/employeeCount），不递归展开孙级。`parentName` 为上级部门名称，根部门时为 null。`level` 为动态计算的层级深度。

---

#### 创建部门

```plain
POST /api/v1/departments
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| name | String | 是 | 部门名称，如"技术部" |
| code | String | 是 | 部门编码（纯数字），如"01"（用于工号生成） |
| parentId | Long | 否 | 上级部门ID，不传或 null 表示根部门 |
| managerId | Long | 否 | 部门负责人ID（需为在职员工） |
| sortOrder | Integer | 是 | 排序序号，越小越靠前 |
| description | String | 否 | 部门描述/职能说明 |

##### 请求示例

```json
{
  "name": "技术部",
  "code": "01",
  "parentId": 1,
  "managerId": 1001,
  "sortOrder": 1,
  "description": "技术研发部门"
}
```

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10,
    "name": "技术部"
  }
}
```
> **说明**：仅 HR 专员和系统管理员可操作。部门名称在未删除部门中必须唯一；部门编码在未删除部门中必须唯一；`sortOrder` 在同级部门（相同 `parentId`）中必须唯一。创建时强制校验层级深度：以指定 `parentId` 为起点向上递归，若已有深度 + 1 > 5，则拒绝创建并提示"超过最大层级深度5级"。`parentId` 不能指向自己或已删除的部门。

---

#### 更新部门

```plain
PUT /api/v1/departments/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 部门ID（路径参数） |
| name | String | 否 | 部门名称 |
| code | String | 否 | 部门编码 |
| parentId | Long | 否 | 上级部门ID（传 null 表示设为根部门） |
| managerId | Long | 否 | 部门负责人ID（传 null 表示清空） |
| sortOrder | Integer | 否 | 排序序号 |
| description | String | 否 | 部门描述（传 null 表示清空） |

##### 请求示例

```json
{
  "name": "技术研发部",
  "description": "更名为技术研发部"
}
```

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10,
    "updatedFields": ["name", "description"]
  }
}
```
> **说明**：仅 HR 专员和系统管理员可操作。修改 `parentId` 时同样需要校验层级深度，且不能将当前部门设为自身的子孙部门（避免循环引用）。修改 `sortOrder` 时需要校验同级部门（相同 `parentId`）中的唯一性。`code` 修改后会影响后续工号生成，历史工号不受影响。

---

#### 删除部门

```plain
DELETE /api/v1/departments/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 部门ID（路径参数） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

##### 错误响应示例

```json
{
  "code": 30001,
  "message": "该部门下存在子部门，请先删除子部门",
  "data": {
    "childDepartments": [
      {"id": 101, "name": "前端组"},
      {"id": 102, "name": "后端组"}
    ]
  }
}
```
```json
{
  "code": 30002,
  "message": "该部门下有在职员工，请先转移员工",
  "data": {
    "employeeCount": 45
  }
}
```
> **说明**：仅 HR 专员和系统管理员可操作。逻辑删除（`is_deleted = 1`）。删除前校验两次：① 是否有未删除的子部门；② 是否有在职员工（试用期/正式）绑定该部门。校验不通过时返回对应的错误码和明细信息。

---

### 职位管理

#### 查询职位列表

```plain
GET /api/v1/positions
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| keyword | String | 否 | 职位名称模糊搜索 |
| sequence | Integer | 否 | 职位序列筛选：1=M 2=P 3=S |
| departmentId | Long | 否 | 所属部门筛选（含 NULL 通用职位） |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 25,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 100,
        "name": "Java开发工程师",
        "sequence": 2,
        "sequenceDesc": "专业序列P",
        "departmentId": 10,
        "departmentName": "技术部",
        "levelRange": "P1-P10",
        "levelMin": 1,
        "levelMax": 10,
        "defaultProbationMonths": 3,
        "description": "负责后端服务开发",
        "createTime": "2026-01-15T09:00:00"
      },
      {
        "id": 200,
        "name": "行政专员",
        "sequence": 3,
        "sequenceDesc": "支持序列S",
        "departmentId": null,
        "departmentName": "全公司通用",
        "levelRange": "S1-S5",
        "levelMin": 1,
        "levelMax": 5,
        "defaultProbationMonths": 2,
        "description": null,
        "createTime": "2026-03-01T10:00:00"
      }
    ]
  }
}
```
> **说明**：数据范围：HR 专员和系统管理员返回全量；部门主管和普通员工仅可查看（用于入职/调岗时下拉选择）。`departmentId` 为 null 时 `departmentName` 显示为"全公司通用"。支持组合筛选：按序列 + 部门 + 关键字同时筛选。

---

#### 查询职位详情

```plain
GET /api/v1/positions/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 职位ID（路径参数） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 100,
    "name": "Java开发工程师",
    "sequence": 2,
    "sequenceDesc": "专业序列P",
    "departmentId": 10,
    "departmentName": "技术部",
    "levelRange": "P1-P10",
    "levelMin": 1,
    "levelMax": 10,
    "defaultProbationMonths": 3,
    "description": "负责后端服务开发与维护",
    "createTime": "2026-01-15T09:00:00",
    "updateTime": "2026-06-20T14:30:00"
  }
}
```
---

#### 查询职位序列枚举

```plain
GET /api/v1/positions/sequences
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| — | — | — | 无参数 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "sequence": 1,
      "sequenceName": "M",
      "sequenceDesc": "管理序列",
      "levelPrefix": "M",
      "levelMin": 1,
      "levelMax": 5,
      "typicalPositions": ["M1主管", "M2经理", "M3总监", "M5VP"]
    },
    {
      "sequence": 2,
      "sequenceName": "P",
      "sequenceDesc": "专业序列",
      "levelPrefix": "P",
      "levelMin": 1,
      "levelMax": 10,
      "typicalPositions": ["P3初级", "P5中级", "P7高级", "P9专家"]
    },
    {
      "sequence": 3,
      "sequenceName": "S",
      "sequenceDesc": "支持序列",
      "levelPrefix": "S",
      "levelMin": 1,
      "levelMax": 5,
      "typicalPositions": ["S1-S5职能等级"]
    }
  ]
}
```
> **说明**：供前端下拉框使用，包含序列名称、职级上下限、典型职位示例。该接口数据来源于后端代码枚举定义。

---

#### 创建职位

```plain
POST /api/v1/positions
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| name | String | 是 | 职位名称，如"Java开发工程师" |
| sequence | Integer | 是 | 职位序列：1=M 2=P 3=S |
| departmentId | Long | 否 | 所属部门ID，不传表示全公司通用 |
| levelMin | Integer | 是 | 职级下限（数字），如P序列P1填1 |
| levelMax | Integer | 是 | 职级上限（数字），如P序列P10填10 |
| defaultProbationMonths | Integer | 是 | 默认试用期（月） |
| description | String | 否 | 职位描述/岗位职责说明 |

##### 请求示例

```json
{
  "name": "Java开发工程师",
  "sequence": 2,
  "departmentId": 10,
  "levelMin": 1,
  "levelMax": 10,
  "defaultProbationMonths": 3,
  "description": "负责后端服务开发与维护"
}
```

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 100,
    "name": "Java开发工程师",
    "levelRange": "P1-P10"
  }
}
```
> **说明**：仅 HR 专员和系统管理员可操作。`levelMin` 和 `levelMax` 的合法范围由 `sequence` 决定，不在对应序列范围内时拒绝创建：M 序列 \[1, 5\]，P 序列 \[1, 10\]，S 序列 \[1, 5\]。`levelMin` 必须 ≤ `levelMax`。同一部门下职位名称不可重复（`department_id = NULL` 视为同一分组）。创建成功后 `levelRange` 由 `sequence` 前缀 + `levelMin` + `-` + `levelMax` 自动拼接。

---

#### 更新职位

```plain
PUT /api/v1/positions/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 职位ID（路径参数） |
| name | String | 否 | 职位名称 |
| sequence | Integer | 否 | 职位序列 |
| departmentId | Long | 否 | 所属部门ID（传 null 表示清空，变为全公司通用） |
| levelMin | Integer | 否 | 职级下限 |
| levelMax | Integer | 否 | 职级上限 |
| defaultProbationMonths | Integer | 否 | 默认试用期（月） |
| description | String | 否 | 职位描述（传 null 表示清空） |

##### 请求示例

```json
{
  "defaultProbationMonths": 6,
  "description": "负责后端服务开发与维护，要求Java 3年以上经验"
}
```

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 100,
    "levelRange": "P1-P10",
    "updatedFields": ["defaultProbationMonths", "description"]
  }
}
```
> **说明**：仅 HR 专员和系统管理员可操作。修改 `sequence` 时同步校验新的 `levelMin`/`levelMax` 范围。

---

#### 删除职位

```plain
DELETE /api/v1/positions/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 职位ID（路径参数） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

##### 错误响应示例

```json
{
  "code": 30011,
  "message": "该职位下有在职员工关联，请先调整员工职位",
  "data": {
    "employeeCount": 12
  }
}
```
> **说明**：仅 HR 专员和系统管理员可操作。逻辑删除（`is_deleted = 1`）。删除前校验是否有在职员工（试用期/正式）的 `employee_work_info.position_id` 关联该职位，若有关联则拒绝删除。

## 关键技术设计

### 部门树构建方案

部门树采用"全量查询 + 内存组装"方式，避免 N+1 查询问题：

1.  **全量查询**：一次查询所有未删除部门（`is_deleted = 0`），按 `sort_order` 排序
    
2.  **内存分组**：将平铺列表按 `parent_id` 分组为 `Map<Long, List<Department>>`
    
3.  **递归组装**：从 `parent_id = null` 的根部门开始，递归填充 `children`
    
4.  **人数统计**：递归过程中同时计算每个节点的 `employeeCount`（含下属部门汇总）
    

```java
// 核心构建逻辑
public List<DepartmentTreeNode> buildTree() {
    List<Department> allDepts = departmentMapper.selectAllUndeleted();
    Map<Long, List<Department>> childrenMap = allDepts.stream()
        .collect(Collectors.groupingBy(d -> 
            d.getParentId() == null ? 0L : d.getParentId()));

    // 统计每个部门的直接员工数（批量查询）
    Map<Long, Integer> directEmployeeCounts = employeeMapper
        .countActiveEmployeesGroupByDept();

    // 从根部门开始递归组装
    return childrenMap.getOrDefault(0L, Collections.emptyList()).stream()
        .map(dept -> buildNode(dept, childrenMap, directEmployeeCounts))
        .collect(Collectors.toList());
}

private DepartmentTreeNode buildNode(Department dept,
        Map<Long, List<Department>> childrenMap,
        Map<Long, Integer> employeeCounts) {
    DepartmentTreeNode node = convertToNode(dept);
    List<DepartmentTreeNode> children = childrenMap
        .getOrDefault(dept.getId(), Collections.emptyList()).stream()
        .map(child -> buildNode(child, childrenMap, employeeCounts))
        .collect(Collectors.toList());
    node.setChildren(children);

    // 人数 = 本部门直接员工 + 所有子部门递归人数
    int directCount = employeeCounts.getOrDefault(dept.getId(), 0);
    int childrenCount = children.stream()
        .mapToInt(DepartmentTreeNode::getEmployeeCount).sum();
    node.setEmployeeCount(directCount + childrenCount);
    return node;
}
```

### 部门层级深度校验方案

创建/更新部门时校验层级深度不超过 5 级：

1.  **向上递归**：从指定的 `parent_id` 出发，递归查询 `parent_id` 逐层向上，直到 `parent_id = null`，统计当前已有深度
    
2.  **深度判断**：若 `已有深度 + 1 > 5`，拒绝操作并提示具体层级信息
    
3.  **循环引用检测**：更新部门 `parent_id` 时，确保新父节点不是当前节点的子/孙节点（向下递归检查），防止形成回路
    

```java
// 计算指定父部门下的层级深度
public int calculateLevel(Long parentId) {
    if (parentId == null) return 1;
    int level = 1;
    Long currentId = parentId;
    while (currentId != null) {
        Department parent = departmentMapper.selectById(currentId);
        if (parent == null || parent.getIsDeleted()) {
            throw new BusinessException("上级部门不存在或已删除");
        }
        level++;
        if (level > 5) break;
        currentId = parent.getParentId();
    }
    return level;
}
```

### 部门删除前置校验方案

部门删除（逻辑删除）前执行两道防线：

1.  **子部门检查**：```sqlSELECT id, name FROM departmentWHERE parent_id = ? AND is_deleted = 0```存在记录时返回子部门列表，提示"请先删除子部门"。
    
2.  **在职员工检查**：```sqlSELECT COUNT(\*) FROM employee_work_info ewiJOIN employee e ON ewi.employee\_id = e.idWHERE ewi.department_id = ? AND e.status IN (1, 2) AND e.is_deleted = 0```员工数 > 0 时返回在职员工数量，提示"请先转移员工"。
    

### 部门编码规则

部门编码用于工号生成（工号格式：年份(4位) + 部门编码(2位) + 序号(3位)，如 `202401005`）：

1.  **编码规则**：由 HR 创建部门时手动指定，建议遵循层级到编码的对应关系（如一级部门 `01`，其下二级部门 `0101`），但系统不做强制约束
    
2.  **唯一性校验**：未删除部门中 `code` 必须唯一
    
3.  **修改影响**：部门编码变更后，使用该编码的新工号将采用新编码生成，历史工号不受影响
    

### 部门合并规则（简要说明，PRD中未说明,将来功能扩展）

> 本小节仅记录部门合并的业务规则约束，供后续版本参考。当前版本不包含部门合并的前端界面和接口。

部门合并时的业务规则：

*   被合并部门的在职员工需先转移至目标部门（HR 手动操作）
    
*   被合并部门的所有子部门随同迁移至目标部门下（保持层级关系）
    
*   合并后需重新计算目标部门的人数统计
    
*   被合并部门做逻辑删除处理
    
*   工号历史记录中的原部门编码保持不变
    

### 职级范围校验方案

职位创建/更新时校验职级范围与序列的匹配：

| **序列** | **合法 levelMin** | **合法 levelMax** | **规则** |
| --- | --- | --- | --- |
| M（管理序列） | 1 - 5 | 1 - 5 | `levelMin` ≤ `levelMax`；`levelMin` ≥ 1；`levelMax` ≤ 5 |
| P（专业序列） | 1 - 10 | 1 - 10 | `levelMin` ≤ `levelMax`；`levelMin` ≥ 1；`levelMax` ≤ 10 |
| S（支持序列） | 1 - 5 | 1 - 5 | `levelMin` ≤ `levelMax`；`levelMin` ≥ 1；`levelMax` ≤ 5 |

```java
// 职位序列职级范围校验
public void validateLevelRange(PositionSequence sequence, int levelMin, int levelMax) {
    if (levelMin > levelMax) {
        throw new BusinessException("职级下限不能大于上限");
    }
    int maxLevel = switch (sequence) {
        case MANAGEMENT    -> 5;
        case PROFESSIONAL -> 10;
        case SUPPORT       -> 5;
    };
    if (levelMin < 1 || levelMax > maxLevel) {
        throw new BusinessException(
            String.format("%s序列的职级范围必须在1-%d之间", 
                sequence.getLabel(), maxLevel));
    }
}
```

### 数据权限过滤方案

组织架构数据遵循"按角色过滤"原则：

| **角色** | **部门管理** | **职位管理** | **部门树** |
| --- | --- | --- | --- |
| 系统管理员 | 全量 | 全量 | 全量 |
| HR 专员 | 全量 | 全量 | 全量 |
| 部门主管 | — | — | 仅本部门 |
| 普通员工 | — | — | 自己所在部门 |
| 财务专员 | — | — | — |

> **说明**：部门主管查看部门树时，仅返回其管辖的部门为根的子树。普通员工获取部门树时，仅返回本人所在部门节点（方便查看组织归属）。职位管理的权限为"查看"级别，供入职/调岗时下拉选择使用，新增/编辑/删除仅限 HR 和系统管理员。

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审产品规格，确认部门层级规则、职位序列枚举、人数统计算法 | 0.5 天 |
| 技术方案 | 完成系分文档评审，确认数据库设计与接口方案 | 1 天 |
| 数据库开发 | 建表（2 张表）、索引优化、初始数据脚本（公司组织架构/标准职位） | 0.5 天 |
| 后端开发-部门管理 | 部门 CRUD、部门树构建、层级校验、人数统计、删除前置校验 | 2 天 |
| 后端开发-职位管理 | 职位 CRUD、序列枚举查询、职级范围校验、部门关联 | 1.5 天 |
| 前端开发 | 部门树页面、部门编辑页、职位列表页、职位编辑页 | 3 天 |
| 联调测试 | 前后端联调、权限场景测试、层级校验边界测试、人数统计精度验证 | 1.5 天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 1 天 |

> **总预估工期**：约 **11 个工作日**

> **依赖说明**：本模块为组织架构基础数据模块，被以下下游模块依赖——员工档案管理（`employee_work_info` 关联 `department_id` 和 `position_id`）、考勤管理（考勤组按部门匹配）、薪资管理（账套按部门/职位分配）、审批流转（通过部门负责人确定审批人）。建议优先开发本模块，为下游模块提供基础数据支持。