package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.dto.attendance.AttendanceRecordQueryRequest;
import com.limou.hrms.model.dto.attendance.ClockRequest;
import com.limou.hrms.model.vo.AttendanceCalendarVO;
import com.limou.hrms.model.vo.AttendanceRecordVO;
import com.limou.hrms.model.vo.ClockResultVO;
import com.limou.hrms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 考勤打卡控制器
 */
@RestController
@RequestMapping("/api/v1/attendance")
@Slf4j
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * 查询打卡记录列表（分页 + 数据权限）
     */
    @GetMapping("/records")
    @AuthCheck
    public BaseResponse<Page<AttendanceRecordVO>> queryRecords(AttendanceRecordQueryRequest queryReq) {
        Page<AttendanceRecordVO> page = attendanceService.queryRecords(queryReq);
        return ResultUtils.success(page);
    }

    /**
     * 考勤日历视图
     */
    @GetMapping("/records/calendar")
    @AuthCheck
    public BaseResponse<AttendanceCalendarVO> getCalendar(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long employeeId) {
        AttendanceCalendarVO vo = attendanceService.getCalendar(year, month, employeeId);
        return ResultUtils.success(vo);
    }

    /**
     * 打卡（上班/下班）
     */
    @PostMapping("/clock")
    @AuthCheck
    public BaseResponse<ClockResultVO> clock(@Valid @RequestBody ClockRequest dto) {
        ClockResultVO result = attendanceService.clock(dto);
        return ResultUtils.success(result);
    }
}
