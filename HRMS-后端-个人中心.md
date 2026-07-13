# HRMS-后端-个人中心

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | - |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 9 部分——个人中心。当前员工无法自助查看个人档案、考勤记录、薪资明细，所有信息查询均需通过 HR 人工提供，效率低且体验差。本模块旨在为每位员工提供统一的个人信息门户，涵盖我的档案（查看+编辑可修改字段）、我的考勤（日历视图+打卡+请假+补卡）、我的请假（申请列表+审批进度）、我的薪资（历史工资条+趋势图）、账号安全（密码修改+手机绑定+登录日志）等核心自助服务能力。本模块主要复用已有业务模块的后端能力，以个人视角聚合展示。

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

1. **我的档案**：查看个人基础信息（敏感字段脱敏），编辑可修改字段（邮箱/现居住地址/紧急联系人等），锁定字段（部门/职位/薪资等）显示但不可编辑并提示"如需修改请联系HR"
2. **我的考勤**：日历视图标记每日出勤状态，打卡按钮（网页端），请假申请快捷入口，补卡申请入口
3. **我的请假**：请假列表及当前状态，审批进度时间线展示，取消申请（仅审批中状态可操作）
4. **我的薪资**：历史工资条列表（按月），工资条详情查看（需二次验证），个人薪资趋势折线图（AntV 近6个月实发工资）
5. **账号安全**：修改密码（需验证旧密码+密码复杂度校验+历史密码检查），绑定/解绑手机（短信验证码验证），登录日志查看

### 功能模块树

```plain
个人中心
├── 我的档案
│   ├── 基本信息查看（敏感字段脱敏展示）
│   ├── 可编辑字段修改（邮箱/地址/紧急联系人等）
│   └── 锁定字段置灰提示
├── 我的考勤
│   ├── 考勤日历视图（按日标记出勤状态+颜色）
│   ├── 网页打卡（上班/下班按钮）
│   ├── 请假申请快捷入口
│   └── 补卡申请入口
├── 我的请假
│   ├── 请假申请列表（按状态筛选）
│   ├── 审批进度时间线
│   └── 取消申请（仅审批中状态）
├── 我的薪资
│   ├── 工资条列表（按月，仅展示已通过批次）
│   ├── 工资条详情（二次验证）
│   └── 个人薪资趋势折线图（近6个月，AntV）
└── 账号安全
    ├── 修改密码（旧密码验证+复杂度+历史检查）
    ├── 绑定/解绑手机（短信验证码）
    └── 登录日志查询（分页）
```

## 流程图

> 对个人中心涉及的核心流程进行梳理。

### 9-1 档案编辑权限控制流程

```plain
员工进入"我的档案"页面
     │
     ▼
后端加载员工完整档案数据
     │
     ▼
根据当前用户角色 + 字段权限规则
对每个字段标记可编辑性：
     ├── 可编辑字段 → 正常展示，表单允许输入
     └── 锁定字段 → 置灰显示 + Tooltip："如需修改XXX请联系HR"
     │
     ▼
员工编辑可修改字段并提交
     │
     ▼
服务端二次校验字段权限：
     ├── 请求修改的字段 ∈ editableFields → 保存
     └── 请求修改的字段 ∉ editableFields → 拒绝，返回错误
     │
     ▼
写入 employee_change_log（CHANGE_TYPE=DIRECT_EDIT）
     │
     ▼
返回 updatedFields 列表
```

### 9-2 工资条查看流程

```plain
员工进入"我的薪资"页面
     │
     ▼
展示历史工资条列表（按月倒序，仅显示审批通过的批次）
     ├── 已通过/已发放 → 可点击查看
     └── 审批中/已驳回 → 不可查看，灰色
     │
     ▼
点击某个工资条
     │
     ▼
检查当前 session 是否已有验证标记：
     ├── 已验证（有效期内）→ 直接展示工资条详情
     └── 未验证 →
          │
          ▼
     弹出二次验证弹窗：
         选择验证方式：短信验证码 / 登录密码
          │
          ▼
     验证处理：
          ├── 通过 → 设置 session 标记（有效期10分钟），展示详情
          └── 失败 → 提示错误，累计3次失败锁定30分钟
```

### 9-3 修改密码流程

