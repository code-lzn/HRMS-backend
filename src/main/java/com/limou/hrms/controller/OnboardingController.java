package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.onboarding.OnboardingAddRequest;
import com.limou.hrms.model.entity.HrOnboarding;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.MutationLogVO;
import com.limou.hrms.model.vo.OnboardingVO;
import com.limou.hrms.service.OnboardingService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/onboarding")
@Slf4j
public class OnboardingController {

    @Resource
    private OnboardingService onboardingService;

    @Resource
    private UserService userService;

    /** 从请求中获取当前登录用户的 userId */
    private Long getLoginUserId(HttpServletRequest request) {
        return userService.getLoginUser(request).getId();
    }

    @GetMapping("/list")
    public BaseResponse<Page<OnboardingVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResultUtils.success(onboardingService.listOnboarding(keyword, statuses, page, size));
    }

    @GetMapping("/detail")
    public BaseResponse<OnboardingVO> detail(@RequestParam Long id) {
        return ResultUtils.success(onboardingService.getOnboardingDetail(id));
    }

    @PostMapping("/draft")
    public BaseResponse<Map<String, Object>> saveDraft(@RequestBody OnboardingAddRequest request,
                                                        HttpServletRequest httpReq) {
        Long userId = getLoginUserId(httpReq);
        HrOnboarding entity = onboardingService.addOnboarding(request, userId, false);
        Map<String, Object> data = new HashMap<>();
        data.put("id", entity.getId());
        data.put("recordId", entity.getRecordId());
        return ResultUtils.success(data);
    }

    @PostMapping("/submit")
    public BaseResponse<Map<String, Object>> submit(@RequestBody OnboardingAddRequest request,
                                                     HttpServletRequest httpReq) {
        Long userId = getLoginUserId(httpReq);
        HrOnboarding entity = onboardingService.addOnboarding(request, userId, true);
        Map<String, Object> data = new HashMap<>();
        data.put("id", entity.getId());
        data.put("recordId", entity.getRecordId());
        return ResultUtils.success(data);
    }

    @PostMapping("/{id}/submit")
    public BaseResponse<Boolean> submitDraft(@PathVariable Long id, HttpServletRequest httpReq) {
        onboardingService.submitForApproval(id, getLoginUserId(httpReq));
        return ResultUtils.success(true);
    }

    @PutMapping("/{id}")
    public BaseResponse<Boolean> update(@PathVariable Long id,
                                         @RequestBody OnboardingAddRequest request,
                                         HttpServletRequest httpReq) {
        onboardingService.updateOnboarding(id, request, getLoginUserId(httpReq));
        return ResultUtils.success(true);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> delete(@PathVariable Long id, HttpServletRequest httpReq) {
        onboardingService.deleteOnboarding(id, getLoginUserId(httpReq));
        return ResultUtils.success(true);
    }

    @PostMapping("/confirm")
    public BaseResponse<Boolean> confirm(@RequestParam Long id,
                                          @RequestParam String actualEntryDate,
                                          HttpServletRequest httpReq) {
        Date date;
        try { date = new SimpleDateFormat("yyyy-MM-dd").parse(actualEntryDate); }
        catch (Exception e) { throw new BusinessException(ErrorCode.PARAMS_ERROR, "日期格式错误，应为 yyyy-MM-dd"); }
        onboardingService.confirmOnboarding(id, date, getLoginUserId(httpReq));
        return ResultUtils.success(true);
    }

    @PostMapping("/abandon")
    public BaseResponse<Boolean> abandon(@RequestParam Long id, HttpServletRequest httpReq) {
        onboardingService.abandonOnboarding(id, getLoginUserId(httpReq));
        return ResultUtils.success(true);
    }

    @PostMapping("/employee-confirm")
    public BaseResponse<Boolean> employeeConfirm(@RequestParam Long id, HttpServletRequest httpReq) {
        Long userId = getLoginUserId(httpReq);
        onboardingService.employeeConfirm(id, userId);
        return ResultUtils.success(true);
    }

    @GetMapping("/mutation-logs")
    public BaseResponse<List<MutationLogVO>> getMutationLogs(HttpServletRequest httpReq) {
        Long userId = getLoginUserId(httpReq);
        List<MutationLogVO> logs = onboardingService.getEmployeeMutationLogs(userId);
        return ResultUtils.success(logs);
    }
}
