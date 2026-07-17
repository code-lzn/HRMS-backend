package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.model.dto.attendance.AttendanceRecordQueryRequest;
import com.limou.hrms.model.dto.attendance.ClockRequest;
import com.limou.hrms.model.dto.attendance.SupplementCardSubmitDTO;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 考勤控制器 — 打卡 / 打卡记录 / 补卡
 */
@RestController
@RequestMapping("/api/v1/attendance")
@Slf4j
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * GET /api/v1/attendance/records — 查询打卡记录列表（分页 + 数据权限）
     */
    @GetMapping("/records")
    @AuthCheck
    public BaseResponse<Page<AttendanceRecordVO>> queryRecords(AttendanceRecordQueryRequest queryReq) {
        log.info("{} 查询打卡记录列表", UserContext.getCurrentUser());
        Page<AttendanceRecordVO> page = attendanceService.queryRecords(queryReq);
        return ResultUtils.success(page);
    }

    /**
     * GET /api/v1/attendance/records/calendar — 考勤日历视图（月度汇总 + 每日状态）
     */
    @GetMapping("/records/calendar")
    @AuthCheck
    public BaseResponse<AttendanceCalendarVO> getCalendar(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long employeeId) {
        log.info("{} 查询考勤日历, year={}, month={}, employeeId={}", UserContext.getCurrentUser(), year, month, employeeId);
        AttendanceCalendarVO vo = attendanceService.getCalendar(year, month, employeeId);
        return ResultUtils.success(vo);
    }

    /**
     * POST /api/v1/attendance/clock — 上班/下班打卡
     */
    @PostMapping("/clock")
    @AuthCheck
    public BaseResponse<ClockResultVO> clock(@Valid @RequestBody ClockRequest dto) {
        log.info("{} 打卡, clockType={}", UserContext.getCurrentUser(), dto.getClockType());
        ClockResultVO result = attendanceService.clock(dto);
        return ResultUtils.success(result);
    }

    /**
     * GET /api/v1/attendance/supplement-cards — 查询补卡申请列表（分页 + 数据权限）
     */
    @GetMapping("/supplement-cards")
    @AuthCheck
    public BaseResponse<Page<SupplementCardListVO>> querySupplementCards(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("{} 查询补卡申请列表", UserContext.getCurrentUser());
        Page<SupplementCardListVO> result = attendanceService.querySupplementCards(
                employeeId, status, startDate, endDate, page, size);
        return ResultUtils.success(result);
    }

    /**
     * POST /api/v1/attendance/supplement-cards — 提交补卡申请（缺卡日期 + 每月≤2次）
     */
    @PostMapping("/supplement-cards")
    @AuthCheck
    public BaseResponse<SupplementCardVO> submitSupplementCard(@Valid @RequestBody SupplementCardSubmitDTO dto) {
        log.info("{} 提交补卡申请, date={}, cardType={}", UserContext.getCurrentUser(), dto.getAttendanceDate(), dto.getCardType());
        SupplementCardVO vo = attendanceService.submitSupplementCard(dto);
        return ResultUtils.success(vo);
    }
}