```plain
员工进入"账号安全" → 修改密码
     │
     ▼
填写：旧密码 + 新密码 + 确认新密码
     │
     ▼
前端基础校验：
     ├── 新密码长度 >= 8位
     ├── 包含大小写字母 + 数字
     └── 确认新密码一致
     │
     ▼
提交后端 → 后端校验：
     ├── 旧密码是否正确？
     ├── 新密码是否符合复杂度规则？
     └── 新密码是否与近3次历史密码重复？
     │
     ▼
更新密码（BCrypt加密）+ 写入 password_history
     │
     ▼
清除该员工所有登录 session（强制重新登录）
     │
     ▼
返回成功
```

## UML 图

> 描述个人中心的核心 VO 模型。

```plain
@startuml
class EmployeeProfileVO {
  - id: Long
  - employeeNo: String
  - name: String
  - genderDesc: String
  - phone: String                "脱敏"
  - email: String
  - idCard: String               "脱敏"
  - birthday: LocalDate
  - registeredAddress: String
  - currentAddress: String
  - departmentName: String
  - positionName: String
  - jobLevel: String
  - statusDesc: String
  - hireDate: LocalDate
  - fieldPermissions: FieldPermissions
}

class FieldPermissions {
  - editableFields: List<String>
  - lockedFields: List<LockedField>
}

class LockedField {
  - fieldName: String            "字段名"
  - fieldDesc: String            "字段中文名"
  - hint: String                 "提示文案"
  - requiredFlow: String         "所需流程名称"
}

class AttendanceCalendarVO {
  - month: String
  - summary: AttendanceSummary
  - dailyRecords: List<DailyRecord>
}

class AttendanceSummary {
  - workingDays: Integer         "应出勤天数"
  - attendedDays: Integer        "实际出勤天数"
  - lateCount: Integer           "迟到次数"
  - earlyCount: Integer          "早退次数"
  - absentDays: Integer          "旷工天数"
  - leaveDays: BigDecimal        "请假天数"
}

class DailyRecord {
  - date: LocalDate
  - status: Integer
  - statusDesc: String
  - clockInTime: String
  - clockOutTime: String
}

class PayslipVO {
  - batchId: Long
  - salaryMonth: String
  - grossPay: BigDecimal         "应发"
  - netPay: BigDecimal           "实发"
  - isViewed: Boolean
  - status: String
  - salaryItems: List<SalaryItemDetail>
}

class SalaryItemDetail {
  - name: String                 "工资项目名称"
  - type: Integer                "类型"
  - amount: BigDecimal           "金额"
}

class LoginLogVO {
  - loginTime: LocalDateTime
  - ip: String
  - device: String
  - loginType: Integer
  - loginTypeDesc: String
  - isSuccess: Boolean
}

EmployeeProfileVO *-- FieldPermissions
FieldPermissions *-- LockedField
AttendanceCalendarVO *-- AttendanceSummary
AttendanceCalendarVO *-- DailyRecord
PayslipVO *-- SalaryItemDetail
@enduml
```

## 数据库设计

> 个人中心主要复用已有模块数据表，仅新增以下安全相关表。

