# HRMS-后端-权限体系

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-13 | 1.0 | 初稿 | 陆博 |
| 2026-07-15 | 2.0 | 删除 userRole 字段，统一为 roleId 关联 RBAC，三层防线全部启用 | 陆博 |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 2 部分——权限体系。当前系统已通过 `roleId` 字段关联 `role` 表实现基于 RBAC 的精细权限控制：

- **精细权限控制**：不同角色对"员工档案、薪资管理、考勤管理、审批管理、组织架构、系统管理"等模块有不同的可见/操作权限
- **数据范围控制**：角色可看的数据范围为"全平台 / 全部员工 / 本部门及下属 / 薪资相关 / 仅本人"
- **字段级权限**：同一个页面（如员工档案），不同角色看到的敏感字段不同（身份证号、薪资信息脱敏或隐藏）

本模块基于 RBAC（Role-Based Access Control）模型，引入 `role` 表（角色-权限码）和 `user.roleId` 字段，实现三层权限控制：**功能权限 → 数据范围 → 字段权限**。

### 相关资料

- [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | 陆博 |

## 功能模块

> 描述权限体系涉及的功能与场景。

本模块核心功能包括：

1. **角色管理**：支持角色 CRUD（增删改查）、角色分页查询、角色启用/禁用、角色权限码配置（JSON 数组）、角色字段权限配置（JSON 对象）、数据范围设置
2. **用户-角色关联**：为系统用户分配角色，更新 `user.roleId` 关联 `role` 表
3. **功能权限校验**：三层防线——PermissionInterceptor（URL 匹配权限码）+ AuthInterceptor（roleId + 角色状态校验）+ PermissionAspect（@RequirePermission 注解 AOP）
4. **数据范围查询**：根据用户角色返回数据范围等级（1~5），业务层据此过滤 SQL 查询范围
5. **字段级权限**：根据角色返回 `fieldPermissions` JSON，前端按角色 ID 控制敏感字段可见性
6. **权限查询接口**：登录后查当前用户完整权限（角色信息 + 权限码列表 + 数据范围 + 字段权限）、检查是否拥有某权限码、获取系统全部可用权限码列表

### 功能模块树

```plain
权限体系
├── 角色管理
│   ├── 角色列表（全量 / 已启用 / 分页）
│   ├── 角色新增（唯一编码校验）
│   ├── 角色编辑
│   ├── 角色删除（软删除）
│   ├── 角色启用/禁用（status 字段）
│   ├── 角色权限配置（permissions JSON 数组）
│   └── 角色字段权限配置（fieldPermissions JSON 对象）
├── 用户-角色关联
│   └── 为用户分配角色（user.roleId）
├── 功能权限校验（三层防线）
│   ├── PermissionInterceptor（URL → 权限码映射）
│   ├── AuthInterceptor（@AuthCheck 角色字符串校验）
│   └── PermissionAspect（@RequirePermission 注解 AOP）
├── 数据范围控制
│   ├── 全平台（dataScope=1）
│   ├── 全部员工（dataScope=2）
│   ├── 本部门及下属（dataScope=3）
│   ├── 薪资相关（dataScope=4）
│   └── 仅本人（dataScope=5）
├── 字段级权限
│   └── fieldPermissions JSON：{"字段名":{"viewable":[角色ID],"editable":[角色ID]}}
└── 权限查询接口
    ├── 当前用户完整权限
    ├── 检查指定权限码
    ├── 当前用户数据范围
    └── 系统全部可用权限码列表
```

## 流程图

> 对权限体系涉及的核心流程进行梳理。

### 2-1 用户登录后获取权限流程

```plain
用户登录成功
     │
     ▼
前端调用 GET /api/permission/current
     │
     ▼
PermissionController.getCurrentPermissions()
     │
     ▼
PermissionService.getUserPermissions(userId)
     │
     ├── 查 User 表 → 获取 roleId
     ├── 查 Role 表 → 获取 roleName / roleCode / status
     ├── 角色已禁用？→ dataScope=仅本人, permissionCodes=空
     ├── 角色已启用？→ 解析 permissions JSON → permissionCodes 列表
     └── 返回 fieldPermissions JSON（字段权限）
     │
     ▼
前端收到 UserPermissionVO
     │
     ├── 根据 permissionCodes 控制菜单/按钮可见性
     ├── 根据 dataScope 控制列表数据范围
     └── 根据 fieldPermissions 控制详情页字段可见性
```

### 2-2 请求权限校验流程（三层防线）

```plain
HTTP 请求到达
     │
     ▼
┌── 第1层: PermissionInterceptor ──────────────────┐
│  URL 是否在映射表中？                             │
│  ├── 是 → 查用户权限码 → 有？→ 放行              │
│  │                   → 无？→ 403 JSON            │
│  └── 否 → 放行                                   │
└──────────────────────────────────────────────────┘
     │
     ▼
┌── 第2层: AuthInterceptor ────────────────────────┐
│  方法上有 @AuthCheck(mustRole="admin")？         │
│  ├── 是 → user.roleId != null？                 │
│  │       ├── 是 → role.status==1？              │
│  │       │       ├── 是 → hasPermission(*:*:*)   │
│  │       │       │       或 hasPermission(role:manage) │
│  │       │       │       ├── 是 → 放行           │
│  │       │       │       └── 否 → 403            │
│  │       │       └── 否 → 403（角色已禁用）       │
│  │       └── 否 → 403（未分配角色）               │
│  └── 否 → 放行                                   │
└──────────────────────────────────────────────────┘
     │
     ▼
┌── 第3层: PermissionAspect ───────────────────────┐
│  方法上有 @RequirePermission("employee:list")？   │
│  ├── 是 → PermissionService.hasPermission()      │
│  │       ├── 查 User.roleId → Role              │
│  │       ├── 角色禁用？→ false                   │
│  │       ├── 解析 permissions JSON               │
│  │       ├── 含 "*:*:*" → true                  │
│  │       ├── 含 "employee:list" → true          │
│  │       └── 否 → false → 403                    │
│  └── 否 → 放行                                   │
└──────────────────────────────────────────────────┘
     │
     ▼
Controller 方法执行
```

### 2-3 角色分配流程

```plain
管理员调用 POST /api/role/assign {userId, roleId}
     │
     ▼
RoleController.assignRole()
     │
     ▼
RoleService.assignRoleToUser(userId, roleId)
     │
     ├── 校验用户是否存在
     ├── 校验角色是否存在
     ├── 更新 user.roleId = roleId
     └── userService.updateById(user)
     │
     ▼
用户下次请求时通过 roleId → role 表获取新权限
```

## UML 图

> 描述权限体系的核心类和依赖关系。

### 权限体系核心领域模型

```plain
@startuml
class Role {
  - id: Long
  - roleName: String             "角色名称"
  - roleCode: String             "角色编码（唯一）"
  - description: String          "角色描述"
  - dataScope: Integer           "数据范围：1=全量 2=全部员工 3=本部门 4=薪资 5=仅本人"
  - status: Integer              "状态：0=禁用 1=启用"
  - permissions: String          "权限编码列表（JSON数组）"
  - fieldPermissions: String     "字段权限（JSON对象）"
  - isDelete: Integer            "软删除"
  --
  + getParsedPermissions(): List<String>
  + getParsedFieldPermissions(): Map<String, Object>
}

class User {
  - id: Long
  - userAccount: String
  - roleId: Long                 "角色ID，关联role.id"
  - employeeId: Long             "员工ID，关联employee.id"
  --
  + isAdmin(): boolean           "判断是否拥有 *:*:* 或 role:manage"
}

class UserPermissionVO {
  - userId: Long
  - roleId: Long
  - roleName: String
  - roleCode: String
  - dataScope: Integer
  - dataScopeDesc: String
  - permissionCodes: List<String>
  - permissions: String          "原始JSON"
  - fieldPermissions: String     "原始JSON"
}

class RoleVO {
  - id: Long
  - roleName: String
  - roleCode: String
  - dataScope: Integer
  - dataScopeDesc: String
  - permissionCodes: List<String>
  - permissions: String
  - fieldPermissions: String
  - status: Integer
}

enum DataScopeEnum {
  ALL           "全量"
  ALL_EMPLOYEE  "全部员工"
  DEPARTMENT    "本部门及下属"
  SALARY        "薪资相关"
  SELF          "仅本人"
}

interface PermissionService {
  + getUserPermissions(Long userId): UserPermissionVO
  + hasPermission(Long userId, String code): boolean
  + getUserDataScope(Long userId): Integer
  + getUserPermissionCodes(Long userId): List<String>
  + getFieldPermissions(Long userId): String
  + getAllPermissionCodes(): List<String>
}

interface RoleService {
  + getAllRoles(): List<Role>
  + getRoleById(Long id): Role
  + addRole(Role): boolean
  + updateRole(Role): boolean
  + deleteRole(Long id): boolean
  + assignRoleToUser(Long userId, Long roleId): boolean
}

class PermissionInterceptor {
  - URL_PERMISSION_MAP: LinkedHashMap
  + preHandle(): boolean
  - matchPermission(String uri): String
}

class PermissionAspect {
  + checkPermission(ProceedingJoinPoint): Object
}

User "1" --> "0..1" Role : roleId
UserPermissionVO ..> Role : 聚合
RoleVO ..> Role : 转换
PermissionService ..> User : 查询
PermissionService ..> Role : 查询
PermissionInterceptor ..> PermissionService : 依赖
PermissionAspect ..> PermissionService : 依赖
@enduml
```

## 时序图

> 描述权限体系核心交互的调用时序。

### 2-1 登录后获取权限时序

```
前端                    PermissionController    PermissionService       UserService      RoleService
 │                              │                      │                    │                │
 │ GET /permission/current      │                      │                    │                │
 │─────────────────────────────>│                      │                    │                │
 │                              │ getLoginUser()       │                    │                │
 │                              │─────────────────────────────────────────>│                │
 │                              │               User (含roleId)             │                │
 │                              │<─────────────────────────────────────────│                │
 │                              │ getUserPermissions() │                    │                │
 │                              │─────────────────────>│                    │                │
 │                              │                      │ getById(userId)    │                │
 │                              │                      │───────────────────>│                │
 │                              │                      │    User             │                │
 │                              │                      │<───────────────────│                │
 │                              │                      │ getRoleById()      │                │
 │                              │                      │───────────────────────────────────>│
 │                              │                      │    Role                              │
 │                              │                      │<───────────────────────────────────│
 │                              │                      │                                    │
 │                              │                      │ 组装 UserPermissionVO:              │
 │                              │                      │ - roleId, roleName, roleCode        │
 │                              │                      │ - dataScope, dataScopeDesc          │
 │                              │                      │ - 解析 permissions → permissionCodes│
 │                              │                      │ - fieldPermissions                  │
 │                              │                      │                                    │
 │                              │   UserPermissionVO   │                                    │
 │                              │<─────────────────────│                                    │
 │       {code:0, data:{...}}  │                      │                                    │
 │<─────────────────────────────│                      │                                    │
```

### 2-2 请求被权限拦截器拒绝时序

```
前端                  PermissionInterceptor        PermissionService
 │                            │                         │
 │ GET /api/employee/list     │                         │
 │───────────────────────────>│                         │
 │                            │ matchPermission(uri)    │
 │                            │ → "employee:list"       │
 │                            │                         │
 │                            │ getLoginUser()          │
 │                            │ → User(id=5, roleId=5)  │
 │                            │                         │
 │                            │ hasPermission(5,        │
 │                            │   "employee:list")      │
 │                            │────────────────────────>│
 │                            │                         │ 查Role(5)=普通员工
 │                            │                         │ permissions=["employee:detail",
 │                            │                         │              "attendance:clock"]
 │                            │                         │ 不含"employee:list"
 │                            │        false            │
 │                            │<────────────────────────│
 │                            │                         │
 │  {"code":40300,            │                         │
 │   "message":"无权限访问，  │                         │
 │    需要权限：employee:list"}│                        │
 │<───────────────────────────│                         │
```

## 数据库设计

> 权限体系模块涉及的核心数据表。

### 角色表 role

```sql
CREATE TABLE IF NOT EXISTS `role` (
    `id`               BIGINT AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    `roleName`         VARCHAR(128)                       NOT NULL COMMENT '角色名称',
    `roleCode`         VARCHAR(128)                       NOT NULL COMMENT '角色编码',
    `description`      VARCHAR(512)                       NULL COMMENT '角色描述',
    `dataScope`        TINYINT  DEFAULT 5                 NOT NULL COMMENT '数据范围：1=全量 2=全部员工 3=本部门及下属 4=薪资相关 5=仅本人',
    `status`           TINYINT  DEFAULT 1                 NOT NULL COMMENT '状态：0-禁用，1-启用',
    `permissions`      JSON                               NULL COMMENT '权限列表（JSON数组，存储权限编码）',
    `fieldPermissions` JSON                               NULL COMMENT '字段权限',
    `createTime`       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime`       DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`         TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT `uk_roleCode` UNIQUE (`roleCode`)
) COMMENT '角色' COLLATE = utf8mb4_unicode_ci;
```

### 用户表 user（新增字段）

```sql
-- user 表在原有基础上新增以下字段：
ALTER TABLE `user`
    ADD COLUMN `roleId`     BIGINT NULL COMMENT '角色ID，关联role.id',
    ADD COLUMN `employeeId` BIGINT NULL COMMENT '员工ID，关联employee.id';
```

### 预置角色数据

```sql
-- 系统管理员（全平台最高权限，不含薪资管理权限，保持职责分离）
INSERT INTO role (roleName, roleCode, description, dataScope, status, permissions, fieldPermissions)
VALUES ('系统管理员', 'admin', '全平台最高权限，不含薪资查看', 1, 1,
        '["employee:list","employee:add","employee:edit","employee:delete","employee:detail",
          "attendance:list","attendance:manage","approval:process",
          "org:manage","role:manage","system:config","system:backup"]',
        NULL);

-- HR专员（员工管理、薪资核算、考勤管理、审批管理）
INSERT INTO role (roleName, roleCode, description, dataScope, status, permissions, fieldPermissions)
VALUES ('HR专员', 'hr', '员工管理、薪资核算、考勤、审批', 2, 1,
        '["employee:list","employee:add","employee:edit","employee:delete","employee:detail",
          "salary:list","salary:view","salary:audit",
          "attendance:list","attendance:manage",
          "approval:process","org:manage"]',
        '{"idCard":{"viewable":[1,2]},"salaryInfo":{"viewable":[2,4]},"bankAccount":{"viewable":[1,2]}}');

-- 部门主管（本部门员工查看、考勤管理、下属审批）
INSERT INTO role (roleName, roleCode, description, dataScope, status, permissions, fieldPermissions)
VALUES ('部门主管', 'dept_head', '本部门及下属管理', 3, 1,
        '["employee:list","employee:detail",
          "attendance:list","attendance:manage",
          "approval:process"]',
        NULL);

-- 财务专员（薪资审核、成本报表）
INSERT INTO role (roleName, roleCode, description, dataScope, status, permissions, fieldPermissions)
VALUES ('财务专员', 'finance', '薪资审核与成本分析', 4, 1,
        '["salary:list","salary:view","salary:audit"]',
        '{"salaryInfo":{"viewable":[4]},"bankAccount":{"viewable":[4]}}');

-- 普通员工（仅本人数据，无管理权限）
INSERT INTO role (roleName, roleCode, description, dataScope, status, permissions, fieldPermissions)
VALUES ('普通员工', 'employee', '仅查看本人信息', 5, 1,
        '[]',
        NULL);
```

## API 设计

### 1. 获取当前用户权限

```plain
GET /api/permission/current
```

#### 请求参数

无（自动从 Session 获取当前登录用户）

#### 响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "userId": "2",
    "userAccount": "admin",
    "userName": "系统管理员",
    "roleId": "1",
    "roleName": "系统管理员",
    "roleCode": "ADMIN",
    "dataScope": 1,
    "dataScopeDesc": "全量",
    "permissions": "[\"*:*:*\"]",
    "permissionCodes": ["*:*:*"],
    "fieldPermissions": null
  }
}
```

> **说明**：登录后前端首先调用此接口，根据返回的 `permissionCodes` 控制菜单/按钮可见性，根据 `dataScope` 控制数据查询范围。

---

### 2. 检查用户权限

```plain
GET /api/permission/check?code=employee:list
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| code | String | 是 | 权限编码，如 employee:list |

