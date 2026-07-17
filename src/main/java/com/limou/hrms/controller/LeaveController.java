package com.limou.hrms.controller;

import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.dto.leave.LeaveRequestSubmitDTO;
import com.limou.hrms.model.vo.LeaveRequestVO;
import com.limou.hrms.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 请假管理控制器
 */
@RestController
@RequestMapping("/api/v1/leave")
@Slf4j
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    /**
     * 提交请假申请
     */
    @PostMapping("/requests")
    @AuthCheck
    public BaseResponse<LeaveRequestVO> submitLeaveRequest(@Valid @RequestBody LeaveRequestSubmitDTO dto) {
        LeaveRequestVO vo = leaveService.submitLeaveRequest(dto);
        return ResultUtils.success(vo);
    }
}
