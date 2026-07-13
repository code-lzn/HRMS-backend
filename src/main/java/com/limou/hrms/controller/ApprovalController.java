package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.approval.ApprovalActionRequest;
import com.limou.hrms.model.dto.approval.DelegationRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.ApprovalDelegationVO;
import com.limou.hrms.model.vo.ApprovalDetailVO;
import com.limou.hrms.model.vo.ApprovalPendingVO;
import com.limou.hrms.service.ApprovalDelegationService;
import com.limou.hrms.service.ApprovalService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 审批中心接口
 */
@RestController
@RequestMapping("/approval")
@Slf4j
public class ApprovalController {

    @Resource
    private ApprovalService approvalService;

    @Resource
    private ApprovalDelegationService delegationService;

    @Resource
    private UserService userService;

    @Resource
    private EmployeeService employeeService;

    // ==================== 审批人工作台 ====================

    /**
     * 待审批列表
     */
    @GetMapping("/pending")
    public BaseResponse<List<ApprovalPendingVO>> getPendingList(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Employee emp = employeeService.getByUserId(loginUser.getId());
        List<ApprovalPendingVO> list = approvalService.getPendingList(emp.getId());
        return ResultUtils.success(list);
    }

    /**
     * 审批详情
     */
    @GetMapping("/detail/{recordId}")
    public BaseResponse<ApprovalDetailVO> getApprovalDetail(
            @PathVariable Long recordId, HttpServletRequest request) {
        userService.getLoginUser(request); // 校验登录
        ApprovalDetailVO vo = approvalService.getApprovalDetail(recordId);
        return ResultUtils.success(vo);
    }

    /**
     * 通过
     */
    @PostMapping("/approve")
    public BaseResponse<Boolean> approve(@RequestBody ApprovalActionRequest actionRequest,
                                         HttpServletRequest request) {
        validateActionRequest(actionRequest);
        User loginUser = userService.getLoginUser(request);
        Employee emp = employeeService.getByUserId(loginUser.getId());
        approvalService.approve(actionRequest.getDetailId(), emp.getId(), actionRequest.getComment());
        return ResultUtils.success(true);
    }

    /**
     * 拒绝
     */
    @PostMapping("/reject")
    public BaseResponse<Boolean> reject(@RequestBody ApprovalActionRequest actionRequest,
                                        HttpServletRequest request) {
        validateActionRequest(actionRequest);
        User loginUser = userService.getLoginUser(request);
        Employee emp = employeeService.getByUserId(loginUser.getId());
        approvalService.reject(actionRequest.getDetailId(), emp.getId(), actionRequest.getComment());
        return ResultUtils.success(true);
    }

    /**
     * 转交
     */
    @PostMapping("/transfer")
    public BaseResponse<Boolean> transfer(@RequestBody ApprovalActionRequest actionRequest,
                                          HttpServletRequest request) {
        validateActionRequest(actionRequest);
        if (actionRequest.getTargetUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择转交目标人");
        }
        User loginUser = userService.getLoginUser(request);
        Employee emp = employeeService.getByUserId(loginUser.getId());
        approvalService.transfer(actionRequest.getDetailId(), emp.getId(),
                actionRequest.getTargetUserId(), actionRequest.getComment());
        return ResultUtils.success(true);
    }

    // ==================== 委托审批 ====================

    /**
     * 我的委托列表
     */
    @GetMapping("/delegation/my")
    public BaseResponse<List<ApprovalDelegationVO>> getMyDelegations(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Employee emp = employeeService.getByUserId(loginUser.getId());
        List<ApprovalDelegationVO> list = delegationService.getMyDelegations(emp.getId());
        return ResultUtils.success(list);
    }

    /**
     * 创建委托
     */
    @PostMapping("/delegation")
    public BaseResponse<Long> createDelegation(@RequestBody DelegationRequest delegationRequest,
                                               HttpServletRequest request) {
        if (delegationRequest == null || delegationRequest.getDelegateId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择被委托人");
        }
        User loginUser = userService.getLoginUser(request);
        Employee emp = employeeService.getByUserId(loginUser.getId());

        Employee delegate = employeeService.getById(delegationRequest.getDelegateId());
        if (delegate == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "被委托人不存在");
        }

        var delegation = delegationService.createDelegation(
                emp.getId(), emp.getEmployeeName(),
                delegate.getId(), delegate.getEmployeeName(),
                delegationRequest.getBusinessTypes(),
                delegationRequest.getStartDate(), delegationRequest.getEndDate());
        return ResultUtils.success(delegation.getId());
    }

    /**
     * 取消委托
     */
    @PostMapping("/delegation/cancel/{id}")
    public BaseResponse<Boolean> cancelDelegation(@PathVariable Long id,
                                                  HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Employee emp = employeeService.getByUserId(loginUser.getId());
        delegationService.cancelDelegation(id, emp.getId());
        return ResultUtils.success(true);
    }

    // ==================== 私有方法 ====================

    private void validateActionRequest(ApprovalActionRequest request) {
        if (request == null || request.getDetailId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
    }
}
