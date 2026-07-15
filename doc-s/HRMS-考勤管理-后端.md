# HRMS-考勤管理-后端

# HRMS-考勤管理-后端

## 变更记录

> 记录每次修订的内容，方便追溯。

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-10 | 1.0 | 初稿 | 凯贾 |
| 2026-07-11 | 1.1 | 修订：新增排班制设计、IP/GPS配置、弹性班规则、加班CRUD、补卡审批独立接口、打卡修正、请假联动、并发幂等、夜班限制等 | 凯贾 |

## 项目背景

> 对本次项目的背景以及目标进行描述，方便开发者理解需求，对齐上下文。

本模块来源于 HRMS（人资管理系统）产品规格说明书中第 6 部分——考勤管理。当前公司内部考勤依赖人工统计与纸质请假条，存在打卡数据不透明、请假审批效率低、考勤统计耗时易错等问题。本模块旨在实现考勤全流程线上化，涵盖考勤规则配置、员工打卡、请假申请与审批、补卡管理、考勤统计与可视化等核心能力，同时为薪资核算模块提供准确的考勤扣款数据。

### 相关资料

*   [人资管理系统（HRMS）详细产品规格说明书](https://yuque.antfin.com/ww89nu/ng0ckr/tttxtqry8pfycc6s)
    

### 参与人

| **项目负责人** | ... |
| --- | --- |
| **产品经理** | ... |
| **设计师** | ... |
| **工程师** | ... |

## 功能模块

> 描述考勤管理涉及的功能与场景。

本模块核心功能包括：

1.  **考勤规则配置**：定义考勤组（固定班/弹性班/排班制）、班次时间、迟到/早退阈值、IP/GPS打卡限制、核心工作时段、工作日设置、节假日管理、排班管理
    
2.  **打卡功能**：网页端打卡、IP/GPS 限制、上班/下班打卡、打卡状态判定（正常/迟到/早退/旷工/缺卡）、弹性班工作时长校验、请假联动拦截、并发幂等
    
3.  **请假管理**：7 种请假类型（年假/病假/事假/婚假/产假/丧假/调休）、假期余额计算（年假不结转清零）、请假申请与多级审批（含HR备案通知模式）、工作交接
    
4.  **补卡管理**：缺卡日期补卡申请、每月最多 2 次限制、独立审批流程、补卡时间取考勤组规定时间
    
5.  **加班记录管理**：加班记录 CRUD、自动转入调休余额（1:1）、调休过期自动清理
    
6.  **考勤统计**：个人维度统计（出勤天数/迟到次数/旷工天数/年假余额等）、部门维度统计（出勤率/迟到率/请假率）、AntV 图表可视化数据、打卡记录手动修正及审计留痕
    

### 功能模块树

```plain
考勤管理
├── 考勤规则配置
│   ├── 考勤组管理（CRUD）
│   │   ├── 班次类型设置（固定班/弹性班/排班制）
│   │   ├── 上下班时间设置
│   │   ├── 迟到/早退阈值设置
│   │   ├── 打卡限制设置（IP白名单/GPS定位）
│   │   ├── 弹性班核心时段设置
│   │   └── 适用人员范围（按部门/职位/个人）
│   ├── 工作日设置
│   │   ├── 标准工作日（周一至周五）
│   │   └── 休息日（周六、周日）
│   ├── 法定节假日管理（对接外部日历服务同步）
│   └── 排班管理（排班制专用）
│       ├── 批量创建排班
│       ├── 查询/修改/删除排班
│       └── 排班与打卡记录联动
├── 打卡功能
│   ├── 网页端打卡
│   ├── 打卡限制（IP 白名单 / GPS 定位）
│   ├── 上班/下班打卡
│   └── 打卡状态判定（正常/迟到/早退/旷工/缺卡）
├── 请假管理
│   ├── 请假类型定义（7 种）
│   ├── 假期余额计算（年假 + 调休）
│   ├── 请假申请（含表单/附件）
│   ├── 审批流路由（按类型+天数）
│   ├── 多级审批流转
│   └── 假期余额扣减与恢复
├── 补卡管理
│   ├── 补卡申请（上班卡/下班卡）
│   ├── 每月 2 次限制校验
│   └── 补卡审批（直接上级，独立接口）
├── 加班记录管理
│   ├── 加班记录 CRUD
│   ├── 自动转入调休余额（1:1）
│   └── 调休过期清理
└── 考勤统计
    ├── 个人维度统计
    ├── 部门维度统计
    ├── 打卡记录手动修正（HR/管理员）
    └── AntV 可视化数据
```

## 流程图

> 对考勤管理涉及的核心流程进行梳理。

### 6-1 打卡流程

```plain
员工登录系统
     │
     ├── 点击"上班打卡"按钮 ────────────────────────┐
     │                                              │
     └── 点击"下班打卡"按钮 ────────────────────────┤
                                                    ▼
                                          校验打卡条件
                                          ├── IP 白名单校验（如配置）
                                          ├── GPS 定位校验（如配置）
                                          └── 校验不通过 → 拒绝并提示原因
                                                    │
                                                    ▼
                                          校验当天是否已请假
                                          ├── 全天请假 → 拒绝并提示"当日已请假，无需打卡"
                                          ├── 半天请假 → 校验当前打卡时段是否在请假范围内
                                          │   └── 是 → 拒绝并提示"当前时段已请假，无需打卡"
                                          └── 无请假 → 继续
                                                    │
                               ┌────────────────────┴────────────────────┐
                               │ 上班打卡                                  │ 下班打卡
                               ▼                                         ▼
                    校验是否已打卡                         记录实际签退时间
                    ├── 已有上班打卡                        │
                    │   → 拒绝并提示"已完成上班打卡"              ├── 签退 >= 规定时间 → 正常
                    └── 无 → 继续                            ├── 规定时间-阈值 <= 签退
                               │                              │    < 规定时间 → 早退
                               ▼                              └── 签退 < 规定时间-阈值
                    记录实际签到时间                                   → 旷工半天
                    ├── 签到 <= 规定时间 → 正常                           │
                    ├── 规定时间 < 签到                                    ▼
                    │   <= 规定时间+阈值 → 迟到                 UPDATE actual_end_time,
                    └── 签到 > 规定时间+阈值                      end_status
                        → 旷工半天
                               │
                               ▼
                    UPDATE actual_start_time,
                    start_status
                               │
                               ▼
                    返回打卡结果（正常/迟到/早退/旷工）
```

### 6-2 打卡记录自动生成流程

```plain
定时任务触发（每日 00:00）
     │
     ▼
查询所有在职员工（状态 = 试用期/正式）
     │
     ▼
遍历每位员工
     │
     ├── 查询当天是否为工作日（work_calendar）
     │   ├── 休息日/节假日 → 跳过，不生成打卡记录
     │   └── 工作日 → 继续
     │
     ├── 查询当天是否有已通过的请假记录（leave_request, status=3）
     │   ├── 有全天请假（leave_days >= 1.0 且覆盖全天） → 跳过，不生成打卡记录
     │   ├── 有半天请假（如仅上午请假） → 仍生成记录，请假时段后续不参与状态判定
     │   └── 无请假 → 继续
     │
     ├── 匹配员工所属考勤组（attendance_group_rule）
     │   ├── 优先按个人匹配 → 按职位匹配 → 按部门匹配
     │   └── 未匹配到考勤组 → 使用默认考勤组
     │
     ├── 获取考勤组规则（上下班时间、阈值等）
     │
     ▼
生成打卡记录（attendance_record）
  - employee_id、attendance_date、attendance_group_id
  - scheduled_start_time、scheduled_end_time
  - actual_start_time = NULL、actual_end_time = NULL
  - start_status = NULL、end_status = NULL
     │
     ▼
批量插入（INSERT IGNORE 防重复）
```

### 6-3 请假申请与审批流程

```plain
员工提交请假申请
     │
     ▼
系统校验
     ├── 请假天数 > 0
     ├── 结束时间 >= 开始时间
     ├── 附件（病假/婚假/产假：必传）
     │
     ▼
假期余额校验（年假/调休类型）
     ├── 剩余可用天数 < 请假天数 → 拒绝提交，提示余额不足
     └── 余额充足 → 继续
     │
     ▼
系统计算请假天数（排除休息日与节假日）
     │
     ▼
确定审批流（按请假类型 + 天数）
     ├── 年假/调休 ≤ 3天        → 直接上级
     ├── 年假/调休 > 3天         → 直接上级 → 部门负责人
     ├── 病假/事假 ≤ 1天         → 直接上级
     ├── 病假/事假 > 1天         → 直接上级 → 部门负责人
     ├── 婚假/产假/丧假           → 直接上级 → HR 备案
     └── 补卡                   → 直接上级
     │
     ▼
逐级审批
     ├── 当前审批人通过 → 进入下一级 或 审批完成
     ├── 当前审批人拒绝 → 流程终止，通知申请人
     ├── 当前审批人转交 → 转给新审批人
     └── 超时 48 小时 → 自动升级或催办
     │
     ▼
审批通过后的处理
     ├── 扣减假期余额（年假/调休类型）
     ├── 更新 leave_request 状态为"已通过"
     └── 同步更新考勤统计
```

### 6-4 补卡申请流程

```plain
员工发现缺卡（上班缺卡/下班缺卡）
     │
     ▼
进入补卡申请页面
     │
     ▼
系统校验当月补卡次数
     ├── 当月已申请 >= 2 次 → 拒绝，提示"每月最多 2 次补卡"
     └── 未超过 2 次 → 继续
     │
     ▼
填写补卡申请
  - 选择补卡日期（只能选有缺卡记录的日期）
  - 选择补卡类型（上班卡/下班卡）
  - 填写补卡原因
     │
     ▼
提交补卡申请 → 进入审批流
     │
     ▼
直接上级审批
     ├── 通过 → 更新 attendance_record：
     │         1. 以考勤组规定的上下班时间作为实际打卡时间
     │         2. 根据打卡状态判定规则重新判定 start_status / end_status
     │         3. 更新 supplement_card_request.status = 3（已通过）
     └── 拒绝 → 更新 supplement_card_request.status = 4，缺卡状态维持
```

### 6-5 考勤统计流程

```plain
定时任务触发（每月第 1 天 02:00 统计上月）
或 HR 手动触发统计
     │
     ▼
锁定上月考勤数据（设置统计状态，禁止修改）
     │
     ▼
遍历所有在职员工（统计期间内在职）
     │
     ├── 应出勤天数 = 上月工作日天数 - 节假日天数
     │
     ├── 实际出勤天数 = 有实际打卡记录的天数
     │
     ├── 迟到次数 = start_status = 迟到 的打卡记录数
     │
     ├── 早退次数 = end_status = 早退 的打卡记录数
     │
     ├── 旷工天数 = 按以下规则汇总：
     │   ├── start_status=3 且 end_status=3 → 全天缺勤（计 1.0 天）
     │   ├── 仅 start_status=3 → 上午旷工（计 0.5 天）
     │   └── 仅 end_status=3 → 下午旷工（计 0.5 天）
     │
     ├── 请假天数 = SUM(请假审批通过的请假天数)
     │   ├── 年假天数
     │   ├── 病假天数
     │   ├── 事假天数
     │   └── 其他类型请假天数
     │
     ├── 加班时长 = SUM(overtime_record.hours)
     │
     ├── 年假余额 = 计算当前年假余额
     │
     ▼
写入 attendance_statistics 表
     │
     ▼
汇总部门维度统计
  - 部门出勤率 = 实际出勤天数 / 应出勤天数
  - 部门迟到率 = 迟到人次 / 部门人数
  - 部门请假率 = 请假天数 / 应出勤天数
```

### 6-6 节假日同步流程

```plain
定时任务触发（每日 03:00）
或 管理员手动触发
     │
     ▼
调用外部日历服务 API
     │
     ▼
获取当年法定节假日列表
     │
     ▼
比对 work_calendar 表已有数据
     ├── 新增的节假日 → INSERT
     ├── 变更的节假日 → UPDATE（名称/日期调整）
     └── 取消的节假日 → UPDATE day_type 恢复为工作日
     │
     ▼
同时更新 work_calendar 中工作日/休息日标记
  - 因节假日调休产生的"周末上班"场景：day_type = 工作日
     │
     ▼
记录同步日志（同步时间、新增/变更/取消条数）
```

## UML 图

> 描述考勤管理模块的核心类和依赖关系。

### 考勤管理核心领域模型

```plain
@startuml

' ==================== 核心实体 ====================

' 考勤组：定义考勤规则模板（固定班/弹性班/排班制）
class AttendanceGroup {
  - id: Long
  - name: String                   "考勤组名称"
  - shiftType: ShiftType           "班次类型"
  - startTime: LocalTime           "上班时间"
  - endTime: LocalTime             "下班时间"
  - restStartTime: LocalTime       "午休开始时间"
  - restEndTime: LocalTime         "午休结束时间"
  - flexStartTime: LocalTime       "弹性最早打卡时间"
  - flexEndTime: LocalTime         "弹性最晚打卡时间"
  - lateThreshold: Integer         "迟到阈值(分钟)"
  - earlyLeaveThreshold: Integer   "早退阈值(分钟)"
  - ipWhitelist: String            "IP白名单"
  - gpsLatitude: BigDecimal        "GPS纬度"
  - gpsLongitude: BigDecimal       "GPS经度"
  - gpsRadius: Integer             "GPS半径(米)"
  - coreStartTime: LocalTime       "核心工作开始时间(弹性班)"
  - coreEndTime: LocalTime         "核心工作结束时间(弹性班)"
  - isDeleted: Boolean             "逻辑删除"
  --
  + getWorkDuration(): Duration
  + isLate(LocalTime): Boolean
  + isEarlyLeave(LocalTime): Boolean
}

' 考勤组适用规则：定义考勤组的人员适用范围（按部门/职位/个人匹配）
class AttendanceGroupRule {
  - id: Long
  - attendanceGroupId: Long        "考勤组ID"
  - ruleType: RuleType             "适用类型：部门/职位/个人"
  - targetId: Long                 "目标ID（ruleType=1→部门ID, 2→职位ID, 3→员工ID）"
}

' 工作日历：记录每日的工作/休息/节假日类型
class WorkCalendar {
  - id: Long
  - calendarDate: LocalDate        "日期"
  - dayType: DayType               "日期类型：工作日/休息日/节假日"
  - holidayName: String            "节假日名称"
}

' 打卡记录：每日定时预生成，打卡时更新实际时间与状态
class AttendanceRecord {
  - id: Long
  - employeeId: Long               "员工ID"
  - attendanceDate: LocalDate      "考勤日期"
  - attendanceGroupId: Long        "适用考勤组ID"
  - scheduledStartTime: LocalTime  "规定上班时间"
  - scheduledEndTime: LocalTime    "规定下班时间"
  - actualStartTime: LocalDateTime "实际上班打卡时间"
  - actualEndTime: LocalDateTime   "实际下班打卡时间"
  - startStatus: AttendanceStatus  "上班状态"
  - endStatus: AttendanceStatus    "下班状态"
  --
  + isAbsent(): Boolean
  + isCardMissing(): Boolean
}

' 请假申请：员工提交请假，支持7种请假类型与多级审批
class LeaveRequest {
  - id: Long
  - employeeId: Long               "员工ID"
  - leaveType: LeaveType           "请假类型"
  - startTime: LocalDateTime       "开始时间"
  - endTime: LocalDateTime         "结束时间"
  - leaveDays: BigDecimal          "请假天数(支持0.5)"
  - reason: String                 "请假事由"
  - handoverEmployeeId: Long       "工作交接人ID"
  - attachmentUrl: String          "附件URL"
  - status: LeaveRequestStatus     "状态"
  - currentApprovalLevel: Integer  "当前审批级别"
  - isDeleted: Boolean             "逻辑删除"
  --
  + canCancel(): Boolean
  + getApprovalChain(): List<Long>
}

' 请假审批记录：记录每级审批的流转详情
class LeaveApprovalLog {
  - id: Long
  - leaveRequestId: Long           "请假申请ID"
  - approverId: Long               "审批人ID"
  - approvalLevel: Integer         "审批级别"
  - status: ApprovalStatus         "审批状态"
  - comment: String                "审批意见"
  - originalApproverId: Long       "原始审批人ID(转交/委托)"
}

' 补卡申请：缺卡日期补卡，独立于请假流程，每月最多2次
class SupplementCardRequest {
  - id: Long
  - employeeId: Long               "员工ID"
  - attendanceDate: LocalDate      "补卡日期"
  - cardType: CardType             "补卡类型：上班/下班"
  - reason: String                 "补卡原因"
  - status: LeaveRequestStatus     "状态"
  - approverId: Long               "审批人ID"
  - approvalComment: String        "审批意见"
  - isDeleted: Boolean             "逻辑删除"
  --
  + canSubmit(): Boolean           "校验当月次数"
}

' 员工假期余额：按年+类型记录年假和调休余额
class EmployeeLeaveBalance {
  - id: Long
  - employeeId: Long               "员工ID"
  - year: Integer                  "年份"
  - leaveType: LeaveType           "请假类型(年假/调休)"
  - totalDays: BigDecimal          "总天数"
  - usedDays: BigDecimal           "已用天数"
  - remainingDays: BigDecimal      "剩余天数"
  --
  + deduct(BigDecimal): void
  + restore(BigDecimal): void
}

' 加班记录：用于调休余额计算，1:1转入调休
class OvertimeRecord {
  - id: Long
  - employeeId: Long               "员工ID"
  - overtimeDate: LocalDate        "加班日期"
  - startTime: LocalDateTime       "开始时间"
  - endTime: LocalDateTime         "结束时间"
  - hours: BigDecimal              "加班小时数"
  - isUsed: Boolean                "是否已转为调休"
  - expireDate: LocalDate          "有效期截止日期"
}

' 考勤月度统计：每月定时任务汇总生成，个人+部门维度
class AttendanceStatistics {
  - id: Long
  - employeeId: Long               "员工ID"
  - statYear: Integer              "统计年份"
  - statMonth: Integer             "统计月份"
  - scheduledDays: BigDecimal      "应出勤天数"
  - actualDays: BigDecimal         "实际出勤天数"
  - lateCount: Integer             "迟到次数"
  - earlyLeaveCount: Integer       "早退次数"
  - absentDays: BigDecimal         "旷工天数"
  - leaveDays: BigDecimal          "请假天数合计"
  - annualLeaveDays: BigDecimal    "年假天数"
  - sickLeaveDays: BigDecimal      "病假天数"
  - personalLeaveDays: BigDecimal  "事假天数"
  - overtimeHours: BigDecimal      "加班小时数"
  - annualLeaveBalance: BigDecimal "年假余额"
}

' ==================== 枚举 ====================

' 班次类型：决定考勤组的打卡时间模式
enum ShiftType {
  FIXED       "固定班"
  FLEXIBLE    "弹性班"
  SCHEDULED   "排班制"
}

' 适用规则类型：考勤组匹配人员的维度
enum RuleType {
  BY_DEPARTMENT   "按部门"
  BY_POSITION     "按职位"
  BY_EMPLOYEE     "按个人"
}

' 日期类型：区分工作日/休息日/节假日
enum DayType {
  WORKDAY     "工作日"
  REST_DAY    "休息日"
  HOLIDAY     "节假日"
}

' 打卡状态：上下班分别判定
enum AttendanceStatus {
  NORMAL              "正常"
  LATE                "迟到"
  EARLY_LEAVE         "早退"
  ABSENT              "旷工"
  START_CARD_MISSING  "上班缺卡"
  END_CARD_MISSING    "下班缺卡"
}

' 请假类型：7种假期，年假和调休有余额管理
enum LeaveType {
  ANNUAL      "年假"
  SICK        "病假"
  PERSONAL    "事假"
  MARRIAGE    "婚假"
  MATERNITY   "产假"
  FUNERAL     "丧假"
  COMP_TIME   "调休"
}

' 请假申请状态：审批流生命周期
enum LeaveRequestStatus {
  DRAFT       "草稿"
  APPROVING   "审批中"
  APPROVED    "已通过"
  REJECTED    "已拒绝"
  CANCELLED   "已取消"
}

' 审批状态：单级审批结果
enum ApprovalStatus {
  PENDING     "待审批"
  APPROVED    "通过"
  REJECTED    "拒绝"
  TRANSFERRED "转交"
}

' 补卡类型：上班卡/下班卡
enum CardType {
  START   "上班卡"
  END     "下班卡"
}

' 排班表：排班制考勤组按日期定义每日班次时间
class AttendanceSchedule {
  - id: Long
  - attendanceGroupId: Long         "考勤组ID"
  - scheduleDate: LocalDate         "排班日期"
  - startTime: LocalTime            "上班时间"
  - endTime: LocalTime              "下班时间"
  - restStartTime: LocalTime        "午休开始时间"
  - restEndTime: LocalTime          "午休结束时间"
}

' 打卡修正日志：记录HR/管理员手动修正操作，用于审计追溯
class AttendanceCorrectionLog {
  - id: Long
  - attendanceRecordId: Long        "打卡记录ID"
  - operatorId: Long                "操作人ID（HR/管理员）"
  - originalStartTime: LocalDateTime "修正前实际上班时间"
  - originalEndTime: LocalDateTime   "修正前实际下班时间"
  - originalStartStatus: AttendanceStatus "修正前上班状态"
  - originalEndStatus: AttendanceStatus   "修正前下班状态"
  - newStartTime: LocalDateTime      "修正后实际上班时间"
  - newEndTime: LocalDateTime        "修正后实际下班时间"
  - newStartStatus: AttendanceStatus "修正后上班状态（重新判定）"
  - newEndStatus: AttendanceStatus   "修正后下班状态（重新判定）"
  - reason: String                   "修正原因"
}

' 加班记录管理接口：HR手动录入或外部系统导入
class OvertimeRecordController {
  --
  + createOvertimeRecord(dto)
  + updateOvertimeRecord(id, dto)
  + deleteOvertimeRecord(id)
  + getOvertimeRecordList(query)
  + getOvertimeRecordDetail(id)
}

' ==================== 关系 ====================

AttendanceGroup "1" -- "*" AttendanceGroupRule
AttendanceGroup "1" -- "*" AttendanceSchedule : 排班制
AttendanceRecord "*" -- "1" AttendanceGroup
AttendanceRecord "*" -- "1" WorkCalendar
AttendanceRecord "1" -- "*" AttendanceCorrectionLog : 修正追溯
AttendanceRecord --> AttendanceStatus

LeaveRequest "*" -- "1" LeaveType
LeaveRequest "1" -- "*" LeaveApprovalLog
LeaveRequest --> LeaveRequestStatus
LeaveApprovalLog --> ApprovalStatus

SupplementCardRequest --> CardType
SupplementCardRequest --> LeaveRequestStatus

EmployeeLeaveBalance "*" -- "1" LeaveType

OvertimeRecord "*" -- "1" EmployeeLeaveBalance : 转入调休余额

AttendanceStatistics "*" -- "1" AttendanceRecord : 汇总生成

EmployeeLeaveBalance "1" -- "*" LeaveRequest : 扣减余额

@enduml
```

[PlantUML在线绘制_PlantUML在线编辑器官网_PlantUML在线编译器免下载 - ProcessOn](https://www.processon.com/plantuml)

## 时序图

> 描述考勤管理核心交互的调用时序。

### 6-1 打卡时序

![1.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/1X3lE5wMAYbjAlJb/img/48e98464-8397-4f39-ac46-aa6116e8b47a.png)

```plain
@startuml
actor 员工
participant "AttendanceController" as AC
participant "AttendanceService" as AS
participant "AttendanceRecordMapper" as ARM
database "MySQL" as DB

员工 -> AC: POST /api/v1/attendance/clock\n{ clockType: 1 }
AC -> AC: 获取当前登录用户ID
AC -> AS: clock(employeeId, clockType)

AS -> AS: 校验打卡条件\n(IP白名单/GPS)
note right: 根据考勤组配置

AS -> ARM: 查询当天打卡记录\n(employeeId + today)
ARM -> DB: SELECT
DB --> ARM: attendance_record
ARM --> AS: record

alt 上班打卡 (clockType = 1)
    AS -> AS: 校验是否已有上班打卡
    alt 已有上班打卡
        AS --> AC: 提示"已完成上班打卡"
    else 无上班打卡
        AS -> AS: 判定上班打卡状态\n(正常/迟到/旷工)
        AS -> ARM: UPDATE actual_start_time, start_status
        ARM -> DB: UPDATE
        AS --> AC: 打卡结果
    end

else 下班打卡 (clockType = 2)
    AS -> AS: 校验是否已有下班打卡
    alt 已有下班打卡
        AS --> AC: 提示"已完成下班打卡"
    else 无下班打卡
        AS -> AS: 判定下班打卡状态\n(正常/早退/旷工)
        AS -> ARM: UPDATE actual_end_time, end_status
        ARM -> DB: UPDATE
        AS --> AC: 打卡结果
    end
end

AC --> 员工: 返回打卡状态
@enduml
```

### 6-2 请假申请时序

![2.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/1X3lE5wMAYbjAlJb/img/edd6becc-fa99-49c6-b562-a62757ee221c.png)

```plain
@startuml
actor 员工
participant "LeaveController" as LC
participant "LeaveService" as LS
participant "LeaveBalanceService" as LBS
participant "ApprovalRoutingService" as ARS
participant "LeaveRequestMapper" as LRM
database "MySQL" as DB

员工 -> LC: POST /api/v1/leave/requests
LC -> LC: 校验 JSR-303 注解
LC -> LS: submitLeaveRequest(dto)

LS -> LS: 计算请假天数\n(排除休息日与节假日)

alt 年假/调休
    LS -> LBS: 校验假期余额
    LBS -> DB: 查询 employee_leave_balance
    alt 余额不足
        LBS --> LS: 余额不足
        LS --> LC: 提示"假期余额不足"
    end
end

LS -> ARS: 确定审批流\n(根据请假类型+天数)
ARS --> LS: 审批链 [直接上级, 部门负责人, ...]

LS -> LRM: INSERT leave_request + leave_approval_log
LRM -> DB: INSERT

alt 病假/婚假/产假
    LS -> LS: 校验附件必传
end

LS --> LC: 请假申请提交成功
LC --> 员工: 返回申请ID + 审批状态
@enduml
```

### 6-3 审批流转时序

![3.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/1X3lE5wMAYbjAlJb/img/d4fde762-af9b-4426-9f56-b38bdea01cfd.png)

```plain
@startuml
actor 审批人
participant "LeaveController" as LC
participant "LeaveService" as LS
participant "LeaveBalanceService" as LBS
participant "NotificationService" as NS
database "MySQL" as DB

审批人 -> LC: POST /api/v1/leave/requests/{id}/approve
LC -> LS: approve(requestId, approverId, action, comment)

LS -> DB: 查询 leave_request + leave_approval_log
LS -> LS: 校验当前审批人权限\n(是否为当前级别审批人)

alt 拒绝
    LS -> DB: UPDATE leave_request.status = REJECTED
    LS -> DB: UPDATE leave_approval_log.status = REJECTED
    LS -> NS: 发送拒绝通知给申请人

else 转交
    LS -> DB: UPDATE leave_approval_log.status = TRANSFERRED
    LS -> DB: INSERT new leave_approval_log (新审批人)
    LS -> NS: 发送审批通知给新审批人

else 通过
    LS -> DB: UPDATE leave_approval_log.status = APPROVED
    alt 还有下一级审批
        LS -> DB: INSERT/UPDATE 下一级 leave_approval_log
        LS -> NS: 发送审批通知给下一级审批人
    else 最后一级审批通过
        LS -> DB: UPDATE leave_request.status = APPROVED
        alt 年假/调休
            LS -> LBS: 扣减假期余额
            LBS -> DB: UPDATE employee_leave_balance
        end
        LS -> NS: 发送通过通知给申请人
    end
end

LS --> LC: 审批操作成功
LC --> 审批人: 返回审批结果
@enduml
```

### 6-4 考勤统计时序

![4.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/1X3lE5wMAYbjAlJb/img/e45951a0-58ef-41c1-bb52-e99e002be1d9.png)

```plain
@startuml
participant "ScheduledTask" as ST
participant "StatisticsService" as SS
participant "AttendanceRecordMapper" as ARM
participant "LeaveRequestMapper" as LRM
participant "EmployeeLeaveBalanceMapper" as ELBM
participant "AttendanceStatisticsMapper" as ASM
database "MySQL" as DB

ST -> SS: generateMonthlyStatistics(year, month)
note right: 每月第1天 02:00 触发

SS -> SS: 锁定上月考勤数据\n(设置统计标记，禁止修改)

SS -> DB: 查询所有在职员工（统计期间内）
SS -> DB: 查询 work_calendar（上月工作日天数）

loop 遍历每位员工

    SS -> ARM: 查询员工上月所有打卡记录
    ARM -> DB: SELECT
    DB --> ARM: attendance_records
    ARM --> SS: records

    SS -> LRM: 查询员工上月已审批请假
    LRM -> DB: SELECT
    DB --> LRM: leave_requests (APPROVED)
    LRM --> SS: approved_leaves

    SS -> ELBM: 查询员工当前年假余额
    ELBM -> DB: SELECT
    DB --> ELBM: leave_balance
    ELBM --> SS: annualLeaveBalance

    SS -> SS: 计算统计指标\n(出勤/迟到/早退/旷工/请假/加班)

    SS -> ASM: INSERT attendance_statistics
    ASM -> DB: INSERT

end

SS -> SS: 汇总部门维度统计
SS -> DB: 缓存部门统计数据

SS --> ST: 统计完成
@enduml
```

## 数据库设计

> 考勤管理模块涉及的核心数据表。

### 考勤组表

#### attendance\_group

```sql
CREATE TABLE IF NOT EXISTS `attendance_group` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`                    VARCHAR(64)       NOT NULL COMMENT '考勤组名称，如"标准工时组"、"弹性工时组"',
    `shift_type`              TINYINT           NOT NULL COMMENT '班次类型：1=固定班 2=弹性班 3=排班制',
    `start_time`              TIME              NOT NULL COMMENT '上班时间，如09:00',
    `end_time`                TIME              NOT NULL COMMENT '下班时间，如18:00',
    `rest_start_time`         TIME                       COMMENT '午休开始时间，如12:00',
    `rest_end_time`           TIME                       COMMENT '午休结束时间，如13:00',
    `flex_start_time`         TIME                       COMMENT '弹性最早打卡时间（弹性班适用）',
    `flex_end_time`           TIME                       COMMENT '弹性最晚打卡时间（弹性班适用）',
    `late_threshold`          INT               NOT NULL DEFAULT 15 COMMENT '迟到阈值（分钟），超过视为迟到',
    `early_leave_threshold`   INT               NOT NULL DEFAULT 15 COMMENT '早退阈值（分钟），提前超过视为早退',
    `ip_whitelist`            VARCHAR(512)               COMMENT 'IP白名单，逗号分隔多个IP地址或网段（如192.168.1.0/24）',
    `gps_latitude`            DECIMAL(10,7)               COMMENT 'GPS定位纬度（打卡地点校验）',
    `gps_longitude`           DECIMAL(10,7)               COMMENT 'GPS定位经度（打卡地点校验）',
    `gps_radius`              INT                        COMMENT 'GPS有效半径（米），超出范围拒绝打卡',
    `core_start_time`         TIME                       COMMENT '核心工作开始时间（弹性班必须在岗），如10:00',
    `core_end_time`           TIME                       COMMENT '核心工作结束时间（弹性班必须在岗），如16:00',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`              TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_is_deleted` (`is_deleted`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤组表，定义考勤规则模板';

> **说明**：系统初始化时预置一条默认考勤组记录（id=1，固定班 09:00-18:00，午休 12:00-13:00，迟到/早退阈值各 15 分钟，不配置 IP/GPS 限制），`is_deleted = 0` 且不允许删除。员工在 `attendance_group_rule` 中未匹配到任何考勤组时，自动归入此默认考勤组。
```

### 考勤组适用规则表

#### attendancegrouprule

```sql
CREATE TABLE IF NOT EXISTS `attendance_group_rule` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `attendance_group_id`     BIGINT UNSIGNED   NOT NULL COMMENT '考勤组ID，关联attendance_group.id',
    `rule_type`               TINYINT           NOT NULL COMMENT '适用类型：1=按部门 2=按职位 3=按个人',
    `target_id`               BIGINT UNSIGNED   NOT NULL COMMENT '目标ID（部门ID/职位ID/员工ID）',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_attendance_group_id` (`attendance_group_id`),
    KEY `idx_rule_type_target` (`rule_type`, `target_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤组适用人员规则表，定义考勤组的人员适用范围';
```

### 排班表

#### attendance\_schedule

> **说明**：排班制专用表。当考勤组 `shift_type = 3`（排班制）时，通过此表定义每日的具体班次时间。排班制的打卡记录预生成和打卡状态判定均以此表为准。

```sql
CREATE TABLE IF NOT EXISTS `attendance_schedule` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `attendance_group_id`     BIGINT UNSIGNED   NOT NULL COMMENT '考勤组ID，关联attendance_group.id',
    `schedule_date`           DATE              NOT NULL COMMENT '排班日期',
    `start_time`              TIME              NOT NULL COMMENT '上班时间，如09:00',
    `end_time`                TIME              NOT NULL COMMENT '下班时间，如18:00',
    `rest_start_time`         TIME                       COMMENT '午休开始时间',
    `rest_end_time`           TIME                       COMMENT '午休结束时间',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_date` (`attendance_group_id`, `schedule_date`),
    KEY `idx_schedule_date` (`schedule_date`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '排班表，排班制考勤组按日期定义每日班次时间';
```

### 工作日历表

#### work\_calendar

```sql
CREATE TABLE IF NOT EXISTS `work_calendar` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `calendar_date`           DATE              NOT NULL COMMENT '日期',
    `day_type`                TINYINT           NOT NULL COMMENT '日期类型：1=工作日 2=休息日 3=节假日',
    `holiday_name`            VARCHAR(64)                COMMENT '节假日名称，如"春节"、"国庆节"',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_calendar_date` (`calendar_date`),
    KEY `idx_day_type` (`day_type`),
    KEY `idx_year_month` ((YEAR(`calendar_date`)), (MONTH(`calendar_date`)))
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '工作日历表，记录每日的工作/休息/节假日类型';
```

### 打卡记录表

#### attendance\_record

```sql
CREATE TABLE IF NOT EXISTS `attendance_record` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID，关联employee.id',
    `attendance_date`         DATE              NOT NULL COMMENT '考勤日期',
    `attendance_group_id`     BIGINT UNSIGNED   NOT NULL COMMENT '当天适用的考勤组ID，关联attendance_group.id',
    `scheduled_start_time`    TIME                       COMMENT '规定上班时间（取自考勤组）',
    `scheduled_end_time`      TIME                       COMMENT '规定下班时间（取自考勤组）',
    `actual_start_time`       DATETIME                   COMMENT '实际上班打卡时间',
    `actual_end_time`         DATETIME                   COMMENT '实际下班打卡时间',
    `start_status`            TINYINT                    COMMENT '上班状态：1=正常 2=迟到 3=旷工半天 4=缺卡',
    `end_status`              TINYINT                    COMMENT '下班状态：1=正常 2=早退 3=旷工半天 4=缺卡',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_date` (`employee_id`, `attendance_date`),
    KEY `idx_attendance_date` (`attendance_date`),
    KEY `idx_start_status` (`start_status`),
    KEY `idx_end_status` (`end_status`),
    KEY `idx_employee_date_status` (`employee_id`, `attendance_date`, `start_status`, `end_status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '打卡记录表，每日由定时任务预生成，打卡时更新实际时间与状态';
```

### 请假申请表

#### leave\_request

```sql
CREATE TABLE IF NOT EXISTS `leave_request` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '申请人ID，关联employee.id',
    `leave_type`              TINYINT           NOT NULL COMMENT '请假类型：1=年假 2=病假 3=事假 4=婚假 5=产假 6=丧假 7=调休',
    `start_time`              DATETIME          NOT NULL COMMENT '请假开始时间（日期+时段）',
    `end_time`                DATETIME          NOT NULL COMMENT '请假结束时间（日期+时段）',
    `leave_days`              DECIMAL(4,1)      NOT NULL COMMENT '请假天数，支持0.5天',
    `reason`                  VARCHAR(256)      NOT NULL COMMENT '请假事由',
    `handover_employee_id`    BIGINT UNSIGNED            COMMENT '工作交接人ID，关联employee.id',
    `attachment_url`          VARCHAR(512)               COMMENT '附件URL（病假/婚假/产假证明材料）',
    `status`                  TINYINT           NOT NULL DEFAULT 1 COMMENT '状态：1=草稿 2=审批中 3=已通过 4=已拒绝 5=已取消',
    `current_approval_level`  INT               NOT NULL DEFAULT 0 COMMENT '当前审批级别（0=未进入审批, 1=第一级, 2=第二级）',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`              TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_status` (`status`),
    KEY `idx_leave_type` (`leave_type`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_employee_status` (`employee_id`, `status`),
    KEY `idx_is_deleted` (`is_deleted`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '请假申请表';
```

### 请假审批记录表

#### leaveapprovallog

```sql
CREATE TABLE IF NOT EXISTS `leave_approval_log` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `leave_request_id`        BIGINT UNSIGNED   NOT NULL COMMENT '请假申请ID，关联leave_request.id',
    `approver_id`             BIGINT UNSIGNED   NOT NULL COMMENT '审批人ID，关联employee.id',
    `approval_level`          INT               NOT NULL COMMENT '审批级别：1=第一级 2=第二级',
    `status`                  TINYINT           NOT NULL DEFAULT 1 COMMENT '审批状态：1=待审批 2=通过 3=拒绝 4=转交',
    `comment`                 VARCHAR(256)               COMMENT '审批意见',
    `original_approver_id`    BIGINT UNSIGNED            COMMENT '原始审批人ID（转交/委托场景，记录原审批人）',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_leave_request_id` (`leave_request_id`),
    KEY `idx_approver_id` (`approver_id`),
    KEY `idx_status` (`status`),
    KEY `idx_approver_status` (`approver_id`, `status`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '请假审批记录表，记录每级审批的流转详情';
```

### 补卡申请表

#### supplementcardrequest

```sql
CREATE TABLE IF NOT EXISTS `supplement_card_request` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '申请人ID，关联employee.id',
    `attendance_date`         DATE              NOT NULL COMMENT '补卡日期（缺卡日期）',
    `card_type`               TINYINT           NOT NULL COMMENT '补卡类型：1=上班卡 2=下班卡',
    `reason`                  VARCHAR(256)      NOT NULL COMMENT '补卡原因',
    `status`                  TINYINT           NOT NULL DEFAULT 1 COMMENT '状态：1=草稿 2=审批中 3=已通过 4=已拒绝',
    `approver_id`             BIGINT UNSIGNED            COMMENT '审批人ID，关联employee.id',
    `approval_comment`        VARCHAR(256)               COMMENT '审批意见',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`              TINYINT           NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=否 1=是',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_attendance_date` (`attendance_date`),
    KEY `idx_status` (`status`),
    KEY `idx_employee_date_card` (`employee_id`, `attendance_date`, `card_type`),
    KEY `idx_is_deleted` (`is_deleted`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '补卡申请表，独立于请假流程，每月最多2次';
```

### 打卡修正日志表

#### attendancecorrectionlog

> **说明**：记录 HR/管理员对打卡记录的手动修正操作，用于审计追溯。

```sql
CREATE TABLE IF NOT EXISTS `attendance_correction_log` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `attendance_record_id`    BIGINT UNSIGNED   NOT NULL COMMENT '打卡记录ID，关联attendance_record.id',
    `operator_id`             BIGINT UNSIGNED   NOT NULL COMMENT '操作人ID（HR/系统管理员），关联employee.id',
    `original_start_time`     DATETIME                   COMMENT '修正前的实际上班时间',
    `original_end_time`       DATETIME                   COMMENT '修正前的实际下班时间',
    `original_start_status`   TINYINT                    COMMENT '修正前的上班状态',
    `original_end_status`     TINYINT                    COMMENT '修正前的下班状态',
    `new_start_time`          DATETIME                   COMMENT '修正后的实际上班时间',
    `new_end_time`            DATETIME                   COMMENT '修正后的实际下班时间',
    `new_start_status`        TINYINT                    COMMENT '修正后的上班状态（重新判定）',
    `new_end_status`          TINYINT                    COMMENT '修正后的下班状态（重新判定）',
    `reason`                  VARCHAR(256)      NOT NULL COMMENT '修正原因',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_attendance_record_id` (`attendance_record_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '打卡修正日志表，记录手动修正操作审计';
```

### 员工假期余额表

#### employeeleavebalance

```sql
CREATE TABLE IF NOT EXISTS `employee_leave_balance` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID，关联employee.id',
    `year`                    SMALLINT          NOT NULL COMMENT '年份，如2026',
    `leave_type`              TINYINT           NOT NULL COMMENT '假期类型：1=年假 7=调休',
    `total_days`              DECIMAL(6,1)      NOT NULL DEFAULT 0.0 COMMENT '总天数',
    `used_days`               DECIMAL(6,1)      NOT NULL DEFAULT 0.0 COMMENT '已使用天数',
    `remaining_days`          DECIMAL(6,1)      NOT NULL DEFAULT 0.0 COMMENT '剩余可用天数（计算列：total - used）',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_year_type` (`employee_id`, `year`, `leave_type`),
    KEY `idx_employee_id` (`employee_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工假期余额表，按年+类型记录年假和调休余额';
```

### 加班记录表

#### overtime\_record

> **说明**：加班记录由 HR 手动录入或外部系统导入，用于调休余额计算。创建后自动将加班时长 1:1 转入调休余额。

```sql
CREATE TABLE IF NOT EXISTS `overtime_record` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID，关联employee.id',
    `overtime_date`           DATE              NOT NULL COMMENT '加班日期',
    `start_time`              DATETIME          NOT NULL COMMENT '加班开始时间',
    `end_time`                DATETIME          NOT NULL COMMENT '加班结束时间',
    `hours`                   DECIMAL(5,1)      NOT NULL COMMENT '加班小时数',
    `is_used`                 TINYINT           NOT NULL DEFAULT 0 COMMENT '是否已转为调休余额：0=未转 1=已转',
    `expire_date`             DATE              NOT NULL COMMENT '调休有效期截止日期（加班当月+次月末）',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_overtime_date` (`overtime_date`),
    KEY `idx_expire_date` (`expire_date`),
    KEY `idx_is_used` (`is_used`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '加班记录表，用于调休余额计算';
```

### 考勤月度统计表

#### attendance\_statistics

```sql
CREATE TABLE IF NOT EXISTS `attendance_statistics` (
    `id`                      BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`             BIGINT UNSIGNED   NOT NULL COMMENT '员工ID，关联employee.id',
    `stat_year`               SMALLINT          NOT NULL COMMENT '统计年份',
    `stat_month`              TINYINT           NOT NULL COMMENT '统计月份（1-12）',
    `scheduled_days`          DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '应出勤天数',
    `actual_days`             DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '实际出勤天数',
    `late_count`              INT               NOT NULL DEFAULT 0 COMMENT '迟到次数',
    `early_leave_count`       INT               NOT NULL DEFAULT 0 COMMENT '早退次数',
    `absent_days`             DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '旷工天数',
    `leave_days`              DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '请假天数合计（所有类型）',
    `annual_leave_days`       DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '年假天数',
    `sick_leave_days`         DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '病假天数',
    `personal_leave_days`     DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '事假天数',
    `marriage_leave_days`     DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '婚假天数',
    `maternity_leave_days`    DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '产假天数',
    `funeral_leave_days`      DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '丧假天数',
    `comp_time_leave_days`    DECIMAL(4,1)      NOT NULL DEFAULT 0.0 COMMENT '调休天数',
    `overtime_hours`          DECIMAL(5,1)      NOT NULL DEFAULT 0.0 COMMENT '加班小时数合计',
    `annual_leave_balance`    DECIMAL(4,1)               COMMENT '截至当月月末的年假余额',
    `comp_time_balance`       DECIMAL(5,1)               COMMENT '截至当月月末的调休余额',
    `create_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_year_month` (`employee_id`, `stat_year`, `stat_month`),
    KEY `idx_stat_year_month` (`stat_year`, `stat_month`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_department_stat` (`stat_year`, `stat_month`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '考勤月度统计表，每月定时任务汇总生成';
```

## API 设计

> 考勤管理模块核心接口。

### 打卡

```plain
POST /api/v1/attendance/clock
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| clockType | Integer | 是 | 打卡类型：1=上班打卡 2=下班打卡 |

#### 请求示例

```json
{
  "clockType": 1
}
```
> **说明**：前端提供"上班打卡"和"下班打卡"两个独立按钮，用户明确选择后传入对应的 `clockType`。打卡前依次校验：① Redis 分布式锁防重复提交（`clock:{employeeId}:{date}`）；② 当天是否有已通过的请假记录（全天请假或当前时段在请假范围内则拒绝）；③ IP 白名单/GPS 定位（根据考勤组配置）。上班打卡已有记录时拒绝重复；下班打卡不校验上班打卡是否存在。若当天无预生成的打卡记录（如临时调班），则自动创建。

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "attendanceDate": "2026-07-10",
    "clockType": 1,
    "clockTypeDesc": "上班打卡",
    "actualTime": "2026-07-10T08:55:00",
    "status": 1,
    "statusDesc": "正常",
    "scheduledTime": "09:00"
  }
}
```
---

### 查询打卡记录列表

```plain
GET /api/v1/attendance/records
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| employeeId | Long | 否 | 员工ID（HR/部门主管查询他人时传入，普通员工仅查自己） |
| departmentId | Long | 否 | 部门ID（HR可筛选指定部门的打卡记录；部门主管仅可传本部门及下属部门ID） |
| startDate | Date | 否 | 考勤日期起始 |
| endDate | Date | 否 | 考勤日期截止 |
| startStatus | Integer | 否 | 上班状态筛选：1=正常 2=迟到 3=旷工半天 4=缺卡 |
| endStatus | Integer | 否 | 下班状态筛选：1=正常 2=早退 3=旷工半天 4=缺卡 |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 22,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 5001,
        "employeeId": 1001,
        "employeeName": "张三",
        "attendanceDate": "2026-07-10",
        "scheduledStartTime": "09:00",
        "scheduledEndTime": "18:00",
        "actualStartTime": "2026-07-10T08:55:00",
        "actualEndTime": "2026-07-10T18:02:00",
        "startStatus": 1,
        "startStatusDesc": "正常",
        "endStatus": 1,
        "endStatusDesc": "正常"
      }
    ]
  }
}
```
> **说明**：数据范围按角色过滤——普通员工仅返回本人记录；部门主管返回本部门及下属部门员工记录；HR 返回全员记录。

---

### 考勤日历视图

```plain
GET /api/v1/attendance/records/calendar
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份（1-12） |
| employeeId | Long | 否 | 员工ID（HR/部门主管可查他人，普通员工仅查自己） |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "year": 2026,
    "month": 7,
    "days": [
      {
        "date": "2026-07-01",
        "dayType": 1,
        "dayTypeDesc": "工作日",
        "startStatus": 1,
        "startStatusDesc": "正常",
        "endStatus": 1,
        "endStatusDesc": "正常",
        "hasLeave": false
      },
      {
        "date": "2026-07-05",
        "dayType": 2,
        "dayTypeDesc": "休息日",
        "startStatus": null,
        "endStatus": null,
        "hasLeave": false
      },
      {
        "date": "2026-07-10",
        "dayType": 1,
        "dayTypeDesc": "工作日",
        "startStatus": 4,
        "startStatusDesc": "缺卡",
        "endStatus": 1,
        "endStatusDesc": "正常",
        "hasLeave": false
      }
    ],
    "summary": {
      "normalDays": 20,
      "lateDays": 1,
      "earlyLeaveDays": 0,
      "absentDays": 0,
      "cardMissingDays": 1,
      "leaveDays": 2
    }
  }
}
```
---

### 提交请假申请

```plain
POST /api/v1/leave/requests
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| leaveType | Integer | 是 | 请假类型：1=年假 2=病假 3=事假 4=婚假 5=产假 6=丧假 7=调休 |
| startTime | DateTime | 是 | 开始时间（日期+时段），如 2026-07-15T09:00:00 |
| endTime | DateTime | 是 | 结束时间（日期+时段），如 2026-07-15T18:00:00 |
| reason | String | 是 | 请假事由 |
| handoverEmployeeId | Long | 否 | 工作交接人ID |
| attachmentUrl | String | 条件必填 | 证明材料附件（病假>1天/婚假/产假必传） |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 3001,
    "leaveDays": 1.0,
    "status": 2,
    "statusDesc": "审批中",
    "approvalChain": [
      {"level": 1, "approverId": 1000, "approverName": "李四", "status": 1, "statusDesc": "待审批"},
      {"level": 2, "approverId": 1010, "approverName": "王五", "status": 1, "statusDesc": "待审批"}
    ]
  }
}
```
> **说明**：年假/调休类型需校验余额，不足时拒绝提交。审批流根据请假类型+天数自动路由。系统自动计算请假天数（排除休息日和节假日，支持 0.5 天）。

---

### 请假审批

```plain
POST /api/v1/leave/requests/{id}/approve
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 请假申请ID（路径参数） |
| action | String | 是 | 审批动作：APPROVE=通过 REJECT=拒绝 TRANSFER=转交 |
| comment | String | 否（拒绝时必填） | 审批意见 |
| transferToId | Long | 否（转交时必填） | 转交目标审批人ID |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "leaveRequestId": 3001,
    "action": "APPROVE",
    "actionDesc": "通过",
    "currentLevel": 1,
    "totalLevels": 2,
    "isFinished": false,
    "nextApproverId": 1010,
    "nextApproverName": "王五"
  }
}
```
> **说明**：仅当前级别的审批人可操作。转交时原审批记录标记为"转交"，新建一条新审批人的审批记录。通过后若还有下一级则流转，若无则完成并扣减假期余额。

---

### 查询假期余额

```plain
GET /api/v1/leave/balances
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| employeeId | Long | 否 | 员工ID（HR/部门主管可查他人，普通员工仅查自己） |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "employeeId": 1001,
    "balances": [
      {
        "leaveType": 1,
        "leaveTypeDesc": "年假",
        "totalDays": 5.0,
        "usedDays": 2.0,
        "remainingDays": 3.0
      },
      {
        "leaveType": 7,
        "leaveTypeDesc": "调休",
        "totalDays": 2.0,
        "usedDays": 0.0,
        "remainingDays": 2.0
      }
    ]
  }
}
```
---

### 提交补卡申请

```plain
POST /api/v1/attendance/supplement-cards
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| attendanceDate | Date | 是 | 补卡日期（必须是缺卡日期） |
| cardType | Integer | 是 | 补卡类型：1=上班卡 2=下班卡 |
| reason | String | 是 | 补卡原因 |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 4001,
    "attendanceDate": "2026-07-10",
    "cardType": 1,
    "cardTypeDesc": "上班卡",
    "status": 2,
    "statusDesc": "审批中",
    "monthlyCount": 1,
    "monthlyLimit": 2
  }
}
```
> **说明**：提交时校验当月补卡次数（≤2 次）、补卡日期必须有对应的缺卡记录。审批人为员工的直接上级。

---

### 补卡审批

```plain
POST /api/v1/attendance/supplement-cards/{id}/approve
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 补卡申请ID（路径参数） |
| action | String | 是 | 审批动作：APPROVE=通过 REJECT=拒绝 |
| comment | String | 否（拒绝时必填） | 审批意见 |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "supplementCardId": 4001,
    "action": "APPROVE",
    "actionDesc": "通过",
    "attendanceDate": "2026-07-10",
    "cardType": 1,
    "cardTypeDesc": "上班卡",
    "filledTime": "09:00",
    "newStatus": 1,
    "newStatusDesc": "正常"
  }
}
```
> **说明**：仅直接上级可审批。通过后取考勤组规定的上下班时间作为补卡的实际打卡时间，并根据打卡状态判定规则重新判定 start_status/end_status。审批流仅一级（直接上级），无多级流转。

---

### 查询补卡申请列表

```plain
GET /api/v1/attendance/supplement-cards
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| employeeId | Long | 否 | 申请人ID（HR可查全部，普通员工仅查自己） |
| status | Integer | 否 | 状态筛选：1=草稿 2=审批中 3=已通过 4=已拒绝 |
| startDate | Date | 否 | 补卡日期起始 |
| endDate | Date | 否 | 补卡日期截止 |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 4001,
        "employeeId": 1001,
        "employeeName": "张三",
        "departmentName": "技术部",
        "attendanceDate": "2026-07-10",
        "cardType": 1,
        "cardTypeDesc": "上班卡",
        "reason": "忘记打卡",
        "status": 2,
        "statusDesc": "审批中",
        "approverId": 1000,
        "approverName": "李四",
        "createTime": "2026-07-11T09:00:00"
      }
    ]
  }
}
```
> **说明**：数据范围按角色过滤——普通员工仅返回本人记录；部门主管返回本部门及下属部门员工记录；HR返回全员记录。审批人的"待审批"列表通过 `status=2` 且 `approver_id = 当前用户ID` 筛选。

---

### 打卡记录手动修正

```plain
PUT /api/v1/attendance/records/{id}/correct
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 打卡记录ID（路径参数） |
| actualStartTime | DateTime | 否 | 修正后的实际上班时间 |
| actualEndTime | DateTime | 否 | 修正后的实际下班时间 |
| reason | String | 是 | 修正原因 |

#### 请求示例

```json
{
  "actualStartTime": "2026-07-10T08:55:00",
  "actualEndTime": "2026-07-10T18:05:00",
  "reason": "员工反映系统故障导致打卡记录异常，经核实予以修正"
}
```

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 5001,
    "employeeId": 1001,
    "attendanceDate": "2026-07-10",
    "actualStartTime": "2026-07-10T08:55:00",
    "actualEndTime": "2026-07-10T18:05:00",
    "startStatus": 1,
    "startStatusDesc": "正常",
    "endStatus": 1,
    "endStatusDesc": "正常"
  }
}
```
> **说明**：仅 HR 专员和系统管理员可操作。修正后系统根据打卡状态判定规则重新判定 start_status/end_status。修正操作记录到 `attendance_correction_log` 表，原值和新值均保留用于审计。若统计月份已锁定（已生成 attendance\_statistics），需先解锁或手动重算统计。

---

### 查询个人考勤统计

```plain
GET /api/v1/attendance/statistics/personal
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份（1-12） |
| employeeId | Long | 否 | 员工ID（HR/部门主管可查他人，普通员工仅查自己） |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "employeeId": 1001,
    "employeeName": "张三",
    "departmentName": "技术部",
    "statYear": 2026,
    "statMonth": 7,
    "scheduledDays": 22.0,
    "actualDays": 21.0,
    "lateCount": 1,
    "earlyLeaveCount": 0,
    "absentDays": 0.0,
    "leaveDays": 1.0,
    "annualLeaveDays": 0.0,
    "sickLeaveDays": 1.0,
    "personalLeaveDays": 0.0,
    "overtimeHours": 6.0,
    "annualLeaveBalance": 3.0,
    "compTimeBalance": 6.0
  }
}
```
---

### 查询部门考勤统计

```plain
GET /api/v1/attendance/statistics/department
```

#### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份（1-12） |
| departmentId | Long | 否 | 部门ID（HR 可筛选，部门主管仅看本部门） |

#### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "statYear": 2026,
    "statMonth": 7,
    "departmentId": 10,
    "departmentName": "技术部",
    "totalEmployees": 45,
    "attendanceRate": 0.955,
    "lateRate": 0.022,
    "leaveRate": 0.045,
    "details": [
      {
        "employeeId": 1001,
        "employeeName": "张三",
        "scheduledDays": 22.0,
        "actualDays": 21.0,
        "lateCount": 1,
        "absentDays": 0.0,
        "leaveDays": 1.0
      }
    ]
  }
}
```

### 图表可视化数据接口

#### 部门出勤率趋势（折线图）

```plain
GET /api/v1/attendance/statistics/charts/attendance-rate
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| months | Integer | 否 | 近 N 个月，默认 6 |
| departmentIds | Long\[\] | 否 | 部门ID列表，不传则返回全部部门 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "months": ["2026-02", "2026-03", "2026-04", "2026-05", "2026-06", "2026-07"],
    "series": [
      {"departmentName": "技术部", "rates": [0.96, 0.95, 0.97, 0.94, 0.96, 0.955]},
      {"departmentName": "产品部", "rates": [0.98, 0.97, 0.98, 0.96, 0.97, 0.98]}
    ]
  }
}
```

#### 请假类型分布（饼图/环形图）

```plain
GET /api/v1/attendance/statistics/charts/leave-distribution
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份（1-12） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {"leaveTypeDesc": "年假", "days": 35.0, "percentage": 0.35},
    {"leaveTypeDesc": "病假", "days": 18.0, "percentage": 0.18},
    {"leaveTypeDesc": "事假", "days": 22.0, "percentage": 0.22},
    {"leaveTypeDesc": "婚假", "days": 3.0, "percentage": 0.03},
    {"leaveTypeDesc": "产假", "days": 0.0, "percentage": 0.0},
    {"leaveTypeDesc": "丧假", "days": 2.0, "percentage": 0.02},
    {"leaveTypeDesc": "调休", "days": 20.0, "percentage": 0.20}
  ]
}
```

#### 迟到早退排行（柱状图）

```plain
GET /api/v1/attendance/statistics/charts/late-early-ranking
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份（1-12） |
| topN | Integer | 否 | 排行条数，默认 10 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {"departmentName": "技术部", "lateCount": 8, "earlyLeaveCount": 2},
    {"departmentName": "市场部", "lateCount": 12, "earlyLeaveCount": 5}
  ]
}
```
---

### 考勤组管理

#### 创建考勤组

```plain
POST /api/v1/attendance/groups
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| name | String | 是 | 考勤组名称，如"标准工时组" |
| shiftType | Integer | 是 | 班次类型：1=固定班 2=弹性班 3=排班制 |
| startTime | String | 是 | 上班时间，格式 HH:mm，如"09:00" |
| endTime | String | 是 | 下班时间，格式 HH:mm，如"18:00" |
| restStartTime | String | 否 | 午休开始时间，如"12:00" |
| restEndTime | String | 否 | 午休结束时间，如"13:00" |
| flexStartTime | String | 否 | 弹性最早打卡时间（弹性班适用） |
| flexEndTime | String | 否 | 弹性最晚打卡时间（弹性班适用） |
| lateThreshold | Integer | 是 | 迟到阈值（分钟），默认 15 |
| earlyLeaveThreshold | Integer | 是 | 早退阈值（分钟），默认 15 |
| ipWhitelist | String | 否 | IP白名单，逗号分隔（如"192.168.1.0/24,10.0.0.1"），不配则不校验 |
| gpsLatitude | BigDecimal | 否 | GPS定位纬度 |
| gpsLongitude | BigDecimal | 否 | GPS定位经度 |
| gpsRadius | Integer | 否 | GPS有效半径（米），经纬度和半径需同时提供才生效 |
| coreStartTime | String | 否 | 核心工作开始时间（弹性班适用），如"10:00" |
| coreEndTime | String | 否 | 核心工作结束时间（弹性班适用），如"16:00" |
| rules | List\<RuleDTO\> | 是 | 适用人员规则列表 |