#### 响应格式

```json
{ "code": 0, "message": "ok", "data": true }
```

---

### 3. 获取用户数据范围

```plain
GET /api/permission/data-scope
```

#### 响应格式

```json
{ "code": 0, "message": "ok", "data": 1 }
```

> **说明**：返回值：1=全量 2=全部员工 3=本部门及下属 4=薪资相关 5=仅本人。业务层据此过滤 SQL 查询范围。

---

### 4. 获取系统全部权限码

```plain
GET /api/permission/codes
```

#### 响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    "employee:list", "employee:add", "employee:edit", "employee:delete", "employee:detail",
    "salary:list", "salary:view", "salary:audit",
    "approval:process",
    "attendance:clock", "attendance:list", "attendance:manage",
    "org:manage",
    "system:config", "system:backup", "role:manage"
  ]
}
```

> **说明**：供前端角色权限分配界面使用，展示全部可分配的权限码。

---

### 5. 角色列表（全部）

```plain
GET /api/role/list/all
```

> **说明**：需 `role:manage` 权限。返回全量角色列表（含已禁用）。

---

### 6. 角色列表（已启用）

```plain
GET /api/role/list/enabled
```

> **说明**：无需登录。供登录页角色选择下拉框使用。

---

### 7. 角色分页查询

```plain
POST /api/role/list/page
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| current | int | 否 | 当前页，默认1 |
| pageSize | int | 否 | 每页条数，默认10 |
| roleName | String | 否 | 角色名称（模糊搜索） |
| roleCode | String | 否 | 角色编码（精确匹配） |
| dataScope | Integer | 否 | 数据范围过滤 |
| status | Integer | 否 | 状态过滤 |
| sortField | String | 否 | 排序字段 |
| sortOrder | String | 否 | ascend/descend |

