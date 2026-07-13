# HRMS-测试用例-用户模块

## 变更记录

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-13 | 1.0 | 初稿 | - |

---

# 一、用户注册

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-001 | 正常注册 | 账号未被注册 | POST /api/user/register | `{"userAccount": "test001", "userPassword": "12345678", "checkPassword": "12345678"}` | code=0, 返回新用户ID, userRole 默认为 "user" |
| TC-USER-002 | 账号不足4位 | 无 | POST /api/user/register | `{"userAccount": "abc", "userPassword": "12345678", "checkPassword": "12345678"}` | code≠0, "用户账号过短" |
| TC-USER-003 | 密码不足8位 | 无 | POST /api/user/register | `{"userAccount": "test002", "userPassword": "123456", "checkPassword": "123456"}` | code≠0, "用户密码过短" |
| TC-USER-004 | 两次密码不一致 | 无 | POST /api/user/register | `{"userAccount": "test003", "userPassword": "12345678", "checkPassword": "87654321"}` | code≠0, "两次输入的密码不一致" |
| TC-USER-005 | 参数为空 | 无 | POST /api/user/register | `{"userAccount": "", "userPassword": "", "checkPassword": ""}` | code≠0, "参数为空" |
| TC-USER-006 | 请求体为空 | 无 | POST /api/user/register | `null` | code≠0, PARAMS_ERROR |
| TC-USER-007 | 注册重复账号 | 账号 "test001" 已存在 | POST /api/user/register | `{"userAccount": "test001", "userPassword": "12345678", "checkPassword": "12345678"}` | code≠0, "账号重复" |
| TC-USER-008 | 并发注册同一账号 | 同时提交两个相同账号的注册请求 | POST /api/user/register | 相同 userAccount | 仅一个成功，另一个返回 "账号重复"（synchronized 保证） |
| TC-USER-009 | 密码加密验证 | 注册成功 | 查 user 表 | 查 user 表对应记录 | userPassword ≠ 明文 "12345678"（已MD5加盐加密） |

---

# 二、用户登录

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-010 | 正常登录 | 用户已注册，账号密码正确 | POST /api/user/login | `{"userAccount": "test001", "userPassword": "12345678"}` | code=0, 返回 LoginUserVO（含 id/userName/userRole，不含 userAccount/userPassword） |
| TC-USER-011 | 账号不存在 | 该账号未注册 | POST /api/user/login | `{"userAccount": "nonexist", "userPassword": "12345678"}` | code≠0, "用户不存在或密码错误" |
| TC-USER-012 | 密码错误（账号存在） | 账号存在但密码错误 | POST /api/user/login | `{"userAccount": "test001", "userPassword": "wrongPass"}` | code≠0, "用户不存在或密码错误"，login_log 表新增一条 isSuccess=0 记录 |
| TC-USER-013 | 密码错误（账号不存在） | 账号不存在 | POST /api/user/login | `{"userAccount": "nonexist", "userPassword": "wrongPass"}` | code≠0, "用户不存在或密码错误"，login_log 表无新记录 |
| TC-USER-014 | 账号为空 | 无 | POST /api/user/login | `{"userAccount": "", "userPassword": "12345678"}` | code≠0, "参数为空" |
| TC-USER-015 | 密码为空 | 无 | POST /api/user/login | `{"userAccount": "test001", "userPassword": ""}` | code≠0, "参数为空" |
| TC-USER-016 | 请求体为空 | 无 | POST /api/user/login | `null` | code≠0, PARAMS_ERROR |
| TC-USER-017 | 账号不足4位 | 无 | POST /api/user/login | `{"userAccount": "ab", "userPassword": "12345678"}` | code≠0, "账号错误" |
| TC-USER-018 | 密码不足8位 | 无 | POST /api/user/login | `{"userAccount": "test001", "userPassword": "12"}` | code≠0, "密码错误" |
| TC-USER-019 | 登录成功写入session | 登录成功 | 后续请求 | session cookie 自动携带 | 通过 getLoginUser 可获取当前用户 |
| TC-USER-020 | 登录成功记录日志 | 登录成功 | 查 login_log 表 | 查 login_log 最新记录 | isSuccess=1, failReason=null, loginType=1, loginTypeText="密码登录" |
| TC-USER-021 | 登录失败记录日志 | 账号存在但密码错误 | 查 login_log 表 | 查 login_log 最新记录 | isSuccess=0, failReason="密码错误" |

---

# 三、用户登出

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-022 | 正常登出 | 用户已登录 | POST /api/user/logout | 无 | code=0, session 中 user_login 被清除 |
| TC-USER-023 | 登出后访问需登录接口 | 已登出 | GET /api/user/get/login | 无 | code≠0, NOT_LOGIN_ERROR |
| TC-USER-024 | 未登录时登出 | 未登录 | POST /api/user/logout | 无 | code≠0, "未登录" |
| TC-USER-025 | 请求为空 | 已登录 | POST /api/user/logout | request=null | code≠0, PARAMS_ERROR |