**RuleDTO：**

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| ruleType | Integer | 是 | 适用类型：1=按部门 2=按职位 3=按个人 |
| targetId | Long | 是 | 目标ID（部门ID/职位ID/员工ID） |

##### 请求示例

```json
{
  "name": "标准工时组",
  "shiftType": 1,
  "startTime": "09:00",
  "endTime": "18:00",
  "restStartTime": "12:00",
  "restEndTime": "13:00",
  "lateThreshold": 15,
  "earlyLeaveThreshold": 15,
  "ipWhitelist": "192.168.1.0/24",
  "gpsLatitude": null,
  "gpsLongitude": null,
  "gpsRadius": null,
  "coreStartTime": null,
  "coreEndTime": null,
  "rules": [
    {"ruleType": 1, "targetId": 10},
    {"ruleType": 1, "targetId": 12}
  ]
}
```

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 2001,
    "name": "标准工时组"
  }
}
```
---

#### 更新考勤组

```plain
PUT /api/v1/attendance/groups/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 考勤组ID（路径参数） |
| name | String | 否 | 考勤组名称 |
| shiftType | Integer | 否 | 班次类型 |
| startTime | String | 否 | 上班时间 |
| endTime | String | 否 | 下班时间 |
| restStartTime | String | 否 | 午休开始时间 |
| restEndTime | String | 否 | 午休结束时间 |
| flexStartTime | String | 否 | 弹性最早打卡时间 |
| flexEndTime | String | 否 | 弹性最晚打卡时间 |
| lateThreshold | Integer | 否 | 迟到阈值 |
| earlyLeaveThreshold | Integer | 否 | 早退阈值 |
| ipWhitelist | String | 否 | IP白名单（传则更新，传null表示清空） |
| gpsLatitude | BigDecimal | 否 | GPS定位纬度 |
| gpsLongitude | BigDecimal | 否 | GPS定位经度 |
| gpsRadius | Integer | 否 | GPS有效半径 |
| coreStartTime | String | 否 | 核心工作开始时间 |
| coreEndTime | String | 否 | 核心工作结束时间 |
| rules | List\<RuleDTO\> | 否 | 适用人员规则列表（传则全量替换） |

