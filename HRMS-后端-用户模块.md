# HRMS-后端-用户模块

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-13 | 1.0 | 初稿，根据实际代码实现编写 | - |
| 2026-07-15 | 2.0 | 重构：移除 userRole 字段，接入 role 表 RBAC；新增状态开关、操作日志、页面访问日志；整合工作台数据看板 | - |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 2 部分——用户管理。当前系统需要一个统一的用户身份管理系统，支持用户注册、登录、登出、角色权限控制，以及管理员的用户 CRUD 操作。本模块是整个系统的认证与授权基础，所有业务模块均依赖此模块提供登录态验证和角色鉴权。

从 v2.0 开始，用户表移除了 `userRole` 字段，全面接入 role 表进行 RBAC（基于角色的访问控制）。同时本模块整合了操作日志（`oper_log`）、页面访问日志（`page_view_log`）以及工作台数据看板相关功能。

### 相关资料

- [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述用户模块涉及的功能与场景。

本模块核心功能包括：

1. **用户注册**：用户自主注册账号（账号>=4位、密码>=8位、确认密码一致、账号不重复）
2. **用户登录**：账号+密码登录，MD5加盐加密验证，登录成功记录登录日志，登录失败（密码错误且账号存在）也记录日志
3. **用户登出**：清除 session 登录态
4. **获取当前用户**：从 session 获取当前登录用户信息，返回脱敏后的 LoginUserVO（含角色名称）
5. **用户管理（管理员）**：创建用户（默认密码12345678）、删除用户（逻辑删除）、更新用户、按ID查询、分页查询、状态开关（启用/禁用）
6. **个人信息修改**：当前用户可修改自己的昵称、头像、简介
7. **账号安全**：修改密码（验证旧密码、历史密码检查）、绑定/解绑手机号
8. **RBAC权限控制**：基于 role 表 + 权限码的鉴权体系，通过 `@AuthCheck` 注解和 `PermissionService` 实现
9. **操作日志**：记录所有用户操作到 `oper_log` 表，支持按模块统计使用频率
10. **页面访问日志**：记录每日页面访问量到 `page_view_log` 表，支持近7天趋势查询

### 功能模块树

```plain
用户模块
├── 认证
│   ├── 用户注册（POST /user/register）
│   ├── 用户登录（POST /user/login）
│   ├── 用户登出（POST /user/logout）
│   └── 获取当前登录用户（GET /user/get/login）
├── 用户管理（管理员）
│   ├── 创建用户（POST /user/add）
│   ├── 删除用户（POST /user/delete）
│   ├── 更新用户（POST /user/update）
│   ├── 状态开关（POST /user/update/status）
│   ├── 按ID查询（GET /user/get）
│   ├── 按ID查询脱敏（GET /user/get/vo）
│   ├── 分页查询（POST /user/list/page）
│   └── 分页查询脱敏（POST /user/list/page/vo）
├── 个人信息
│   └── 修改个人信息（POST /user/update/my）
├── 账号安全
│   ├── 修改密码（POST /account/change-password）
│   └── 绑定/解绑手机（POST /account/bind-phone）
└── 工作台（数据看板）
    ├── 核心指标（GET /dashboard/metrics）
    ├── 近7天访问趋势（GET /dashboard/visit-trend）
    ├── 功能模块使用频率（GET /dashboard/module-usage）
    └── 最近操作动态（GET /dashboard/recent-logs）
```

## 流程图

### 用户注册流程

```plain
POST /user/register
     │
     ▼
参数校验：
     ├── 非空校验（userAccount/userPassword/checkPassword）
     ├── 账号长度 >= 4
     ├── 密码长度 >= 8
     └── 两次密码一致
     │
     ▼
账号去重（synchronized 加锁）：
     ├── 账号已存在 → 抛出异常 "账号重复"
     └── 账号不存在 → 继续
     │
     ▼
密码加密：MD5(SALT + userPassword), SALT="limou"
     │
     ▼
插入 user 表 → 返回 userId
```

### 用户登录流程

```plain
POST /user/login
     │
     ▼
参数校验（非空、长度）
     │
     ▼
密码加密：MD5(SALT + userPassword)
     │
     ▼
查询 user WHERE userAccount = ? AND userPassword = ?
     ├── 未找到 → 尝试按账号查找用户（记录登录失败日志）
     │          → 如有用户则记录 isSuccess=0, failReason="密码错误"
     │          → 抛出异常 "用户不存在或密码错误"
     └── 找到 → session.setAttribute("user_login", user)
              → 记录登录成功日志（isSuccess=1）
              → 返回 LoginUserVO（脱敏，含角色名称）
```

### 角色名称查询流程（RBAC）

```plain
用户查询 → UserServiceImpl.getUserVO / getLoginUserVO
     │
     ▼
BeanUtils.copyProperties(user, vo)
     │
     ▼
user.getRoleId() != null ?
     ├── 是 → roleMapper.selectById(roleId) → 设置 roleName / userRoleName
     └── 否 → 跳过
     │
     ▼
返回 VO（含角色名称）
```

## 数据库设计

### 用户表 user

> 使用 MyBatis-Plus 雪花算法生成主键（IdType.ASSIGN_ID），字段采用 camelCase 命名。v2.0 起移除 userRole 字段，角色信息通过 roleId 关联 role 表获取。

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | BIGINT (ASSIGN_ID) | 主键，雪花ID |
| userAccount | VARCHAR | 登录账号 |
| userPassword | VARCHAR | MD5加密密码（SALT="limou"） |
| userName | VARCHAR | 用户昵称 |
| userAvatar | VARCHAR | 用户头像URL |
| userProfile | VARCHAR | 用户简介 |
| roleId | BIGINT | 角色ID，关联 role 表 |
| employeeId | BIGINT | 关联员工ID |
| createTime | DATETIME | 创建时间 |
| updateTime | DATETIME | 更新时间 |
| isDelete | TINYINT | 逻辑删除：0=启用 1=禁用（用于用户状态开关） |

### 操作日志表 oper_log

> 记录用户操作行为，用于工作台"最近操作动态"和"功能模块使用频率"。

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | BIGINT (AUTO) | 主键 |
| operatorName | VARCHAR(64) | 操作人姓名 |
| module | VARCHAR(64) | 操作模块（如"用户管理"、"角色权限"） |
| action | VARCHAR(64) | 操作类型（如"新增用户"、"修改配置"） |
| description | VARCHAR(512) | 操作描述 |
| operateTime | DATETIME | 操作时间 |

### 页面访问日志表 page_view_log

> 记录每日页面访问量，用于工作台"近7天访问趋势"折线图。

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | BIGINT (AUTO) | 主键 |
| viewDate | DATE | 访问日期（唯一键） |
| viewCount | BIGINT | 页面访问量 |

## 实体类与 DTO 设计

### User（用户实体）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long (ASSIGN_ID) | 主键 |
| userAccount | String | 账号 |
| userPassword | String | 密码（MD5加密） |
| userName | String | 昵称 |
| userAvatar | String | 头像URL |
| userProfile | String | 简介 |
| roleId | Long | 角色ID（关联 role 表） |
| employeeId | Long | 员工ID |
| createTime | Date | 创建时间 |
| updateTime | Date | 更新时间 |
| isDelete | Integer | 逻辑删除：0=启用 1=禁用 |

### LoginUserVO（登录用户视图，脱敏）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long | 用户ID |
| userName | String | 昵称 |
| userAvatar | String | 头像URL |
| userProfile | String | 简介 |
| roleId | Long | 角色ID |
| roleName | String | 角色名称（从 role 表联查） |
| employeeId | Long | 员工ID |
| createTime | Date | 创建时间 |
| updateTime | Date | 更新时间 |

> 注意：LoginUserVO 不包含 userAccount、userPassword、isDelete 等敏感字段。roleName 通过 roleId 联查 role 表获取。

### UserVO（用户视图，脱敏）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long | 用户ID |
| userName | String | 昵称 |
| userAvatar | String | 头像URL |
| userProfile | String | 简介 |
| userRoleName | String | 角色名称（从 role 表联查，通过 roleId） |
| createTime | Date | 创建时间 |

### OperLog（操作日志实体）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long (AUTO) | 主键 |
| operatorName | String | 操作人姓名 |
| module | String | 操作模块 |
| action | String | 操作类型 |
| description | String | 操作描述 |
| operateTime | Date | 操作时间 |

### PageViewLog（页面访问日志实体）

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| id | Long (AUTO) | 主键 |
| viewDate | Date | 访问日期 |
| viewCount | Long | 页面访问量 |
```

## API 设计

> 基础路径: `/user`、`/dashboard`，服务 context-path: `/api`，完整路径: `/api/user/*`、`/api/dashboard/*`

### 1. 用户注册

```plain
POST /user/register
```

**请求参数** (UserRegisterRequest):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| userAccount | String | 是 | 账号（>=4位） |
| userPassword | String | 是 | 密码（>=8位） |
| checkPassword | String | 是 | 确认密码（需与密码一致） |

**校验规则**:
1. 参数非空
2. userAccount.length() >= 4
3. userPassword.length() >= 8
4. userPassword.equals(checkPassword)
5. 账号不重复（synchronized 加锁查询）

**响应**: `BaseResponse<Long>` — 返回新用户ID

---

### 2. 用户登录

```plain
POST /user/login
```

**请求参数** (UserLoginRequest):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| userAccount | String | 是 | 账号 |
| userPassword | String | 是 | 密码 |

**响应** (LoginUserVO):

```json
{
  "code": 0,
  "data": {
    "id": 2075829151662010370,
    "userName": "张三",
    "userAvatar": "https://...",
    "userProfile": "前端工程师",
    "roleId": 3,
    "roleName": "部门主管",
    "employeeId": 15,
    "createTime": "2026-01-01 00:00:00",
    "updateTime": "2026-07-10 12:00:00"
  }
}
```

**后端行为**:
- 登录成功: session 存储 User 对象（key="user_login"），记录登录日志
- 登录失败: 若账号存在则记录失败日志（failReason="密码错误"）

---

### 3. 用户登出

```plain
POST /user/logout
```

**说明**: 清除 session 中 "user_login" 属性

**响应**: `BaseResponse<Boolean>` — true

---

### 4. 获取当前登录用户

```plain
GET /user/get/login
```

**说明**: 从 session 读取当前用户，查询数据库获取最新信息，返回脱敏后的 LoginUserVO（含角色名称）

---

### 5. 用户管理（管理员，需 @AuthCheck(mustRole = "admin")）

#### 创建用户

```plain
POST /user/add
```

**请求参数** (UserAddRequest):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| userName | String | 否 | 昵称 |
| userAccount | String | 是 | 账号 |
| userAvatar | String | 否 | 头像URL |

默认密码: `12345678`（MD5 加密后存储）

#### 删除用户

```plain
POST /user/delete   { "id": 123 }
```

**说明**: 基于 MyBatis-Plus `@TableLogic` 逻辑删除，将 isDelete 设为 1。

#### 更新用户

```plain
POST /user/update
```

**请求参数** (UserUpdateRequest):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 用户ID |
| userName | String | 否 | 昵称 |
| userAvatar | String | 否 | 头像URL |
| userProfile | String | 否 | 简介 |
| roleId | Long | 否 | 角色ID |

#### 状态开关（启用/禁用）

```plain
POST /user/update/status?id=123&status=1
```

**参数**:

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 用户ID |
| status | Integer | 是 | 1=启用(isDelete=0), 0=禁用(isDelete=1) |

**说明**: 基于逻辑删除实现，不再修改 userRole 字段。禁用后用户自动从所有查询中过滤。

#### 按ID查询

```plain
GET /user/get?id=123          # 返回完整 User 对象（需管理员权限）
GET /user/get/vo?id=123       # 返回脱敏 UserVO（含角色名称）
```

#### 分页查询

```plain
POST /user/list/page           # 返回 Page<UserVO>（需管理员权限）
POST /user/list/page/vo        # 返回 Page<UserVO>（限制 pageSize <= 20）
```

**请求参数** (UserQueryRequest 继承 PageRequest):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| current | long | 是 | 页码 |
| pageSize | long | 是 | 每页条数（/vo 接口限制 <=20） |
| id | Long | 否 | 用户ID精确查询 |
| userName | String | 否 | 昵称模糊查询 |
| userProfile | String | 否 | 简介模糊查询 |
| roleId | Long | 否 | 角色ID精确查询（v2.0 替换原 userRole） |
| unionId | String | 否 | 微信unionId精确查询 |
| mpOpenId | String | 否 | 公众号openId精确查询 |
| sortField | String | 否 | 排序字段 |
| sortOrder | String | 否 | 排序方向：asc/desc |

---

### 6. 修改个人信息

```plain
POST /user/update/my
```

**请求参数** (UserUpdateMyRequest):

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| userName | String | 否 | 昵称 |
| userAvatar | String | 否 | 头像URL |
| userProfile | String | 否 | 简介 |

**说明**: 仅允许修改自己的信息，从 session 自动获取当前用户ID。

---

### 7. 工作台数据看板

#### 核心指标

```plain
GET /dashboard/metrics
```

**响应** (DashboardMetricsVO):

| **字段** | **类型** | **说明** |
| --- | --- | --- |
| totalUsers | Long | 总用户数（从 user 表实时查询） |
| totalUsersGrowth | BigDecimal | 本月新增环比增长率(%) |
| activeUsers | Long | 今日活跃用户数（login_log 去重） |
| activeUsersGrowth | BigDecimal | 活跃用户环比变化(%) |
| todayOrders | Long | 当前待审批数（approval_record APPROVING 计数） |
| todayOrdersGrowth | BigDecimal | 待审批环比变化(%) |
| systemHealth | Integer | 系统健康度（今日登录成功率%） |
| systemHealthChange | BigDecimal | 健康度环比变化 |

#### 近7天访问趋势

```plain
GET /dashboard/visit-trend
```

**说明**: 从 `page_view_log` 表查询近7天数据，缺失日期补0。

**响应**: `List<VisitTrendVO>` — date + viewCount

#### 功能模块使用频率

```plain
GET /dashboard/module-usage
```

**说明**: 从 `oper_log` 表按 module 字段 GROUP BY 统计，降序排列。

**响应**: `List<ModuleUsageVO>` — moduleName + usageCount

#### 最近操作动态

```plain
GET /dashboard/recent-logs
```

**说明**: 从 `oper_log` 表按 operateTime 降序取最近5条。

**响应**: `List<RecentLogVO>` — operatorName + actionType + description + operateTime

---

## 关键技术设计

### 密码加密方案

```java
public static final String SALT = "limou";

// 加密
String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
```

> 使用 MD5 + 固定 SALT。所有密码相关操作（注册、登录、修改密码）统一使用此方案。

### Session 登录态

```java
// 登录时存储
request.getSession().setAttribute("user_login", user);

// 获取当前用户
User currentUser = (User) request.getSession().getAttribute("user_login");
if (currentUser == null || currentUser.getId() == null) {
    throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
}
// 从数据库重新查询确保数据最新
currentUser = this.getById(currentUser.getId());

// 登出时清除
request.getSession().removeAttribute("user_login");
```

### RBAC 权限控制（v2.0）

v2.0 起全面迁移至基于 role 表的 RBAC 鉴权体系。用户通过 `roleId` 关联 role 表，AuthInterceptor 在执行 `@AuthCheck` 标注的方法前校验权限。

```java
// AuthInterceptor.java
@Around("@annotation(authCheck)")
public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
    User loginUser = userService.getLoginUser(request);
    
    // 检查用户是否有 roleId
    if (loginUser.getRoleId() == null) {
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未分配角色");
    }
    
    // 检查角色是否被禁用
    Role role = roleService.getRoleById(loginUser.getRoleId());
    if (role == null || role.getStatus() != 1) {
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "角色已被禁用");
    }
    
    // @AuthCheck(mustRole = "admin") → 检查管理员等效权限
    if ("admin".equals(mustRole)) {
        if (permissionService.hasPermission(loginUser.getId(), "*:*:*") ||
            permissionService.hasPermission(loginUser.getId(), "role:manage")) {
            return joinPoint.proceed();
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
    }
    
    return joinPoint.proceed();
}
```

**与 v1.0 的主要差异**:
- 移除了 `userRole` 字段（不再在 user 表存储角色字符串）
- `@AuthCheck(mustRole = "admin")` 改为通过 `PermissionService.hasPermission()` 判断
- `isAdmin()` 方法改为检查用户是否拥有 `*:*:*` 或 `role:manage` 权限码
- 管理员角色判断不再依赖硬编码的 `"admin"` 字符串

### 角色名称联查

`getUserVO()` 和 `getLoginUserVO()` 通过 `roleId` 联查 role 表获取角色名称：

```java
// 单条查询
if (user.getRoleId() != null) {
    Role role = roleMapper.selectById(user.getRoleId());
    if (role != null) {
        userVO.setUserRoleName(role.getRoleName());
    }
}

// 批量查询（避免 N+1）
Set<Long> roleIds = userList.stream()
    .map(User::getRoleId).filter(Objects::nonNull).collect(Collectors.toSet());
Map<Long, Role> roleMap = roleMapper.selectBatchIds(roleIds).stream()
    .collect(Collectors.toMap(Role::getId, Function.identity()));
```

### 用户状态开关（逻辑删除）

v2.0 起用户启用/禁用直接操作 `isDelete` 字段，不再修改 `userRole`：

```java
// POST /user/update/status
User user = new User();
user.setId(id);
user.setIsDelete(status == 1 ? 0 : 1);
userService.updateById(user);
```

> MyBatis-Plus `@TableLogic` 全局配置：`logic-delete-value: 1`，`logic-not-delete-value: 0`。被禁用（isDelete=1）的用户自动从所有查询中过滤。

### 注册账号去重

使用 `synchronized (userAccount.intern())` 对账号字符串加锁，防止并发注册同账号：

```java
synchronized (userAccount.intern()) {
    long count = this.baseMapper.selectCount(queryWrapper);
    if (count > 0) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
    }
    // ... 插入
}
```

### 登录日志记录

在 `userLogin()` 方法中直接调用 `LoginLogService.recordLoginLog()`，成功和失败均记录：

```java
// 成功
loginLogService.recordLoginLog(user.getId(), request.getRemoteAddr(),
    request.getHeader("User-Agent"), 1, true, null);

// 失败（账号存在但密码错误）
loginLogService.recordLoginLog(existUser.getId(), request.getRemoteAddr(),
    request.getHeader("User-Agent"), 1, false, "密码错误");
```

### 脱敏处理

`getLoginUserVO()` 和 `getUserVO()` 通过 `BeanUtils.copyProperties()` 从 User 拷贝到 VO，自动排除 User 中独有的敏感字段（userAccount、userPassword、isDelete）。两者差异：

* LoginUserVO：额外包含 roleId、roleName、employeeId、updateTime
* UserVO：包含 userRoleName（角色名称），仅包含 createTime

### 工作台数据看板

工作台 4 个接口从不同的数据源聚合展示：

| 接口 | 数据源 | 查询方式 |
| --- | --- | --- |
| /dashboard/metrics | user / login_log / approval_record | 实时统计 + 环比计算 |
| /dashboard/visit-trend | page_view_log | 按日期范围查询，缺失补0 |
| /dashboard/module-usage | oper_log | GROUP BY module 降序 |
| /dashboard/recent-logs | oper_log | ORDER BY operateTime DESC LIMIT 5 |

## 排期

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审用户角色权限体系 | 0.5天 |
| 技术方案 | 确认认证方案、密码策略、session管理 | 0.5天 |
| 数据库开发 | user / oper_log / page_view_log 表创建与初始化 | 0.5天 |
| 后端开发 | 注册/登录/登出、用户CRUD、权限AOP、登录日志、操作日志、数据看板 | 2天 |
| 前端开发 | 登录页、注册页、用户管理页、个人设置页、工作台 | 2天 |
| 联调测试 | 认证流程测试、权限校验测试、看板数据测试 | 1天 |

> **总预估工期**：约 6.5 个工作日
