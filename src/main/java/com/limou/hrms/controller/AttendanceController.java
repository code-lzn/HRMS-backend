package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.attendance.PunchRequest;
import com.limou.hrms.model.entity.User;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
           @RequestParam String month,
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
            @RequestParam String month,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<AttendanceVO> list = attendanceService.getMonthRecords(loginUser.getId(), month);
        return ResultUtils.success(list);
    }

    /**
     * 生成某天的考勤记录（为未打卡员工创建缺勤/旷工记录）
     */
    @PostMapping("/generate/{date}")
    public BaseResponse<Map<String, Object>> generateDailyRecords(@PathVariable String date) {
        int count = attendanceService.generateDailyRecords(date);
        Map<String, Object> result = new HashMap<>();
        result.put("generated", count);
        return ResultUtils.success(result);
    }

    /**
     * 日终评估：检查缺卡情况，标记异常（上班缺卡/下班缺卡）
     */
    @PostMapping("/evaluate/{date}")
    public BaseResponse<Map<String, Object>> evaluateEndOfDay(@PathVariable String date) {
        int count = attendanceService.evaluateEndOfDay(date);
        Map<String, Object> result = new HashMap<>();
        result.put("updated", count);
        return ResultUtils.success(result);
    }

    /**
     * 同步考勤异常审批结果（由定时任务调用）
     * 将已拒绝的异常审批对应的考勤状态恢复为正常
     */
    @PostMapping("/sync-anomaly-approvals")
    public BaseResponse<Map<String, Object>> syncAnomalyApprovals() {
        int count = attendanceService.syncAnomalyApprovals();
        Map<String, Object> result = new HashMap<>();
        result.put("synced", count);
        return ResultUtils.success(result);
    }

    /**
     * 兜底：确保当天考勤记录已生成（页面加载时调用，幂等）
     */
    @PostMapping("/ensure-today")
    public BaseResponse<Map<String, Object>> ensureTodayRecords() {
        String today = cn.hutool.core.date.DateUtil.formatDate(new java.util.Date());
        int count = attendanceService.generateDailyRecords(today);
        Map<String, Object> result = new HashMap<>();
        result.put("date", today);
        result.put("generated", count);
        return ResultUtils.success(result);
    }
}