##### 请求示例

```json
{
  "name": "标准工时组（修订）",
  "lateThreshold": 10
}
```

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 2001,
    "updatedFields": ["name", "lateThreshold"]
  }
}
```
> **说明**：更新考勤组后，次日生成的打卡记录才会应用新规则，当天已有的打卡记录不受影响。

---

#### 删除考勤组

```plain
DELETE /api/v1/attendance/groups/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 考勤组ID（路径参数） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```
> **说明**：逻辑删除。删除前校验是否仍有员工关联（`attendance_group_rule` 中有引用），若有关联则拒绝删除并提示"该考勤组下仍有适用人员，请先调整人员归属"。

---

#### 查询考勤组列表

```plain
GET /api/v1/attendance/groups
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| keyword | String | 否 | 考勤组名称模糊搜索 |
| shiftType | Integer | 否 | 班次类型筛选 |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 2001,
        "name": "标准工时组",
        "shiftType": 1,
        "shiftTypeDesc": "固定班",
        "startTime": "09:00",
        "endTime": "18:00",
        "lateThreshold": 15,
        "earlyLeaveThreshold": 15,
        "ruleSummary": "技术部、产品部（2个部门）",
        "createTime": "2026-01-15T09:00:00"
      }
    ]
  }
}
```
---

#### 查询考勤组详情

```plain
GET /api/v1/attendance/groups/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 考勤组ID（路径参数） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 2001,
    "name": "标准工时组",
    "shiftType": 1,
    "shiftTypeDesc": "固定班",
    "startTime": "09:00",
    "endTime": "18:00",
    "restStartTime": "12:00",
    "restEndTime": "13:00",
    "flexStartTime": null,
    "flexEndTime": null,
    "lateThreshold": 15,
    "earlyLeaveThreshold": 15,
    "ipWhitelist": "192.168.1.0/24",
    "gpsLatitude": null,
    "gpsLongitude": null,
    "gpsRadius": null,
    "coreStartTime": null,
    "coreEndTime": null,
    "rules": [
      {"ruleType": 1, "ruleTypeDesc": "按部门", "targetId": 10, "targetName": "技术部"},
      {"ruleType": 1, "ruleTypeDesc": "按部门", "targetId": 12, "targetName": "产品部"}
    ],
    "createTime": "2026-01-15T09:00:00",
    "updateTime": "2026-06-20T14:30:00"
  }
}
```

### 请假记录查询与操作

#### 修改请假申请

```plain
PUT /api/v1/leave/requests/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 请假申请ID（路径参数） |
| leaveType | Integer | 否 | 请假类型 |
| startTime | DateTime | 否 | 开始时间 |
| endTime | DateTime | 否 | 结束时间 |
| reason | String | 否 | 请假事由 |
| handoverEmployeeId | Long | 否 | 工作交接人ID（传 null 表示清空） |
| attachmentUrl | String | 否 | 附件URL |