---

# 四、获取当前登录用户

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-026 | 已登录获取 | 用户已登录 | GET /api/user/get/login | 无 | code=0, 返回 LoginUserVO，与登录时返回一致，不含 userAccount/userPassword |
| TC-USER-027 | 未登录获取 | 无登录态 | GET /api/user/get/login | 无 | code≠0, NOT_LOGIN_ERROR |
| TC-USER-028 | 返回字段完整性 | 已登录 | GET /api/user/get/login | 无 | 返回含 id/userName/userAvatar/userProfile/userRole/createTime/updateTime |

---

# 五、用户管理（管理员）

## 5.1 创建用户

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-029 | 管理员创建用户 | 管理员已登录 | POST /api/user/add | `{"userName": "新员工", "userAccount": "newuser", "userAvatar": "...", "userRole": "user"}` | code=0, 返回新用户ID, 默认密码 12345678 已加密 |
| TC-USER-030 | 非管理员创建用户 | 普通用户已登录 | POST /api/user/add | `{...}` | code≠0, NO_AUTH_ERROR 或 FORBIDDEN_ERROR |
| TC-USER-031 | 未登录创建用户 | 无登录态 | POST /api/user/add | `{...}` | code≠0, NOT_LOGIN_ERROR |
| TC-USER-032 | 请求体为空 | 管理员已登录 | POST /api/user/add | `null` | code≠0, PARAMS_ERROR |
| TC-USER-033 | 创建用户默认密码验证 | 管理员已登录 | POST /api/user/add → 使用返回的ID登录 | 登录 `userAccount: newuser, userPassword: 12345678` | 登录成功 |

## 5.2 删除用户

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-034 | 管理员删除用户 | 管理员已登录，目标用户存在 | POST /api/user/delete | `{"id": 目标用户ID}` | code=0, 用户被逻辑删除（isDelete=1） |
| TC-USER-035 | 删除不存在的用户 | 管理员已登录 | POST /api/user/delete | `{"id": 99999}` | code=0（逻辑删除返回 true） |
| TC-USER-036 | ID为空或<=0 | 管理员已登录 | POST /api/user/delete | `{"id": 0}` | code≠0, PARAMS_ERROR |
| TC-USER-037 | 请求体为空 | 管理员已登录 | POST /api/user/delete | `null` | code≠0, PARAMS_ERROR |
| TC-USER-038 | 非管理员删除 | 普通用户已登录 | POST /api/user/delete | `{"id": 123}` | code≠0, 无权限 |

## 5.3 更新用户

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-039 | 管理员更新用户 | 管理员已登录 | POST /api/user/update | `{"id": 目标ID, "userName": "新昵称", "userRole": "admin"}` | code=0, 用户昵称和角色已更新 |
| TC-USER-040 | 封禁用户 | 管理员已登录 | POST /api/user/update | `{"id": 目标ID, "userRole": "ban"}` | code=0, userRole="ban" |
| TC-USER-041 | 更新不存在的用户 | 管理员已登录 | POST /api/user/update | `{"id": 99999, "userName": "..."}` | code=0（MyBatis-Plus updateById 不影响记录时返回 false），返回码≠0 |
| TC-USER-042 | ID为空 | 管理员已登录 | POST /api/user/update | `{"userName": "..."}` | code≠0, PARAMS_ERROR |
| TC-USER-043 | 请求体为空 | 管理员已登录 | POST /api/user/update | `null` | code≠0, PARAMS_ERROR |
| TC-USER-044 | 非管理员更新 | 普通用户已登录 | POST /api/user/update | `{...}` | code≠0, 无权限 |

## 5.4 按ID查询

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-045 | 管理员查询用户 | 管理员已登录 | GET /api/user/get | `id=有效用户ID` | code=0, 返回完整 User（含 userAccount） |
| TC-USER-046 | 查询脱敏VO | 任意用户已登录 | GET /api/user/get/vo | `id=有效用户ID` | code=0, 返回 UserVO（不含 userAccount/userPassword） |
| TC-USER-047 | 查询不存在的用户 | 已登录 | GET /api/user/get | `id=99999` | code≠0, NOT_FOUND_ERROR |
| TC-USER-048 | ID <= 0 | 管理员已登录 | GET /api/user/get | `id=0` | code≠0, PARAMS_ERROR |

