package com.limou.hrms.controller;

import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest;
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
     * 创建考勤组
     */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<AttendanceGroupVO> createAttendanceGroup(@Valid @RequestBody AttendanceGroupCreateRequest dto) {
        AttendanceGroupVO vo = attendanceGroupService.createAttendanceGroup(dto);
        return ResultUtils.success(vo);
    }
}