##### 请求示例

```json
{
  "reason": "调整请假事由说明",
  "handoverEmployeeId": 1020
}
```

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 3001,
    "leaveDays": 1.5,
    "updatedFields": ["reason", "handoverEmployeeId"]
  }
}
```
> **说明**：仅 status=1（草稿）状态可修改。修改后系统重新计算请假天数。修改不会重置审批状态。

---

#### 删除请假申请

```plain
DELETE /api/v1/leave/requests/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 请假申请ID（路径参数） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```
> **说明**：仅 status=1（草稿）状态可删除，逻辑删除（`is_deleted = 1`）。审批中或已完成的请假记录不可删除。仅申请者本人能够进行删除

---

#### 查询请假申请列表

```plain
GET /api/v1/leave/requests
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| employeeId | Long | 否 | 申请人ID（HR/部门主管查询他人时传入，普通员工仅查自己） |
| leaveType | Integer | 否 | 请假类型筛选 |
| status | Integer | 否 | 状态筛选：1=草稿 2=审批中 3=已通过 4=已拒绝 5=已取消 |
| startDate | Date | 否 | 请假开始日期筛选 |
| endDate | Date | 否 | 请假结束日期筛选 |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 15,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 3001,
        "employeeId": 1001,
        "employeeName": "张三",
        "departmentName": "技术部",
        "leaveType": 1,
        "leaveTypeDesc": "年假",
        "startTime": "2026-07-15T09:00:00",
        "endTime": "2026-07-15T18:00:00",
        "leaveDays": 1.0,
        "status": 2,
        "statusDesc": "审批中",
        "currentApprovalLevel": 1,
        "totalApprovalLevels": 2,
        "createTime": "2026-07-10T10:00:00"
      }
    ]
  }
}
```
> **说明**：数据范围按角色过滤，普通员工仅返回本人的申请记录。

---

#### 查询请假申请详情

```plain
GET /api/v1/leave/requests/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 请假申请ID（路径参数） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 3001,
    "employeeId": 1001,
    "employeeName": "张三",
    "departmentName": "技术部",
    "leaveType": 1,
    "leaveTypeDesc": "年假",
    "startTime": "2026-07-15T09:00:00",
    "endTime": "2026-07-15T18:00:00",
    "leaveDays": 1.0,
    "reason": "个人事务",
    "handoverEmployeeId": 1002,
    "handoverEmployeeName": "李四",
    "attachmentUrl": null,
    "status": 2,
    "statusDesc": "审批中",
    "currentApprovalLevel": 1,
    "createTime": "2026-07-10T10:00:00",
    "updateTime": "2026-07-10T10:00:00",
    "approvalLogs": [
      {
        "approvalLevel": 1,
        "approverId": 1000,
        "approverName": "李四",
        "status": 1,
        "statusDesc": "待审批",
        "comment": null,
        "originalApproverName": null,
        "createTime": "2026-07-10T10:00:00"
      },
      {
        "approvalLevel": 2,
        "approverId": 1010,
        "approverName": "王五",
        "status": 1,
        "statusDesc": "待审批",
        "comment": null,
        "originalApproverName": null,
        "createTime": "2026-07-10T10:00:00"
      }
    ]
  }
}
```
---

#### 取消请假申请

```plain
POST /api/v1/leave/requests/{id}/cancel
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 请假申请ID（路径参数） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```
> **说明**：仅 status=2（审批中）状态可取消。取消后将所有待审批的 `leave_approval_log` 标记为已取消，`leave_request.status` 更新为 5（已取消）。若请假类型为年假/调休且已预扣余额，则恢复余额。

---

### 工作日历管理

#### 查询工作日历

```plain
GET /api/v1/work-calendar
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| year | Integer | 是 | 年份，如 2026 |
| month | Integer | 是 | 月份（1-12） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "year": 2026,
    "month": 7,
    "days": [
      {
        "calendarDate": "2026-07-01",
        "dayType": 1,
        "dayTypeDesc": "工作日",
        "holidayName": null
      },
      {
        "calendarDate": "2026-07-05",
        "dayType": 2,
        "dayTypeDesc": "休息日",
        "holidayName": null
      },
      {
        "calendarDate": "2026-07-04",
        "dayType": 3,
        "dayTypeDesc": "节假日",
        "holidayName": "国庆节补休"
      }
    ],
    "summary": {
      "workDays": 22,
      "restDays": 8,
      "holidays": 1
    }
  }
}
```
---

#### 手动触发节假日同步

```plain
POST /api/v1/work-calendar/sync-holidays
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| year | Integer | 是 | 同步年份，如 2026 |

