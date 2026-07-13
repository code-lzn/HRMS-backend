# HRMS-测试用例-个人中心与审批中心

## 变更记录

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-13 | 1.0 | 初稿 | - |

---

# 一、个人中心

## 1.1 我的考勤

### 1.1.1 打卡

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-ATT-001 | 上班打卡 | 员工已登录，今日未打过上班卡 | POST /api/attendance/punch | `{"punchType": 0, "location": "公司"}` | code=0, 返回 AttendanceVO（含打卡时间、状态） |
| TC-ATT-002 | 下班打卡 | 员工已登录，今日已打上班卡但未打下班卡 | POST /api/attendance/punch | `{"punchType": 1, "location": "公司"}` | code=0, 返回 AttendanceVO |
| TC-ATT-003 | 重复上班打卡 | 今日已打过上班卡 | POST /api/attendance/punch | `{"punchType": 0}` | code≠0, 提示已打卡或操作错误 |
| TC-ATT-004 | 请求参数为空 | 员工已登录 | POST /api/attendance/punch | `null` | code≠0, PARAMS_ERROR |
| TC-ATT-005 | 未登录打卡 | 无登录态 | POST /api/attendance/punch | `{"punchType": 0}` | code≠0, NOT_LOGIN_ERROR |

### 1.1.2 今日打卡状态

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-ATT-006 | 查询今日状态（已打卡） | 员工今日已打卡 | GET /api/attendance/today | 无 | code=0, 返回打卡时间和状态 |
| TC-ATT-007 | 查询今日状态（未打卡） | 员工今日未打卡 | GET /api/attendance/today | 无 | code=0, punchInTime=null |
| TC-ATT-008 | 未登录查询 | 无登录态 | GET /api/attendance/today | 无 | code≠0, NOT_LOGIN_ERROR |

### 1.1.3 考勤日历

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-ATT-009 | 查询当月日历 | 员工已登录，当月有考勤记录 | GET /api/attendance/calendar | `month=2026-07` | code=0, 返回正常/迟到/请假/缺卡天数统计 + dailyStatus |
| TC-ATT-010 | 查询无记录月份 | 员工已登录，该月无考勤 | GET /api/attendance/calendar | `month=2026-01` | code=0, 各项统计为0, dailyStatus为空 |
| TC-ATT-011 | 未登录查询 | 无登录态 | GET /api/attendance/calendar | `month=2026-07` | code≠0, NOT_LOGIN_ERROR |

### 1.1.4 当月考勤记录

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-ATT-012 | 查询当月记录 | 员工已登录，当月有考勤数据 | GET /api/attendance/records | `month=2026-07` | code=0, 返回 List\<AttendanceVO\> |

---

## 1.2 我的请假

### 1.2.1 申请请假

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-LEAVE-001 | 正常申请年假 | 员工已登录，无重叠请假 | POST /api/attendance/leave/apply | `{"leaveType": 2, "startDate": "2026-07-20", "endDate": "2026-07-22", "reason": "年假休息"}` | code=0, 返回 LeaveVO, status=0(待审批) |
| TC-LEAVE-002 | 开始日期晚于结束日期 | 员工已登录 | POST /api/attendance/leave/apply | `{"leaveType": 2, "startDate": "2026-07-25", "endDate": "2026-07-22", "reason": "..."}` | code≠0, "开始日期不能晚于结束日期" |
| TC-LEAVE-003 | 请假时间与已有请假重叠 | 员工已有 2026-07-20~07-22 的请假 | POST /api/attendance/leave/apply | `{"leaveType": 1, "startDate": "2026-07-21", "endDate": "2026-07-23", "reason": "..."}` | code≠0, LEAVE_OVERLAP_ERROR |
| TC-LEAVE-004 | 请求参数为空 | 员工已登录 | POST /api/attendance/leave/apply | `null` | code≠0, PARAMS_ERROR |
| TC-LEAVE-005 | 未登录申请 | 无登录态 | POST /api/attendance/leave/apply | `{...}` | code≠0, NOT_LOGIN_ERROR |
| TC-LEAVE-006 | 各种请假类型 | 员工已登录 | POST /api/attendance/leave/apply | leaveType 依次取 0~6 | code=0, 每种类型均可正常提交 |

