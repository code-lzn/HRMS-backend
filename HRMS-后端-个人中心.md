# HRMS-后端-个人中心

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | - |
| 2026-07-12 | 1.1 | 根据实际代码实现更新：修正表结构、API路径、响应格式、密码加密方案 | - |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 9 部分——个人中心。当前员工无法自助查看个人档案、考勤记录、薪资明细，所有信息查询均需通过 HR 人工提供，效率低且体验差。本模块旨在为每位员工提供统一的个人信息门户，涵盖我的考勤（日历视图+打卡）、我的请假（申请列表+审批进度+取消）、我的薪资（历史工资条+趋势图+二次验证）、账号安全（密码修改+手机绑定+登录日志）等核心自助服务能力。本模块主要复用已有业务模块的后端能力，以个人视角聚合展示。

### 相关资料

- [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述个人中心涉及的功能与场景。

本模块定位为员工自助服务入口，功能上复用员工档案、考勤管理、薪资管理等模块的后端能力，以个人视角聚合展示：

1. **我的考勤**：日历视图标记每日出勤状态，上班/下班打卡（网页端），月度考勤记录查询
2. **我的请假**：请假申请、请假列表及当前状态、审批进度查看、取消申请（仅审批中状态可操作）
3. **我的薪资**：历史工资条列表（按月倒序），工资条详情查看（需二次密码验证），个人薪资趋势数据（近6个月）
4. **账号安全**：修改密码（旧密码验证+历史密码检查），绑定/解绑手机，登录日志查看（最近30条）

### 功能模块树

```plain
个人中心
├── 我的考勤
│   ├── 上班/下班打卡（POST /attendance/punch）
│   ├── 今日打卡状态（GET /attendance/today）
│   ├── 考勤日历视图（GET /attendance/calendar?month=）
│   └── 当月考勤记录（GET /attendance/records?month=）
├── 我的请假
│   ├── 请假申请（POST /attendance/leave/apply）
│   ├── 我的请假列表（GET /attendance/leave/my）
│   ├── 审批进度查看（GET /attendance/leave/{id}/progress）
│   ├── 取消申请（POST /attendance/leave/cancel/{id}）
│   └── 审批请假（POST /attendance/leave/approve）
├── 我的薪资
│   ├── 工资条列表（GET /salary/slips）
│   ├── 工资条详情 + 二次密码验证（POST /salary/slip/{id}）
│   └── 薪资趋势数据（GET /salary/trend）
└── 账号安全
    ├── 修改密码（POST /account/changePassword）
    ├── 绑定/解绑手机（POST /account/bindPhone）
    └── 登录日志查询（GET /account/loginLogs）
```

## 流程图

> 对个人中心涉及的核心流程进行梳理。

### 9-1 工资条查看流程

```plain
员工进入"我的薪资"页面
     │
     ▼
GET /salary/slips → 展示历史工资条列表（按月倒序）
     │
     ▼
点击某个工资条
     │
     ▼
POST /salary/slip/{id}  +  { "password": "..." }
     │
     ▼
后端校验密码（MD5(SALT+password) 比对 user.userPassword）：
     ├── 通过 → 返回 SalarySlipDetailVO
     └── 失败 → 返回 SALARY_VERIFY_FAILED (50015)
```

### 9-2 修改密码流程

```plain
员工进入"账号安全" → 修改密码
     │
     ▼
POST /account/changePassword
Body: { oldPassword, newPassword, confirmPassword }
     │
     ▼
后端校验：
     ├── 参数非空校验
     ├── 新密码长度 >= 8位
     ├── 新密码与确认密码一致
     ├── 旧密码是否正确？（MD5(SALT+oldPassword) == user.userPassword）
     ├── 新密码是否与旧密码相同？→ PASSWORD_SAME_AS_OLD (50019)
     └── 新密码是否与近3次历史密码重复？→ PASSWORD_RECENTLY_USED (50020)
     │
     ▼
保存旧密码到 password_history（每人仅保留最近3条）
     │
     ▼
更新 user.userPassword = MD5(SALT+newPassword)
     │
     ▼
返回成功
```

> **注意**: 当前实现未做"修改密码后强制所有设备重新登录"（session 清除），如需该功能可后续加入。

## 数据库设计

> 个人中心主要复用已有模块数据表，仅新增以下安全及薪资相关表。

### 登录日志表 login_log

```sql
CREATE TABLE IF NOT EXISTS login_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    userId              BIGINT          NOT NULL                COMMENT '用户ID',
    loginTime           DATETIME        NOT NULL                COMMENT '登录时间',
    ip                  VARCHAR(45)     NOT NULL                COMMENT '登录IP',
    device              VARCHAR(256)    DEFAULT NULL            COMMENT '设备信息（User-Agent）',
    loginType           TINYINT         NOT NULL                COMMENT '登录方式：1=密码登录 2=短信验证码登录',
    isSuccess           TINYINT         NOT NULL DEFAULT 1      COMMENT '是否成功：0=失败 1=成功',
    failReason          VARCHAR(128)    DEFAULT NULL            COMMENT '失败原因',
    PRIMARY KEY (id),
    KEY idx_user_id (userId),
    KEY idx_login_time (loginTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
```

### 密码历史表 password_history

```sql
CREATE TABLE IF NOT EXISTS password_history (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    userId              BIGINT          NOT NULL                COMMENT '用户ID',
    passwordHash        VARCHAR(128)    NOT NULL                COMMENT '历史密码哈希',
    createTime          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id_time (userId, createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码历史表';
```

> 每个用户仅保留最近 3 条密码历史，多余的旧记录在保存新记录时自动清除。

### 薪资核算批次表 sal_batch

```sql
CREATE TABLE IF NOT EXISTS sal_batch (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batchNo                 VARCHAR(32)     NOT NULL                 COMMENT '批次号',
    salaryMonth             VARCHAR(7)      NOT NULL                 COMMENT '薪资月份: YYYY-MM',
    status                  VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT=草稿, PENDING_CONFIRM=待确认, APPROVING=审批中, APPROVED=已通过, PAID=已发放',
    totalEmployeeCount      INT             NOT NULL DEFAULT 0       COMMENT '核算员工总数',
    totalGross              DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '应发工资总额',
    totalDeduction          DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '扣除总额',
    totalNet                DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '实发工资总额',
    createdBy               BIGINT          NOT NULL                 COMMENT '创建人ID',
    approvedBy              BIGINT          DEFAULT NULL             COMMENT '审批人ID',
    paidAt                  DATETIME        DEFAULT NULL             COMMENT '实际发放时间',
    createdAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updatedAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batchNo),
    UNIQUE KEY uk_salary_month (salaryMonth),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算批次表';
```

### 薪资核算明细表（工资条） sal_batch_detail

```sql
CREATE TABLE IF NOT EXISTS sal_batch_detail (
    id                      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    batchId                 BIGINT          NOT NULL                 COMMENT '批次ID',
    employeeId              BIGINT          NOT NULL                 COMMENT '员工ID',
    baseSalary              DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '基本工资',
    allowance               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '岗位津贴',
    performanceBonus        DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '绩效奖金',
    overtimePay             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '加班费',
    lateDeduction           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '迟到扣款',
    leaveDeduction          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '请假扣款',
    socialPension           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '养老保险',
    socialMedical           DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '医疗保险',
    socialUnemployment      DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '失业保险',
    housingFund             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '住房公积金',
    incomeTax               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '个人所得税',
    grossSalary             DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '应发工资',
    totalDeduction          DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '应扣合计',
    netSalary               DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '实发工资',
    hasAnomaly              TINYINT         NOT NULL DEFAULT 0       COMMENT '是否有异常: 0=正常, 1=预警, 2=阻断',
    anomalyReason           VARCHAR(256)    DEFAULT NULL             COMMENT '异常说明',
    manualAdjust            DECIMAL(12,2)   NOT NULL DEFAULT 0.00    COMMENT '手动调整金额',
    adjustReason            VARCHAR(256)    DEFAULT NULL             COMMENT '手动调整原因',
    createdAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updatedAt               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_batch_id (batchId),
    KEY idx_employee_id (employeeId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资核算明细表（工资条）';
```

## API 设计

### 1. 我的考勤

> 基础路径: `/attendance`

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| /attendance/punch | POST | 上班/下班打卡 |
| /attendance/today | GET | 获取今日打卡状态 |
| /attendance/calendar?month= | GET | 考勤日历视图 |
| /attendance/records?month= | GET | 当月考勤记录列表 |

#### 打卡请求 (PunchRequest)

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| punchType | Integer | 是 | 0=上班打卡, 1=下班打卡 |
| location | String | 否 | 打卡位置 |

#### 考勤日历响应 (AttendanceCalendarVO)

```json
{
  "code": 0,
  "data": {
    "month": "2026-07",
    "normalDays": 5,
    "lateDays": 0,
    "leaveDays": 0,
    "missingDays": 0,
    "dailyStatus": {
      "2026-07-03": 2,
      "2026-07-06": 0,
      "2026-07-07": 0
    },
    "makeupAvailableDates": []
  }
}
```

---

### 2. 我的请假

> 基础路径: `/attendance/leave`

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| /attendance/leave/apply | POST | 申请请假 |
| /attendance/leave/approve | POST | 审批请假（审批人操作） |
| /attendance/leave/my | GET | 获取我的请假记录列表 |
| /attendance/leave/cancel/{id} | POST | 取消申请（仅审批中状态） |
| /attendance/leave/{id}/progress | GET | 查看审批进度 |

#### 请假申请请求 (LeaveApplyRequest)

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| leaveType | Integer | 是 | 0=事假 1=病假 2=年假 3=婚假 4=产假 5=丧假 6=调休 |
| startDate | String | 是 | 开始日期 (yyyy-MM-dd) |
| endDate | String | 是 | 结束日期 (yyyy-MM-dd) |
| reason | String | 是 | 请假原因 |

#### 请假记录响应 (LeaveVO)

```json
{
  "id": 500,
  "employeeId": 15,
  "employeeName": "张三",
  "leaveType": 2,
  "leaveTypeText": "年假",
  "startDate": "2026-07-15",
  "endDate": "2026-07-17",
  "totalDays": 3.0,
  "reason": "家庭事务",
  "status": 0,
  "statusText": "待审批",
  "approverId": null,
  "approveTime": null,
  "approveComment": null,
  "createTime": "2026-07-10T09:00:00"
}
```

#### 审批进度响应 (LeaveProgressVO)

```json
{
  "code": 0,
  "data": {
    "leave": { "...": "LeaveVO 对象" },
    "progressNodes": [
      {
        "nodeName": "提交申请",
        "status": 0,
        "operatorName": "张三",
        "operateTime": "2026-07-10T09:00:00",
        "comment": "家庭事务"
      },
      {
        "nodeName": "审批",
        "status": 1,
        "operatorName": null,
        "operateTime": null,
        "comment": null
      }
    ]
  }
}
```

**节点状态 (status)**:
| 值 | 含义 |
| --- | --- |
| 0 | 已完成 |
| 1 | 进行中 |
| 2 | 未开始/已跳过 |

---

### 3. 我的薪资

> 基础路径: `/salary`

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| /salary/slips | GET | 获取我的工资条列表（按月倒序） |
| /salary/slip/{id} | POST | 查看工资条详情（需密码验证） |
| /salary/trend | GET | 获取近6个月薪资趋势 |

#### 工资条列表响应 (SalarySlipVO)

```json
{
  "code": 0,
  "data": [
    {
      "id": 1001,
      "salaryMonth": "2026-07",
      "batchStatus": "APPROVED",
      "grossSalary": 15000.00,
      "totalDeduction": 3200.00,
      "netSalary": 11800.00,
      "hasAnomaly": 0
    }
  ]
}
```

#### 工资条详情 (SalarySlipDetailVO)

请求体: `{ "password": "用户登录密码" }`

```json
{
  "code": 0,
  "data": {
    "id": 1001,
    "salaryMonth": "2026-07",
    "employeeName": "张三",
    "employeeNo": "EMP001",
    "baseSalary": 12000.00,
    "allowance": 2000.00,
    "performanceBonus": 1000.00,
    "overtimePay": 0.00,
    "manualAdjust": 0.00,
    "adjustReason": null,
    "grossSalary": 15000.00,
    "lateDeduction": 0.00,
    "leaveDeduction": 0.00,
    "socialPension": 1200.00,
    "socialMedical": 300.00,
    "socialUnemployment": 150.00,
    "housingFund": 1200.00,
    "incomeTax": 350.00,
    "totalDeduction": 3200.00,
    "netSalary": 11800.00
  }
}
```

**验证逻辑**: 使用 `MD5(SALT + password)` 与 `user.userPassword` 比对，SALT = "limou"。

**验证失败**: 返回错误码 50015 (SALARY_VERIFY_FAILED, "密码验证失败")。

> **注意**: 当前仅支持登录密码验证，未实现短信验证码方式。未实现暴力破解锁定（50016 错误码已预留但未使用）。

#### 薪资趋势响应 (SalaryTrendVO)

```json
{
  "code": 0,
  "data": [
    { "month": "2026-02", "netSalary": 11500.00, "grossSalary": 14800.00 },
    { "month": "2026-03", "netSalary": 11800.00, "grossSalary": 15000.00 },
    { "month": "2026-04", "netSalary": 11600.00, "grossSalary": 15000.00 },
    { "month": "2026-05", "netSalary": 12000.00, "grossSalary": 15200.00 },
    { "month": "2026-06", "netSalary": 11800.00, "grossSalary": 15000.00 },
    { "month": "2026-07", "netSalary": 11800.00, "grossSalary": 15000.00 }
  ]
}
```

> 取最近 6 条工资条记录，按时间正序返回（适合直接渲染折线图）。

---

### 4. 账号安全

> 基础路径: `/account`

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| /account/changePassword | POST | 修改密码 |
| /account/bindPhone | POST | 绑定/解绑手机 |
| /account/loginLogs | GET | 登录日志（最近30条） |

#### 修改密码请求 (ChangePasswordRequest)

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| oldPassword | String | 是 | 旧密码 |
| newPassword | String | 是 | 新密码（>=8位） |
| confirmPassword | String | 是 | 确认新密码（需与 newPassword 一致） |

**校验规则**:
1. 参数非空
2. newPassword.length() >= 8
3. newPassword.equals(confirmPassword)
4. MD5(SALT+oldPassword) == user.userPassword → 否则 PASSWORD_ERROR (50018)
5. MD5(SALT+newPassword) != user.userPassword → 否则 PASSWORD_SAME_AS_OLD (50019)
6. 新密码哈希不在 password_history 近 3 条中 → 否则 PASSWORD_RECENTLY_USED (50020)

**成功**: 旧密码存入 password_history → 更新 user.userPassword → 返回 true

#### 绑定手机请求 (BindPhoneRequest)

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| phone | String | 是 | 手机号（传空字符串或 null 表示解绑） |

**校验规则**:
1. 非空时需匹配正则 `^1[3-9]\d{9}$`
2. 手机号不能被其他员工已绑定 → 否则 PHONE_ALREADY_BOUND (50021)

**存储位置**: `employee.phone` 字段（非 user 表）

#### 登录日志响应 (LoginLogVO)

```json
{
  "code": 0,
  "data": [
    {
      "id": 18,
      "loginTime": "2026-05-25T14:32:00",
      "ip": "114.25.66.78",
      "device": "Mozilla/5.0 Chrome/121 Android",
      "loginType": 1,
      "loginTypeText": "密码登录",
      "isSuccess": 1,
      "failReason": null
    }
  ]
}
```

**登录日志记录机制**: 在 `UserServiceImpl.userLogin()` 方法中直接调用 `loginLogService.recordLoginLog()`，不管是登录成功还是密码错误都会记录。登录类型目前固定为 1（密码登录）。

---

## 关键技术设计

### 密码加密方案

```java
private static final String SALT = "limou";

// 加密
String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
// 结果示例: e10adc3949ba59abbe56e057f20f883e (SALT+"12345678" 的 MD5)
```

> 使用 MD5 + 固定 SALT，非 BCrypt。

### 薪资二次验证

```java
public SalarySlipDetailVO getSalarySlipDetail(Long detailId, Long userId, String password) {
    User user = userService.getById(userId);
    String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    
    // 比对密码
    ThrowUtils.throwIf(!Objects.equals(user.getUserPassword(), encryptPassword),
            ErrorCode.SALARY_VERIFY_FAILED);  // 50015
    
    // 校验工资条存在且属于当前员工
    SalarySlip slip = this.getById(detailId);
    ThrowUtils.throwIf(slip == null, ErrorCode.SALARY_NOT_FOUND);  // 50017
    Employee emp = getEmployee(userId);
    ThrowUtils.throwIf(!Objects.equals(slip.getEmployeeId(), emp.getId()),
            ErrorCode.NO_AUTH_ERROR);
    
    // 组装返回 SalarySlipDetailVO...
}
```

> 当前每次查看都需要传入密码，没有 session 级别的验证标记（未实现"10分钟内免验证"）。

### 密码修改

```java
public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
    // 1. 基础校验（非空、长度、一致）
    // 2. 验证旧密码
    String oldHash = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes());
    if (!Objects.equals(user.getUserPassword(), oldHash)) {
        throw new BusinessException(ErrorCode.PASSWORD_ERROR);  // 50018
    }
    // 3. 新密码不能与旧密码相同
    String newHash = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());
    if (Objects.equals(user.getUserPassword(), newHash)) {
        throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD);  // 50019
    }
    // 4. 检查近3次历史
    if (passwordHistoryService.isRecentlyUsed(userId, newHash)) {
        throw new BusinessException(ErrorCode.PASSWORD_RECENTLY_USED);  // 50020
    }
    // 5. 保存旧密码到历史 + 更新密码
    passwordHistoryService.savePasswordHistory(userId, user.getUserPassword());
    user.setUserPassword(newHash);
    this.updateById(user);
}
```

### 手机绑定

手机号存储在 `employee.phone` 字段（非 `user` 表）。更新时通过 `employeeService.lambdaQuery().eq(Employee::getUserId, userId)` 找到员工记录后修改。

校验逻辑：
- 格式校验：`^1[3-9]\d{9}$`
- 唯一性校验：检查其他员工是否已绑定该手机号
- 传空字符串 = 解绑（将 phone 设为 null）

### 登录日志记录

在 `UserServiceImpl.userLogin()` 方法中直接记录（非 AOP 切面）：

```java
// 登录失败（密码错误但账号存在）
loginLogService.recordLoginLog(existUser.getId(),
    request.getRemoteAddr(), request.getHeader("User-Agent"),
    1, false, "密码错误");

// 登录成功
loginLogService.recordLoginLog(user.getId(),
    request.getRemoteAddr(), request.getHeader("User-Agent"),
    1, true, null);
```

### 审批进度构建

`LeaveServiceImpl.getApprovalProgress()` 构建两个节点的进度：
1. **提交申请** (status=0 已完成)：操作人=申请人，时间=创建时间
2. **审批** (status 根据实际状态):
   - PENDING → status=1 (进行中)
   - CANCELLED → status=2 (已跳过)，提示"申请人已撤销此申请"
   - 其他（已通过/已拒绝）→ status=0 (已完成)，含审批人信息和意见

## 错误码

| **错误码** | **常量** | **说明** |
| --- | --- | --- |
| 50015 | SALARY_VERIFY_FAILED | 密码验证失败（工资条二次验证） |
| 50016 | SALARY_VERIFY_LOCKED | 验证次数过多（已预留，当前未使用） |
| 50017 | SALARY_NOT_FOUND | 工资条不存在 |
| 50018 | PASSWORD_ERROR | 原密码错误 |
| 50019 | PASSWORD_SAME_AS_OLD | 新密码不能与旧密码相同 |
| 50020 | PASSWORD_RECENTLY_USED | 新密码与近期使用过的密码重复 |
| 50021 | PHONE_ALREADY_BOUND | 该手机号已被其他账号绑定 |

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审个人中心功能范围，确认安全策略 | 0.5天 |
| 技术方案 | 完成系分文档评审，确认 API 设计与安全方案 | 1天 |
| 数据库开发 | 新增 login_log、password_history、sal_batch、sal_batch_detail 表 | 0.5天 |
| 后端开发 | 考勤打卡/日历、请假CRUD+审批进度、工资条列表/详情+二次验证+趋势、密码修改/手机绑定/登录日志 | 4天 |
| 前端开发 | 考勤日历+打卡、请假列表/详情/进度、工资条列表/详情/趋势图、密码修改页、登录日志页 | 4天 |
| 联调测试 | 前后端联调、安全测试（密码策略/二次验证） | 2天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 1.5天 |

> **总预估工期**：约 13.5 个工作日