##### 请求示例

```json
{
  "year": 2026
}
```

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "year": 2026,
    "added": 3,
    "updated": 1,
    "removed": 0,
    "details": [
      {"calendarDate": "2026-10-01", "operation": "ADDED", "dayType": 3, "holidayName": "国庆节"},
      {"calendarDate": "2026-10-02", "operation": "ADDED", "dayType": 3, "holidayName": "国庆节"},
      {"calendarDate": "2026-10-03", "operation": "ADDED", "dayType": 3, "holidayName": "国庆节"},
      {"calendarDate": "2026-09-27", "operation": "UPDATED", "dayType": 1, "holidayName": "国庆节调休上班"}
    ]
  }
}
```
> **说明**：同步来源为外部日历服务 API。同步完成后同时更新调休日（周末上班）标记。若外部服务不可用，返回错误提示并建议稍后重试。

---

### 加班记录管理

> **说明**：加班记录由 HR 手动录入或外部系统导入，用于调休余额计算。创建后自动转入调休余额。

#### 创建加班记录

```plain
POST /api/v1/overtime-records
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| employeeId | Long | 是 | 员工ID |
| overtimeDate | Date | 是 | 加班日期 |
| startTime | DateTime | 是 | 加班开始时间 |
| endTime | DateTime | 是 | 加班结束时间 |
| hours | BigDecimal | 是 | 加班小时数（系统自动计算或手动指定） |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 8001,
    "employeeId": 1001,
    "overtimeDate": "2026-07-05",
    "hours": 4.0,
    "expireDate": "2026-08-31",
    "compTimeAdded": 4.0
  }
}
```
> **说明**：创建后自动将加班时长 1:1 转入调休余额（`employee_leave_balance` 中 `leave_type=7`），有效期截止日期为加班当月+次月末（如7月加班，9月1日过期）。仅 HR 可操作。

---

#### 查询加班记录列表

```plain
GET /api/v1/overtime-records
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| employeeId | Long | 否 | 员工ID |
| startDate | Date | 否 | 加班日期起始 |
| endDate | Date | 否 | 加班日期截止 |
| isUsed | Integer | 否 | 是否已转为调休：0=未转 1=已转 |
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 10,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 8001,
        "employeeId": 1001,
        "employeeName": "张三",
        "departmentName": "技术部",
        "overtimeDate": "2026-07-05",
        "startTime": "2026-07-05T18:00:00",
        "endTime": "2026-07-05T22:00:00",
        "hours": 4.0,
        "isUsed": 0,
        "isUsedDesc": "未使用",
        "expireDate": "2026-08-31",
        "createTime": "2026-07-06T09:00:00"
      }
    ]
  }
}
```
> **说明**：仅 HR 可操作。支持按员工、日期范围筛选。

---

#### 编辑加班记录

```plain
PUT /api/v1/overtime-records/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 加班记录ID（路径参数） |
| overtimeDate | Date | 否 | 加班日期 |
| startTime | DateTime | 否 | 加班开始时间 |
| endTime | DateTime | 否 | 加班结束时间 |
| hours | BigDecimal | 否 | 加班小时数 |