### 1.2.2 我的请假列表

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-LEAVE-007 | 查询我的请假列表 | 员工已登录，有多条请假记录 | GET /api/attendance/leave/my | 无 | code=0, 返回 List\<LeaveVO\>, 按创建时间倒序 |
| TC-LEAVE-008 | 查询无请假记录 | 员工已登录，无请假记录 | GET /api/attendance/leave/my | 无 | code=0, 返回空数组 |
| TC-LEAVE-009 | 未登录查询 | 无登录态 | GET /api/attendance/leave/my | 无 | code≠0, NOT_LOGIN_ERROR |

### 1.2.3 审批进度

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-LEAVE-010 | 查看待审批的进度 | 员工已登录，有一条待审批的请假 | GET /api/attendance/leave/{id}/progress | id=待审批请假ID | code=0, progressNodes[0]=提交申请(完成), progressNodes[1]=审批(进行中) |
| TC-LEAVE-011 | 查看已通过的进度 | 员工已登录，有一条已通过的请假 | GET /api/attendance/leave/{id}/progress | id=已通过请假ID | code=0, progressNodes[0]=提交申请(完成), progressNodes[1]=审批(完成,含审批人/意见) |
| TC-LEAVE-012 | 查看已拒绝的进度 | 员工已登录，有一条已拒绝的请假 | GET /api/attendance/leave/{id}/progress | id=已拒绝请假ID | code=0, progressNodes[1]=审批(完成,含审批人/拒绝意见) |
| TC-LEAVE-013 | 查看已撤销的进度 | 员工已登录，有一条已撤销的请假 | GET /api/attendance/leave/{id}/progress | id=已撤销请假ID | code=0, progressNodes[1]=审批(已跳过), comment="申请人已撤销此申请" |
| TC-LEAVE-014 | 查看不存在的请假 | 员工已登录 | GET /api/attendance/leave/99999/progress | 无 | code≠0, NOT_FOUND_ERROR |
| TC-LEAVE-015 | 查看他人的请假进度 | 员工A已登录，查看员工B的请假 | GET /api/attendance/leave/{B的请假ID}/progress | 无 | code≠0, NO_AUTH_ERROR |
| TC-LEAVE-016 | 未登录查看 | 无登录态 | GET /api/attendance/leave/{id}/progress | 无 | code≠0, NOT_LOGIN_ERROR |

### 1.2.4 取消申请

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-LEAVE-017 | 取消待审批的请假 | 员工已登录，有一条待审批请假 | POST /api/attendance/leave/cancel/{id} | id=待审批请假ID | code=0, 请假状态变为3(已撤销), 考勤状态还原 |
| TC-LEAVE-018 | 取消已通过的请假 | 员工已登录，有一条已通过请假 | POST /api/attendance/leave/cancel/{id} | id=已通过请假ID | code≠0, APPROVAL_NOT_PENDING_ERROR |
| TC-LEAVE-019 | 取消已拒绝的请假 | 员工已登录，有一条已拒绝请假 | POST /api/attendance/leave/cancel/{id} | id=已拒绝请假ID | code≠0, APPROVAL_NOT_PENDING_ERROR |
| TC-LEAVE-020 | 取消他人的请假 | 员工A已登录，取消员工B的请假 | POST /api/attendance/leave/cancel/{B的请假ID} | 无 | code≠0, NO_AUTH_ERROR |
| TC-LEAVE-021 | 取消不存在的请假 | 员工已登录 | POST /api/attendance/leave/cancel/99999 | 无 | code≠0, NOT_FOUND_ERROR |
| TC-LEAVE-022 | 未登录取消 | 无登录态 | POST /api/attendance/leave/cancel/{id} | 无 | code≠0, NOT_LOGIN_ERROR |

### 1.2.5 审批请假

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-LEAVE-023 | 审批通过 | 审批人已登录 | POST /api/attendance/leave/approve | `{"id": 请假ID, "result": 1, "comment": "同意"}` | code=0, status=1(已通过) |
| TC-LEAVE-024 | 审批拒绝 | 审批人已登录 | POST /api/attendance/leave/approve | `{"id": 请假ID, "result": 2, "comment": "不同意"}` | code=0, status=2(已拒绝), 考勤状态还原 |
| TC-LEAVE-025 | 重复审批 | 请假已被审批 | POST /api/attendance/leave/approve | `{"id": 请假ID, "result": 1}` | code≠0, APPROVAL_NOT_PENDING_ERROR |
| TC-LEAVE-026 | 请求参数为空 | 审批人已登录 | POST /api/attendance/leave/approve | `null` | code≠0, PARAMS_ERROR |

