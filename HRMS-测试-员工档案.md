# HRMS-测试-员工档案管理模块

## 变更记录

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-13 | 1.0 | 初稿 | - |
| 2026-07-14 | 1.1 | 修正 API 路径（/employees → /employee）、移除不存在的工号/导出端点、修正参数名 | - |

## 一、测试范围

| 模块 | 接口数 | 说明 |
|---|---|---|
| 员工管理 | 6 | 列表查询、详情、新增、更新、删除、变更历史 |

## 二、测试环境

| 项目 | 说明 |
|---|---|
| 基础路径 | `http://localhost:8123/api` |
| 接口文档 | `http://localhost:8123/api/doc.html` |
| 数据库 | MySQL `szml` |
| 前置条件 | employee 表已建；department、position 表有初始化数据 |

---

## 三、员工管理测试用例

### 3.1 查询员工列表 — `GET /api/employee/list`

| # | 用例名称 | 操作 | 预期结果 |
|---|---|---|---|
| TC-E-001 | 正常分页查询 | `?page=1&size=10` | code=0，返回 `total`、`page`、`size`、`records` |
| TC-E-002 | 默认分页 | 不传 page/size | 使用默认值（page=1, size=20） |
| TC-E-003 | 关键词搜索 | `?keyword=张三` | 返回姓名或工号匹配"张三"的员工 |
| TC-E-004 | 关键词搜索工号 | `?keyword=2025` | 返回工号包含"2025"的员工 |
| TC-E-005 | 按部门筛选 | `?departmentIds=7` | 只返回 departmentId=7 的员工 |
| TC-E-006 | 按职位筛选 | `?positionIds=3` | 只返回 positionId=3 的员工 |
| TC-E-007 | 按状态筛选 | `?statuses=1` | 只返回在职员工 |
| TC-E-008 | 按离职状态 | `?statuses=4` | 只返回已离职员工 |
| TC-E-009 | 组合筛选 | `?departmentIds=7&statuses=2&keyword=李` | 同时满足所有条件 |
| TC-E-010 | 无匹配结果 | `?keyword=不存在的名字` | code=0，`total=0`，`records=[]` |
| TC-E-011 | 部门名正确 | 正常查询 | `departmentName` 为部门表对应名称 |
| TC-E-012 | 职位名正确 | 正常查询 | `positionName` 为职位表对应名称 |
| TC-E-013 | 大数据分页 | `?page=100&size=50` | 正常返回或空数组 |

### 3.2 查询员工详情 — `GET /api/employee/detail`

| # | 用例名称 | 操作 | 预期结果 |
|---|---|---|---|
| TC-E-020 | 正常查询 | `GET ?id=101` | code=0，返回完整员工信息 |
| TC-E-021 | 返回部门名 | 正常查询 | `departmentName` 不为空 |
| TC-E-022 | 返回职位名 | 正常查询 | `positionName` 不为空 |
| TC-E-023 | 身份证脱敏 | 正常查询 | `idCard` 格式：`3301**********1234` |
| TC-E-024 | 紧急联系人脱敏 | 正常查询 | `emergencyContactPhone` 中间脱敏 |
| TC-E-025 | 查询不存在员工 | `GET ?id=99999` | code≠0，提示"员工不存在" |
| TC-E-026 | 查询已删除员工 | 员工已软删除 | code≠0，提示"员工不存在" |
| TC-E-027 | 参数非法 | `GET ?id=0` 或不传 | code=40000 |

### 3.3 新增员工 — `POST /api/employee/add`

| # | 用例名称 | 操作 | 预期结果 |
|---|---|---|---|
| TC-E-030 | 正常新增 | `{employeeName:"张三", gender:1, phone:"13800000001", departmentId:7, positionId:3, employmentType:"FULL_TIME"}` | code=0，返回新员工 `id` |
| TC-E-031 | 最少必填字段 | 只传必填字段（姓名+性别+手机号+录用类型+部门+职位） | code=0 |
| TC-E-032 | 全部字段 | 传所有可选字段 | code=0，所有字段写入 |
| TC-E-033 | 姓名为空 | `{employeeName:"", ...}` | code≠0，提示"姓名不能为空" |
| TC-E-034 | 部门不存在 | `{..., departmentId:99999}` | code≠0，提示"部门不存在" |
| TC-E-035 | 部门不传 | 不传 `departmentId` | code≠0，提示"部门必填" |
| TC-E-036 | 职位不存在 | `{..., positionId:99999}` | code≠0，提示"职位不存在" |
| TC-E-037 | 工号自动生成 | 正常新增 → 查详情 | 详情中 `employeeNo` 格式：`年份(4) + 部门编码(2) + 序号(3)` |
| TC-E-038 | 工号唯一 | 连续新增两个同部门员工 → 查详情对比 | 工号序号递增，不重复 |
| TC-E-039 | 性别边界 | `gender:0`（女） | code=0 |
| TC-E-040 | 录用类型枚举 | `employmentType:"PART_TIME"` | code=0 |
| TC-E-041 | 非法录用类型 | `employmentType:"INVALID"` | 视校验规则决定 |
| TC-E-042 | 超长姓名 | employeeName 超 64 字符 | code≠0 |
| TC-E-043 | 邮箱格式 | `email:"not-an-email"` | 视校验规则，建议前端拦截 |