> **说明**：修改后同步调整对应员工的调休余额。若加班记录已转为调休且已被使用，则拒绝修改。仅 HR 可操作。

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 8001,
    "overtimeDate": "2026-07-10",
    "hours": 1.5,
    "expireDate": "2026-10-10",
    "updatedFields": ["hours"]
  }
}
```
---

#### 删除加班记录

```plain
DELETE /api/v1/overtime-records/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 加班记录ID（路径参数） |

> **说明**：删除后同步扣减对应员工的调休余额（`total_days` 和 `remaining_days`）。若已使用的调休额度超过扣减后总额，则拒绝删除并提示"该加班记录对应的调休已被使用，无法删除"。仅 HR 可操作。

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```
---

### 排班管理

> **说明**：排班制专用。当考勤组 `shift_type = 3`（排班制）时，通过排班管理定义每日的具体班次时间。排班制的打卡记录预生成时以排班表而非考勤组默认时间为准。

#### 批量创建排班

```plain
POST /api/v1/attendance/schedules/batch
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| attendanceGroupId | Long | 是 | 考勤组ID |
| schedules | List\<ScheduleItemDTO\> | 是 | 排班列表 |

**ScheduleItemDTO：**

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| scheduleDate | Date | 是 | 排班日期 |
| startTime | String | 是 | 上班时间，格式 HH:mm |
| endTime | String | 是 | 下班时间，格式 HH:mm |
| restStartTime | String | 否 | 午休开始时间 |
| restEndTime | String | 否 | 午休结束时间 |