---

## 1.3 我的薪资

### 1.3.1 工资条列表

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-SAL-001 | 查询工资条列表 | 员工已登录，有工资数据 | GET /api/salary/slips | 无 | code=0, 返回 List\<SalarySlipVO\>, 按月倒序, 含月份/应发/应扣/实发/状态 |
| TC-SAL-002 | 查询无工资数据 | 员工已登录，无工资数据 | GET /api/salary/slips | 无 | code=0, 返回空数组 |
| TC-SAL-003 | 未登录查询 | 无登录态 | GET /api/salary/slips | 无 | code≠0, NOT_LOGIN_ERROR |
| TC-SAL-004 | 员工档案不存在 | 用户已登录但无对应employee记录 | GET /api/salary/slips | 无 | code≠0, NOT_FOUND_ERROR("员工档案不存在") |

### 1.3.2 工资条详情（二次验证）

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-SAL-005 | 密码正确查看详情 | 员工已登录 | POST /api/salary/slip/{id} | `{"password": "正确密码"}` | code=0, 返回完整 SalarySlipDetailVO（含收入项/扣除项/汇总） |
| TC-SAL-006 | 密码错误 | 员工已登录 | POST /api/salary/slip/{id} | `{"password": "错误密码"}` | code=50015, "密码验证失败" |
| TC-SAL-007 | 密码为空 | 员工已登录 | POST /api/salary/slip/{id} | `{}` 或 `{"password": ""}` | code≠0, PARAMS_ERROR |
| TC-SAL-008 | 请求体为空 | 员工已登录 | POST /api/salary/slip/{id} | `null` | code≠0, PARAMS_ERROR |
| TC-SAL-009 | 工资条不存在 | 员工已登录，密码正确 | POST /api/salary/slip/99999 | `{"password": "..."}` | code=50017, "工资条不存在" |
| TC-SAL-010 | 查看他人的工资条 | 员工A已登录，查看员工B的工资条 | POST /api/salary/slip/{B的工资条ID} | `{"password": "A的密码"}` | code≠0, NO_AUTH_ERROR |
| TC-SAL-011 | 未登录查看 | 无登录态 | POST /api/salary/slip/{id} | `{"password": "..."}` | code≠0, NOT_LOGIN_ERROR |

### 1.3.3 薪资趋势

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-SAL-012 | 查询薪资趋势（>=6条数据） | 员工已登录，有6条以上工资记录 | GET /api/salary/trend | 无 | code=0, 返回最近6条, 按时间正序, 含month/netSalary/grossSalary |
| TC-SAL-013 | 查询薪资趋势（<6条数据） | 员工已登录，仅有3条工资记录 | GET /api/salary/trend | 无 | code=0, 返回3条, 按时间正序 |
| TC-SAL-014 | 无工资记录 | 员工已登录，无工资数据 | GET /api/salary/trend | 无 | code=0, 返回空数组 |
| TC-SAL-015 | 数据顺序验证 | 员工已登录，有6条记录 | GET /api/salary/trend | 无 | 按月递增排列（最早在前、最晚在后），适合前端直接渲染折线图 |
| TC-SAL-016 | 未登录查询 | 无登录态 | GET /api/salary/trend | 无 | code≠0, NOT_LOGIN_ERROR |

---

## 1.4 账号安全

