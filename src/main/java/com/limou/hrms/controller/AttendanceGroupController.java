package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest;
import com.limou.hrms.model.dto.attendance.AttendanceGroupUpdateRequest;
import com.limou.hrms.model.vo.AttendanceGroupListVO;
import com.limou.hrms.model.vo.AttendanceGroupVO;
import com.limou.hrms.service.AttendanceGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 考勤组管理控制器 — CRUD
 */
@RestController
@RequestMapping("/api/v1/attendance/groups")
@Slf4j
@RequiredArgsConstructor
public class AttendanceGroupController {

    private final AttendanceGroupService attendanceGroupService;

    /**
     * GET /api/v1/attendance/groups — 查询考勤组列表（分页 + 数据权限）
     */
    @GetMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<Page<AttendanceGroupListVO>> queryAttendanceGroups(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer shiftType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("{} 查询考勤组列表, keyword={}, shiftType={}", UserContext.getCurrentUser(), keyword, shiftType);
        Page<AttendanceGroupListVO> result = attendanceGroupService.queryAttendanceGroups(keyword, shiftType, page, size);
        return ResultUtils.success(result);
    }

    /**
     * POST /api/v1/attendance/groups — 创建考勤组（含适用规则校验）
     */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<AttendanceGroupVO> createAttendanceGroup(@Valid @RequestBody AttendanceGroupCreateRequest dto) {
        log.info("{} 创建考勤组, name={}", UserContext.getCurrentUser(), dto.getName());
        AttendanceGroupVO vo = attendanceGroupService.createAttendanceGroup(dto);
        return ResultUtils.success(vo);
    }

    /**
     * PUT /api/v1/attendance/groups/{id} — 更新考勤组（部分更新，rules 传则全量替换）
     */
    @PutMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<AttendanceGroupVO> updateAttendanceGroup(@PathVariable Long id,
                                                                  @Valid @RequestBody AttendanceGroupUpdateRequest dto) {
        log.info("{} 更新考勤组, id={}", UserContext.getCurrentUser(), id);
        AttendanceGroupVO vo = attendanceGroupService.updateAttendanceGroup(id, dto);
        return ResultUtils.success(vo);
    }

    /**
     * DELETE /api/v1/attendance/groups/{id} — 删除考勤组（逻辑删除，需先清空规则）
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<Void> deleteAttendanceGroup(@PathVariable Long id) {
        log.info("{} 删除考勤组, id={}", UserContext.getCurrentUser(), id);
        attendanceGroupService.deleteAttendanceGroup(id);
        return ResultUtils.success(null);
    }
}