##### 请求示例

```json
{
  "attendanceGroupId": 2003,
  "schedules": [
    {"scheduleDate": "2026-07-13", "startTime": "09:00", "endTime": "18:00", "restStartTime": "12:00", "restEndTime": "13:00"},
    {"scheduleDate": "2026-07-14", "startTime": "08:00", "endTime": "17:00", "restStartTime": "12:00", "restEndTime": "13:00"},
    {"scheduleDate": "2026-07-15", "startTime": "10:00", "endTime": "19:00", "restStartTime": "13:00", "restEndTime": "14:00"}
  ]
}
```

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "createdCount": 3,
    "failedDates": []
  }
}
```
> **说明**：批量创建使用 INSERT IGNORE 保证幂等（`uk_group_date` 唯一索引），已存在的排班记录不会重复创建。仅 HR 可操作。排班日期不支持跨天（00:00-23:59 内）。

---

#### 查询排班

```plain
GET /api/v1/attendance/schedules
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| attendanceGroupId | Long | 是 | 考勤组ID |
| startDate | Date | 是 | 排班日期起始 |
| endDate | Date | 是 | 排班日期截止 |

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 9001,
      "attendanceGroupId": 2003,
      "scheduleDate": "2026-07-13",
      "startTime": "09:00",
      "endTime": "18:00",
      "restStartTime": "12:00",
      "restEndTime": "13:00"
    }
  ]
}
```
---

#### 更新排班

```plain
PUT /api/v1/attendance/schedules/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 排班记录ID（路径参数） |
| startTime | String | 否 | 上班时间，格式 HH:mm |
| endTime | String | 否 | 下班时间，格式 HH:mm |
| restStartTime | String | 否 | 午休开始时间 |
| restEndTime | String | 否 | 午休结束时间 |

> **说明**：仅 HR 可操作。更新后次日生效，当天已生成的打卡记录不受影响。

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 6001,
    "scheduleDate": "2026-07-15",
    "startTime": "09:00",
    "endTime": "18:00",
    "updatedFields": ["startTime", "endTime"]
  }
}
```
---

#### 删除排班

```plain
DELETE /api/v1/attendance/schedules/{id}
```

##### 请求参数

| **参数** | **类型** | **必填** | **描述** |
| --- | --- | --- | --- |
| id | Long | 是 | 排班记录ID（路径参数） |

> **说明**：物理删除。若删除的排班日期当天已生成 `attendance_record`，则同步删除该日期所有关联员工的打卡记录并重新生成（使用默认考勤组规则）。仅 HR 可操作。

##### 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```
---

## 关键技术设计

### 打卡记录定时生成方案

每日 00:00 通过定时任务（Spring `@Scheduled` 或 XXL-Job）为所有在职员工生成当日打卡记录：

1.  **查询在职员工**：`SELECT * FROM employee WHERE status IN (1, 2) AND is_deleted = 0`
    
2.  **判断工作日**：查询 `work_calendar` 判断当天是否为工作日，休息日/节假日跳过
    
3.  **排除请假员工**：查询 `leave_request`（`status = 3` 已通过，且 `start_time <= 当天 AND end_time >= 当天`）
    
    *   全天请假（`leave_days >= 1.0` 且覆盖整天）→ 跳过，不生成打卡记录
        
    *   半天请假 → 仍生成记录，但对应时段（上午/下午）不参与后续状态判定
        
4.  **匹配考勤组**：按优先级匹配——先查 `attendance_group_rule` 中 `rule_type=3`（按个人）→ `rule_type=2`（按职位）→ `rule_type=1`（按部门），取优先级最高的考勤组
    
5.  **确定班次时间**：
    
    *   若考勤组 `shift_type = 3`（排班制），查询 `attendance_schedule` 获取当天的排班时间
        
    *   若排班表中无当天排班记录，使用考勤组的默认上下班时间
        
    *   若考勤组 `shift_type != 3`，直接使用考勤组的 `start_time` / `end_time`
        
6.  **生成记录**：`INSERT IGNORE INTO attendance_record (employee_id, attendance_date, attendance_group_id, scheduled_start_time, scheduled_end_time) VALUES (...)`
    
7.  **幂等保障**：`uk_employee_date` 唯一索引防止重复生成
    

### 考勤组优先级匹配方案

员工与考勤组的关系通过 `attendance_group_rule` 多对多关联，匹配优先级为：

| **优先级** | **rule\_type** | **说明** |
| --- | --- | --- |
| 1（最高） | 3 - 按个人 | 精确匹配 employee.id |
| 2 | 2 - 按职位 | 匹配 employee_work_info.position\_id |
| 3（最低） | 1 - 按部门 | 匹配 employee_work_info.department\_id（含上级部门继承） |

> 若同一优先级有多条匹配，取 `attendance_group_id` 最小的。若全部未匹配，使用系统默认考勤组（id=1，固定班 09:00-18:00，午休 12:00-13:00，迟到/早退阈值各 15 分钟，无 IP/GPS 限制）。

### 打卡状态判定规则

当员工打卡时，根据以下规则实时判定并更新 `start_status` / `end_status`：

| **场景** | **判定逻辑** | **status 值** |
| --- | --- | --- |
| 上班打卡 ≤ 规定时间 | 正常 | `start_status = 1` |
| 规定时间 < 上班打卡 ≤ 规定时间 + 迟到阈值 | 迟到 | `start_status = 2` |
| 上班打卡 > 规定时间 + 迟到阈值 | 旷工半天 | `start_status = 3` |
| 下班打卡 ≥ 规定时间 | 正常 | `end_status = 1` |
| 规定时间 - 早退阈值 ≤ 下班打卡 < 规定时间 | 早退 | `end_status = 2` |
| 下班打卡 < 规定时间 - 早退阈值 | 旷工半天 | `end_status = 3` |
| 当日无打卡记录（当日结束时） | 缺勤 | 定时任务批量标记 |
| 上班未打卡、下班已打卡 | 上班缺卡 | `start_status = 4` |
| 上班已打卡、下班未打卡 | 下班缺卡 | `end_status = 4` |

### 弹性班打卡判定规则

弹性班（`shift_type = 2`）的打卡判定逻辑不同于固定班，需结合弹性范围、核心工作时段和工作时长三项条件综合判定：

#### 上班打卡判定

| **场景** | **判定** | **说明** |
| --- | --- | --- |
| 打卡时间 ≤ `flex_start_time` | 正常 | 早于弹性最早时间也算正常（早到） |
| `flex_start_time` < 打卡时间 ≤ `core_start_time` | 正常 | 在弹性范围内 |
| `core_start_time` < 打卡时间 ≤ `core_start_time` + 迟到阈值 | 迟到 | 超过核心工作开始时间但在阈值内 |
| 打卡时间 > `core_start_time` + 迟到阈值 | 旷工半天 | 超过核心工作开始时间+阈值 |

#### 下班打卡判定

| **场景** | **判定** | **说明** |
| --- | --- | --- |
| 打卡时间 ≥ `flex_end_time` | 正常 | 晚于弹性最晚时间也算正常 |
| `core_end_time` ≤ 打卡时间 < `flex_end_time` | 早退 | 早于弹性最晚时间但在核心工作结束后 |
| 打卡时间 < `core_end_time` | 早退 | 早于核心工作结束时间 |

#### 工作时长校验

弹性班除打卡时间判定外，还需满足最低工作时长要求：

```plaintext
实际工作时长 = (下班打卡时间 - 上班打卡时间 - 午休时长)

工作时长 >= 8h → 正常
工作时长 >= 4h 且 < 8h → 标记为"工时不足"（warning，非旷工）
工作时长 < 4h → 标记为"旷工半天"
```
> **说明**：工作时长校验在**下班打卡时**执行。若下班打卡时发现工作时长不足，`end_status` 需根据工作时长重新判定，可能覆盖基于时间的初步判定结果。

### 打卡状态判定规则（固定班）

> **全天缺勤与半天旷工的区分说明：**

*   `start_status = 3` 且 `end_status = 3` → **全天缺勤**（旷工全天，计 1.0 天旷工）
    
*   `start_status = 3` 且 `end_status != 3` → **上午旷工**（半天，计 0.5 天旷工）
    
*   `start_status != 3` 且 `end_status = 3` → **下午旷工**（半天，计 0.5 天旷工）
    
*   `start_status = 4` 且 `end_status = 4` → **全天缺卡**（上下班均未打卡）
    

### 请假天数计算方案

系统根据请假的开始/结束时间自动计算请假天数，核心逻辑：

1.  **解析时段**：开始/结束时间为"日期+时段"格式，时段分为上午（09:00）和下午（13:00），上下各计 0.5 天
    
2.  **遍历日期**：从开始日期到结束日期，逐日判断
    
3.  **排除非工作日**：排除 `work_calendar.day_type IN (2, 3)` 的休息日和节假日
    
4.  **计算方式**：
    
    *   同一天上午+下午 = 1.0 天
        
    *   仅上午或仅下午 = 0.5 天
        
    *   跨天按日期拆分计算
        
5.  **示例**：7/10 下午 → 7/11 上午 = 1.0 天（7/10 下午 0.5 + 7/11 上午 0.5）
    

### 审批流路由方案

请假审批流采用**策略模式 + 配置驱动**设计，根据请假类型和天数动态确定审批链：

```java
// 审批流路由规则（可配置化）
@ConfigurationProperties("attendance.approval")
public class ApprovalRoutingConfig {
    // key: "leaveType:minDays:maxDays" → value: [审批级别列表]
    // 示例：
    // "1:0:3"    → [DIRECT_REPORT]                    // 年假 ≤3天
    // "1:3:99"   → [DIRECT_REPORT, DEPT_MANAGER]      // 年假 >3天
    // "2:0:1"    → [DIRECT_REPORT]                    // 病假 ≤1天
    // "2:1:99"   → [DIRECT_REPORT, DEPT_MANAGER]      // 病假 >1天
    // "4:0:99"   → [DIRECT_REPORT, HR_FILING]         // 婚假
    // "5:0:99"   → [DIRECT_REPORT, HR_FILING]         // 产假
    // "6:0:99"   → [DIRECT_REPORT, HR_FILING]         // 丧假
}
```

每个审批级别常量映射到实际审批人获取策略：

*   `DIRECT_REPORT`：取 `employee_work_info.direct_report_id`
    
*   `DEPT_MANAGER`：取 `department.manager_id`
    