### 1.4.1 修改密码

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-SEC-001 | 正常修改密码 | 用户已登录，旧密码正确 | POST /api/account/changePassword | `{"oldPassword": "正确旧密码", "newPassword": "NewPass123", "confirmPassword": "NewPass123"}` | code=0, 密码更新成功 |
| TC-SEC-002 | 旧密码错误 | 用户已登录 | POST /api/account/changePassword | `{"oldPassword": "错误密码", "newPassword": "NewPass123", "confirmPassword": "NewPass123"}` | code=50018, "原密码错误" |
| TC-SEC-003 | 新密码与旧密码相同 | 用户已登录 | POST /api/account/changePassword | oldPassword=newPassword=相同密码 | code=50019, "新密码不能与旧密码相同" |
| TC-SEC-004 | 新密码与历史密码重复 | 用户已登录，近3次历史含该密码 | POST /api/account/changePassword | newPassword=某条历史密码 | code=50020, "新密码与近期使用过的密码重复" |
| TC-SEC-005 | 新密码长度不足8位 | 用户已登录 | POST /api/account/changePassword | `{"oldPassword": "...", "newPassword": "Abc123", "confirmPassword": "Abc123"}` | code≠0, "新密码长度不能少于8位" |
| TC-SEC-006 | 两次新密码不一致 | 用户已登录 | POST /api/account/changePassword | `{"oldPassword": "...", "newPassword": "NewPass123", "confirmPassword": "NewPass456"}` | code≠0, "两次输入的新密码不一致" |
| TC-SEC-007 | 参数为空 | 用户已登录 | POST /api/account/changePassword | `{"oldPassword": "", ...}` | code≠0, "密码不能为空" |
| TC-SEC-008 | 请求体为空 | 用户已登录 | POST /api/account/changePassword | `null` | code≠0, PARAMS_ERROR |
| TC-SEC-009 | 未登录修改 | 无登录态 | POST /api/account/changePassword | `{...}` | code≠0, NOT_LOGIN_ERROR |
| TC-SEC-010 | 历史密码限制验证 | 连续修改密码3次后，第4次设置回第1次密码 | POST /api/account/changePassword | newPassword=第1次密码 | code=50020, 因第1次密码仍在近3条历史中 |

### 1.4.2 绑定手机

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-SEC-011 | 绑定有效手机号 | 员工已登录，该手机号未被占用 | POST /api/account/bindPhone | `{"phone": "13912345678"}` | code=0, employee.phone 更新成功 |
| TC-SEC-012 | 手机号格式错误 | 员工已登录 | POST /api/account/bindPhone | `{"phone": "12345"}` | code≠0, "手机号格式不正确" |
| TC-SEC-013 | 手机号已被其他员工绑定 | 员工A已登录，该手机号已被员工B绑定 | POST /api/account/bindPhone | `{"phone": "员工B的手机号"}` | code=50021, "该手机号已被其他账号绑定" |
| TC-SEC-014 | 解绑手机（传空字符串） | 员工已登录，已有手机号 | POST /api/account/bindPhone | `{"phone": ""}` | code=0, employee.phone=null |
| TC-SEC-015 | 请求参数为空 | 员工已登录 | POST /api/account/bindPhone | `null` | code≠0, PARAMS_ERROR |
| TC-SEC-016 | 未登录操作 | 无登录态 | POST /api/account/bindPhone | `{"phone": "13912345678"}` | code≠0, NOT_LOGIN_ERROR |

### 1.4.3 登录日志

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-SEC-017 | 查询登录日志 | 用户已登录，有登录历史 | GET /api/account/loginLogs | 无 | code=0, 最近30条, 按时间倒序, 含IP/设备/登录方式/是否成功 |
| TC-SEC-018 | 登录成功自动记录 | 用户执行一次成功登录 | GET /api/account/loginLogs | 无 | 最新一条 isSuccess=1, failReason=null, loginTypeText="密码登录" |
| TC-SEC-019 | 登录失败自动记录 | 用户输入错误密码登录 | GET /api/account/loginLogs | 无 | 最新一条 isSuccess=0, failReason="密码错误" |
| TC-SEC-020 | 无登录历史 | 新用户已登录 | GET /api/account/loginLogs | 无 | code=0, 返回空数组 |
| TC-SEC-021 | 上限30条验证 | 用户超过30次登录后查询 | GET /api/account/loginLogs | 无 | 仅返回最近30条 |
| TC-SEC-022 | 未登录查询 | 无登录态 | GET /api/account/loginLogs | 无 | code≠0, NOT_LOGIN_ERROR |

---

# 二、审批中心

