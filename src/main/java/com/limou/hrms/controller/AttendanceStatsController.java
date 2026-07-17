package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.AttendanceStatsService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/attendance/stats")
@Slf4j
public class AttendanceStatsController {

    @Resource
    private AttendanceStatsService attendanceStatsService;

    @Resource
    private UserService userService;

    @GetMapping("/personal")
    public BaseResponse<AttendanceStatsVO> getPersonalStats(
            @RequestParam String month,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        AttendanceStatsVO vo = attendanceStatsService.getPersonalStats(loginUser.getId(), month);
        return ResultUtils.success(vo);
    }

    @GetMapping("/department")
    public BaseResponse<List<DepartmentAttendanceStatsVO>> getDepartmentStats(
            @RequestParam String month) {
        List<DepartmentAttendanceStatsVO> list = attendanceStatsService.getDepartmentStats(month);
        return ResultUtils.success(list);
    }

    @GetMapping("/trend")
    public BaseResponse<AttendanceTrendVO> getAttendanceTrend(
            @RequestParam Long departmentId,
            @RequestParam(defaultValue = "6") Integer months) {
        AttendanceTrendVO vo = attendanceStatsService.getAttendanceTrend(departmentId, months);
        return ResultUtils.success(vo);
    }

    @GetMapping("/leave-distribution")
    public BaseResponse<LeaveTypeDistributionVO> getLeaveTypeDistribution(
            @RequestParam String month) {
        LeaveTypeDistributionVO vo = attendanceStatsService.getLeaveTypeDistribution(month);
        return ResultUtils.success(vo);
    }

    @GetMapping("/late-early-ranking")
    public BaseResponse<List<AttendanceStatsVO>> getLateEarlyRanking(
            @RequestParam String month) {
        List<AttendanceStatsVO> list = attendanceStatsService.getLateEarlyRanking(month);
        return ResultUtils.success(list);
    }
}
