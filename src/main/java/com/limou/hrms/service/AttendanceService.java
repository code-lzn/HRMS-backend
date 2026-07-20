package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.Attendance;
import com.limou.hrms.model.vo.AttendanceCalendarVO;
import com.limou.hrms.model.vo.AttendanceVO;

import java.util.List;

/**
 * 考勤打卡服务
 */
public interface AttendanceService extends IService<Attendance> {

    /**
     * 上班/下班打卡
     */
    AttendanceVO punch(Long userId, Integer punchType, String location);

    /**
     * 获取当月考勤日历视图
     */
    AttendanceCalendarVO getCalendar(Long userId, String month);

    /**
     * 获取当月考勤记录列表
     */
    List<AttendanceVO> getMonthRecords(Long userId, String month);

    /**
     * 获取今日打卡状态
     */
    AttendanceVO getTodayStatus(Long userId);

    /**
     * 生成某天的考勤记录（为未打卡员工创建缺勤记录）
     */
    int generateDailyRecords(String date);

    /**
     * 日终评估：检查缺卡情况，标记异常
     */
    int evaluateEndOfDay(String date);

    /**
     * 同步考勤异常审批结果（定时任务调用）
     * 查询已完成的 ATTENDANCE_ANOMALY 审批，若拒绝则恢复考勤状态为正常
     */
    int syncAnomalyApprovals();

    /**
     * 修正今日因定时任务误标记为迟到的记录
     * 仅处理未打卡的记录，按个人考勤规则重新判断
     */
    int correctTodayLateStatus();
}