## 2.1 审批工作台 — 待办列表

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-APR-001 | 查询待审批列表（直接审批） | 当前人是某审批节点的审批人，状态为PENDING | GET /api/approval/pending | 无 | code=0, 返回 List\<ApprovalPendingVO\>, 含 recordId/detailId/businessTypeText/applicantName/currentNodeName |
| TC-APR-002 | 查询待审批列表（委托审批） | 当前人被委托人委托，委托有效期内，有对应业务类型的待审批 | GET /api/approval/pending | 无 | code=0, 返回委托的待审批项（与直接审批去重） |
| TC-APR-003 | 委托审批按业务类型过滤 | 委托仅限 LEAVE 类型，待审批含 LEAVE 和 PATCH_CLOCK | GET /api/approval/pending | 无 | 仅返回 LEAVE 类型的委托待审批 |
| TC-APR-004 | 委托范围=全部类型 | 委托 businessTypes=null | GET /api/approval/pending | 无 | 返回委托人所有类型的待审批项 |
| TC-APR-005 | 委托已过期 | 委托 endDate < 今天 | GET /api/approval/pending | 无 | 不返回该委托的待审批项 |
| TC-APR-006 | 委托已取消 | 委托 status=0 | GET /api/approval/pending | 无 | 不返回该委托的待审批项 |
| TC-APR-007 | 无待审批项 | 当前人没有待审批 | GET /api/approval/pending | 无 | code=0, 返回空数组 |
| TC-APR-008 | 未登录查询 | 无登录态 | GET /api/approval/pending | 无 | code≠0, NOT_LOGIN_ERROR |
| TC-APR-009 | 员工档案不存在 | 用户已登录但无对应employee记录 | GET /api/approval/pending | 无 | code≠0, "员工档案不存在" |
| TC-APR-010 | 待审批项去重 | 同一条审批记录有多个节点PENDING | GET /api/approval/pending | 无 | 同一 recordId 仅出现一次 |

---

## 2.2 审批详情

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-APR-011 | 查看审批详情（含节点历史） | 审批记录存在 | GET /api/approval/detail/{recordId} | recordId=有效ID | code=0, 返回 ApprovalDetailVO, 含 businessTypeText/statusText/currentStep/totalSteps/nodeHistory |
| TC-APR-012 | nodeHistory 按 stepOrder 升序 | 审批记录有多个节点 | GET /api/approval/detail/{recordId} | recordId | nodeHistory 按 stepOrder 从小到大排列 |
| TC-APR-013 | 查看代审批标记 | 某节点是被委托人审批 | GET /api/approval/detail/{recordId} | recordId | 该节点 isDelegated=1, delegatedByName 有值 |
| TC-APR-014 | 审批记录不存在 | 无该审批记录 | GET /api/approval/detail/99999 | 无 | code=50022, APPROVAL_NOT_FOUND |
| TC-APR-015 | 未登录查看 | 无登录态 | GET /api/approval/detail/{recordId} | 无 | code≠0, NOT_LOGIN_ERROR |

---

## 2.3 审批操作 — 通过

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-APR-016 | 审批通过（非最后节点） | 审批人有权限，detail PENDING | POST /api/approval/approve | `{"detailId": 201, "comment": "同意"}` | code=0, detail.action=APPROVE, record.currentStep+1 |
| TC-APR-017 | 审批通过（最后节点） | 审批人有权限，detail PENDING，currentStep=totalSteps | POST /api/approval/approve | `{"detailId": 202, "comment": "同意"}` | code=0, detail.action=APPROVE, record.status=APPROVED, record.finishedAt 不为空 |
| TC-APR-018 | 无审批意见（comment为空） | 审批人有权限 | POST /api/approval/approve | `{"detailId": 201}` | code=0, 审批通过, comment=null |
| TC-APR-019 | detailId 为空 | 审批人已登录 | POST /api/approval/approve | `{"comment": "同意"}` | code≠0, PARAMS_ERROR |
| TC-APR-020 | 请求体为空 | 审批人已登录 | POST /api/approval/approve | `null` | code≠0, PARAMS_ERROR |
| TC-APR-021 | 审批已处理的节点 | detail.action 不是 PENDING | POST /api/approval/approve | `{"detailId": 已处理的detailId}` | code=50023, APPROVAL_NOT_PENDING |
| TC-APR-022 | 无权限审批 | 当前人既不是审批人也不是有效被委托人 | POST /api/approval/approve | `{"detailId": 他人的detailId}` | code=50024, APPROVAL_NO_PERMISSION |
| TC-APR-023 | 被委托人代为审批 | 当前人是有效被委托人 | POST /api/approval/approve | `{"detailId": 委托人的detailId, "comment": "代审批通过"}` | code=0, detail.isDelegated=1, detail.delegatedBy=原审批人ID |
| TC-APR-024 | detail 不存在 | 审批人已登录 | POST /api/approval/approve | `{"detailId": 99999}` | code=50022, APPROVAL_NOT_FOUND |
| TC-APR-025 | 未登录操作 | 无登录态 | POST /api/approval/approve | `{...}` | code≠0, NOT_LOGIN_ERROR |