```
-- ============== 考勤模块 ==============

-- 考勤打卡记录表
CREATE TABLE IF NOT EXISTS attendance (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
    userId          BIGINT          NOT NULL                 COMMENT '用户ID',
    attendanceDate  DATE            NOT NULL                 COMMENT '考勤日期',
    punchInTime     DATETIME        DEFAULT NULL             COMMENT '上班打卡时间',
    punchOutTime    DATETIME        DEFAULT NULL             COMMENT '下班打卡时间',
    status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=正常 1=迟到 2=早退 3=缺卡 4=请假 5=旷工',
    punchInType     TINYINT         DEFAULT NULL             COMMENT '上班打卡方式：0=网页 1=APP',
    punchOutType    TINYINT         DEFAULT NULL             COMMENT '下班打卡方式：0=网页 1=APP',
    punchInLocation VARCHAR(256)    DEFAULT NULL             COMMENT '上班打卡位置',
    punchOutLocation VARCHAR(256)   DEFAULT NULL             COMMENT '下班打卡位置',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDeleted               tinyint  default 0                 not null comment '逻辑删除：0=否 1=是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_date (employeeId, attendanceDate),
    KEY idx_user_id (userId),
    KEY idx_attendance_date (attendanceDate),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤打卡记录表';


-- 请假申请表
CREATE TABLE IF NOT EXISTS `leave`(
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
    userId          BIGINT          NOT NULL                 COMMENT '用户ID',
    leaveType       TINYINT         NOT NULL                 COMMENT '请假类型：0=事假 1=病假 2=年假 3=婚假 4=产假 5=丧假 6=调休',
    startDate       DATE            NOT NULL                 COMMENT '开始日期',
    endDate         DATE            NOT NULL                 COMMENT '结束日期',
    totalDays       DECIMAL(4,1)    NOT NULL                 COMMENT '请假总天数',
    reason          VARCHAR(512)    NOT NULL                 COMMENT '请假原因',
    status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=待审批 1=已通过 2=已拒绝 3=已撤销',
    approverId      BIGINT          DEFAULT NULL             COMMENT '审批人ID',
    approveTime     DATETIME        DEFAULT NULL             COMMENT '审批时间',
    approveComment  VARCHAR(512)    DEFAULT NULL             COMMENT '审批意见',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDeleted               tinyint  default 0                 not null comment '逻辑删除：0=否 1=是',
    PRIMARY KEY (id),
    KEY idx_employee_id (employeeId),
    KEY idx_user_id (userId),
    KEY idx_status (status),
    KEY idx_start_date (startDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';


-- 补卡申请表
CREATE TABLE IF NOT EXISTS makeup_punch (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
    userId          BIGINT          NOT NULL                 COMMENT '用户ID',
    punchDate       DATE            NOT NULL                 COMMENT '补卡日期',
    punchType       TINYINT         NOT NULL                 COMMENT '补卡类型：0=上班补卡 1=下班补卡',
    punchTime       DATETIME        NOT NULL                 COMMENT '实际到岗/离岗时间',
    reason          VARCHAR(512)    NOT NULL                 COMMENT '缺卡原因',
    status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=待审批 1=已通过 2=已拒绝',
    approverId      BIGINT          DEFAULT NULL             COMMENT '审批人ID',
    approveTime     DATETIME        DEFAULT NULL             COMMENT '审批时间',
    approveComment  VARCHAR(512)    DEFAULT NULL             COMMENT '审批意见',
    createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDeleted               tinyint  default 0                 not null comment '逻辑删除：0=否 1=是',
    PRIMARY KEY (id),
    KEY idx_employee_id (employeeId),
    KEY idx_user_id (userId),
    KEY idx_status (status),
    KEY idx_punch_date (punchDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';




-- 考勤打卡记录表
CREATE TABLE IF NOT EXISTS attendance_record (
                                                 id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
                                                 employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
                                                 userId          BIGINT          NOT NULL                 COMMENT '用户ID',
                                                 attendanceDate  DATE            NOT NULL                 COMMENT '考勤日期',
                                                 punchInTime     DATETIME        DEFAULT NULL             COMMENT '上班打卡时间',
                                                 punchOutTime    DATETIME        DEFAULT NULL             COMMENT '下班打卡时间',
                                                 status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=正常 1=迟到 2=早退 3=缺卡 4=请假 5=旷工',
                                                 punchInType     TINYINT         DEFAULT NULL             COMMENT '上班打卡方式：0=网页 1=APP',
                                                 punchOutType    TINYINT         DEFAULT NULL             COMMENT '下班打卡方式：0=网页 1=APP',
                                                 punchInLocation VARCHAR(256)    DEFAULT NULL             COMMENT '上班打卡位置',
                                                 punchOutLocation VARCHAR(256)   DEFAULT NULL             COMMENT '下班打卡位置',
                                                 remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
                                                 createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                 updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                 isDeleted       TINYINT         DEFAULT 0 NOT NULL       COMMENT '逻辑删除：0=否 1=是',
                                                 PRIMARY KEY (id),
                                                 UNIQUE KEY uk_employee_date (employeeId, attendanceDate),
                                                 KEY idx_user_id (userId),
                                                 KEY idx_attendance_date (attendanceDate),
                                                 KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤打卡记录表';


-- 请假申请表
CREATE TABLE IF NOT EXISTS leave_request (
                                             id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
                                             employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
                                             userId          BIGINT          NOT NULL                 COMMENT '用户ID',
                                             leaveType       TINYINT         NOT NULL                 COMMENT '请假类型：0=事假 1=病假 2=年假 3=婚假 4=产假 5=丧假 6=调休',
                                             startDate       DATE            NOT NULL                 COMMENT '开始日期',
                                             endDate         DATE            NOT NULL                 COMMENT '结束日期',
                                             totalDays       DECIMAL(4,1)    NOT NULL                 COMMENT '请假总天数',
                                             reason          VARCHAR(512)    NOT NULL                 COMMENT '请假原因',
                                             status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=待审批 1=已通过 2=已拒绝 3=已撤销',
                                             approverId      BIGINT          DEFAULT NULL             COMMENT '审批人ID',
                                             approveTime     DATETIME        DEFAULT NULL             COMMENT '审批时间',
                                             approveComment  VARCHAR(512)    DEFAULT NULL             COMMENT '审批意见',
                                             createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             isDeleted       TINYINT         DEFAULT 0 NOT NULL       COMMENT '逻辑删除：0=否 1=是',
                                             PRIMARY KEY (id),
                                             KEY idx_employee_id (employeeId),
                                             KEY idx_user_id (userId),
                                             KEY idx_status (status),
                                             KEY idx_start_date (startDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';


-- 补卡申请表
CREATE TABLE IF NOT EXISTS makeup_punch (
                                            id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
                                            employeeId      BIGINT          NOT NULL                 COMMENT '员工ID',
                                            userId          BIGINT          NOT NULL                 COMMENT '用户ID',
                                            punchDate       DATE            NOT NULL                 COMMENT '补卡日期',
                                            punchType       TINYINT         NOT NULL                 COMMENT '补卡类型：0=上班补卡 1=下班补卡',
                                            punchTime       DATETIME        NOT NULL                 COMMENT '实际到岗/离岗时间',
                                            reason          VARCHAR(512)    NOT NULL                 COMMENT '缺卡原因',
                                            status          TINYINT         NOT NULL DEFAULT 0       COMMENT '状态：0=待审批 1=已通过 2=已拒绝',
                                            approverId      BIGINT          DEFAULT NULL             COMMENT '审批人ID',
                                            approveTime     DATETIME        DEFAULT NULL             COMMENT '审批时间',
                                            approveComment  VARCHAR(512)    DEFAULT NULL             COMMENT '审批意见',
                                            createTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            updateTime      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            isDeleted       TINYINT         DEFAULT 0 NOT NULL       COMMENT '逻辑删除：0=否 1=是',
                                            PRIMARY KEY (id),
                                            KEY idx_employee_id (employeeId),
                                            KEY idx_user_id (userId),
                                            KEY idx_status (status),
                                            KEY idx_punch_date (punchDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';
```


