package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.model.dto.overtime.OvertimeRecordCreateDTO;
import com.limou.hrms.model.dto.overtime.OvertimeRecordUpdateDTO;
import com.limou.hrms.model.vo.OvertimeRecordListVO;
import com.limou.hrms.model.vo.OvertimeRecordVO;
import com.limou.hrms.service.OvertimeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 加班记录控制器 — CRUD（仅HR可操作）
 */
@RestController
@RequestMapping("/api/v1/overtime-records")
@Slf4j
@RequiredArgsConstructor
public class OvertimeRecordController {

    private final OvertimeRecordService overtimeRecordService;

    /**
     * GET /api/v1/overtime-records — 查询加班记录列表（分页，仅HR）
     */
    @GetMapping
    @AuthCheck(mustRole = {UserConstant.HR_ROLE})
    public BaseResponse<Page<OvertimeRecordListVO>> queryRecords(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer isUsed,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("{} 查询加班记录列表, employeeId={}, isUsed={}", UserContext.getCurrentUser(), employeeId, isUsed);
        Page<OvertimeRecordListVO> result = overtimeRecordService.queryRecords(
                employeeId, startDate, endDate, isUsed, page, size);
        return ResultUtils.success(result);
    }

    /**
     * POST /api/v1/overtime-records — 创建加班记录（自动 1:1 转入调休余额）
     */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.HR_ROLE})
    public BaseResponse<OvertimeRecordVO> createOvertimeRecord(@Valid @RequestBody OvertimeRecordCreateDTO dto) {
        log.info("{} 创建加班记录, employeeId={}, hours={}", UserContext.getCurrentUser(), dto.getEmployeeId(), dto.getHours());
        OvertimeRecordVO vo = overtimeRecordService.createOvertimeRecord(dto);
        return ResultUtils.success(vo);
    }

    /**
     * PUT /api/v1/overtime-records/{id} — 编辑加班记录（差额调整调休余额）
     */
    @PutMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.HR_ROLE})
    public BaseResponse<OvertimeRecordVO> updateOvertimeRecord(@PathVariable Long id,
                                                                @Valid @RequestBody OvertimeRecordUpdateDTO dto) {
        log.info("{} 更新加班记录, id={}", UserContext.getCurrentUser(), id);
        OvertimeRecordVO vo = overtimeRecordService.updateOvertimeRecord(id, dto);
        return ResultUtils.success(vo);
    }

    /**
     * DELETE /api/v1/overtime-records/{id} — 删除加班记录（扣减对应调休余额，已使用则拒绝）
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.HR_ROLE})
    public BaseResponse<Void> deleteOvertimeRecord(@PathVariable Long id) {
        log.info("{} 删除加班记录, id={}", UserContext.getCurrentUser(), id);
        overtimeRecordService.deleteOvertimeRecord(id);
        return ResultUtils.success(null);
    }
}