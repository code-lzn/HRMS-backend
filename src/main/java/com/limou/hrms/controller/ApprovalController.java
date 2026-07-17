package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.approval.ApprovalActionDTO;
import com.limou.hrms.model.dto.approval.DelegateSettingDTO;
import com.limou.hrms.model.entity.ApprovalDelegate;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.ApprovalInstanceVO;
import com.limou.hrms.model.vo.PendingItemVO;
import com.limou.hrms.model.vo.ProcessedItemVO;
import com.limou.hrms.service.ApprovalDelegateService;
import com.limou.hrms.service.ApprovalFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批中心 — 通用审批接口
 */
@Api(tags = "审批中心")
@RestController
@RequestMapping("/api/v1/approvals")
@Slf4j
public class ApprovalController {

    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ApprovalDelegateService approvalDelegateService;

    @ApiOperation("查询待办列表")
    @GetMapping("/pending")
    @AuthCheck
    public BaseResponse<Page<PendingItemVO>> getPendingList(ApprovalQuery query) {
        return ResultUtils.success(approvalFlowService.getPendingList(query));
    }

    @ApiOperation("查询待办红点数量")
    @GetMapping("/pending-count")
    @AuthCheck
    public BaseResponse<Map<String, Long>> getPendingCount() {
        long count = approvalFlowService.getPendingCount();
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ResultUtils.success(result);
    }

    @ApiOperation("查询已办列表")
    @GetMapping("/processed")
    @AuthCheck
    public BaseResponse<Page<ProcessedItemVO>> getProcessedList(ApprovalQuery query) {
        return ResultUtils.success(approvalFlowService.getProcessedList(query));
    }

    @ApiOperation("获取审批详情（含节点时间线）")
    @GetMapping("/{instanceId}")
    @AuthCheck
    public BaseResponse<ApprovalInstanceVO> getDetail(@PathVariable Long instanceId) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(approvalFlowService.getDetail(instanceId));
    }

    @ApiOperation("审批通过")
    @PostMapping("/{nodeId}/approve")
    @AuthCheck
    public BaseResponse<?> approve(@PathVariable Long nodeId, @RequestBody ApprovalActionDTO dto) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        approvalFlowService.approve(nodeId, dto.getComment());
        return ResultUtils.success("ok");
    }

    @ApiOperation("审批拒绝")
    @PostMapping("/{nodeId}/reject")
    @AuthCheck
    public BaseResponse<?> reject(@PathVariable Long nodeId, @RequestBody ApprovalActionDTO dto) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        approvalFlowService.reject(nodeId, dto.getComment());
        return ResultUtils.success("ok");
    }

    @ApiOperation("审批转交")
    @PostMapping("/{nodeId}/transfer")
    @AuthCheck
    public BaseResponse<?> transfer(@PathVariable Long nodeId, @RequestBody ApprovalActionDTO dto) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        approvalFlowService.transfer(nodeId, dto.getToApproverId());
        return ResultUtils.success("ok");
    }

    @ApiOperation("撤回申请")
    @PostMapping("/{instanceId}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long instanceId) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        approvalFlowService.cancel(instanceId);
        return ResultUtils.success("ok");
    }

    @ApiOperation("设置委托审批")
    @PostMapping("/delegates")
    @AuthCheck
    public BaseResponse<ApprovalDelegate> createDelegate(@RequestBody DelegateSettingDTO dto) {
        return ResultUtils.success(approvalDelegateService.createDelegate(dto));
    }

    @ApiOperation("取消委托审批")
    @DeleteMapping("/delegates/{id}")
    @AuthCheck
    public BaseResponse<?> cancelDelegate(@PathVariable Long id) {
        approvalDelegateService.cancelDelegate(id);
        return ResultUtils.success(null);
    }

    @ApiOperation("查询我的委托（我托别人 + 别人托我）")
    @GetMapping("/delegates/my")
    @AuthCheck
    public BaseResponse<Map<String, List<ApprovalDelegate>>> getMyDelegates() {
        return ResultUtils.success(approvalDelegateService.getMyDelegates());
    }
}
