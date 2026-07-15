package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.approval.ApprovalActionDTO;
import com.limou.hrms.model.dto.approval.DelegateSettingDTO;
import com.limou.hrms.model.entity.ApprovalDelegate;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.ApprovalInstanceVO;
import com.limou.hrms.model.vo.PendingItemVO;
import com.limou.hrms.model.vo.ProcessedItemVO;
import com.limou.hrms.service.ApprovalDelegateService;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private ApprovalDelegateService approvalDelegateService;
    @Resource
    private UserService userService;

    // ==================== 查询接口 ====================

    @GetMapping("/pending")
    public BaseResponse<Page<PendingItemVO>> getPendingList(ApprovalQuery query) {
        return ResultUtils.success(approvalFlowService.getPendingList(getCurrentEmployeeId(), query));
    }

    @GetMapping("/pending-count")
    public BaseResponse<Map<String, Long>> getPendingCount() {
        long count = approvalFlowService.getPendingCount(getCurrentEmployeeId());
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ResultUtils.success(result);
    }

    @GetMapping("/processed")
    public BaseResponse<Page<ProcessedItemVO>> getProcessedList(ApprovalQuery query) {
        return ResultUtils.success(approvalFlowService.getProcessedList(getCurrentEmployeeId(), query));
    }

    @GetMapping("/{instanceId}")
    public BaseResponse<ApprovalInstanceVO> getDetail(@PathVariable Long instanceId) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(approvalFlowService.getDetail(instanceId));
    }

    // ==================== 操作接口 ====================

    @PostMapping("/{nodeId}/approve")
    public BaseResponse<?> approve(@PathVariable Long nodeId, @RequestBody ApprovalActionDTO dto) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        approvalFlowService.approve(nodeId, getCurrentEmployeeId(), dto.getComment());
        return ResultUtils.success("ok");
    }

    @PostMapping("/{nodeId}/reject")
    public BaseResponse<?> reject(@PathVariable Long nodeId, @RequestBody ApprovalActionDTO dto) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        if (StringUtils.isBlank(dto.getComment())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拒绝时必须填写审批意见");
        }
        approvalFlowService.reject(nodeId, getCurrentEmployeeId(), dto.getComment());
        return ResultUtils.success("ok");
    }

    @PostMapping("/{nodeId}/transfer")
    public BaseResponse<?> transfer(@PathVariable Long nodeId, @RequestBody ApprovalActionDTO dto) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(dto.getToApproverId() == null || dto.getToApproverId() <= 0,
                ErrorCode.PARAMS_ERROR, "目标审批人不能为空");
        approvalFlowService.transfer(nodeId, getCurrentEmployeeId(), dto.getToApproverId());
        return ResultUtils.success("ok");
    }

    @PostMapping("/{instanceId}/cancel")
    public BaseResponse<?> cancel(@PathVariable Long instanceId) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        approvalFlowService.cancel(instanceId, getCurrentEmployeeId());
        return ResultUtils.success("ok");
    }

    // ==================== 委托审批接口 ====================

    @PostMapping("/delegates")
    public BaseResponse<ApprovalDelegate> createDelegate(@RequestBody DelegateSettingDTO dto) {
        ApprovalDelegate result = approvalDelegateService.createDelegate(
                getCurrentEmployeeId(), dto.getDelegateId(), dto.getStartTime(), dto.getEndTime());
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delegates/{id}")
    public BaseResponse<?> cancelDelegate(@PathVariable Long id) {
        approvalDelegateService.cancelDelegate(id, getCurrentEmployeeId());
        return ResultUtils.success(null);
    }

    @GetMapping("/delegates/my")
    public BaseResponse<Map<String, List<ApprovalDelegate>>> getMyDelegates() {
        return ResultUtils.success(approvalDelegateService.getMyDelegates(getCurrentEmployeeId()));
    }

    // ==================== 辅助方法 ====================

    /**
     * 从当前登录态获取 employee.id。
     */
    private Long getCurrentEmployeeId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attrs.getRequest();
        User loginUser = userService.getLoginUser(request);
        return loginUser.getId();
    }
}
