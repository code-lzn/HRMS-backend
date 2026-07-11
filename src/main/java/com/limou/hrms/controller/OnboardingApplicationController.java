package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.onboarding.OnboardingApplicationAddRequest;
import com.limou.hrms.model.dto.onboarding.OnboardingApplicationConfirmHireRequest;
import com.limou.hrms.model.dto.onboarding.OnboardingApplicationQueryRequest;
import com.limou.hrms.model.entity.OnboardingApplication;
import com.limou.hrms.model.vo.OnboardingApplicationVO;
import com.limou.hrms.service.OnboardingApplicationService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 入职申请控制器
 */
@RestController
@RequestMapping("/v1/onboarding-applications")
@Slf4j
public class OnboardingApplicationController {

    @Resource
    private OnboardingApplicationService onboardingApplicationService;

    @Resource
    private UserService userService;

    /**
     * 创建入职申请（保存草稿 / 提交审批）
     */
    @PostMapping
    public BaseResponse<Long> createApplication(@RequestBody OnboardingApplicationAddRequest addRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = userService.getLoginUser(request).getId();
        Long id = onboardingApplicationService.createApplication(addRequest, userId);
        return ResultUtils.success(id);
    }

    /**
     * 提交审批（草稿 → 审批中）
     */
    @PostMapping("/{id}/submit")
    public BaseResponse<Boolean> submitForApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingApplicationService.submitForApproval(id);
        return ResultUtils.success(true);
    }

    /**
     * 确认入职（审批通过后HR确认实际到岗）
     */
    @PutMapping("/{id}/confirm-hire")
    public BaseResponse<Boolean> confirmHire(@PathVariable Long id,
                                              @RequestBody OnboardingApplicationConfirmHireRequest confirmRequest) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(confirmRequest == null, ErrorCode.PARAMS_ERROR);
        onboardingApplicationService.confirmHire(id, confirmRequest);
        return ResultUtils.success(true);
    }

    /**
     * 放弃入职
     */
    @PutMapping("/{id}/abandon")
    public BaseResponse<Boolean> abandon(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingApplicationService.abandon(id);
        return ResultUtils.success(true);
    }

    /**
     * 根据ID获取入职申请详情
     */
    @GetMapping("/{id}")
    public BaseResponse<OnboardingApplicationVO> getById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        OnboardingApplication entity = onboardingApplicationService.getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(onboardingApplicationService.getVO(entity));
    }

    /**
     * 分页查询入职申请列表
     */
    @GetMapping
    public BaseResponse<Page<OnboardingApplicationVO>> listByPage(OnboardingApplicationQueryRequest queryRequest) {
        if (queryRequest == null) {
            queryRequest = new OnboardingApplicationQueryRequest();
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<OnboardingApplication> page = onboardingApplicationService.page(
                new Page<>(current, size),
                onboardingApplicationService.getQueryWrapper(queryRequest));
        Page<OnboardingApplicationVO> voPage = new Page<>(current, size, page.getTotal());
        List<OnboardingApplicationVO> voList = onboardingApplicationService.getVOList(page.getRecords());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