### 工资条验证记录表 payslip_verification



## API 设计

### 1. 我的档案

```plain
GET    /api/employee/profile              # 查看我的档案（含字段权限元数据）
PUT    /api/employee/profileUpdate       # 编辑我的档案（仅允许可编辑字段）
```

#### 查看档案响应

```json
{
  "code": 0,
  "data": {
    "employeeNo": "202401005",
    "name": "张三",
    "genderDesc": "男",
    "phone": "138****1234",
    "email": "zhangsan@example.com",
    "idCard": "3301**********1234",
    "departmentName": "技术部",
    "positionName": "Java开发工程师",
    "jobLevel": "P5",
    "statusDesc": "正式",
    "hireDate": "2024-01-15",
    "fieldPermissions": {
      "editableFields": ["email", "currentAddress", "birthday"],
      "lockedFields": [
        { "fieldName": "phone", "fieldDesc": "手机号", "hint": "如需修改手机号请联系HR", "requiredFlow": "信息变更申请" },
        { "fieldName": "departmentName", "fieldDesc": "部门", "hint": "部门变更需走调岗流程", "requiredFlow": "调岗申请" }
      ]
    }
  }
}
```

---

### 2. 我的考勤

```plain
GET    /api/attendance/calendar?month=2024-07       # 考勤日历
POST   /api/attendance/punch                   # 上班，下班打卡
GET    /api/attendance/statistics?month=2024-07     # 月度统计
GET    /api/attendance/today     # 获取今天的考勤状态
```

```
/** 正常 */
int ATTENDANCE_STATUS_NORMAL = 0;
/** 迟到 */
int ATTENDANCE_STATUS_LATE = 1;
/** 早退 */
int ATTENDANCE_STATUS_LEAVE_EARLY = 2;
/** 缺卡 */
int ATTENDANCE_STATUS_MISSING = 3;
/** 请假 */
int ATTENDANCE_STATUS_LEAVE = 4;
/** 旷工 */
int ATTENDANCE_STATUS_ABSENT = 5;
```