---

## 2.4 审批操作 — 拒绝

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-APR-026 | 审批拒绝 | 审批人有权限，detail PENDING | POST /api/approval/reject | `{"detailId": 201, "comment": "原因不充分"}` | code=0, detail.action=REJECT, record.status=REJECTED, record.finishedAt 不为空 |
| TC-APR-027 | 拒绝后状态不可恢复 | 已被拒绝的审批 | POST /api/approval/approve | `{"detailId": 已拒绝的detailId}` | code=50023, APPROVAL_NOT_PENDING |
| TC-APR-028 | 无权限拒绝 | 当前人无审批权限 | POST /api/approval/reject | `{"detailId": 他人的detailId}` | code=50024, APPROVAL_NO_PERMISSION |
| TC-APR-029 | 未登录操作 | 无登录态 | POST /api/approval/reject | `{...}` | code≠0, NOT_LOGIN_ERROR |

---

## 2.5 审批操作 — 转交

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-APR-030 | 审批转交 | 审批人有权限，目标审批人存在 | POST /api/approval/transfer | `{"detailId": 201, "targetUserId": 20, "comment": "由王五代审"}` | code=0, 原 detail.action=TRANSFER, 新 detail 创建(action=PENDING, approverId=20) |
| TC-APR-031 | 未指定转交目标人 | 审批人已登录 | POST /api/approval/transfer | `{"detailId": 201, "comment": "..."}` | code≠0, "请选择转交目标人" |
| TC-APR-032 | 目标审批人不存在 | 审批人已登录 | POST /api/approval/transfer | `{"detailId": 201, "targetUserId": 99999}` | code≠0, "目标审批人不存在" |
| TC-APR-033 | 无权转交 | 当前人无审批权限 | POST /api/approval/transfer | `{"detailId": 他人的detailId, "targetUserId": 20}` | code=50024, APPROVAL_NO_PERMISSION |
| TC-APR-034 | 转交后新审批人可审批 | 转交成功 | POST /api/approval/approve | 新审批人对新 detailId 操作 | code=0, 审批通过 |
| TC-APR-035 | 未登录操作 | 无登录态 | POST /api/approval/transfer | `{...}` | code≠0, NOT_LOGIN_ERROR |

---

## 2.6 委托审批

### 2.6.1 我的委托列表

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-DLG-001 | 查询我的委托 | 员工已登录，有生效中的委托 | GET /api/approval/delegation/my | 无 | code=0, 返回 List\<ApprovalDelegationVO\>, 仅 status=1 的委托 |
| TC-DLG-002 | 无委托记录 | 员工已登录，无委托 | GET /api/approval/delegation/my | 无 | code=0, 返回空数组 |
| TC-DLG-003 | 不返回已取消的委托 | 员工有 status=0 的委托 | GET /api/approval/delegation/my | 无 | 返回列表不含 status=0 的记录 |
| TC-DLG-004 | 委托倒序排列 | 员工有多条委托 | GET /api/approval/delegation/my | 无 | 按 createTime 倒序排列 |
| TC-DLG-005 | 未登录查询 | 无登录态 | GET /api/approval/delegation/my | 无 | code≠0, NOT_LOGIN_ERROR |

