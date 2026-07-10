# HRMS-后端-考勤管理

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | - |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 6 部分——考勤管理。当前公司考勤依赖人工登记和 Excel 统计，存在打卡不便、迟到早退不易追踪、请假审批与考勤数据脱节、月末统计耗时等问题。本模块旨在实现考勤规则的集中配置、打卡数据自动采集与判定、请假全流程线上化、考勤多维统计与可视化展示，同时为薪资核算模块提供考勤扣款数据源。

### 相关资料

- [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述考勤管理涉及的功能与场景。

本模块核心功能包括：

1. **考勤规则配置**：考勤组定义（名称/适用人员/班次类型/上下班时间/迟到早退阈值），工作日设置（标准工作日/休息日/法定节假日）
2. **打卡功能**：网页端打卡（IP白名单/GPS定位限制），打卡判定（正常/迟到/早退/旷工/缺勤/缺卡）
3. **请假管理**：7种请假类型（年假/病假/事假/婚假/产假/丧假/调休），假期余额计算（年假按工龄分段+入职折算，调休按加班1:1累计），请假申请表单及多级审批
4. **补卡管理**：缺卡申请补卡，每月最多2次，需审批
5. **考勤统计**：个人维度（出勤天数/迟到次数/早退次数/旷工天数/请假天数/加班时长/年假余额），部门维度（出勤率/迟到率/请假率），AntV 数据可视化（折线图/饼图/柱状图/考勤日历）

### 功能模块树

```plain
考勤管理
├── 考勤规则配置
│   ├── 考勤组CRUD
│   │   ├── 考勤组名称
│   │   ├── 适用人员配置（按部门/职位/个人）
│   │   ├── 班次类型（固定班/弹性班/排班制）
│   │   ├── 上下班时间及弹性范围
│   │   └── 迟到/早退阈值
│   ├── 工作日设置
│   │   ├── 标准工作日（周一至周五）
│   │   └── 休息日（周六日）
│   └── 法定节假日配置（后台导入）
├── 打卡管理
│   ├── 上班打卡
│   ├── 下班打卡
│   ├── 打卡记录查询（日历视图）
│   └── 打卡判定逻辑（正常/迟到/早退/旷工/缺卡）
├── 请假管理
│   ├── 请假类型定义
│   ├── 假期余额查询
│   │   ├── 年假余额（按工龄 + 入职折算）
│   │   └── 调休余额（加班累计 - 已使用）
│   ├── 请假申请（保存草稿/提交审批）
│   ├── 请假审批流转
│   └── 请假记录查询
├── 补卡管理
│   ├── 补卡申请（限定缺卡日期）
│   ├── 每月次数限制（最多2次）
│   └── 补卡审批
├── 加班管理
│   ├── 加班申请
│   ├── 加班审批
│   └── 加班时长累计 → 调休余额
└── 考勤统计
    ├── 个人考勤统计
    │   ├── 应出勤天数
    │   ├── 实际出勤天数
    │   ├── 迟到/早退/旷工次数
    │   ├── 请假天数按类型汇总
    │   └── 加班时长/年假余额
    ├── 部门考勤统计
    │   ├── 出勤率
    │   ├── 迟到率
    │   └── 请假率
    └── AntV 数据可视化
        ├── 出勤率趋势折线图
        ├── 请假类型分布饼图
        ├── 迟到早退排行柱状图
        └── 考勤日历图
```

## 流程图

> 对考勤管理涉及的核心流程进行梳理。

### 6-1 打卡判定流程

```plain
用户点击打卡
     │
     ▼
记录打卡时间 + IP/GPS信息
     │
     ▼
获取用户所在考勤组
     │
     ▼
获取考勤组规则（上班时间/下班时间/阈值）
     │
     ▼
判定打卡类型（上班卡 / 下班卡）
     │
     ├── 上班卡判定：
     │   ├── 打卡时间 <= 上班时间 → 正常
     │   ├── 上班时间 < 打卡时间 <= 上班时间+阈值 → 迟到
     │   ├── 打卡时间 > 上班时间+阈值 → 旷工半天
     │   └── 当日已打卡 → 重复打卡，忽略或覆盖
     │
     └── 下班卡判定：
         ├── 打卡时间 >= 下班时间 → 正常
         ├── 下班时间-阈值 <= 打卡时间 < 下班时间 → 早退
         ├── 打卡时间 < 下班时间-阈值 → 旷工半天
         └── 已有上班卡 → 配对完成，一次正常出勤
     │
     ▼
写入打卡记录，更新日统计缓存
```

### 6-2 考勤日汇总流程（每日凌晨定时任务）

```plain
定时任务：每天 02:00 执行
     │
     ▼
遍历所有考勤组
     │
     ▼
遍历考勤组内每个员工
     │
     ├── 获取当日打卡记录
     ├── 判定当日出勤状态：
     │   ├── 上下班均有正常打卡 → 正常出勤
     │   ├── 无任何打卡记录 → 缺勤
     │   ├── 仅上班打卡 → 下班缺卡
     │   ├── 仅下班打卡 → 上班缺卡
     │   ├── 迟到标记 → 迟到
     │   ├── 早退标记 → 早退
     │   └── 旷工标记 → 旷工
     ├── 关联请假记录（如当日有审批通过的请假 → 不计缺勤）
     ├── 写入 daily_attendance 表
     └── 更新月度汇总缓存
```

### 6-3 请假审批流

```plain
员工提交请假申请
     │
     ▼
系统校验
     ├── 假期余额是否充足？（年假/调休类型）
     │   → 否 → 提示"余额不足"
     ├── 是否与已有请假时间重叠？
     │   → 是 → 提示"时间段冲突"
     │
     ▼
创建请假记录 (status = PENDING)
     │
     ▼
根据请假类型+天数路由审批流：
     ├── 年假/调休 ≤ 3天 → 直接上级
     ├── 年假/调休 > 3天 → 直接上级 → 部门负责人
     ├── 病假/事假 ≤ 1天 → 直接上级
     ├── 病假/事假 > 1天 → 直接上级 → 部门负责人
     ├── 婚假/产假/丧假 → 直接上级 → HR备案
     │
     ▼
各级审批人操作：
     ├── 通过 → 流转至下一级 / 完成 → 扣减假期余额
     ├── 拒绝 → 流程终止，恢复余额
     └── 转交 → 变更审批人
```

## UML 图

> 描述考勤管理的核心类和依赖关系。

### 考勤核心领域模型

```plain
@startuml
class AttendanceGroup {
  - id: Long
  - name: String                "考勤组名称"
  - members: List<Long>         "适用人员（部门/职位/个人ID集合）"
  - shiftType: ShiftType        "班次类型"
  - startTime: LocalTime        "上班时间"
  - endTime: LocalTime          "下班时间"
  - restStart: LocalTime        "午休开始"
  - restEnd: LocalTime          "午休结束"
  - flexStart: LocalTime        "弹性最早打卡"
  - flexEnd: LocalTime          "弹性最晚打卡"
  - lateThreshold: Int          "迟到阈值(分钟)"
  - earlyThreshold: Int         "早退阈值(分钟)"
  - isDeleted: Integer
}

class AttendanceRecord {
  - id: Long
  - employeeId: Long
  - recordDate: LocalDate       "日期"
  - clockInTime: LocalDateTime  "上班打卡时间"
  - clockOutTime: LocalDateTime "下班打卡时间"
  - clockInStatus: ClockStatus  "上班打卡判定"
  - clockOutStatus: ClockStatus "下班打卡判定"
  - dailyStatus: DailyStatus    "当日汇总状态"
  - clockInIp: String           "上班打卡IP"
  - clockOutIp: String          "下班打卡IP"
}

class LeaveApplication {
  - id: Long
  - employeeId: Long
  - leaveType: LeaveType        "请假类型"
  - startTime: LocalDateTime    "开始时间"
  - endTime: LocalDateTime      "结束时间"
  - leaveDays: BigDecimal       "请假天数(支持0.5)"
  - reason: String              "请假事由"
  - handoverPersonId: Long      "工作交接人"
  - attachments: String         "附件URL(JSON数组)"
  - status: ApprovalStatus
}

class LeaveBalance {
  - id: Long
  - employeeId: Long
  - year: Integer               "年份"
  - annualLeaveTotal: BigDecimal "年假总额"
  - annualLeaveUsed: BigDecimal  "年假已用"
  - compensatoryLeaveTotal: BigDecimal "调休总额"
  - compensatoryLeaveUsed: BigDecimal  "调休已用"
}

class OvertimeApplication {
  - id: Long
  - employeeId: Long
  - overtimeDate: LocalDate     "加班日期"
  - startTime: LocalDateTime    "开始时间"
  - endTime: LocalDateTime      "结束时间"
  - hours: BigDecimal           "加班时长"
  - reason: String              "加班原因"
  - status: ApprovalStatus
}

class Holiday {
  - id: Long
  - name: String                "节日名称"
  - holidayDate: LocalDate      "日期"
  - isWorkday: Boolean          "是否为调休工作日"
}

enum ShiftType {
  FIXED       "固定班"
  FLEXIBLE    "弹性班"
  SCHEDULING  "排班制"
}

enum ClockStatus {
  NORMAL    "正常"
  LATE      "迟到"
  EARLY     "早退"
  ABSENT    "旷工"
  MISSING   "缺卡"
}

enum DailyStatus {
  ATTENDED   "正常出勤"
  ABSENT     "缺勤"
  LATE       "迟到"
  EARLY      "早退"
  ABSENT_HALF "旷工半天"
  ABSENT_FULL "旷工全天"
  LEAVE      "请假"
}

enum LeaveType {
  ANNUAL         "年假"
  SICK           "病假"
  PERSONAL       "事假"
  MARRIAGE       "婚假"
  MATERNITY      "产假"
  FUNERAL        "丧假"
  COMPENSATORY   "调休"
}

AttendanceGroup "1" -- "*" AttendanceRecord
AttendanceRecord "*" -- "1" ClockStatus
AttendanceRecord "*" -- "1" DailyStatus
LeaveApplication "*" -- "1" LeaveType
Employee "1" -- "*" LeaveApplication
Employee "1" -- "1" LeaveBalance
@enduml
```

## 时序图

### 6-1 打卡时序

![打卡时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-attendance-clock.svg)

### 6-2 请假审批时序

![请假审批时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-leave-approval.svg)

### 6-3 考勤日汇总定时任务时序

![考勤日汇总时序](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/placeholder-daily-summary.svg)

## 数据库设计

> 考勤管理模块涉及的核心数据表。

### 考勤组表 attendance_group

```sql
CREATE TABLE IF NOT EXISTS `attendance_group` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                VARCHAR(64)       NOT NULL COMMENT '考勤组名称',
    `shift_type`          TINYINT           NOT NULL COMMENT '班次类型：1=固定班 2=弹性班 3=排班制',
    `start_time`          TIME              NOT NULL COMMENT '上班时间',
    `end_time`            TIME              NOT NULL COMMENT '下班时间',
    `rest_start`          TIME                       COMMENT '午休开始',
    `rest_end`            TIME                       COMMENT '午休结束',
    `flex_start`          TIME                       COMMENT '弹性最早打卡（弹性班适用）',
    `flex_end`            TIME                       COMMENT '弹性最晚打卡（弹性班适用）',
    `late_threshold`      INT               NOT NULL DEFAULT 15 COMMENT '迟到阈值（分钟）',
    `early_threshold`     INT               NOT NULL DEFAULT 15 COMMENT '早退阈值（分钟）',
    `is_deleted`          TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤组表';
```

### 考勤组成员表 attendance_group_member

```sql
CREATE TABLE IF NOT EXISTS `attendance_group_member` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id`            BIGINT UNSIGNED   NOT NULL COMMENT '考勤组ID',
    `member_type`         TINYINT           NOT NULL COMMENT '成员类型：1=部门 2=职位 3=个人',
    `member_id`           BIGINT UNSIGNED   NOT NULL COMMENT '成员ID（部门ID/职位ID/员工ID）',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_member` (`group_id`, `member_type`, `member_id`),
    KEY `idx_group_id` (`group_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤组成员表';
```

### 打卡记录表 attendance_record

```sql
CREATE TABLE IF NOT EXISTS `attendance_record` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`           BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `record_date`           DATE              NOT NULL COMMENT '考勤日期',
    `clock_in_time`         DATETIME                   COMMENT '上班打卡时间',
    `clock_out_time`        DATETIME                   COMMENT '下班打卡时间',
    `clock_in_status`       TINYINT                    COMMENT '上班打卡判定：1=正常 2=迟到 3=旷工 4=缺卡',
    `clock_out_status`      TINYINT                    COMMENT '下班打卡判定：1=正常 2=早退 3=旷工 4=缺卡',
    `daily_status`          TINYINT           NOT NULL COMMENT '当日汇总：1=正常出勤 2=缺勤 3=迟到 4=早退 5=旷工半天 6=旷工全天 7=请假',
    `clock_in_ip`           VARCHAR(45)                COMMENT '上班打卡IP',
    `clock_out_ip`          VARCHAR(45)                COMMENT '下班打卡IP',
    `clock_in_location`     VARCHAR(128)               COMMENT '上班打卡GPS坐标',
    `clock_out_location`    VARCHAR(128)               COMMENT '下班打卡GPS坐标',
    `source_type`           TINYINT           NOT NULL DEFAULT 1 COMMENT '数据来源：1=正常打卡 2=补卡 3=系统修正',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_date` (`employee_id`, `record_date`),
    KEY `idx_record_date` (`record_date`),
    KEY `idx_daily_status` (`daily_status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '打卡记录表';
```

### 请假申请表 leave_application

```sql
CREATE TABLE IF NOT EXISTS `leave_application` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`           BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `leave_type`            TINYINT           NOT NULL COMMENT '请假类型：1=年假 2=病假 3=事假 4=婚假 5=产假 6=丧假 7=调休',
    `start_time`            DATETIME          NOT NULL COMMENT '开始时间',
    `end_time`              DATETIME          NOT NULL COMMENT '结束时间',
    `leave_days`            DECIMAL(4,1)      NOT NULL COMMENT '请假天数，支持0.5天',
    `reason`                VARCHAR(512)      NOT NULL COMMENT '请假事由',
    `handover_person_id`    BIGINT UNSIGNED            COMMENT '工作交接人ID',
    `attachments`           VARCHAR(1024)              COMMENT '附件URL列表（JSON数组）',
    `status`                TINYINT           NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=审批中 2=已通过 3=已拒绝 4=已取消',
    `approval_chain`        VARCHAR(512)               COMMENT '审批链快照（JSON）',
    `cancel_reason`         VARCHAR(256)               COMMENT '取消原因',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_leave_type` (`leave_type`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '请假申请表';
```

### 假期余额表 leave_balance

```sql
CREATE TABLE IF NOT EXISTS `leave_balance` (
    `id`                              BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`                     BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `year`                            SMALLINT          NOT NULL COMMENT '年份',
    `annual_leave_total`              DECIMAL(5,1)      NOT NULL DEFAULT 0 COMMENT '年假总额',
    `annual_leave_used`               DECIMAL(5,1)      NOT NULL DEFAULT 0 COMMENT '年假已用',
    `compensatory_leave_total`        DECIMAL(5,1)      NOT NULL DEFAULT 0 COMMENT '调休总额',
    `compensatory_leave_used`         DECIMAL(5,1)      NOT NULL DEFAULT 0 COMMENT '调休已用',
    `create_time`                     DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                     DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_year` (`employee_id`, `year`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '假期余额表';
```

### 补卡申请表 makeup_application

```sql
CREATE TABLE IF NOT EXISTS `makeup_application` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `record_date`         DATE              NOT NULL COMMENT '补卡日期',
    `makeup_type`         TINYINT           NOT NULL COMMENT '补卡类型：1=上班卡 2=下班卡 3=全部',
    `clock_time`          DATETIME          NOT NULL COMMENT '补卡时间',
    `reason`              VARCHAR(256)      NOT NULL COMMENT '补卡原因',
    `status`              TINYINT           NOT NULL DEFAULT 1 COMMENT '状态：1=审批中 2=已通过 3=已拒绝',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_record_date` (`record_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '补卡申请表';
```

### 加班申请表 overtime_application

```sql
CREATE TABLE IF NOT EXISTS `overtime_application` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`         BIGINT UNSIGNED   NOT NULL COMMENT '员工ID',
    `overtime_date`       DATE              NOT NULL COMMENT '加班日期',
    `start_time`          DATETIME          NOT NULL COMMENT '开始时间',
    `end_time`            DATETIME          NOT NULL COMMENT '结束时间',
    `hours`               DECIMAL(4,1)      NOT NULL COMMENT '加班时长',
    `reason`              VARCHAR(512)      NOT NULL COMMENT '加班原因',
    `status`              TINYINT           NOT NULL DEFAULT 1 COMMENT '状态：1=审批中 2=已通过 3=已拒绝',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_overtime_date` (`overtime_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '加班申请表';
```

### 法定节假日表 holiday

```sql
CREATE TABLE IF NOT EXISTS `holiday` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                VARCHAR(64)       NOT NULL COMMENT '节日名称',
    `holiday_date`        DATE              NOT NULL COMMENT '日期',
    `is_workday`          TINYINT           NOT NULL DEFAULT 0 COMMENT '是否为调休工作日：0=节假日 1=调休上班日',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_holiday_date` (`holiday_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '法定节假日表';
```

## API 设计

### 1. 考勤组 CRUD

```plain
GET    /api/v1/attendance-groups          # 考勤组列表
POST   /api/v1/attendance-groups          # 新建考勤组
PUT    /api/v1/attendance-groups/{id}     # 编辑考勤组
DELETE /api/v1/attendance-groups/{id}     # 删除考勤组
GET    /api/v1/attendance-groups/{id}     # 考勤组详情
```

### 2. 考勤组成员管理

```plain
POST   /api/v1/attendance-groups/{id}/members    # 添加成员
DELETE /api/v1/attendance-groups/{id}/members    # 移除成员
GET    /api/v1/attendance-groups/{id}/members    # 查询成员列表
```

### 3. 打卡

```plain
POST   /api/v1/attendance/clock-in     # 上班打卡
POST   /api/v1/attendance/clock-out    # 下班打卡
```

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "recordDate": "2024-07-10",
    "clockInTime": "2024-07-10T08:55:00",
    "status": "正常",
    "statusCode": 1
  }
}
```

---

### 4. 获取打卡记录（日历视图）

```plain
GET /api/v1/attendance/records
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| month | String | 是 | 月份，格式 yyyy-MM |
| employeeId | Long | 否 | 员工ID（HR/主管查看他人时传入） |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "month": "2024-07",
    "workingDays": 23,
    "actualDays": 21,
    "records": [
      {
        "recordDate": "2024-07-01",
        "clockInTime": "08:55",
        "clockOutTime": "18:05",
        "clockInStatus": 1,
        "clockInStatusDesc": "正常",
        "clockOutStatus": 1,
        "clockOutStatusDesc": "正常",
        "dailyStatus": 1,
        "dailyStatusDesc": "正常出勤"
      }
    ]
  }
}
```

---

### 5. 请假申请

```plain
POST   /api/v1/leave-applications              # 提交请假申请（草稿/提交）
GET    /api/v1/leave-applications              # 我的请假列表
GET    /api/v1/leave-applications/{id}         # 请假详情
PUT    /api/v1/leave-applications/{id}/cancel  # 取消申请（仅审批中）
```

#### 提交请假请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| leaveType | Integer | 是 | 请假类型 |
| startTime | DateTime | 是 | 开始时间（精确到半天） |
| endTime | DateTime | 是 | 结束时间 |
| reason | String | 是 | 请假事由 |
| handoverPersonId | Long | 否 | 工作交接人 |
| attachments | String[] | 条件必填 | 病假/婚假/产假需上传证明 |
| submit | Boolean | 否 | 是否直接提交审批 |

---

### 6. 假期余额查询

```plain
GET /api/v1/leave-balance
```

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "annualLeave": { "total": 5, "used": 2, "remaining": 3 },
    "compensatoryLeave": { "total": 8, "used": 4, "remaining": 4 }
  }
}
```

---

### 7. 补卡申请

```plain
POST /api/v1/makeup-applications
```

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| recordDate | Date | 是 | 补卡日期 |
| makeupType | Integer | 是 | 补卡类型：1=上班 2=下班 3=全部 |
| clockTime | DateTime | 是 | 补打卡时间 |
| reason | String | 是 | 补卡原因 |

> **说明**：系统校验当月补卡次数 ≤ 2。

---

### 8. 加班申请

```plain
POST /api/v1/overtime-applications
```

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| overtimeDate | Date | 是 | 加班日期 |
| startTime | DateTime | 是 | 开始时间 |
| endTime | DateTime | 是 | 结束时间 |
| reason | String | 是 | 加班原因 |

---

### 9. 考勤统计数据

```plain
GET /api/v1/attendance/statistics/personal?month=2024-07       # 个人月度统计
GET /api/v1/attendance/statistics/department?month=2024-07     # 部门月度统计（HR/主管）
GET /api/v1/attendance/statistics/trend?months=6               # 出勤率趋势（AntV折线图数据）
GET /api/v1/attendance/statistics/leave-distribution?month=2024-07  # 请假类型分布（AntV饼图）
GET /api/v1/attendance/statistics/late-ranking?month=2024-07   # 迟到早退排行（AntV柱状图）
```

---

## 关键技术设计

### 打卡判定引擎

打卡判定逻辑抽离为独立策略模块：

```java
public interface ClockJudgeStrategy {
    ClockStatus judge(LocalDateTime clockTime, AttendanceGroup group);
}

public class ClockInJudgeStrategy implements ClockJudgeStrategy {
    public ClockStatus judge(LocalDateTime clockTime, AttendanceGroup group) {
        LocalTime startTime = group.getStartTime();
        int threshold = group.getLateThreshold();
        
        if (!clockTime.toLocalTime().isAfter(startTime)) return ClockStatus.NORMAL;
        if (clockTime.toLocalTime().isBefore(startTime.plusMinutes(threshold))) return ClockStatus.LATE;
        return ClockStatus.ABSENT;
    }
}
```

### 年假余额计算

```java
public BigDecimal calculateAnnualLeave(LocalDate hireDate, int currentYear) {
    long years = ChronoUnit.YEARS.between(hireDate, LocalDate.of(currentYear, 1, 1));
    int baseDays;
    if (years < 1) baseDays = 0;
    else if (years < 10) baseDays = 5;
    else if (years < 20) baseDays = 10;
    else baseDays = 15;
    
    // 当年入职按剩余月份折算
    if (hireDate.getYear() == currentYear) {
        int remainingMonths = 12 - hireDate.getMonthValue() + 1;
        return BigDecimal.valueOf(baseDays * remainingMonths / 12.0)
            .setScale(1, RoundingMode.HALF_UP);
    }
    return BigDecimal.valueOf(baseDays);
}
```

### 调休有效期管理

加班审批通过后累计至调休余额，当月及次月有效：

- 月初定时任务清理过期调休：`WHERE overtime_date < 上月1号 → compensatory_leave_total 扣减已过期额度`
- 请假扣减调休时优先扣减最早到期的额度（FIFO）

### 节假日/工作日判断

```java
public boolean isWorkday(LocalDate date) {
    // 1. 先查 holiday 表（法定节假日/调休工作日）
    Holiday holiday = holidayMapper.findByDate(date);
    if (holiday != null) return holiday.getIsWorkday() == 1;
    
    // 2. 默认周一至周五为工作日
    DayOfWeek dow = date.getDayOfWeek();
    return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
}
```

### AntV 数据接口设计

统计接口返回格式适配前端 AntV 图表直接使用：

```json
// 出勤率趋势折线图
{
  "data": [
    { "month": "2024-02", "rate": 96.5 },
    { "month": "2024-03", "rate": 97.1 }
  ]
}

// 请假类型分布饼图
{
  "data": [
    { "type": "年假", "value": 15 },
    { "type": "病假", "value": 8 }
  ]
}

// 迟到早退排行柱状图
{
  "data": [
    { "department": "技术部", "lateCount": 3, "earlyCount": 1 },
    { "department": "销售部", "lateCount": 8, "earlyCount": 5 }
  ]
}
```

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审考勤规则、请假类型、审批流程 | 1天 |
| 技术方案 | 完成系分文档评审，确认打卡判定引擎、余额计算逻辑 | 2天 |
| 数据库开发 | 建表、索引优化、初始化考勤组与节假日数据 | 1天 |
| 后端开发 | 考勤组CRUD、打卡API、打卡判定引擎、请假CRUD+审批、补卡、加班、假期余额、统计接口 | 7天 |
| 前端开发 | 打卡页、考勤日历、请假表单、补卡申请、加班申请、考勤统计页（含AntV图表） | 5天 |
| 联调测试 | 前后端联调、打卡判定测试、余额计算测试、定时任务测试 | 3天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 2天 |

> **总预估工期**：约 21 个工作日