#### 响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 5,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": "1",
        "roleName": "系统管理员",
        "roleCode": "ADMIN",
        "description": "全平台最高权限",
        "dataScope": 1,
        "dataScopeDesc": "全量",
        "permissions": "[\"*:*:*\"]",
        "permissionCodes": ["*:*:*"],
        "fieldPermissions": null,
        "status": 1
      }
    ]
  }
}
```

---

### 8. 查询单个角色

```plain
GET /api/role/get?id=1
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 角色ID |

> **说明**：需 `role:manage` 权限。

---

### 9. 新增角色

```plain
POST /api/role/add
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| roleName | String | 是 | 角色名称 |
| roleCode | String | 是 | 角色编码（唯一） |
| description | String | 否 | 角色描述 |
| dataScope | Integer | 否 | 数据范围，默认5 |
| permissions | String | 否 | 权限编码 JSON 数组 |
| fieldPermissions | String | 否 | 字段权限 JSON 对象 |

> **说明**：需 `role:manage` 权限。`roleCode` 重复时返回"角色编码已存在"错误。

---

### 10. 更新角色

```plain
POST /api/role/update
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 角色ID |
| roleName | String | 否 | 角色名称 |
| roleCode | String | 否 | 角色编码 |
| description | String | 否 | 角色描述 |
| dataScope | Integer | 否 | 数据范围 |
| status | Integer | 否 | 状态：0-禁用 1-启用 |
| permissions | String | 否 | 权限编码 JSON 数组 |
| fieldPermissions | String | 否 | 字段权限 JSON 对象 |