### 2.6.2 创建委托

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-DLG-006 | 创建委托（指定业务类型） | 员工已登录 | POST /api/approval/delegation | `{"delegateId": 20, "businessTypes": "LEAVE,PATCH_CLOCK", "startDate": "2026-07-01", "endDate": "2026-07-31"}` | code=0, 返回委托ID |
| TC-DLG-007 | 创建委托（全部业务类型） | 员工已登录 | POST /api/approval/delegation | `{"delegateId": 20, "startDate": "2026-07-01", "endDate": "2026-07-31"}` | code=0, businessTypes=null(全部) |
| TC-DLG-008 | 未选择被委托人 | 员工已登录 | POST /api/approval/delegation | `{"startDate": "2026-07-01", "endDate": "2026-07-31"}` | code≠0, "请选择被委托人" |
| TC-DLG-009 | 请求体为空 | 员工已登录 | POST /api/approval/delegation | `null` | code≠0, PARAMS_ERROR |
| TC-DLG-010 | 被委托人不存在 | 员工已登录 | POST /api/approval/delegation | `{"delegateId": 99999, ...}` | code≠0, "被委托人不存在" |
| TC-DLG-011 | 委托给自己 | 员工已登录 | POST /api/approval/delegation | `{"delegateId": 自己的employeeId, ...}` | code≠0, "不能委托给自己" |
| TC-DLG-012 | 开始日期晚于结束日期 | 员工已登录 | POST /api/approval/delegation | `{"delegateId": 20, "startDate": "2026-07-31", "endDate": "2026-07-01"}` | code≠0, "开始日期不能晚于结束日期" |
| TC-DLG-013 | 未指定日期范围 | 员工已登录 | POST /api/approval/delegation | `{"delegateId": 20}` | code≠0, "请选择委托日期范围" |
| TC-DLG-014 | 未登录操作 | 无登录态 | POST /api/approval/delegation | `{...}` | code≠0, NOT_LOGIN_ERROR |

### 2.6.3 取消委托

| 用例编号 | 测试场景 | 前置条件 | 请求方式 | 请求参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-DLG-015 | 委托人自己取消 | 委托人已登录 | POST /api/approval/delegation/cancel/{id} | id=生效中的委托ID | code=0, status 变为 0 |
| TC-DLG-016 | 非委托人取消 | 其他人已登录（非委托人） | POST /api/approval/delegation/cancel/{id} | id=他人的委托ID | code≠0, NO_AUTH_ERROR |
| TC-DLG-017 | 取消不存在的委托 | 员工已登录 | POST /api/approval/delegation/cancel/99999 | 无 | code=50025, DELEGATION_NOT_FOUND |
| TC-DLG-018 | 重复取消 | 委托已被取消 | POST /api/approval/delegation/cancel/{id} | id=已取消的委托ID | 第一次成功，之后的调用可能无变化或提示错误 |
| TC-DLG-019 | 未登录操作 | 无登录态 | POST /api/approval/delegation/cancel/{id} | 无 | code≠0, NOT_LOGIN_ERROR |

---

## 2.7 审批流引擎

### 2.7.1 startApproval（业务模块调用，非直接HTTP接口）

| 用例编号 | 测试场景 | 前置条件 | 调用方式 | 参数 | 预期结果 |
| --- | --- | --- | --- | --- | --- |
| TC-FLOW-001 | 正常发起审批 | 审批流定义已配置且启用 | approvalService.startApproval() | businessType="LEAVE", businessId=300, applicantEmployeeId=15, applicantName="张三" | 创建 record(status=APPROVING, currentStep=1) + 对应节点数的 detail(全部PENDING) |
| TC-FLOW-002 | 审批流未定义 | businessType 无对应审批流 | approvalService.startApproval() | businessType="UNKNOWN_TYPE" | 抛出异常 NOT_FOUND_ERROR |
| TC-FLOW-003 | 审批流已禁用 | 对应 flow.status=0 | approvalService.startApproval() | businessType=已禁用的类型 | 抛出异常 NOT_FOUND_ERROR |
| TC-FLOW-004 | 审批节点为空 | 对应 flow 无 flow_node | approvalService.startApproval() | businessType=无节点的类型 | 抛出异常 SYSTEM_ERROR |
| TC-FLOW-005 | DEPT_MANAGER 审批人解析 | 申请人有所属部门，部门有负责人 | approvalService.startApproval() | 节点 approverType=DEPT_MANAGER | detail.approverId=部门负责人ID |
| TC-FLOW-006 | DEPT_MANAGER 无部门 | 申请人无 departmentId | approvalService.startApproval() | 节点 approverType=DEPT_MANAGER | detail.approverId=null |
| TC-FLOW-007 | SPECIFIED 审批人解析 | 节点 approverType=SPECIFIED | approvalService.startApproval() | 节点 approverType=SPECIFIED, approverId=20 | detail.approverId=20 |

