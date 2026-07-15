# HRMS 测试用例 — 个人中心 → 我的档案

**文档版本**：v1.0  
**编写日期**：2026-07-14  
**对应需求**：HRMS.md 第 9.1 节「我的档案」  
**测试范围**：`ProfileController` 三个接口 + `EmployeeServiceImpl` 业务逻辑

---

## 目录

1. [前置数据](#一前置数据)
2. [接口1：获取我的档案详情](#二接口1获取我的档案详情-get-employeeprofilegetmyprofile)
3. [接口2：修改个人可编辑档案](#三接口2修改个人可编辑档案-put-employeeprofileupdatemydetail)
4. [接口3：查询修改日志](#四接口3查询个人档案修改日志-get-employeeprofileloglist)
5. [端到端集成测试](#五端到端集成测试)
6. [权限矩阵验证](#六权限矩阵验证)
7. [数据脱敏验证](#七数据脱敏验证)
8. [变更日志完整性验证](#八变更日志完整性验证)

---

## 一、前置数据

| 数据项 | 说明 |
|--------|------|
| 测试环境 | `http://localhost:8123/api` |
| 鉴权方式 | Session Cookie（`JSESSIONID`），所有接口需先登录 |
| 测试角色 | 普通员工 A（status=正式）、普通员工 B（status=已离职）、管理员/HR |

### 预置测试账号

| 角色 | userAccount | employeeId | 在职状态 | 备注 |
|------|-------------|------------|----------|------|
| 普通员工 A | 13800138000 | 15 | 2（正式） | 主要测试账号 |
| 普通员工 B | 13800138001 | 16 | 4（已离职） | 离职拦截测试 |
| 管理员/HR | admin | - | - | userRole=admin |

### 数据表关联

```
user ──(userId)──▶ employee ──(employeeId)──▶ employee_detail
                       │
                       └──(employeeId)──▶ employee_change_log
```

---

## 二、接口1：获取我的档案详情 `GET /employee/profile/getMyProfile`

### TC-1.1 普通员工查看自己档案（不传 employeeId）

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-1.1 |
| **用例名称** | 普通员工不传 employeeId，查看自己完整档案 |
| **前置条件** | 以普通员工 A 登录；employee_detail 存在关联数据 |
| **请求方式** | `GET /employee/profile/getMyProfile`（不传参数） |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回** ||
| 字段 | 预期值/规则 |
| `id` | 等于 employee 表中的员工 ID |
| `employeeName` | 员工姓名，明文 |
| `employeeNo` | 工号，明文 |
| `account` | 系统账号，明文 |
| `status` / `statusDesc` | 在职状态码 + 中文描述 |
| `gender` / `genderDesc` | 性别码 + "男"/"女" |
| `phone` | **脱敏**：`138****3800` |
| `email` | 明文邮箱 |
| `departmentId` / `departmentName` | 部门 ID + 名称 |
| `positionId` / `positionName` | 职位 ID + 名称 |
| `hireDate` | 入职日期 |
| `idCard` | **脱敏**：前 4 位 + `****` + 后 4 位 |
| `currentAddress` | 现居住地址，明文 |
| `emergencyContactName` | 紧急联系人姓名，明文 |
| `emergencyContactPhone` | **脱敏**：`138****5678` |
| `birthday` | 生日 |
| `registeredAddress` | 户籍地址，明文 |
| `jobLevel` | 职级（从 employee_detail 读取） |
| `directReportId` / `directReportName` | 直接汇报人 ID + 姓名 |
| `workLocation` | 工作地点 |
| `contractType` / `contractTypeDesc` | 合同类型码 + 中文描述 |
| `contractExpireDate` | 合同到期日 |
| `probationRatio` | 试用期比例 |
| `baseSalary` | **null（普通员工隐藏）** |
| `bankAccount` | **null（普通员工隐藏）** |
| `bankName` | 开户行，明文 |
| `editableFields` | `["email", "currentAddress", "emergencyContactName"]` |
| `isSensitiveRole` | `false` |

---

### TC-1.2 普通员工查看自己档案（传自己的 employeeId）

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-1.2 |
| **用例名称** | 普通员工传自己的 employeeId，能正常查看 |
| **前置条件** | 以普通员工 A 登录，其 employeeId = 15 |
| **请求方式** | `GET /employee/profile/getMyProfile?employeeId=15` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回** | 同 TC-1.1，正常返回自己的档案 |

---

### TC-1.3 普通员工越权查看他人档案

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-1.3 |
| **用例名称** | 普通员工传他人的 employeeId，被拦截 |
| **前置条件** | 以普通员工 A 登录，尝试查 employeeId = 16 |
| **请求方式** | `GET /employee/profile/getMyProfile?employeeId=16` |
| **预期状态码** | `code = 50027 (PROFILE_PERMISSION_DENIED)` |
| **预期 message** | `"无权查看他人档案，仅可查看本人"` |

---

### TC-1.4 管理员/HR 查看他人档案

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-1.4 |
| **用例名称** | 管理员传他人 employeeId，能正常查看且返回完整明文 |
| **前置条件** | 以管理员/HR 登录 |
| **请求方式** | `GET /employee/profile/getMyProfile?employeeId=15` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回** | 与 TC-1.1 对比，差异如下： |
| `phone` | **明文** `13800138000` |
| `idCard` | **明文**（不脱敏） |
| `emergencyContactPhone` | **明文** |
| `baseSalary` | **明文数值**（不为 null） |
| `bankAccount` | **明文**（不为 null） |
| `isSensitiveRole` | `true` |

---

### TC-1.5 管理员/HR 不传 employeeId 查看自己档案

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-1.5 |
| **用例名称** | 管理员不传 employeeId，默认查自己的档案 |
| **前置条件** | 以管理员/HR 登录（该管理员也有关联的 employee） |
| **请求方式** | `GET /employee/profile/getMyProfile`（不传参数） |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回** | 返回管理员的档案数据，`isSensitiveRole = true` |

---

### TC-1.6 员工档案不存在（employee 表无记录）

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-1.6 |
| **用例名称** | 登录用户关联的 employee 不存在 |
| **前置条件** | 以某个 userId 登录，但 employee 表中不存在对应记录 |
| **请求方式** | `GET /employee/profile/getMyProfile` |
| **预期状态码** | `code = 40400 (NOT_FOUND_ERROR)` |
| **预期 message** | `"员工档案不存在"` |

---

### TC-1.7 员工无 employee_detail 记录

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-1.7 |
| **用例名称** | employee 存在但 employee_detail 表无关联记录 |
| **前置条件** | 以普通员工登录，其 employee_detail 表中无对应数据 |
| **请求方式** | `GET /employee/profile/getMyProfile` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回** | employee 主表字段正常返回；detail 表字段（idCard、baseSalary 等）均为 `null`，不报错 |

---

### TC-1.8 已离职员工查看自己档案

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-1.8 |
| **用例名称** | 已离职员工仍可查看自己的档案 |
| **前置条件** | 以普通员工 B（status=4 已离职）登录 |
| **请求方式** | `GET /employee/profile/getMyProfile` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回** | 正常返回档案数据，`statusDesc = "已离职"` |
| **备注** | 已离职仅拦截"修改"操作，查看不受影响 |

---

### TC-1.9 未登录调用接口

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-1.9 |
| **用例名称** | 未登录 / Token 失效，直接调用接口 |
| **前置条件** | 不携带有效 Session Cookie |
| **请求方式** | `GET /employee/profile/getMyProfile` |
| **预期状态码** | `code = 40100 (NOT_LOGIN_ERROR)` |
| **预期 message** | `"未登录"` |

---

## 三、接口2：修改个人可编辑档案 `PUT /employee/profile/updateMyDetail`

### TC-2.1 修改现居住地址（单个字段）

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.1 |
| **用例名称** | 普通员工修改自己的现居住地址 |
| **前置条件** | 以普通员工 A 登录，employeeId = 15 |
| **请求体** | `{ "employeeId": 15, "currentAddress": "北京市朝阳区XX路XX号" }` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回 data** | `{ "employeeId": 15, "updateTime": "2026-07-14 HH:mm:ss" }` |
| **DB验证** | `employee_detail.currentAddress` = `"北京市朝阳区XX路XX号"` |
| **日志验证** | `employee_change_log` 新增一条，`fieldName=currentAddress`，`oldValue=原地址`，`newValue=新地址` |

---

### TC-2.2 修改紧急联系人姓名 + 电话（两个字段）

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.2 |
| **用例名称** | 同时修改紧急联系人姓名和电话 |
| **前置条件** | 以普通员工 A 登录 |
| **请求体** | `{ "employeeId": 15, "emergencyContactName": "李四", "emergencyContactPhone": "13912345678" }` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **DB验证** | `emergencyContactName` 和 `emergencyContactPhone` 均更新 |
| **日志验证** | 生成 **2 条** change_log 记录 |

---

### TC-2.3 修改全部三个可编辑字段

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.3 |
| **用例名称** | 同时修改 currentAddress + emergencyContactName + emergencyContactPhone |
| **前置条件** | 以普通员工 A 登录 |
| **请求体** | `{ "employeeId": 15, "currentAddress": "上海市浦东新区XX路", "emergencyContactName": "王五", "emergencyContactPhone": "13712345678" }` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **日志验证** | 生成 **3 条** change_log 记录，每条对应一个字段 |

---

### TC-2.4 仅传一个有变化的字段，其余不传

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.4 |
| **用例名称** | 请求体只含一个字段，其余字段不传 |
| **前置条件** | 以普通员工 A 登录 |
| **请求体** | `{ "employeeId": 15, "currentAddress": "广州市天河区XX路" }` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **DB验证** | 仅 `currentAddress` 更新；`emergencyContactName` 和 `emergencyContactPhone` 保持不变 |
| **日志验证** | 仅生成 **1 条** change_log（只有 currentAddress 变更） |

---

### TC-2.5 填写与当前值相同的值（无实际变更）

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.5 |
| **用例名称** | 提交与数据库现有值完全相同的内容 |
| **前置条件** | 以普通员工 A 登录，已知 currentAddress = "广州市天河区XX路" |
| **请求体** | `{ "employeeId": 15, "currentAddress": "广州市天河区XX路" }` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回 data** | `{ "employeeId": 15, "updateTime": "..." }` |
| **日志验证** | **不生成**任何 change_log 记录（新旧值相同，跳过） |

---

### TC-2.6 越权修改他人档案（普通员工）

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.6 |
| **用例名称** | 普通员工尝试修改他人 employee 档案 |
| **前置条件** | 以普通员工 A（employeeId=15）登录 |
| **请求体** | `{ "employeeId": 16, "currentAddress": "黑客地址" }` |
| **预期状态码** | `code = 50028 (PROFILE_UPDATE_DENIED)` |
| **预期 message** | `"无权修改他人档案，仅可修改本人"` |

---

### TC-2.7 已离职员工修改档案

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.7 |
| **用例名称** | 已离职员工尝试修改自己的档案 |
| **前置条件** | 以普通员工 B（status=4 已离职）登录 |
| **请求体** | `{ "employeeId": 16, "currentAddress": "新地址" }` |
| **预期状态码** | `code = 50026 (EMPLOYEE_RESIGNED)` |
| **预期 message** | `"已离职员工不可修改档案，仅可查看"` |

---

### TC-2.8 紧急联系人电话格式错误

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.8 |
| **用例名称** | emergencyContactPhone 格式不符合 11 位手机号 |
| **前置条件** | 以普通员工 A 登录 |
| **测试数据** | 以下均触发错误： |
| | ① `"emergencyContactPhone": "12345"`（不足 11 位） |
| | ② `"emergencyContactPhone": "123456789012"`（12 位，超长） |
| | ③ `"emergencyContactPhone": "12345678901"`（首位不是 1） |
| | ④ `"emergencyContactPhone": "12012345678"`（第二位不在 3-9 范围） |
| | ⑤ `"emergencyContactPhone": "不是手机号"`（含非数字） |
| **预期状态码** | `code = 50029 (PHONE_FORMAT_ERROR)` |
| **预期 message** | `"紧急联系人电话格式不正确，需为11位手机号"` |

---

### TC-2.9 紧急联系人电话格式正确

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.9 |
| **用例名称** | 合法手机号格式，提交成功 |
| **前置条件** | 以普通员工 A 登录 |
| **测试数据** | 以下均合法： |
| | `"emergencyContactPhone": "13812345678"` |
| | `"emergencyContactPhone": "15900001111"` |
| | `"emergencyContactPhone": "18888888888"` |
| | `"emergencyContactPhone": "13600000000"` |
| **预期状态码** | `code = 0 (SUCCESS)` |

---

### TC-2.10 请求体为空

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.10 |
| **用例名称** | 请求体为 null |
| **前置条件** | 以普通员工 A 登录 |
| **请求体** | `null`（不传 body 或 `{}` 但不含 employeeId） |
| **预期状态码** | `code = 40000 (PARAMS_ERROR)` |
| **预期 message** | `"请求参数错误"` 或 `"员工ID不能为空"` |

---

### TC-2.11 employeeId 为 null / 0 / 负数

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.11 |
| **用例名称** | employeeId 不传、传 0 或传负数 |
| **前置条件** | 以普通员工 A 登录 |
| **测试数据** ||
| | ① `{ "employeeId": null, "currentAddress": "..." }` |
| | ② `{ "employeeId": 0, "currentAddress": "..." }` |
| | ③ `{ "employeeId": -1, "currentAddress": "..." }` |
| **预期状态码** | `code = 40000 (PARAMS_ERROR)` |
| **预期 message** | `"员工ID不能为空"` |

---

### TC-2.12 管理员修改他人档案（合法）

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.12 |
| **用例名称** | 管理员修改任意员工的档案（包括普通员工的三个字段） |
| **前置条件** | 以管理员/HR 登录 |
| **请求体** | `{ "employeeId": 15, "currentAddress": "管理员代填地址" }` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **日志验证** | `operatorId` 为管理员的 userId（而非员工 15 的 userId） |

---

### TC-2.13 employee_detail 不存在时首次新增

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-2.13 |
| **用例名称** | employee_detail 表中无记录时，自动创建并写入 |
| **前置条件** | employee 表的某员工在 employee_detail 表中无对应行 |
| **请求体** | `{ "employeeId": XX, "currentAddress": "新地址" }` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **DB验证** | `employee_detail` 新增一行，`currentAddress` 已写入 |

---

## 四、接口3：查询个人档案修改日志 `GET /employee/profile/log/list`

### TC-3.1 普通员工不传 employeeId，查自己日志

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-3.1 |
| **用例名称** | 普通员工不传 employeeId，默认查自己的修改日志 |
| **前置条件** | 以普通员工 A 登录；已执行 TC-2.1 ~ TC-2.3 产生了多条日志 |
| **请求方式** | `GET /employee/profile/log/list?pageNum=1&pageSize=10` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回结构** ||
| 字段 | 说明 |
| `data.total` | 日志总条数 |
| `data.records[]` | 日志列表，按 `createTime` 倒序 |
| `data.current` | 当前页码 |
| `data.size` | 每页条数 |

---

### TC-3.2 日志字段内容验证

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-3.2 |
| **用例名称** | 验证单条日志记录的字段完整性 |
| **前置条件** | 以普通员工 A 登录，已存在至少一条 change_log |
| **请求方式** | `GET /employee/profile/log/list?pageNum=1&pageSize=10` |
| **预期返回（单条日志）** ||
| 字段 | 预期值/说明 |
| `id` | 日志主键 |
| `employeeId` | 员工 ID |
| `fieldName` | 字段名，如 `"currentAddress"` |
| `oldValue` | 变更前的值 |
| `newValue` | 变更后的值 |
| `changeType` | `"个人档案自主修改"` |
| `operatorId` | 操作人 ID |
| `remark` | 如 `"员工自行更新现居住地址"` |
| `createTime` | 创建时间 |

---

### TC-3.3 分页功能验证

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-3.3 |
| **用例名称** | 分页查询：首页、换页、不同每页条数 |
| **前置条件** | 以普通员工 A 登录，先通过 TC-2.1 连续 15 次修改产生 15+ 条日志 |
| **测试数据** ||
| | ① `pageNum=1&pageSize=5` → `total=15, records.length=5` |
| | ② `pageNum=2&pageSize=5` → `total=15, records.length=5` |
| | ③ `pageNum=3&pageSize=5` → `total=15, records.length=5` |
| | ④ `pageNum=1&pageSize=20` → `total=15, records.length=15` |
| | ⑤ `pageNum=2&pageSize=20` → `total=15, records.length=0` |

---

### TC-3.4 没有修改记录的员工查询日志

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-3.4 |
| **用例名称** | 从未修改过档案的员工查询日志，返回空列表 |
| **前置条件** | 以某个无变更历史的员工登录 |
| **请求方式** | `GET /employee/profile/log/list?pageNum=1&pageSize=10` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回** | `data.total = 0`，`data.records = []` |

---

### TC-3.5 管理员/HR 查他人日志

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-3.5 |
| **用例名称** | 管理员传入他人 employeeId 查看日志 |
| **前置条件** | 以管理员/HR 登录 |
| **请求方式** | `GET /employee/profile/log/list?employeeId=15&pageNum=1&pageSize=10` |
| **预期状态码** | `code = 0 (SUCCESS)` |
| **预期返回** | 返回员工 15 的所有变更日志 |

---

### TC-3.6 普通员工越权查他人日志

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-3.6 |
| **用例名称** | 普通员工传别人 employeeId 查日志，被拦截 |
| **前置条件** | 以普通员工 A（employeeId=15）登录 |
| **请求方式** | `GET /employee/profile/log/list?employeeId=16&pageNum=1&pageSize=10` |
| **预期状态码** | `code = 50027 (PROFILE_PERMISSION_DENIED)` |
| **预期 message** | `"无权查看他人档案，仅可查看本人"` |

---

### TC-3.7 分页参数校验 — pageNum 为空/0/负数

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-3.7 |
| **用例名称** | pageNum 参数非法 |
| **前置条件** | 已登录 |
| **测试数据** ||
| | ① 不传 `pageNum` → 报错 |
| | ② `pageNum=0` → 报错 |
| | ③ `pageNum=-1` → 报错 |
| **预期状态码** | `code = 40000 (PARAMS_ERROR)` |
| **预期 message** | `"页码不能为空且必须大于0"` |

---

### TC-3.8 分页参数校验 — pageSize 为空/0/负数

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-3.8 |
| **用例名称** | pageSize 参数非法 |
| **前置条件** | 已登录 |
| **测试数据** ||
| | ① 不传 `pageSize` → 报错 |
| | ② `pageSize=0` → 报错 |
| | ③ `pageSize=-10` → 报错 |
| **预期状态码** | `code = 40000 (PARAMS_ERROR)` |
| **预期 message** | `"每页条数不能为空且必须大于0"` |

---

### TC-3.9 日志按时间倒序排列

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-3.9 |
| **用例名称** | 验证多条日志按 createTime 倒序返回 |
| **前置条件** | 连续修改 3 次（间隔 > 1 秒），确认日志条数 >= 3 |
| **请求方式** | `GET /employee/profile/log/list?pageNum=1&pageSize=10` |
| **预期返回** | records[0].createTime > records[1].createTime > records[2].createTime |

---

## 五、端到端集成测试

### TC-E2E-1 完整业务流程

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 普通员工 A 登录 | 获取 Session Cookie |
| 2 | `GET /employee/profile/getMyProfile` | 返回完整档案，敏感字段脱敏/隐藏 |
| 3 | 记录 `currentAddress` 原值 → `oldAddr` | - |
| 4 | `PUT /employee/profile/updateMyDetail`<br>`{"employeeId":15, "currentAddress":"新地址E2E"}` | `code=0` |
| 5 | `GET /employee/profile/getMyProfile` | `currentAddress = "新地址E2E"`，确认刷新成功 |
| 6 | `GET /employee/profile/log/list?pageNum=1&pageSize=5` | 能看到 `fieldName=currentAddress`、`oldValue=oldAddr`、`newValue=新地址E2E` 的记录 |
| 7 | `PUT /employee/profile/updateMyDetail`<br>`{"employeeId":15, "currentAddress":"新地址E2E"}` | `code=0`，但不产生新日志（值未变） |
| 8 | `GET /employee/profile/log/list?pageNum=1&pageSize=5` | 日志条数与步骤 6 一致，无新增 |

---

### TC-E2E-2 切换账号越权测试

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 普通员工 A 登录 | Session A |
| 2 | `GET /employee/profile/getMyProfile?employeeId=15` | 正常（查自己） |
| 3 | `GET /employee/profile/getMyProfile?employeeId=16` | `code=50027` 越权拦截 |
| 4 | `PUT /employee/profile/updateMyDetail` → `{"employeeId":16,...}` | `code=50028` 越权拦截 |
| 5 | 管理员登录 | Session Admin |
| 6 | `GET /employee/profile/getMyProfile?employeeId=15` | 正常，返回完整明文 |
| 7 | `PUT /employee/profile/updateMyDetail` → `{"employeeId":15,...}` | `code=0` 正常修改 |
| 8 | `GET /employee/profile/log/list?employeeId=15&pageNum=1&pageSize=10` | 正常，能看到员工 15 的日志 |

---

## 六、权限矩阵验证

| 测试场景 | 普通员工 | 管理员/HR |
|----------|:--------:|:---------:|
| 查看自己档案 | ✅ TC-1.1 | ✅ TC-1.5 |
| 查看他人档案（传 employeeId） | ❌ TC-1.3 (50027) | ✅ TC-1.4 |
| 修改自己档案 | ✅ TC-2.1 | ✅ TC-2.3 |
| 修改他人档案 | ❌ TC-2.6 (50028) | ✅ TC-2.12 |
| 查自己变更日志 | ✅ TC-3.1 | ✅ TC-3.1 |
| 查他人变更日志 | ❌ TC-3.6 (50027) | ✅ TC-3.5 |
| 已离职员工查看 | ✅ TC-1.8 | - |
| 已离职员工修改 | ❌ TC-2.7 (50026) | - |

---

## 七、数据脱敏验证

| 字段 | 普通员工 | 管理员/HR | 脱敏规则 | 验证用例 |
|------|----------|-----------|----------|----------|
| `phone` | `138****3800` | `13800138000` | 前三后四 | TC-1.1 / TC-1.4 |
| `idCard` | `4101********1234` | 完整明文 | 首四尾四 | TC-1.1 / TC-1.4 |
| `emergencyContactPhone` | `138****5678` | 完整明文 | 前三后四 | TC-1.1 / TC-1.4 |
| `baseSalary` | `null` | 数值 | 普通员工隐藏 | TC-1.1 / TC-1.4 |
| `bankAccount` | `null` | 数值 | 普通员工隐藏 | TC-1.1 / TC-1.4 |

---

## 八、变更日志完整性验证

| 验证项 | 规则 | 验证用例 |
|--------|------|----------|
| 单字段修改 → 1 条日志 | 仅 changed 字段生成日志 | TC-2.1 |
| 双字段修改 → 2 条日志 | 每个 changed 字段独立一条 | TC-2.2 |
| 三字段修改 → 3 条日志 | 每个 changed 字段独立一条 | TC-2.3 |
| 值未变化 → 0 条日志 | `!Objects.equals(oldVal, newVal)` | TC-2.5 |
| 日志记录旧值 | `oldValue` = 数据库原值 | TC-E2E-1 |
| 日志记录新值 | `newValue` = 前端传入值 | TC-E2E-1 |
| 日志记录操作人 | `operatorId` = 登录人 userId | TC-2.12 |
| 日志记录变更类型 | `changeType = "个人档案自主修改"` | TC-3.2 |
| 日志记录备注 | 根据字段自动填充 | TC-3.2 |

---

## 附录 A：错误码速查

| 错误码 | 枚举名 | message | 触发条件 |
|--------|--------|---------|----------|
| 0 | SUCCESS | ok | 正常返回 |
| 40000 | PARAMS_ERROR | 请求参数错误 | body 为 null、employeeId/pageNum/pageSize 校验失败 |
| 40100 | NOT_LOGIN_ERROR | 未登录 | Session 不存在或过期 |
| 40400 | NOT_FOUND_ERROR | 请求数据不存在 | 员工档案不存在 |
| 50026 | EMPLOYEE_RESIGNED | 已离职员工不可修改档案，仅可查看 | status=4 的员工调 updateMyDetail |
| 50027 | PROFILE_PERMISSION_DENIED | 无权查看他人档案，仅可查看本人 | 普通员工传别人 employeeId 查档案/日志 |
| 50028 | PROFILE_UPDATE_DENIED | 无权修改他人档案，仅可修改本人 | 普通员工传别人 employeeId 修改档案 |
| 50029 | PHONE_FORMAT_ERROR | 紧急联系人电话格式不正确，需为11位手机号 | emergencyContactPhone 不匹配正则 |

---

## 附录 B：接口速查表

| 接口 | 方法 | 路径 | 鉴权 | 分页 |
|------|------|------|:----:|:----:|
| 获取档案详情 | GET | `/employee/profile/getMyProfile` | ✅ | - |
| 修改可编辑信息 | PUT | `/employee/profile/updateMyDetail` | ✅ | - |
| 查询修改日志 | GET | `/employee/profile/log/list` | ✅ | ✅ |

---

> **共计测试用例**：**31 条**  
> - 接口1（获取档案）：9 条  
> - 接口2（修改档案）：13 条  
> - 接口3（查询日志）：9 条  
> - 端到端集成：2 条  
> - 权限矩阵：1 张表  
> - 脱敏验证：1 张表  
> - 日志完整性：1 张表