## 5.5 分页查询

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-049 | 管理员分页查询 | 管理员已登录 | POST /api/user/list/page | `{"current": 1, "pageSize": 10}` | code=0, 返回 Page\<User\>（含 total/records） |
| TC-USER-050 | 分页查询VO | 任意用户已登录 | POST /api/user/list/page/vo | `{"current": 1, "pageSize": 10}` | code=0, 返回 Page\<UserVO\> |
| TC-USER-051 | pageSize 超过20（VO接口） | 已登录 | POST /api/user/list/page/vo | `{"current": 1, "pageSize": 100}` | code≠0, PARAMS_ERROR |
| TC-USER-052 | 按角色筛选 | 管理员已登录 | POST /api/user/list/page | `{"current": 1, "pageSize": 10, "userRole": "admin"}` | code=0, 仅返回 role=admin 的用户 |
| TC-USER-053 | 按昵称模糊搜索 | 管理员已登录 | POST /api/user/list/page | `{"current": 1, "pageSize": 10, "userName": "张"}` | code=0, 仅返回昵称含"张"的用户 |
| TC-USER-054 | 排序验证 | 管理员已登录 | POST /api/user/list/page | `{"current": 1, "pageSize": 10, "sortField": "createTime", "sortOrder": "desc"}` | code=0, 按创建时间倒序 |

---

# 六、修改个人信息

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-USER-055 | 修改个人昵称 | 用户已登录 | POST /api/user/update/my | `{"userName": "新昵称"}` | code=0, 当前用户昵称已更新 |
| TC-USER-056 | 修改个人简介 | 用户已登录 | POST /api/user/update/my | `{"userProfile": "新的个人简介"}` | code=0, 当前用户简介已更新 |
| TC-USER-057 | 修改个人头像 | 用户已登录 | POST /api/user/update/my | `{"userAvatar": "https://new-avatar.url"}` | code=0, 当前用户头像已更新 |
| TC-USER-058 | 同时修改多个字段 | 用户已登录 | POST /api/user/update/my | `{"userName": "新昵称", "userProfile": "新简介", "userAvatar": "..."}` | code=0, 所有字段已更新 |
| TC-USER-059 | 请求体为空 | 用户已登录 | POST /api/user/update/my | `null` | code≠0, PARAMS_ERROR |
| TC-USER-060 | 未登录修改 | 无登录态 | POST /api/user/update/my | `{"userName": "新昵称"}` | code≠0, NOT_LOGIN_ERROR |
| TC-USER-061 | 不可提权 | 普通用户已登录 | POST /api/user/update/my | 尝试包含 userRole 字段 | 即使传了 userRole 也会被忽略（BeanUtils 从 UserUpdateMyRequest 拷贝，该DTO无 userRole 字段） |

---

# 七、角色权限验证

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 预期结果 |
| --- | --- | --- | --- | --- |
| TC-USER-062 | 普通用户访问管理员接口 | 普通用户已登录 | POST /api/user/add | code≠0, 被 @AuthCheck 拦截 |
| TC-USER-063 | 管理员访问管理员接口 | 管理员已登录 | POST /api/user/add | code=0, 正常执行 |
| TC-USER-064 | 未登录访问管理员接口 | 无登录态 | POST /api/user/add | code≠0, NOT_LOGIN_ERROR |
| TC-USER-065 | 被封禁用户访问需登录接口 | ban用户尝试登录 | POST /api/user/login | 登录成功（login 无 @AuthCheck），但后续通过 getLoginUser 可正常获取，业务接口是否限制取决于具体 @AuthCheck 配置 |

---

# 八、Session 管理

| 用例编号 | 测试场景 | 前置条件 | 操作 | 预期结果 |
| --- | --- | --- | --- | --- |
| TC-USER-066 | Session 持久化 | 登录成功 | 关闭浏览器 → 重新打开 → 访问接口 | 若 session cookie 未过期（30天有效），仍可获取当前用户 |
| TC-USER-067 | Session 过期 | session 已过期 | 访问接口 | code≠0, NOT_LOGIN_ERROR |
| TC-USER-068 | 不同用户隔离 | 用户A登录 | 用户B通过修改 session cookie 冒充A | 无法成功（session 服务端存储） |

---

# 九、错误码汇总

| 错误码 | 触发场景 |
| --- | --- |
| PARAMS_ERROR | 参数为空/格式错误/账号重复/密码不一致/账号或密码过短 |
| NOT_LOGIN_ERROR | 未登录访问需登录接口 |
| NOT_FOUND_ERROR | 查询不存在的用户 |
| NO_AUTH_ERROR / FORBIDDEN_ERROR | 非管理员访问管理员接口 |
| OPERATION_ERROR | 数据库操作失败 |

---

## 附录：测试数据准备

| 数据 | 说明 |
| --- | --- |
| 管理员账号 | 拥有 admin 角色，可执行用户管理操作 |
| 普通用户 | 拥有 user 角色，userId=2075829151662010370（测试用） |
| 新注册用户 | 测试注册流程时动态创建，测试完成后可清理 |
| 被封禁用户 | userRole="ban"，用于测试封禁效果 |
| Session配置 | 超时30天（2592000秒），测试时可临时改短 |
