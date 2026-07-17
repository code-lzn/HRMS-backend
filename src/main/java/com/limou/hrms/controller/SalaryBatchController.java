package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.salary.SalaryBatchCreateRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchQueryRequest;
import com.limou.hrms.model.dto.salary.SalaryDetailAdjustRequest;
import com.limou.hrms.model.dto.salary.SalaryDetailQueryRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.salary.SalaryBatchVO;
import com.limou.hrms.model.vo.salary.SalaryDetailVO;
import com.limou.hrms.service.UserService;
import com.limou.hrms.service.salary.SalaryBatchService;
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
@RequestMapping("/v1/salary-batches")
@Slf4j
public class SalaryBatchController {

    @Resource
    private SalaryBatchService salaryBatchService;

    @Resource
    private UserService userService;

    @ApiOperation("查询批次列表")
    @GetMapping
    public BaseResponse<Page<SalaryBatchVO>> listBatches(SalaryBatchQueryRequest request) {
        if (request == null) {
            request = new SalaryBatchQueryRequest();
        }
        Page<SalaryBatchVO> page = salaryBatchService.listBatches(request);
        return ResultUtils.success(page);
    }

    @ApiOperation("创建核算批次")
    @PostMapping
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
    public BaseResponse<Boolean> executeCalculate(@ApiParam("批次ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.executeCalculate(id);
        return ResultUtils.success(true);
    }

    @ApiOperation("获取批次详情")
    @GetMapping("/{id}")
    public BaseResponse<SalaryBatchVO> getBatchDetail(@ApiParam("批次ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SalaryBatchVO vo = salaryBatchService.getBatchDetail(id);
        return ResultUtils.success(vo);
    }

    @ApiOperation("查询批次下的薪资明细")
    @GetMapping("/{id}/details")
    public BaseResponse<Page<SalaryDetailVO>> listDetails(
            @ApiParam("批次ID") @PathVariable Long id,
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
    public BaseResponse<Boolean> adjustDetail(
            @ApiParam("明细ID") @PathVariable Long detailId,
            @RequestBody SalaryDetailAdjustRequest request) {
        if (request == null || detailId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.adjustDetail(detailId, request);
        return ResultUtils.success(true);
    }

    @ApiOperation("提交审批（创建审批实例，后续统一走 /api/v1/approvals 接口）")
    @PutMapping("/{id}/submit")
    public BaseResponse<Boolean> submitForApproval(@ApiParam("批次ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.submitForApproval(id);
        return ResultUtils.success(true);
    }

    @ApiOperation("标记已发放")
    @PutMapping("/{id}/paid")
    public BaseResponse<Boolean> markAsPaid(@ApiParam("批次ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.markAsPaid(id);
        return ResultUtils.success(true);
    }
}
