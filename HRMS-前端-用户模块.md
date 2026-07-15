# HRMS-前端-用户模块

# 1. 需求背景

> 构建统一的用户认证与管理系统，支持用户自主注册登录、管理员进行用户管理、个人设置修改、账号安全以及工作台数据看板。

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

PRD 文档：[HRMS.md](./HRMS.md) 第2章

后端系分：[HRMS-后端-用户模块.md](./HRMS-后端-用户模块.md)

# 2. 详细设计

## 2.1 前端迭代目标

1. 登录页面：账号密码登录表单
2. 注册页面：账号密码注册表单
3. 用户管理（管理员）：用户列表 + CRUD操作 + 状态开关
4. 个人设置：修改个人信息（昵称/头像/简介）
5. 账号安全：修改密码、绑定/解绑手机
6. 工作台（数据看板）：核心指标卡片、访问趋势折线图、模块使用频率、最近操作动态

## 2.2 迭代具体描述

### 2.2.1 登录页面

##### 路由

```
/login
```

##### UI

- 居中卡片布局
- 系统Logo + 标题
- 账号输入框（placeholder: "请输入账号"）
- 密码输入框（placeholder: "请输入密码"）
- 「登录」按钮
- 底部「没有账号？立即注册」链接

##### 登录流程

```typescript
// 1. 收集表单
const { userAccount, userPassword } = form.getFieldsValue();

// 2. 提交登录
POST /api/user/login
Body: { userAccount, userPassword }

// 3. 处理响应
Response: BaseResponse<LoginUserVO>
- 成功 → 存储用户信息到全局状态（或 session cookie 自动携带），跳转首页
- 失败 → 展示错误信息（"用户不存在或密码错误"）
```

##### 表单校验

| 字段 | 规则 |
| --- | --- |
| userAccount | 必填，>=4位 |
| userPassword | 必填，>=8位 |

---

### 2.2.2 注册页面

##### 路由

```
/register
```

##### UI

- 居中卡片布局
- 账号输入框
- 密码输入框
- 确认密码输入框
- 「注册」按钮
- 底部「已有账号？立即登录」链接

##### 注册流程

```typescript
POST /api/user/register
Body: { userAccount, userPassword, checkPassword }
```

##### 表单校验

| 字段 | 规则 |
| --- | --- |
| userAccount | 必填，>=4位 |
| userPassword | 必填，>=8位 |
| checkPassword | 必填，与 userPassword 一致 |

##### 错误提示映射

| 后端返回 | 前端显示 |
| --- | --- |
| "参数为空" | "请填写完整信息" |
| "用户账号过短" | "账号长度不能少于4位" |
| "用户密码过短" | "密码长度不能少于8位" |
| "两次输入的密码不一致" | "两次输入的密码不一致" |
| "账号重复" | "该账号已被注册" |

---

### 2.2.3 用户管理（管理员功能）

##### 路由

```
/admin/users
```

##### UI

- 搜索区：账号/昵称/角色筛选 + 搜索按钮 + 「新建用户」按钮
- 数据表格：用户列表，每行含操作列
- 新建/编辑用户 Modal

##### 用户列表展示字段

| 字段名称 | 说明 | 来源字段 |
| --- | --- | --- |
| ID | 雪花ID | id |
| 账号 | - | userAccount（仅管理员可见完整 User） |
| 昵称 | - | userName |
| 头像 | 缩略图 | userAvatar |
| 简介 | - | userProfile |
| 角色 | Tag 展示角色名称 | userRoleName（从 role 表联查） |
| 状态 | Switch 开关 | isDelete 反向映射：0=启用(绿) / 1=禁用(灰) |
| 创建时间 | YYYY-MM-DD HH:mm | createTime |
| 操作 | 编辑 / 删除 | - |

> **v2.0 变更**：角色列不再使用 `userRole`（admin/user/ban 字符串），改为展示 `userRoleName`（从 role 表 roleName 联查），支持更丰富的角色名称（如"部门主管"、"财务专员"）。

##### 状态开关

- 行内 Switch 组件
- 开启 = 启用（isDelete=0），关闭 = 禁用（isDelete=1）
- 调用 `POST /api/user/update/status?id=xxx&status=1/0`

```typescript
// 状态切换处理
const onStatusChange = async (userId: number, checked: boolean) => {
  await fetch(`/api/user/update/status?id=${userId}&status=${checked ? 1 : 0}`, {
    method: 'POST'
  });
  message.success(checked ? '已启用' : '已禁用');
  refreshList();
};
```

##### 新建用户弹窗

| 字段名称 | 输入方式 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 账号 | Input | Y | 登录账号 |
| 昵称 | Input | N | 用户昵称 |
| 头像 | Input/Upload | N | 头像URL |
| 角色 | Select（下拉选择角色） | N | 从 role 接口获取角色列表，存储 roleId |

> 默认密码 12345678（后端固定），前端无需展示密码字段。

##### 编辑用户弹窗

| 字段名称 | 输入方式 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 昵称 | Input | N | |
| 头像 | Input/Upload | N | |
| 简介 | TextArea | N | |
| 角色 | Select（下拉选择角色） | N | 存储 roleId |

