package com.limou.hrms.service;

import com.limou.hrms.model.vo.*;

import java.util.List;

public interface AttendanceStatsService {

    AttendanceStatsVO getPersonalStats(Long userId, String month);

    List<DepartmentAttendanceStatsVO> getDepartmentStats(String month);

    AttendanceTrendVO getAttendanceTrend(Long departmentId, Integer months, String endMonth);

    LeaveTypeDistributionVO getLeaveTypeDistribution(String month, Long departmentId);

    List<AttendanceStatsVO> getLateEarlyRanking(String month, Long departmentId);

    AttendanceTrendVO getPersonalTrend(Long userId, Integer months);

    LeaveTypeDistributionVO getPersonalLeaveDistribution(Long userId, String month);
}