#### 打卡响应

```json
{
  "code": 0,
  "data": {
    "recordDate": "2024-07-10",
    "clockInTime": "08:55:00",
    "status": "正常"
  }
}
```

---

得到考勤日历响应

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
      "2026-07-07": 0,
      "2026-07-08": 0,
      "2026-07-09": 0,
      "2026-07-10": 0
    },
    "makeupAvailableDates": []
  },
  "message": "ok"
}
```

当有缺卡的时候可以进行补卡申请



### 3. 我的请假

```plain
POST    /api/attendance/leave/apply             #申请请假
POST   /api/attendance/leave/cancel/{id}           # 取消申请
GET    /api/attendance/leave/my           #获取我的请假记录
POST	/api/attendance/leave/{id}/progress	#查看我的审批进度
```

#### 请假详情响应

```json
{
  "code": 0,
  "data": {
    "id": 500,
    "leaveTypeDesc": "年假",
    "startTime": "2024-07-15T09:00:00",
    "endTime": "2024-07-17T18:00:00",
    "leaveDays": 3.0,
    "reason": "家庭事务",
    "statusDesc": "审批中",
    "approvalProgress": [
      { "nodeName": "直接上级审批", "approverName": "李四", "status": "APPROVED", "opinion": "同意", "time": "2024-07-10T10:00:00" },
      { "nodeName": "部门负责人审批", "approverName": "王五", "status": "PENDING" }
    ]
  }
}
```

---

### 4. 我的薪资

```plain
GET    /api/salary/slips                                # 工资条列表
POST    /api/salary/slips/{id}                                 # 工资条详情（需验证）
GET    /api/salary/slips/trend                                # 获取6个月的趋势
```

#### 二次验证请求

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| verifyType | Integer | 是 | 1=短信验证码 2=登录密码 |
| verifyCode | String | verifyType=1时必填 | 短信验证码 |
| password | String | verifyType=2时必填 | 登录密码 |

---

### 5. 账号安全

```plain
POST   /api/account/changePassword          #修改密码
POST    /api/account/bindPhone                    # 绑定/解绑手机号
GET    /api/account/login-logs                # 登录日志（分页）
```

#### 修改密码请求

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| oldPassword | String | 是 | 旧密码 |
| newPassword | String | 是 | 新密码（>=8位，含大小写+数字） |

---

日志响应格式

```json
    {
      "id": "18",
      "loginTime": "2026-05-25T06:32:00.000+00:00",
      "ip": "114.25.66.78",
      "device": "Mozilla/5.0 Chrome/121 Android",
      "loginType": 1,
      "loginTypeText": "密码登录",
      "isSuccess": 1,
      "failReason": null
    },
```







## 关键技术设计

### 字段权限控制

个人中心的档案编辑复用员工档案模块的权限方案，服务端二次校验：

```java
public void updateMyProfile(Long employeeId, Map<String, Object> fields) {
    // 获取当前用户对该员工的字段权限
    FieldPermissions permissions = fieldPermissionService
        .getPermissions(getCurrentUserRole(), true);
    
    // 校验每个要修改的字段是否在可编辑列表中
    for (String fieldName : fields.keySet()) {
        if (!permissions.getEditableFields().contains(fieldName)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, 
                "字段 [" + fieldName + "] 不允许直接编辑，需走对应流程");
        }
    }
    
    // 保存变更
    employeeService.updateFields(employeeId, fields);
    
    // 记录变更日志
    changeLogService.log(DIRECT_EDIT, employeeId, fields);
}
```

### 工资条二次验证防暴力破解

```java
private static final int MAX_FAIL_COUNT = 3;
private static final int LOCK_MINUTES = 30;
private static final int SESSION_TTL_SECONDS = 600; // 10分钟

