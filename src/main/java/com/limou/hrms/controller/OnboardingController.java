package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.onboarding.OnboardingCreateDTO;
import com.limou.hrms.model.dto.onboarding.OnboardingUpdateDTO;
import com.limou.hrms.model.query.OnboardingQuery;
import com.limou.hrms.model.vo.OnboardingDetailVO;
import com.limou.hrms.model.vo.OnboardingListVO;
import com.limou.hrms.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 入职管理控制器 — 仅参数绑定，业务逻辑全部在 Service 层
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@Slf4j
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /** 查询入职列表（分页 + 角色路由） */
    @GetMapping
    @AuthCheck
    public BaseResponse<Page<OnboardingListVO>> list(OnboardingQuery query) {
        return ResultUtils.success(onboardingService.list(query));
    }

    /** 获取入职详情（含审批进度） */
    @GetMapping("/{id}")
    @AuthCheck
    public BaseResponse<OnboardingDetailVO> getDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(onboardingService.getDetail(id));
    }

    /** 创建入职申请（保存草稿或直接提交审批） */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> create(@Valid @RequestBody OnboardingCreateDTO dto) {
        return ResultUtils.success(onboardingService.createApplication(dto));
    }

    /** 更新入职草稿（仅草稿状态可编辑） */
    @PutMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> updateDraft(@PathVariable Long id, @RequestBody OnboardingUpdateDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.updateDraft(id, dto);
        return ResultUtils.success(null);
    }

    /** 删除入职草稿 */
    @DeleteMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> deleteDraft(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.deleteDraft(id);
        return ResultUtils.success(null);
    }

    /** 提交入职审批（草稿→审批中） */
    @PostMapping("/{id}/submit")
    @AuthCheck
    public BaseResponse<?> submitToApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.submitToApproval(id);
        return ResultUtils.success(null);
    }

    /** 撤回入职申请（仅第一级审批节点可撤回） */
    @PostMapping("/{id}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.cancel(id);
        return ResultUtils.success(null);
    }

    /** 确认入职（已批准待入职→已入职） */
    @PostMapping("/{id}/confirm-join")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<?> confirmJoin(@PathVariable Long id, @RequestParam LocalDate actualHireDate) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.confirmJoin(id, actualHireDate);
        return ResultUtils.success(null);
    }

    /** 标记放弃入职 */
    @PostMapping("/{id}/abandon")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<?> abandon(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.abandon(id);
        return ResultUtils.success(null);
    }
}
