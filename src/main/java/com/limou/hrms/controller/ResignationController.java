package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.resignation.ResignationCreateDTO;
import com.limou.hrms.model.dto.resignation.ResignationUpdateDTO;
import com.limou.hrms.model.query.ResignationQuery;
import com.limou.hrms.model.vo.ResignationDetailVO;
import com.limou.hrms.model.vo.ResignationListVO;
import com.limou.hrms.service.ResignationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 离职管理控制器
 */
@RestController
@RequestMapping("/resignations")
@Slf4j
@RequiredArgsConstructor
public class ResignationController {

    private final ResignationService resignationService;

    @GetMapping
    @AuthCheck
    public BaseResponse<Page<ResignationListVO>> list(ResignationQuery query) {
        return ResultUtils.success(resignationService.list(query));
    }

    @GetMapping("/{id}")
    @AuthCheck
    public BaseResponse<ResignationDetailVO> getDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(resignationService.getDetail(id));
    }

    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> create(@Valid @RequestBody ResignationCreateDTO dto) {
        return ResultUtils.success(resignationService.createApplication(dto));
    }

    @PutMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> updateDraft(@PathVariable Long id, @RequestBody ResignationUpdateDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        resignationService.updateDraft(id, dto);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> deleteDraft(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        resignationService.deleteDraft(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/submit")
    @AuthCheck
    public BaseResponse<?> submitToApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        resignationService.submitToApproval(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        resignationService.cancel(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/confirm")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<?> confirmResignation(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        resignationService.confirmResignation(id);
        return ResultUtils.success(null);
    }
}