*   `HR_FILING`：取 HR 角色中负责考勤的人员（**通知备案模式**：直接上级审批通过后，该级别自动标记为 APPROVED，无需 HR 手动操作，仅发送通知给 HR）
    

> **HR\_FILING 通知备案说明**：婚假/产假/丧假的审批流为"直接上级 → HR备案"。直接上级（第一级）审批通过后，HR备案（第二级）的 `leave_approval_log.status` 自动设为"通过"（APPROVED），同时触发通知给 HR 人员（告知有新的请假备案）。HR 无需在审批工作台操作，仅做知情备案。这区别于真正的二级审批（如年假>3天需要部门负责人审批），HR 不参与"通过/拒绝"决策。

### 年假余额计算方案

年假余额按员工入职时长分级计算，**当年未休完直接清零，不结转至下一年**。每年1月1日定时任务重新计算每位在职员工的年度年假额度。

| **入职年限** | **年假天数/年** | **当年入职折算** |
| --- | --- | --- |
| 满 1 年不满 10 年 | 5 天 | 剩余月份/12 × 5 |
| 满 10 年不满 20 年 | 10 天 | 剩余月份/12 × 10 |
| 满 20 年 | 15 天 | 剩余月份/12 × 15 |

**计算触发时机**：

*   **每年 1 月 1 日 00:00 定时任务**：将所有在职员工的年假余额（`leave_type=1`）重置，按当前入职年限重新计算 `total_days`，`used_days` 归零，`remaining_days = total_days`
    
*   **新员工入职审批通过后**：自动初始化当年年假余额（按剩余月份折算）
    
*   **请假审批通过后**：实时扣减 `used_days` 和 `remaining_days`
    

**核心代码逻辑**：

```java
public int calculateAnnualLeaveDays(LocalDate hireDate) {
    long years = ChronoUnit.YEARS.between(hireDate, LocalDate.now());
    if (years < 1) {
        // 当年入职：按剩余月份折算
        int remainingMonths = 12 - hireDate.getMonthValue() + 1;
        return Math.round(5 * remainingMonths / 12.0f);
    } else if (years < 10) {
        return 5;
    } else if (years < 20) {
        return 10;
    } else {
        return 15;
    }
}
```

### 调休余额与过期清理方案

调休来源于加班记录（`overtime_record`），1:1 转换为调休时长：

1.  **转入时机**：HR 录入加班记录后，系统自动将 `hours` 累加到 `employee_leave_balance`（`leave_type=7` 调休）的 `total_days`
    
2.  **有效期**：加班当月及次月有效。例如 7 月加班可在 7 月、8 月调休，9 月 1 日过期
    
3.  **过期清理**：每日定时任务扫描 `overtime_record`，将 `expire_date < NOW()` 且 `is_used = 0` 的记录标记为过期，同步从 `employee_leave_balance` 中扣减对应额度
    
4.  **使用记录追溯**：调休使用时不直接关联具体加班记录，按"先进先出"原则优先消耗最早到期的额度
    

### 补卡次数限制方案

每月补卡次数上限 2 次，通过数据库查询 + 唯一索引兜底：

1.  **提交时校验**：```sqlSELECT COUNT(\*) FROM supplement_card_requestWHERE employee\_id = ? AND status != 4AND YEAR(create_time) = ? AND MONTH(create_time) = ?AND is\_deleted = 0```
    
2.  **计数口径**：仅统计审批中（status=2）和已通过（status=3）的申请，草稿（status=1）和已拒绝（status=4）不计入限制
    
3.  **并发控制**：使用 Redis 分布式锁（`supplement_card:{employeeId}:{yearMonth}`）防止并发提交超限
    

### 节假日同步方案

对接外部日历服务（如国家法定节假日查询接口），每日凌晨定时同步：

1.  **调用外部 API**：获取当前年份法定节假日列表
    
2.  **增量更新**：与 `work_calendar` 表已有数据比对，新增、变更、撤销分别处理
    
3.  **调休处理**：因节假日导致的周末上班（调休），`day_type` 设为 1（工作日）
    
4.  **兜底策略**：外部服务不可用时，使用本地预置的节假日配置，并发送告警通知管理员手动维护
    
5.  **同步日志**：记录每次同步的时间、来源、新增/修改/删除条数，便于排查
    

### 考勤统计聚合方案

每月第 1 天凌晨 02:00 由定时任务触发统计，采用**先锁后算**策略：

1.  **锁定数据**：设置 `stat_year + stat_month` 级别标记，禁止统计期间修改考勤数据
    
2.  **逐员工计算**：汇总 `attendance_record`、`leave_request`（status=3 已通过）、`overtime_record`、`employee_leave_balance` 四表数据
    
3.  **批量写入**：`INSERT INTO attendance_statistics ... ON DUPLICATE KEY UPDATE ...` 支持幂等
    
4.  **部门聚合**：基于员工统计结果，按部门维度汇总出勤率、迟到率、请假率
    
5.  **解锁**：统计完成，释放锁定标记
    
6.  **手动重算**：HR 可通过接口对单月或单员工发起重算，覆盖已有统计结果
    

### 数据权限过滤方案

考勤数据遵循"按角色过滤"原则，在 MyBatis SQL 拦截器层统一处理：

| **角色** | **打卡记录** | **请假记录** | **考勤统计** |
| --- | --- | --- | --- |
| 系统管理员 | 全量 | 全量 | 全量 |
| HR 专员 | 全量 | 全量 | 全量 |
| 部门主管 | 本部门及下属部门 | 本部门及下属部门 | 本部门 |
| 普通员工 | 仅本人 | 仅本人 | 仅本人 |
| 财务专员 | — | — | —（无考勤模块权限） |

> 部门主管的数据范围需递归查询部门树，获取所有下级部门 ID 列表作为过滤条件。考勤组配置和节假日日历为全局共享数据，所有角色均可查看。

### 打卡与请假联动方案

请假审批通过后，与打卡记录之间遵循以下联动规则：

1.  **打卡记录生成**：定时任务生成打卡记录时，若员工当天有已通过的全天请假（`leave_request.status = 3`），则跳过不生成打卡记录
    
2.  **打卡操作拦截**：员工打卡时，系统先查询当天是否有已通过的请假记录：
    
    *   全天请假 → 拒绝打卡，提示"当日已请假，无需打卡"
        
    *   半天请假（如上午请假） → 若当前打卡时段在请假范围内，拒绝打卡并提示"当前时段已请假，无需打卡"
        
    *   无请假 → 正常打卡
        
3.  **半天请假场景**：若员工仅请了半天假（如上午请假），则：
    
    *   下午打卡记录正常生成
        
    *   上午时段不参与状态判定（`start_status` 不标记为缺卡/旷工）
        
    *   员工下午可正常下班打卡
        
4.  **请假取消后的恢复**：若请假申请被取消（status 变为 CANCELLED），已跳过的打卡记录**不会自动补生成**，需 HR 手动通过修正接口补录
    

### 排班制打卡方案

排班制（`shift_type = 3`）的打卡流程与固定班/弹性班不同：

1.  **排班数据来源**：`attendance_schedule` 表按考勤组+日期定义每日班次时间
    
2.  **打卡记录预生成**：定时任务生成打卡记录时，若考勤组为排班制，则从 `attendance_schedule` 取当天排班时间作为 `scheduled_start_time` / `scheduled_end_time`；若排班表中无当天记录，使用考勤组的默认 `start_time` / `end_time`
    
3.  **打卡状态判定**：以排班表中的上下班时间为准，判定逻辑与固定班一致
    
4.  **排班变更**：若 HR 修改了某天排班，当天已生成的打卡记录不受影响；次日及之后的记录才应用新排班
    
5.  **夜班限制**：当前版本不支持跨天夜班场景，排班的上下班时间必须在同一天内（00:00-23:59），系统在创建排班时校验 `start_time < end_time`
    

### 打卡并发幂等方案

打卡接口通过 Redis 分布式锁防止重复点击导致的多条记录更新：

*   **锁 Key**：`clock:{employeeId}:{attendanceDate}`
    
*   **锁超时**：5 秒
    
*   **实现方式**：`SET clock:{employeeId}:{date} 1 NX EX 5`
    
*   **锁获取失败**：返回提示"操作处理中，请勿重复点击"
    
*   **锁释放**：打卡完成后主动释放（`DEL`），或等待自动过期
    

### 审批委托兼容方案

审批委托由审批中心模块（第8章）统一管理，考勤模块仅需在审批接口中兼容以下场景：

1.  **请假审批接口**（`POST /api/v1/leave/requests/{id}/approve`）：支持传入 `originalApproverId` 参数，记录实际审批人的同时标记委托人
    
2.  **补卡审批接口**（`POST /api/v1/attendance/supplement-cards/{id}/approve`）：同上
    
3.  **待办列表查询**：查询当前用户的待审批列表时，需同时包含直接分配给该用户的审批任务和被委托的审批任务（此逻辑由审批中心模块提供的数据服务支持，考勤模块调用审批中心接口获取当前用户的有效审批范围）
    
4.  **审批记录**：`leave_approval_log.original_approver_id` 字段记录委托人（若为委托审批），审批意见中系统自动追加"（XXX 代 YYY 审批）"
    

### 边界与限制

| **限制项** | **说明** |
| --- | --- |
| 跨天夜班 | 当前版本**不支持**。打卡记录以单日为单位（`attendance_date`），班次时间必须在同一天内（00:00-23:59）。夜班场景（如22:00-次日06:00）留待后续版本支持 |
| 排班制跨天 | 排班的 `start_time` 必须严格小于 `end_time`，校验不通过拒绝创建 |
| 每月补卡上限 | 2 次，按自然月统计（以 `create_time` 的年份+月份为准） |
| 打卡IP/GPS校验 | 仅当考勤组配置了对应限制时才生效；未配置则不校验 |
| 年假结转 | 不结转，每年1月1日清零并按当年入职年限重新计算 |
| 调休有效期 | 加班当月及次月有效，过期清零（每日定时任务扫描清理） |
| 统计锁定 | 月度统计生成后，对应的打卡和请假数据锁定（禁止修改），需手动解锁后才能修正 |

## 排期

> 对研发时间计划进行排期。

| **阶段** | **内容** | **预估工期** |
| --- | --- | --- |
| 需求评审 | 评审产品规格，确认考勤规则、请假类型、审批流、统计维度 | 1.5 天 |
| 技术方案 | 完成系分文档评审，确认数据库设计与接口方案、定时任务方案 | 2 天 |
| 数据库开发 | 建表（12 张表）、索引优化、初始数据脚本（考勤组/工作日历） | 2 天 |
| 后端开发-考勤规则 | 考勤组 CRUD、工作日历管理、节假日同步、考勤组匹配、排班管理 | 2.5 天 |
| 后端开发-打卡 | 打卡接口、状态判定（含弹性班）、打卡记录定时生成、IP/GPS 限制、请假联动、并发幂等 | 3 天 |
| 后端开发-请假 | 请假申请/审批、假期余额计算、审批流路由（含HR备案）、余额扣减 | 3 天 |
| 后端开发-补卡 | 补卡申请/审批（独立接口）、次数限制、缺卡数据更新 | 1.5 天 |
| 后端开发-加班 | 加班记录 CRUD、调休余额自动转入、过期清理 | 1 天 |
| 后端开发-统计 | 考勤月度统计、部门聚合、图表可视化数据接口、打卡修正及审计 | 2.5 天 |
| 前端开发 | 考勤规则配置页、打卡页、请假申请/审批页、补卡页、加班记录页、排班管理页、考勤统计页（含 AntV 图表） | 9 天 |
| 联调测试 | 前后端联调、权限场景测试、打卡并发测试、定时任务测试、节假日同步测试、排班场景测试 | 5 天 |
| 回归上线 | 全量回归、预发验证、正式上线 | 2 天 |

> **总预估工期**：约 **34 个工作日**

> **依赖说明**：本模块依赖第 4 章「员工档案管理」模块（`employee` 表、`department` 表、`position` 表），需该模块完成后方可联调。审批流转依赖第 8 章「审批中心」模块（委托审批、审批工作台）。节假日同步依赖外部日历服务 API。