> **说明**：需 `role:manage` 权限。禁用角色后，该角色下的用户权限立即失效（`isRoleActive()` 返回 false）。

---

### 11. 删除角色

```plain
POST /api/role/delete?id=1
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 角色ID |

> **说明**：需 `role:manage` 权限。采用 MyBatis-Plus 逻辑删除（isDelete=1）。

---

### 12. 为用户分配角色

```plain
POST /api/role/assign
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| userId | Long | 是 | 用户ID |
| roleId | Long | 是 | 角色ID |

> **说明**：需 `role:manage` 权限。分配时更新 `user.roleId` 字段，用户下次请求即刻生效（无需重新登录）。

---

### 13. 权限码常量（代码层引用）

```java
// 在 Controller 方法上使用注解控制权限
@RequirePermission(PermissionConstant.EMPLOYEE_LIST)
public BaseResponse<Page<Employee>> listEmployees() { ... }

// 所有可用权限码
PermissionConstant.EMPLOYEE_LIST    = "employee:list"     // 员工列表
PermissionConstant.EMPLOYEE_ADD     = "employee:add"      // 新增员工
PermissionConstant.EMPLOYEE_EDIT    = "employee:edit"     // 编辑员工
PermissionConstant.EMPLOYEE_DELETE  = "employee:delete"   // 删除员工
PermissionConstant.EMPLOYEE_DETAIL  = "employee:detail"   // 员工详情
PermissionConstant.SALARY_LIST      = "salary:list"       // 薪资列表
PermissionConstant.SALARY_VIEW      = "salary:view"       // 薪资查看
PermissionConstant.SALARY_AUDIT     = "salary:audit"      // 薪资审核
PermissionConstant.APPROVAL_PROCESS = "approval:process"  // 审批处理
PermissionConstant.ATTENDANCE_CLOCK = "attendance:clock"  // 打卡
PermissionConstant.ATTENDANCE_LIST  = "attendance:list"   // 考勤列表
PermissionConstant.ATTENDANCE_MANAGE= "attendance:manage" // 考勤管理
PermissionConstant.ORG_MANAGE       = "org:manage"        // 组织架构管理
PermissionConstant.SYSTEM_CONFIG    = "system:config"     // 系统配置
PermissionConstant.SYSTEM_BACKUP    = "system:backup"     // 数据备份
PermissionConstant.ROLE_MANAGE      = "role:manage"       // 角色管理
PermissionConstant.SUPER_ADMIN      = "*:*:*"             // 超级通配符
```

