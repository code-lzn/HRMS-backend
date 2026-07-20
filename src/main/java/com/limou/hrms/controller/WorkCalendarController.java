package com.limou.hrms.controller;

import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.model.dto.attendance.WorkCalendarBatchRequest;
import com.limou.hrms.model.vo.WorkCalendarVO;
import com.limou.hrms.service.WorkCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 工作日历管理控制器
 */
@RestController
@RequestMapping("/work-calendars")
@Slf4j
@RequiredArgsConstructor
public class WorkCalendarController {

    private final WorkCalendarService workCalendarService;

    /**
     * GET /api/work-calendars?year=&month= — 查询某月日历
     */
    @GetMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<WorkCalendarVO> getCalendar(@RequestParam int year,
                                                     @RequestParam int month) {
        log.info("{} 查询工作日历, year={}, month={}", UserContext.getCurrentUser(), year, month);
        WorkCalendarVO vo = workCalendarService.getCalendar(year, month);
        return ResultUtils.success(vo);
    }

    /**
     * PUT /api/work-calendars — 批量设置日期属性
     */
    @PutMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Void> batchUpdate(@Valid @RequestBody WorkCalendarBatchRequest request) {
        log.info("{} 批量设置工作日历, 数量={}", UserContext.getCurrentUser(), request.getDays().size());
        workCalendarService.batchUpdate(request);
        return ResultUtils.success(null);
    }

    /**
     * POST /api/work-calendars/generate/{year} — 生成整年标准日历
     */
    @PostMapping("/generate/{year}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Void> generateYear(@PathVariable int year) {
        log.info("{} 生成 {} 年标准日历", UserContext.getCurrentUser(), year);
        workCalendarService.generateYear(year);
        return ResultUtils.success(null);
    }

    /**
     * POST /api/work-calendars/sync/{year} — 从外部 API 同步法定节假日
     */
    @PostMapping("/sync/{year}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Integer> syncFromExternal(@PathVariable int year) {
        log.info("{} 同步 {} 年法定节假日", UserContext.getCurrentUser(), year);
        int count = workCalendarService.syncFromExternal(year);
        return ResultUtils.success(count);
    }
}
