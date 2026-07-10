package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.salary.SalaryAdjustRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchCreateRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchQueryRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchRejectRequest;
import com.limou.hrms.model.vo.salary.AnomalyVO;
import com.limou.hrms.model.vo.salary.SalaryBatchPreviewVO;
import com.limou.hrms.model.vo.salary.SalaryBatchVO;
import com.limou.hrms.service.salary.SalaryBatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪资核算批次 Controller
 */
@Api(tags = "月度薪资核算")
@RestController
@RequestMapping("/v1/salary-batches")
@Slf4j
public class SalaryBatchController {

    @Resource
    private SalaryBatchService salaryBatchService;

    @ApiOperation("分页查询核算批次列表")
    @GetMapping
    public BaseResponse<Page<SalaryBatchVO>> listBatches(SalaryBatchQueryRequest request) {
        if (request == null) {
            request = new SalaryBatchQueryRequest();
        }
        Page<SalaryBatchVO> page = salaryBatchService.listBatches(request);
        return ResultUtils.success(page);
    }

    @ApiOperation("新建核算批次")
    @PostMapping
    public BaseResponse<Long> createBatch(@RequestBody SalaryBatchCreateRequest request,
                                           HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long createBy = getOperatorId(httpRequest);
        Long batchId = salaryBatchService.createBatch(request, createBy);
        return ResultUtils.success(batchId);
    }

    @ApiOperation("执行计算（异步，轮询状态）")
    @PostMapping("/{id}/calculate")
    public BaseResponse<Boolean> calculate(
            @ApiParam("批次ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.executeCalculate(id);
        return ResultUtils.success(true);
    }

    @ApiOperation("预览核算结果（分页）")
    @GetMapping("/{id}/preview")
    public BaseResponse<SalaryBatchPreviewVO> preview(
            @ApiParam("批次ID") @PathVariable Long id,
            @ApiParam("页码") @RequestParam(defaultValue = "1") int page,
            @ApiParam("每页大小") @RequestParam(defaultValue = "20") int size) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SalaryBatchPreviewVO preview = salaryBatchService.preview(id, page, size);
        return ResultUtils.success(preview);
    }

    @ApiOperation("查看异常项")
    @GetMapping("/{id}/anomalies")
    public BaseResponse<List<AnomalyVO>> getAnomalies(
            @ApiParam("批次ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<AnomalyVO> anomalies = salaryBatchService.getAnomalies(id);
        return ResultUtils.success(anomalies);
    }

    @ApiOperation("手动调整薪资")
    @PutMapping("/{id}/adjust")
    public BaseResponse<Boolean> adjust(
            @ApiParam("批次ID") @PathVariable Long id,
            @RequestBody SalaryAdjustRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.adjust(request);
        return ResultUtils.success(true);
    }

    @ApiOperation("提交审批")
    @PostMapping("/{id}/submit")
    public BaseResponse<Boolean> submitForApproval(
            @ApiParam("批次ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.submitForApproval(id);
        return ResultUtils.success(true);
    }

    @ApiOperation("审批通过")
    @PostMapping("/{id}/approve")
    public BaseResponse<Boolean> approve(
            @ApiParam("批次ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.approve(id);
        return ResultUtils.success(true);
    }

    @ApiOperation("驳回（需填原因）")
    @PostMapping("/{id}/reject")
    public BaseResponse<Boolean> reject(
            @ApiParam("批次ID") @PathVariable Long id,
            @RequestBody SalaryBatchRejectRequest request) {
        if (id == null || id <= 0 || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.reject(id, request.getReason());
        return ResultUtils.success(true);
    }

    @ApiOperation("标记已发放")
    @PostMapping("/{id}/mark-paid")
    public BaseResponse<Boolean> markPaid(
            @ApiParam("批次ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryBatchService.markPaid(id);
        return ResultUtils.success(true);
    }

    private Long getOperatorId(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute("user_login");
        if (userObj != null) {
            try {
                return (Long) userObj.getClass().getMethod("getId").invoke(userObj);
            } catch (Exception e) {
                return 1L;
            }
        }
        return 1L;
    }
}
