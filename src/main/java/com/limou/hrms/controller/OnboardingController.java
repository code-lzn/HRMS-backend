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
import java.util.HashMap;
import java.util.Map;

/**
 * 入职管理控制器
 */
@RestController
@RequestMapping("/onboarding")
@Slf4j
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping
    @AuthCheck
    public BaseResponse<Page<OnboardingListVO>> list(OnboardingQuery query) {
        return ResultUtils.success(onboardingService.list(query));
    }

    @GetMapping("/{id}")
    @AuthCheck
    public BaseResponse<OnboardingDetailVO> getDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(onboardingService.getDetail(id));
    }

    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> create(@Valid @RequestBody OnboardingCreateDTO dto) {
        return ResultUtils.success(onboardingService.createApplication(dto));
    }

    @PutMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> updateDraft(@PathVariable Long id, @RequestBody OnboardingUpdateDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.updateDraft(id, dto);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> deleteDraft(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.deleteDraft(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/submit")
    @AuthCheck
    public BaseResponse<?> submitToApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.submitToApproval(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.cancel(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/confirm-join")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<?> confirmJoin(@PathVariable Long id, @RequestParam LocalDate actualHireDate) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.confirmJoin(id, actualHireDate);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/abandon")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<?> abandon(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.abandon(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/generate-employee-no")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Map<String, String>> previewEmployeeNo(@RequestParam Long departmentId) {
        String employeeNo = onboardingService.previewEmployeeNo(departmentId);
        Map<String, String> result = new HashMap<>();
        result.put("employeeNo", employeeNo);
        return ResultUtils.success(result);
    }

    @GetMapping("/check-phone")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Map<String, Object>> checkPhone(@RequestParam String phone,
                                                         @RequestParam(required = false) Long excludeId) {
        boolean available = onboardingService.isPhoneAvailable(phone, excludeId);
        Map<String, Object> result = new HashMap<>();
        result.put("available", available);
        result.put("message", available ? "可用" : "该手机号已被占用");
        return ResultUtils.success(result);
    }
}
