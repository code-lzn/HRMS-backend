package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.probation.ProbationCreateDTO;
import com.limou.hrms.model.dto.probation.ProbationHandleResultDTO;
import com.limou.hrms.model.dto.probation.ProbationUpdateDTO;
import com.limou.hrms.model.query.ProbationQuery;
import com.limou.hrms.model.vo.PendingEmployeeVO;
import com.limou.hrms.model.vo.ProbationDetailVO;
import com.limou.hrms.model.vo.ProbationListVO;
import com.limou.hrms.service.ProbationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 转正管理控制器
 */
@RestController
@RequestMapping("/api/probation")
@Slf4j
@RequiredArgsConstructor
public class ProbationController {

    private final ProbationService probationService;

    @GetMapping
    @AuthCheck
    public BaseResponse<Page<ProbationListVO>> list(ProbationQuery query) {
        return ResultUtils.success(probationService.list(query));
    }

    @GetMapping("/{id}")
    @AuthCheck
    public BaseResponse<ProbationDetailVO> getDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(probationService.getDetail(id));
    }

    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> create(@Valid @RequestBody ProbationCreateDTO dto) {
        return ResultUtils.success(probationService.createApplication(dto));
    }

    @PutMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> updateDraft(@PathVariable Long id, @RequestBody ProbationUpdateDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.updateDraft(id, dto);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> deleteDraft(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.deleteDraft(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/submit")
    @AuthCheck
    public BaseResponse<?> submitToApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.submitToApproval(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.cancel(id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/handle-result")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<?> handleResult(@PathVariable Long id, @Valid @RequestBody ProbationHandleResultDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.handleResult(id, dto);
        return ResultUtils.success(null);
    }

    @GetMapping("/pending-employees")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<List<PendingEmployeeVO>> getPendingEmployees(@RequestParam(defaultValue = "7") Integer days) {
        return ResultUtils.success(probationService.getPendingEmployees(days));
    }
}
