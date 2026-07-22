package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.salary.SalaryBatchCreateRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchQueryRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchRejectRequest;
import com.limou.hrms.model.dto.salary.SalaryDetailAdjustRequest;
import com.limou.hrms.model.dto.salary.SalaryDetailQueryRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.salary.SalaryBatchVO;
import com.limou.hrms.model.vo.salary.SalaryDetailVO;
import com.limou.hrms.service.UserService;
import com.limou.hrms.service.salary.SalaryBatchService;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.constant.UserConstant;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪资核算批次 Controller
 */
@Api(tags = "薪资核算批次")
@RestController
@RequestMapping("/salary-batches")
@Slf4j
public class SalaryBatchController {

    @Resource
    private SalaryBatchService salaryBatchService;

    @Resource
    private UserService userService;

    @ApiOperation("查询批次列表")
    @GetMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<Page<SalaryBatchVO>> listBatches(SalaryBatchQueryRequest request) {
        if (request == null) {
            request = new SalaryBatchQueryRequest();
        }
        Page<SalaryBatchVO> page = salaryBatchService.listBatches(request);
        return ResultUtils.success(page);
    }

    @ApiOperation("创建核算批次")
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> createBatch(@RequestBody SalaryBatchCreateRequest request,
                                          HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        Long batchId = salaryBatchService.createBatch(request, loginUser.getId());
        return ResultUtils.success(batchId);
    }

    @ApiOperation("执行异步计算")
    @PostMapping("/{id}/execute")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Boolean> executeCalculate(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.executeCalculate(id);
        return ResultUtils.success(true);
    }

    @ApiOperation("获取批次详情")
    @GetMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<SalaryBatchVO> getBatchDetail(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SalaryBatchVO vo = salaryBatchService.getBatchDetail(id);
        return ResultUtils.success(vo);
    }

    @ApiOperation("查询批次下的薪资明细")
    @GetMapping("/{id}/details")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<Page<SalaryDetailVO>> listDetails(
            @PathVariable Long id,
            SalaryDetailQueryRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (request == null) {
            request = new SalaryDetailQueryRequest();
        }
        Page<SalaryDetailVO> page = salaryBatchService.listDetails(id, request);
        return ResultUtils.success(page);
    }

    @ApiOperation("手动调整员工工资条")
    @PutMapping("/details/{detailId}/adjust")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Boolean> adjustDetail(
            @PathVariable Long detailId,
            @RequestBody SalaryDetailAdjustRequest request) {
        if (request == null || detailId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.adjustDetail(detailId, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("提交审批（创建审批实例）")
    @PutMapping("/{id}/submit")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Boolean> submitForApproval(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.submitForApproval(id);
        return ResultUtils.success(true);
    }

    @ApiOperation("审批通过")
    @PutMapping("/{id}/approve")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<Boolean> approve(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.approve(id);
        return ResultUtils.success(true);
    }

    @ApiOperation("审批驳回")
    @PutMapping("/{id}/reject")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<Boolean> reject(@PathVariable Long id,
                                        @RequestBody SalaryBatchRejectRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.reject(id, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("标记已发放")
    @PutMapping("/{id}/paid")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<Boolean> markAsPaid(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.markAsPaid(id);
        return ResultUtils.success(true);
    }
}
