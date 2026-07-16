package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.attendance.ApprovalRequest;
import com.limou.hrms.model.dto.attendance.LeaveApplyRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.model.vo.LeaveProgressVO;
import com.limou.hrms.model.vo.LeaveVO;
import com.limou.hrms.service.EmployeeLeaveBalanceService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.LeaveService;
import com.limou.hrms.service.UserService;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 请假接口
 */
@RestController
@RequestMapping("/attendance/leave")
@Slf4j
public class LeaveController {

    @Resource
    private LeaveService leaveService;

    @Resource
    private UserService userService;

    @Resource
    private EmployeeService employeeService;

    @Resource
    private EmployeeLeaveBalanceService employeeLeaveBalanceService;

    /**
     * 查询当前员工请假余额
     */
    @GetMapping("/balance")
    public BaseResponse<LeaveBalanceVO> getBalance(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Employee employee = employeeService.getByUserId(loginUser.getId());
        if (employee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工信息不存在");
        }
        LeaveBalanceVO vo = employeeLeaveBalanceService.getCurrentYearBalance(employee.getId());
        return ResultUtils.success(vo);
    }

    /**
     * 申请请假
     */
    @PostMapping("/apply")
    public BaseResponse<LeaveVO> apply(@RequestBody LeaveApplyRequest applyRequest,
                                               HttpServletRequest request) {
        if (applyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        LeaveVO vo = leaveService.apply(loginUser.getId(),
                applyRequest.getLeaveType(), applyRequest.getStartDate(),
                applyRequest.getEndDate(), applyRequest.getReason());
        return ResultUtils.success(vo);
    }

    /**
     * 审批请假
     */
    @PostMapping("/approve")
    public BaseResponse<LeaveVO> approve(@RequestBody ApprovalRequest approvalRequest,
                                                 HttpServletRequest request) {
        if (approvalRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        LeaveVO vo = leaveService.approve(approvalRequest.getId(),
                approvalRequest.getResult(), approvalRequest.getComment(), loginUser.getId());
        return ResultUtils.success(vo);
    }

    /**
     * 获取我的请假记录
     */
    @GetMapping("/my")
    public BaseResponse<List<LeaveVO>> getMyLeaves(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<LeaveVO> list = leaveService.getMyLeaves(loginUser.getId());
        return ResultUtils.success(list);
    }

    /**
     * 撤销请假
     */
    @PostMapping("/cancel/{id}")
    public BaseResponse<Boolean> cancel(@ApiParam(value = "请假ID", required = true) @PathVariable Long id,
                                         HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        leaveService.cancel(id, loginUser.getId());
        return ResultUtils.success(true);
    }

    /**
     * 查看审批进度
     */
    @GetMapping("/{id}/progress")
    public BaseResponse<LeaveProgressVO> getApprovalProgress(
            @PathVariable Long id,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        LeaveProgressVO vo = leaveService.getApprovalProgress(id, loginUser.getId());
        return ResultUtils.success(vo);
    }
}