## 关键技术设计

### 三层权限防线

| 层级 | 实现 | 机制 | 适用范围 |
| --- | --- | --- | --- |
| 第1层 | `PermissionInterceptor` | URL → PermissionUrlEnum 映射匹配，命中则查权限码 | 全局（35+ 条 URL 规则） |
| 第2层 | `AuthInterceptor` | 检查 `@AuthCheck(mustRole)` → roleId + role.status + hasPermission | 12 个标注方法 |
| 第3层 | `PermissionAspect` | 检查 `@RequirePermission` 注解 → hasPermission(权限码) | 7 个标注方法 |

### 权限码格式规范

```
模块:操作
───── ────
employee  :  list    → 员工列表
salary    :  view    → 薪资查看
system    :  config  → 系统配置
*         :  *       → 超级通配符（管理员拥有所有权限）

模块：employee / salary / approval / attendance / org / system / role
操作：list / add / edit / delete / detail / view / audit / process / clock / manage / config / backup
```

### 权限校验统一方案

```
统一体系：user.roleId → role 表 → permissions JSON 数组
  │
  ├── PermissionInterceptor  → URL 匹配 PermissionUrlEnum → hasPermission(权限码)
  ├── AuthInterceptor        → @AuthCheck → roleId != null → role.status==1 → hasPermission
  └── PermissionAspect       → @RequirePermission → hasPermission(权限码)

权限来源：role.permissions（JSON 数组）→ PermissionService.getUserPermissionCodes() 解析
通配符：*:*:* = 超级管理员，通过所有权限检查
角色禁用：role.status = 0 → isRoleActive() = false → 所有权限失效
```

