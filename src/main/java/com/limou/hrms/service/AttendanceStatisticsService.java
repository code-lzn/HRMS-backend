package com.limou.hrms.service;

import com.limou.hrms.model.vo.AttendanceRateChartVO;
import com.limou.hrms.model.vo.LeaveDistributionVO;
import com.limou.hrms.model.vo.LeaveEarlyRankingVO;

import java.util.List;

public interface AttendanceStatisticsService {

    AttendanceRateChartVO getAttendanceRate(int months, List<Long> departmentIds);

    List<LeaveDistributionVO> getLeaveDistribution(int year, int month);

    List<LeaveEarlyRankingVO> getLateEarlyRanking(int year, int month, int topN);
}