### 2.7.2 审批流推进

| 用例编号 | 测试场景 | 前置条件 | 操作 | 预期结果 |
| --- | --- | --- | --- | --- |
| TC-FLOW-008 | 单节点审批完成 | 1个节点，审批通过 | 审批通过 | record.status=APPROVED, finishedAt 有值 |
| TC-FLOW-009 | 多节点逐级推进 | 3个节点 | 依次通过3个节点 | currentStep: 1→2→3→APPROVED |
| TC-FLOW-010 | 中间节点通过后状态 | 3个节点，第1个通过 | 审批通过第1个 | currentStep=2, status=APPROVING |
| TC-FLOW-011 | 任意节点拒绝终止 | 3个节点，第1个拒绝 | 拒绝第1个 | status=REJECTED，后续节点不可再审批 |

---

## 2.8 委托审批路由

| 用例编号 | 测试场景 | 前置条件 | 操作 | 预期结果 |
| --- | --- | --- | --- | --- |
| TC-DLG-020 | 委托生效后被委托人可审批 | 员工2委托员工15审批LEAVE，有LEAVE待审批 | 员工15审批 | code=0, isDelegated=1, delegatedBy=2 |
| TC-DLG-021 | 委托日期范围外不可审批 | 委托 startDate=明天 | 被委托人审批 | code=50024, APPROVAL_NO_PERMISSION |
| TC-DLG-022 | 委托业务类型不匹配 | 委托仅限 LEAVE，待审批为 PATCH_CLOCK | 被委托人审批 | code=50024, APPROVAL_NO_PERMISSION |
| TC-DLG-023 | 取消委托后不可审批 | 委托已取消(status=0) | 被委托人审批 | code=50024, APPROVAL_NO_PERMISSION |

---

## 2.9 错误码验证

| 用例编号 | 错误码 | 触发条件 | 预期返回 |
| --- | --- | --- | --- |
| TC-ERR-001 | 50022 APPROVAL_NOT_FOUND | 操作不存在的审批明细/记录 | message="审批记录不存在" |
| TC-ERR-002 | 50023 APPROVAL_NOT_PENDING | 重复审批同一个节点 | message="该审批节点已处理" |
| TC-ERR-003 | 50024 APPROVAL_NO_PERMISSION | 无权限的人尝试审批 | message="无审批权限" |
| TC-ERR-004 | 50025 DELEGATION_NOT_FOUND | 取消不存在的委托 | message="委托记录不存在" |

---

## 附录：测试数据准备

### A. 个人中心测试数据

| 数据 | 说明 |
| --- | --- |
| 用户 | userId=2075829151662010370, 账号可正常登录 |
| 员工 | employeeId=15, 关联上述 userId |
| 考勤 | 确保 employeeId=15 在测试月有正常出勤、迟到、缺卡等不同状态的考勤记录 |
| 请假 | 确保 employeeId=15 有待审批、已通过、已拒绝、已撤销状态的请假记录 |
| 薪资 | sal_batch 至少 6 个月已通过(APPROVED)状态的数据，sal_batch_detail 有对应 employeeId=15 的明细 |
| 密码 | 确保 password_history 有 userId=2075829151662010370 的 3 条旧记录 |

### B. 审批中心测试数据

| 数据 | 说明 |
| --- | --- |
| 审批流定义 | 7 种审批类型均已初始化（ONBOARDING/REGULARIZATION/TRANSFER/RESIGNATION/LEAVE/PATCH_CLOCK/SALARY_BATCH） |
| 待审批记录 | LEAV(recordId=100) APPROVING, PATCH_CLOCK(recordId=101) APPROVING |
| 已审批记录 | REGULARIZATION(recordId=200) APPROVED, RESIGNATION(recordId=300) REJECTED |
| 委托关系 | employeeId=2 → employeeId=15, businessTypes="LEAVE,PATCH_CLOCK", 日期 2026-07-01~2026-07-31 |

---

> **测试环境**: `http://localhost:8123/api`, Content-Type: application/json, 除登录接口外均需携带 session cookie。