public void verifyPayslip(Long employeeId, Long batchId, 
                           Integer verifyType, String credential) {
    // 检查锁定状态
    Integer failCount = verificationMapper
        .countRecentFail(employeeId, batchId, LocalDateTime.now().minusMinutes(LOCK_MINUTES));
    if (failCount >= MAX_FAIL_COUNT) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, 
            "验证失败次数过多，请30分钟后再试");
    }
    
    boolean success = (verifyType == 1) 
        ? smsService.verify(employeeId, credential) 
        : passwordEncoder.matches(credential, getEmployeePassword(employeeId));
    
    if (success) {
        // Redis 标记已验证，10分钟有效
        redisTemplate.opsForValue().set(
            "payslip:verified:" + employeeId + ":" + batchId, "1",
            SESSION_TTL_SECONDS, TimeUnit.SECONDS);
    } else {
        verificationMapper.insert(new PayslipVerification(employeeId, batchId, verifyType, 0));
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证失败");
    }
}
```

### 密码策略

```java
private static final Pattern PASSWORD_PATTERN = 
    Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

public void changePassword(Long employeeId, String oldPassword, String newPassword) {
    Employee employee = employeeMapper.selectById(employeeId);
    
    // 1. 验证旧密码
    if (!passwordEncoder.matches(oldPassword, employee.getPassword())) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
    }
    
    // 2. 复杂度校验
    if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, 
            "密码需至少8位，包含大小写字母和数字");
    }
    
    // 3. 不能与旧密码相同
    if (passwordEncoder.matches(newPassword, employee.getPassword())) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
    }
    
    // 4. 不能与近3次历史密码重复
    List<PasswordHistory> histories = passwordHistoryMapper
        .findRecentByEmployeeId(employeeId, 3);
    for (PasswordHistory h : histories) {
        if (passwordEncoder.matches(newPassword, h.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能使用近期使用过的密码");
        }
    }
    
    // 5. 更新密码
    String encoded = passwordEncoder.encode(newPassword);
    employeeMapper.updatePassword(employeeId, encoded);
    
    // 6. 保存历史（仅保留最近3次）
    passwordHistoryMapper.insert(employeeId, encoded);
    passwordHistoryMapper.deleteExcess(employeeId, 3);
    
    // 7. 清除所有登录态
    sessionService.invalidateAllByEmployeeId(employeeId);
}
```

### 敏感字段脱敏

```java
public class MaskUtils {
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
    
    public static String maskBankAccount(String account) {
        if (account == null || account.length() < 4) return account;
        return "****" + account.substring(account.length() - 4);
    }
}
```

### 登录日志记录

登录成功/失败均记录日志，通过 Spring AOP 切面统一处理：

```java
@Aspect
@Component
public class LoginLogAspect {
    @AfterReturning(pointcut = "execution(* login(..))", returning = "result")
    public void logLogin(JoinPoint jp, Object result) {
        LoginLog log = new LoginLog();
        log.setEmployeeId(getEmployeeId(jp));
        log.setIp(getClientIp());
        log.setDevice(getUserAgent());
        log.setLoginType(getLoginType(jp));
        log.setIsSuccess(result != null);
        loginLogMapper.insert(log);
    }
    
    @AfterThrowing(pointcut = "execution(* login(..))", throwing = "ex")
    public void logLoginFail(JoinPoint jp, Exception ex) {
        LoginLog log = new LoginLog();
        log.setEmployeeId(getEmployeeId(jp));
        log.setIp(getClientIp());
        log.setIsSuccess(0);
        log.setFailReason(ex.getMessage());
        loginLogMapper.insert(log);
    }
}
```

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审个人中心功能范围，确认字段权限与安全策略 | 0.5天 |
| 技术方案 | 完成系分文档评审，确认 API 设计与安全方案 | 1天 |
| 数据库开发 | 新增 login_log、password_history、payslip_verification 表 | 0.5天 |
| 后端开发 | 我的档案聚合API、考勤日历API、打卡API、请假列表/详情、工资条列表/详情+二次验证、密码修改/手机绑定、登录日志 | 4天 |
| 前端开发 | 个人中心布局、档案查看/编辑页、考勤日历（含打卡）、请假列表/详情、工资条列表/详情、密码修改页、登录日志页、薪资趋势图 | 4天 |
| 联调测试 | 前后端联调、权限场景测试、安全测试（二次验证防暴破/密码策略） | 2天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 1.5天 |

> **总预估工期**：约 13.5 个工作日

---

> **说明**：个人中心模块主要复用已有业务模块（员工档案、考勤管理、薪资管理、审批中心）的后端能力，新增开发量相对较小，重点是 `/api/v1/my/*` 前缀的聚合 API、字段权限校验和安全机制（密码策略、二次验证、防暴力破解）。
