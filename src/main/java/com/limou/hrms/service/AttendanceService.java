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
}
