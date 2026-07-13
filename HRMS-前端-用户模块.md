# HRMS-前端-用户模块

# 1. 需求背景

> 构建统一的用户认证与管理系统，支持用户自主注册登录、管理员进行用户管理、个人设置修改等。

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
3. 用户管理（管理员）：用户列表 + CRUD操作
4. 个人设置：修改个人信息（昵称/头像/简介）

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
- 数据表格：用户列表
- 新建/编辑用户 Modal

##### 用户列表展示字段

| 字段名称 | 说明 | 来源字段 |
| --- | --- | --- |
| ID | 雪花ID | id |
| 账号 | - | userAccount（仅管理员可见完整 User） |
| 昵称 | - | userName |
| 头像 | 缩略图 | userAvatar |
| 简介 | - | userProfile |
| 角色 | Tag：管理员(红)/用户(蓝)/封禁(灰) | userRole |
| 创建时间 | YYYY-MM-DD HH:mm | createTime |
| 操作 | 编辑 / 删除 | - |

##### 新建用户弹窗

| 字段名称 | 输入方式 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 账号 | Input | Y | 登录账号 |
| 昵称 | Input | N | 用户昵称 |
| 头像 | Input/Upload | N | 头像URL |
| 角色 | Select | N | user / admin，默认 user |

> 默认密码 12345678（后端固定），前端无需展示密码字段。

##### 编辑用户弹窗

| 字段名称 | 输入方式 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| 昵称 | Input | N | |
| 头像 | Input/Upload | N | |
| 简介 | TextArea | N | |
| 角色 | Select | N | user / admin / ban |

> 编辑用户不可修改账号和密码。

##### 删除用户

- 二次确认弹窗："确定要删除该用户吗？"
- 支持逻辑删除

##### 所需API

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST /api/user/add | 创建用户 | admin |
| POST /api/user/delete | 删除用户 | admin |
| POST /api/user/update | 更新用户 | admin |
| GET /api/user/get?id= | 查询用户详情 | admin |
| POST /api/user/list/page | 分页查询用户 | admin |
| POST /api/user/list/page/vo | 分页查询（脱敏） | 无需admin |

##### 分页请求参数

```json
{
  "current": 1,
  "pageSize": 10,
  "userName": "",       // 可选，模糊匹配
  "userProfile": "",    // 可选，模糊匹配
  "userRole": "",       // 可选，精确匹配
  "sortField": "createTime",
  "sortOrder": "desc"
}
```

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

### 2.2.5 全局登录态管理

##### 获取当前用户

应用初始化时（如 App.tsx mount）调用 `GET /api/user/get/login` 获取当前登录用户信息：

```json
{
  "id": 2075829151662010370,
  "userName": "张三",
  "userAvatar": "https://...",
  "userProfile": "前端工程师",
  "userRole": "user",
  "createTime": "2026-01-01T00:00:00",
  "updateTime": "2026-07-10T12:00:00"
}
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
| 用户管理 | admin | 仅管理员可见 |
| 个人设置 | 所有登录用户 | |

---

## 2.4 模块划分与工作量评估

| 模块 | 细节 | 开发 | 联调 | 自测 |
| --- | --- | --- | --- | --- |
| 登录页 | 表单 + 校验 + 登录态持久化 | 0.5 | 0.5 | 0.5 |
| 注册页 | 表单 + 校验 | 0.5 | 0.5 | 0.5 |
| 用户管理 | 列表 + 搜索 + CRUD Modal | 1.5 | 0.5 | 0.5 |
| 个人设置 | 表单 + 头像上传 | 0.5 | 0.5 | 0.5 |
| 全局登录态 | getLogin + 路由守卫 + 登出 | 0.5 | 0.5 | 0.5 |

\[20260711~~20260712\] 实现登录+注册页面
\[20260712~~20260713\] 实现全局登录态管理
\[20260713~~20260714\] 实现用户管理（管理员）
\[20260714~~20260715\] 实现个人设置
\[20260715~~20260717\] 联调测试

# 3. 监控和埋点

- 登录成功/失败埋点
- 注册成功埋点
- 用户管理操作埋点（创建/删除/更新）

# 4. 发布计划

- 预发验证：登录注册流程、权限校验、session 持久化
- 灰度发布：全员开放

# 5. 其他

## 5.1 风险评估

- 登录态基于 session cookie，需确保前后端 cookie 传递正常（同域或 CORS 配置 withCredentials）
- 管理员接口需要路由守卫，前端需根据 userRole 隐藏/显示对应菜单
- session 过期时间 30 天（后端配置 `spring.session.timeout: 2592000`），前端无需额外处理 token 刷新

## 5.2 变更记录

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-13 | 1.0 | 初稿，根据实际后端API编写 | - |