> 编辑用户不可修改账号和密码。

##### 删除用户

- 二次确认弹窗："确定要删除该用户吗？"
- 逻辑删除（isDelete=1），用户从列表中消失

##### 所需API

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST /api/user/add | 创建用户 | admin |
| POST /api/user/delete | 删除用户 | admin |
| POST /api/user/update | 更新用户 | admin |
| POST /api/user/update/status | 状态开关（v2.0 新增） | admin |
| GET /api/user/get?id= | 查询用户详情 | admin |
| POST /api/user/list/page | 分页查询用户（返回 UserVO） | admin |
| POST /api/user/list/page/vo | 分页查询（脱敏，限制 pageSize<=20） | 无需admin |

##### 分页请求参数

```json
{
  "current": 1,
  "pageSize": 10,
  "userName": "",
  "userProfile": "",
  "roleId": null,
  "sortField": "createTime",
  "sortOrder": "desc"
}
```

> **v2.0 变更**：筛选参数从 `userRole`（字符串）改为 `roleId`（Long），支持按角色ID精确筛选。

---

### 2.2.4 个人设置

##### 路由

```
/profile/settings
```

##### UI

- 表单布局
- 头像上传区域
- 昵称输入框
- 简介输入框
- 「保存」按钮

##### 提交

```typescript
POST /api/user/update/my
Body: { userName, userAvatar, userProfile }
```

> 注意：此接口不可修改密码（密码修改在 /api/account/changePassword）、不可修改角色。

##### 所需API

| 接口 | 说明 |
| --- | --- |
| POST /api/user/update/my | 更新个人信息 |

---

### 2.2.5 账号安全

##### 路由

```
/profile/security
```

##### UI

- **修改密码区域**：
  - 旧密码输入框
  - 新密码输入框
  - 确认新密码输入框
  - 「修改密码」按钮
- **手机绑定区域**：
  - 当前手机号展示（已绑定显示，未绑定显示"未绑定"）
  - 绑定/修改手机号 Button → 弹窗输入新手机号
  - 解绑手机号 Button → 二次确认

##### 修改密码

```typescript
POST /api/account/changePassword
Body: { oldPassword, newPassword, confirmPassword }
```

错误提示映射：

| 后端返回 | 前端显示 |
| --- | --- |
| PASSWORD_ERROR | "旧密码不正确" |
| PASSWORD_SAME_AS_OLD | "新密码不能与旧密码相同" |
| PASSWORD_RECENTLY_USED | "新密码在最近3次使用过，请更换" |

##### 绑定/解绑手机

```typescript
// 绑定手机
POST /api/account/bindPhone
Body: { phone: "13800138000" }

// 解绑手机
POST /api/account/bindPhone
Body: { phone: "" }
```

手机号校验：格式 `^1[3-9]\d{9}$`，提示"请输入正确的手机号"

##### 所需API

| 接口 | 说明 |
| --- | --- |
| POST /api/account/changePassword | 修改密码 |
| POST /api/account/bindPhone | 绑定/解绑手机 |

---

### 2.2.6 工作台（数据看板）

##### 路由

```
/dashboard
```

##### UI 布局

顶部4个指标卡片 → 左侧折线图（访问趋势） + 右侧柱状图（模块使用）
底部左侧环形图（来源分布） + 右侧柱状图（转化率） + 最近操作动态列表

##### 2.2.6.1 核心指标卡片

4 个卡片并排展示，调用 `GET /api/dashboard/metrics`：

| 卡片 | 主数值 | 环比标签 | 来源字段 |
| --- | --- | --- | --- |
| 总用户数 | totalUsers | totalUsersGrowth + "%" | 绿色↑ / 红色↓ |
| 活跃用户 | activeUsers | activeUsersGrowth + "%" | 绿色↑ / 红色↓ |
| 待审批数 | todayOrders | todayOrdersGrowth + "%" | 红色↑ / 绿色↓ |
| 系统健康度 | systemHealth + "%" | systemHealthChange | 绿色↑ / 红色↓ |

环比箭头规则：正数 → ↑ 绿色，负数 → ↓ 红色。

##### 2.2.6.2 近7天访问趋势（折线图）

调用 `GET /api/dashboard/visit-trend`，使用 ECharts/Recharts 折线图：

- X轴：日期（YYYY-MM-DD）
- Y轴：页面访问量（viewCount）
- 平滑曲线，tooltip 显示具体数值

##### 2.2.6.3 功能模块使用频率（柱状图）

调用 `GET /api/dashboard/module-usage`，使用水平柱状图：

- Y轴：模块名称（moduleName）
- X轴：使用次数（usageCount）
- 按使用次数降序排列

##### 2.2.6.4 最近操作动态（列表）

调用 `GET /api/dashboard/recent-logs`，使用 Timeline 或列表组件：

| 展示字段 | 来源字段 | 说明 |
| --- | --- | --- |
| 操作人 | operatorName | 加粗显示 |
| 操作类型 | actionType | Tag 标签 |
| 描述 | description | 正文 |
| 时间 | operateTime | 相对时间（如"5分钟前"）或 YYYY-MM-DD HH:mm |

