package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.PageRequest;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.transfer.TransferCreateDTO;
import com.limou.hrms.model.dto.transfer.TransferUpdateDTO;
import com.limou.hrms.model.query.TransferQuery;
import com.limou.hrms.model.vo.TransferDetailVO;
import com.limou.hrms.model.vo.TransferHistoryVO;
import com.limou.hrms.model.vo.TransferListVO;
import com.limou.hrms.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 调岗管理控制器
 */
@RestController
@RequestMapping("/api/transfers")
@Slf4j
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping
    @AuthCheck
    public BaseResponse<Page<TransferListVO>> list(TransferQuery query) {
        return ResultUtils.success(transferService.list(query));
    }

    @GetMapping("/{id}")
    @AuthCheck
    public BaseResponse<TransferDetailVO> getDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(transferService.getDetail(id));
    }

    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> create(@Valid @RequestBody TransferCreateDTO dto) {
        return ResultUtils.success(transferService.createApplication(dto));
    }

    @PutMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> updateDraft(@PathVariable Long id, @RequestBody TransferUpdateDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        transferService.updateDraft(id, dto);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> deleteDraft(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        transferService.deleteDraft(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/submit")
    @AuthCheck
    public BaseResponse<?> submitToApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        transferService.submitToApproval(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        transferService.cancel(id);
        return ResultUtils.success(null);
    }

    @GetMapping("/history/{employeeId}")
    @AuthCheck
    public BaseResponse<Page<TransferHistoryVO>> getHistory(@PathVariable Long employeeId, PageRequest page) {
        ThrowUtils.throwIf(employeeId == null || employeeId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(transferService.getHistory(employeeId, page));
    }
}
