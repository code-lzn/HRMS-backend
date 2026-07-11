package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.attendance.PunchRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.AttendanceCalendarVO;
import com.limou.hrms.model.vo.AttendanceVO;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.UserService;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 考勤打卡接口
 */
@RestController
@RequestMapping("/attendance")
@Slf4j
public class AttendanceController {

    @Resource
    private AttendanceService attendanceService;

    @Resource
    private UserService userService;

    /**
     * 上班/下班打卡
     */
    @PostMapping("/punch")
    public BaseResponse<AttendanceVO> punch(@RequestBody PunchRequest punchRequest,
                                                   HttpServletRequest request) {
        if (punchRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        AttendanceVO vo = attendanceService.punch(
                loginUser.getId(), punchRequest.getPunchType(), punchRequest.getLocation());
        return ResultUtils.success(vo);
    }

    /**
     * 获取今日打卡状态
     */
    @GetMapping("/today")
    public BaseResponse<AttendanceVO> getTodayStatus(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        AttendanceVO vo = attendanceService.getTodayStatus(loginUser.getId());
        return ResultUtils.success(vo);
    }

    /**
     * 获取考勤日历视图
     */
    @GetMapping("/calendar")
    public BaseResponse<AttendanceCalendarVO> getCalendar(
            @ApiParam(value = "月份 yyyy-MM", required = true) @RequestParam String month,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        AttendanceCalendarVO vo = attendanceService.getCalendar(loginUser.getId(), month);
        return ResultUtils.success(vo);
    }

    /**
     * 获取当月考勤记录列表
     */
    @GetMapping("/records")
    public BaseResponse<List<AttendanceVO>> getMonthRecords(
            @ApiParam(value = "月份 yyyy-MM", required = true) @RequestParam String month,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<AttendanceVO> list = attendanceService.getMonthRecords(loginUser.getId(), month);
        return ResultUtils.success(list);
    }
}
