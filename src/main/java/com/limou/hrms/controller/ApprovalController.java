package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.approval.ApprovalActionDTO;
import com.limou.hrms.model.entity.ApprovalInstance;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.ApprovalInstanceVO;
import com.limou.hrms.model.vo.PendingItemVO;
import com.limou.hrms.model.vo.ProcessedItemVO;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 审批中心 Controller — 通用审批接口
 */
@RestController
@RequestMapping("/approvals")
@Slf4j
public class ApprovalController {

    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private UserService userService;

    // ==================== 查询接口 ====================

    /**
     * 查询待办列表
     */
    @GetMapping("/pending")
    public BaseResponse<Page<PendingItemVO>> getPendingList(ApprovalQuery query, HttpServletRequest request) {
        Long employeeId = getCurrentEmployeeId(request);
        Page<PendingItemVO> result = approvalFlowService.getPendingList(employeeId, query);
        return ResultUtils.success(result);
    }

    /**
     * 查询已办列表
     */
    @GetMapping("/processed")
    public BaseResponse<Page<ProcessedItemVO>> getProcessedList(ApprovalQuery query, HttpServletRequest request) {
        Long employeeId = getCurrentEmployeeId(request);
        Page<ProcessedItemVO> result = approvalFlowService.getProcessedList(employeeId, query);
        return ResultUtils.success(result);
    }

    /**
     * 获取审批详情
     */
    @GetMapping("/{instanceId}")
    public BaseResponse<ApprovalInstanceVO> getDetail(@PathVariable Long instanceId) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        ApprovalInstanceVO vo = approvalFlowService.getDetail(instanceId);
        return ResultUtils.success(vo);
    }

    // ==================== 操作接口 ====================

    /**
     * 审批通过
     */
    @PostMapping("/{nodeId}/approve")
    public BaseResponse<?> approve(@PathVariable Long nodeId,
                                   @RequestBody ApprovalActionDTO dto,
                                   HttpServletRequest request) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        Long employeeId = getCurrentEmployeeId(request);
        approvalFlowService.approve(nodeId, employeeId, dto.getComment());
        return ResultUtils.success("ok");
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/{nodeId}/reject")
    public BaseResponse<?> reject(@PathVariable Long nodeId,
                                  @RequestBody ApprovalActionDTO dto,
                                  HttpServletRequest request) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        if (StringUtils.isBlank(dto.getComment())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拒绝时必须填写审批意见");
        }
        Long employeeId = getCurrentEmployeeId(request);
        approvalFlowService.reject(nodeId, employeeId, dto.getComment());
        return ResultUtils.success("ok");
    }

    /**
     * 审批转交
     */
    @PostMapping("/{nodeId}/transfer")
    public BaseResponse<?> transfer(@PathVariable Long nodeId,
                                    @RequestBody ApprovalActionDTO dto,
                                    HttpServletRequest request) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(dto.getToApproverId() == null || dto.getToApproverId() <= 0,
                ErrorCode.PARAMS_ERROR, "目标审批人不能为空");
        Long employeeId = getCurrentEmployeeId(request);
        approvalFlowService.transfer(nodeId, employeeId, dto.getToApproverId());
        return ResultUtils.success("ok");
    }

    /**
     * 撤回申请
     */
    @PostMapping("/{instanceId}/cancel")
    public BaseResponse<?> cancel(@PathVariable Long instanceId, HttpServletRequest request) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        Long employeeId = getCurrentEmployeeId(request);
        approvalFlowService.cancel(instanceId, employeeId);
        return ResultUtils.success("ok");
    }

    // ==================== 辅助方法 ====================

    /**
     * 从当前登录用户获取 employee.id。
     * 当前系统用 User + HttpSession 认证，User.id 即为 employee 体系中的 user_id。
     * 需要查 employee 表获取对应的 employee.id。
     */
    private Long getCurrentEmployeeId(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 简化：User.id 作为 employee_id（假设 user 和 employee 共享 id）
        // 实际情况需查 employee 表 by user_id
        // 第一版：直接使用 employee.id = loginUser.getId() 的映射
        return loginUser.getId();
    }
}