### 3.4 更新员工 — `PUT /api/employee/update`

| # | 用例名称 | 操作 | 预期结果 |
|---|---|---|---|
| TC-E-050 | 正常更新 | `{id:101, email:"new@example.com"}` | code=0，更新成功 |
| TC-E-051 | 更新部门（锁定字段） | `{id:101, departmentId:8}` | code≠0，部门为锁定字段，需走调岗流程 |
| TC-E-052 | 更新职位（锁定字段） | `{id:101, positionId:4}` | code≠0，职位为锁定字段，需走调岗流程 |
| TC-E-053 | 更新姓名 | `{id:101, employeeName:"张四"}` | code=0 |
| TC-E-054 | 更新部门不存在 | `{id:101, departmentId:99999}` | code≠0 |
| TC-E-055 | 更新职位不存在 | `{id:101, positionId:99999}` | code≠0 |
| TC-E-056 | 员工不存在 | `{id:99999}` | code≠0，提示"员工不存在" |
| TC-E-057 | 不传 id | 请求体不含 id | code=40000 |
| TC-E-058 | 工号不可修改 | 不传 employeeNo 字段 | 更新不影响工号 |
| TC-E-059 | 部分字段更新 | 只传需要改的字段 | 其他字段不受影响 |

### 3.5 删除员工 — `POST /api/employee/delete`

| # | 用例名称 | 操作 | 预期结果 |
|---|---|---|---|
| TC-E-070 | 正常删除 | `{id: 目标id}` | code=0，软删除 |
| TC-E-071 | 删除后列表不显示 | 已删除 | `GET /list` | records 中不包含该员工 |
| TC-E-072 | 删除后详情查不到 | 已删除 | `GET /detail?id=已删除id` | code≠0，提示"员工不存在" |
| TC-E-073 | 员工不存在 | `{id:99999}` | code≠0，提示"员工不存在" |
| TC-E-074 | 重复删除 | 对已删除员工再次删除 | code≠0，提示"员工不存在" |

> 注：工号生成（`generateEmployeeNo`）和字段权限（`getFieldPermissions`）已改为 Service 内部方法，不对外暴露 Controller 端点。

---

## 四、集成测试场景

| # | 场景 | 步骤 | 验证点 |
|---|---|---|---|
| TC-I-001 | 完整员工生命周期 | 新增→查详情→编辑→查列表→删除→查详情 | 每步数据一致，删除后不可查 |
| TC-I-002 | 部门关联一致性 | 部门改名称→查员工详情 | `departmentName` 同步更新 |
| TC-I-003 | 职位关联一致性 | 职位改名称→查员工列表 | `positionName` 同步更新 |
| TC-I-004 | 部门合并后员工变化 | A部门员工→合并AB→查员工 | 原A部门员工的 `departmentId` 变为 B |
| TC-I-005 | 员工删除后部门人数 | 部门有5名员工→删除1名→查部门树 | `employeeCount` 减 1 |
| TC-I-006 | 工号跨年 | 模拟不同年份新增员工 | 工号年份正确 |

---

## 五、边界与异常测试

| # | 用例 | 操作 | 预期 |
|---|---|---|---|
| TC-EX-001 | 超长名称 | employeeName 超 128 字符 | code≠0 |
| TC-EX-002 | 非法 gender 值 | `gender:9` | 视校验规则 |
| TC-EX-003 | 非法 status | `status:99` | 筛选时忽略或报错 |
| TC-EX-004 | 超大页码 | `?page=99999` | 返回空数组，不报错 |
| TC-EX-005 | SQL 注入 | `?keyword=' OR 1=1 --` | 安全过滤，不影响查询 |
| TC-EX-006 | 空请求体 | `POST /add` body 为空 | code=40000 |
| TC-EX-007 | 并发新增同部门 | 两个请求同时新增 | 工号不重复 |

---

## 六、与组织架构模块联测

| # | 场景 | 验证点 |
|---|---|---|
| TC-CROSS-01 | 部门有员工时不可删除 | 部门删除接口拒绝 |
| TC-CROSS-02 | 职位有员工引用时不可删除 | 职位删除接口拒绝 |
| TC-CROSS-03 | 部门合并后员工部门更新 | 员工 departmentId 正确变更 |
| TC-CROSS-04 | 新增员工选部门下拉 | 数据源来自部门树接口 |
| TC-CROSS-05 | 新增员工选职位下拉 | 数据源来自职位列表接口 |

---

## 七、测试用例统计

| 模块 | 用例数 |
|---|---|
| 员工列表 | 13 |
| 员工详情 | 8 |
| 新增员工 | 14 |
| 更新员工 | 10 |
| 删除员工 | 5 |
| 集成场景 | 6 |
| 边界异常 | 7 |
| 跨模块联测 | 5 |
| **合计** | **68** |
