package com.limou.hrms.controller;

import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 考勤组管理控制器
 */
@RestController
@RequestMapping("/api/v1/attendance/groups")
@Slf4j
@RequiredArgsConstructor
public class AttendanceGroupController {

    private final AttendanceGroupService attendanceGroupService;

    /**
     * 查询考勤组列表（分页 + 数据权限）
     */
    @GetMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<Page<AttendanceGroupListVO>> queryAttendanceGroups(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer shiftType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AttendanceGroupListVO> result = attendanceGroupService.queryAttendanceGroups(keyword, shiftType, page, size);
        return ResultUtils.success(result);
    }

    /**
     * 删除考勤组（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<Void> deleteAttendanceGroup(@PathVariable Long id) {
        attendanceGroupService.deleteAttendanceGroup(id);
        return ResultUtils.success(null);
    }

    /**
     * 创建考勤组
     */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<AttendanceGroupVO> createAttendanceGroup(@Valid @RequestBody AttendanceGroupCreateRequest dto) {
        AttendanceGroupVO vo = attendanceGroupService.createAttendanceGroup(dto);
        return ResultUtils.success(vo);
    }

    /**
     * 更新考勤组（部分更新，rules 传则全量替换）
     */
    @PutMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<AttendanceGroupVO> updateAttendanceGroup(@PathVariable Long id,
                                                                  @Valid @RequestBody AttendanceGroupUpdateRequest dto) {
        AttendanceGroupVO vo = attendanceGroupService.updateAttendanceGroup(id, dto);
        return ResultUtils.success(vo);
    }
}