##### 所需API

| 接口 | 说明 |
| --- | --- |
| GET /api/dashboard/metrics | 核心指标（4个卡片） |
| GET /api/dashboard/visit-trend | 近7天访问趋势 |
| GET /api/dashboard/module-usage | 功能模块使用频率 |
| GET /api/dashboard/recent-logs | 最近操作动态（最近5条） |

---

### 2.2.7 全局登录态管理

##### 获取当前用户

应用初始化时（如 App.tsx mount）调用 `GET /api/user/get/login` 获取当前登录用户信息：

```json
{
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
```

> **v2.0 变更**：`userRole` 字段已移除。登录响应中改为 `roleId` + `roleName`。前端权限判断应使用 `roleName` 或通过 `roleId` 判断。

##### 权限判断（路由守卫）

```typescript
// v2.0 路由守卫示例 - 基于角色名称判断管理员
const isAdmin = currentUser?.roleName === '超级管理员' 
  || currentUser?.roleId === adminRoleId;

// 或通过后端 isAdmin 判断（前端仅做 UI 层面的菜单显隐）
// 实际权限校验由后端 AuthInterceptor 保证
```

##### 登出

```typescript
POST /api/user/logout
// 成功后清除前端用户状态，跳转登录页
```

---

## 2.3 菜单与权限变动

| 菜单路径 | 权限 | 备注 |
| --- | --- | --- |
| 工作台 | 所有登录用户 | v2.0 新增 |
| 用户管理 | admin | 仅管理员可见 |
| 个人设置 | 所有登录用户 | |
| 账号安全 | 所有登录用户 | v2.0 新增 |

## 2.4 模块划分与工作量评估

| 模块 | 细节 | 开发 | 联调 | 自测 |
| --- | --- | --- | --- | --- |
| 登录页 | 表单 + 校验 + 登录态持久化 | 0.5 | 0.5 | 0.5 |
| 注册页 | 表单 + 校验 | 0.5 | 0.5 | 0.5 |
| 用户管理 | 列表 + 搜索 + CRUD Modal + 状态开关 + 角色选择 | 1.5 | 0.5 | 0.5 |
| 个人设置 | 表单 + 头像上传 | 0.5 | 0.5 | 0.5 |
| 账号安全 | 修改密码 + 手机绑定/解绑 | 1.0 | 0.5 | 0.5 |
| 工作台 | 指标卡片 + 折线图 + 柱状图 + 操作日志列表 | 1.5 | 0.5 | 0.5 |
| 全局登录态 | getLogin + 路由守卫 + 登出 | 0.5 | 0.5 | 0.5 |

\[20260711~~20260712\] 实现登录+注册页面
\[20260712~~20260713\] 实现全局登录态管理
\[20260713~~20260714\] 实现用户管理（管理员）
\[20260714~~20260715\] 实现个人设置
\[20260715~~20260716\] 实现账号安全
\[20260716~~20260718\] 实现工作台数据看板
\[20260718~~20260719\] 联调测试

# 3. 监控和埋点

- 登录成功/失败埋点
- 注册成功埋点
- 用户管理操作埋点（创建/删除/更新/状态切换）
- 密码修改埋点
- 手机绑定/解绑埋点
- 工作台页面访问埋点

# 4. 发布计划

- 预发验证：登录注册流程、权限校验、session 持久化、工作台数据展示
- 灰度发布：全员开放

# 5. 其他

## 5.1 风险评估

- 登录态基于 session cookie，需确保前后端 cookie 传递正常（同域或 CORS 配置 withCredentials）
- 管理员接口需要路由守卫，前端需根据 roleName 或 roleId 隐藏/显示对应菜单
- session 过期时间 30 天（后端配置 `spring.session.timeout: 2592000`），前端无需额外处理 token 刷新
- v2.0 移除 userRole 字段后，前端所有角色判断需改用 roleName/roleId，建议在全局状态中缓存当前用户的角色信息

## 5.2 v2.0 前后端对接变更清单

| 变更项 | v1.0（旧） | v2.0（新） | 影响范围 |
| --- | --- | --- | --- |
| 登录响应 | userRole: "user" | roleId: 3, roleName: "部门主管" | 登录页、全局状态 |
| 用户列表角色列 | userRole（admin/user/ban） | userRoleName（从 role 表联查） | 用户管理列表 |
| 新建/编辑用户 | 角色 Select: user/admin | 角色 Select: 从 role 接口取列表 | 新建/编辑弹窗 |
| 分页筛选 | userRole: "admin" | roleId: 3 | 搜索筛选 |
| 状态切换 | 无此功能 | POST /user/update/status | 用户列表行操作 |
| 工作台 | 无此功能 | /dashboard/* 4个接口 | 新增模块 |
| 账号安全 | 无此功能 | /account/* 2个接口 | 新增模块 |

## 5.3 变更记录

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-13 | 1.0 | 初稿，根据实际后端API编写 | - |
| 2026-07-15 | 2.0 | 移除 userRole，接入 role 表 RBAC；新增状态开关、账号安全、工作台数据看板 | - |