### 数据权限过滤方案

业务 Controller 在查询前获取当前用户 `dataScope`，按等级过滤：

```java
Integer scope = permissionService.getUserDataScope(currentUser.getId());
switch (scope) {
    case 1: // ALL - 全平台，不加过滤
        break;
    case 2: // ALL_EMPLOYEE - 全部员工，不加过滤
        break;
    case 3: // DEPARTMENT - 本部门及下属
        queryWrapper.in("departmentId", getSubDeptIds(currentUser.getDeptId()));
        break;
    case 4: // SALARY - 仅薪资相关
        // 仅薪资模块使用，其他模块不返回数据
        break;
    case 5: // SELF - 仅本人
        queryWrapper.eq("id", currentUser.getId());
        break;
}
```

### 字段级权限控制方案

| **字段** | HR 专员 | 部门主管 | 普通员工 | 财务专员 | 系统管理员 |
| --- | --- | --- | --- | --- | --- |
| 姓名 | 查看 | 查看 | 查看（仅自己） | - | 查看 |
| 手机号 | 查看 | 查看 | 查看（仅自己） | - | 查看 |
| 身份证号 | 查看 | 不可见 | 查看（仅自己） | - | 查看 |
| 薪资信息 | 查看 | 不可见 | 查看（仅自己） | 查看 | - |
| 紧急联系人 | 查看 | 不可见 | 查看（仅自己） | - | 查看 |
| 银行卡号 | 查看 | 不可见 | 查看（仅自己） | - | 查看 |

> 字段级权限通过 `role.fieldPermissions` JSON 配置，前端调用 `/api/permission/current` 获取后控制字段显隐。

### 角色禁用即时生效

角色 `status` 设为 0（禁用）后，`isRoleActive()` 立即返回 false，该角色下所有用户的：
- `permissionCodes` → 空列表
- `dataScope` → 仅本人（5）
- `fieldPermissions` → null

无需用户重新登录，下次 API 调用即刻生效。

### URL 权限映射表（PermissionUrlEnum）

```java
// 定义在 PermissionUrlEnum 中，AntPathMatcher 匹配，精确路径在前
// 自服务接口（/api/employee/profile、/api/salary/slips 等）不在此处拦截

// 员工管理（管理类）
EMPLOYEE_LIST  ("/api/employee/list")        → employee:list
EMPLOYEE_ADD   ("/api/employee/add")         → employee:add
EMPLOYEE_EDIT  ("/api/employee/update")      → employee:edit
EMPLOYEE_DELETE("/api/employee/delete")      → employee:delete
EMPLOYEE_DETAIL("/api/employee/detail")      → employee:detail

// 用户管理
USER_ADD       ("/api/user/add")             → role:manage
USER_UPDATE    ("/api/user/update")          → role:manage
USER_DELETE    ("/api/user/delete")          → role:manage
USER_LIST_PAGE ("/api/user/list/page/**")    → role:manage

// 薪资管理（管理端）
SALARY_BATCH_APPROVE("/api/salary-manage/batches/*/approve")  → salary:audit
SALARY_BATCH_REJECT ("/api/salary-manage/batches/*/reject")   → salary:audit
SALARY_MANAGE_ALL   ("/api/salary-manage/**")                  → salary:list
// 自服务：/api/salary/slips、/api/salary/slip/{id}、/api/salary/trend → 不拦截

// 组织架构（读写分离）
DEPT_TREE  ("/api/departments/tree")      → employee:list
DEPT_ADD   ("/api/departments/add")       → org:manage
POS_LIST   ("/api/positions/list")        → employee:list
POS_ADD    ("/api/positions/add")         → org:manage

// 审批管理
APPROVAL_PENDING ("/api/approval/pending")   → approval:process
LEAVE_APPROVE    ("/api/attendance/leave/approve") → approval:process
// 自服务：/api/attendance/leave/apply、/api/approval/delegation/** → 不拦截

// 角色管理
ROLE_ALL("/api/role/**") → role:manage
```

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审权限体系设计，确认角色定义与权限码规范 | 0.5天 |
| 技术方案 | 完成系分文档评审，确认三层防线架构与新旧兼容方案 | 1天 |
| 数据库开发 | 建 role 表、user 表新增字段、初始化5个预置角色 | 0.5天 |
| 后端开发 | 角色CRUD、权限校验核心逻辑、三层防线实现、权限查询接口 | 3天 |
| 前端开发 | 角色管理页、权限分配页、菜单/按钮权限控制、字段权限控制 | 3天 |
| 联调测试 | 前后端联调、5个角色权限场景全覆盖测试、禁用角色即时生效测试 | 2天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 1天 |

> **总预估工期**：约 11 个工作日
