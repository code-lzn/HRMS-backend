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

import java.util.List;
import com.limou.hrms.service.ProbationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 转正管理控制器 — 仅参数绑定，业务逻辑全部在 Service 层
 */
@RestController
@RequestMapping("/api/v1/probation")
@Slf4j
@RequiredArgsConstructor
public class ProbationController {

    private final ProbationService probationService;

    /** 查询转正列表（分页 + 角色路由） */
    @GetMapping
    @AuthCheck
    public BaseResponse<Page<ProbationListVO>> list(ProbationQuery query) {
        return ResultUtils.success(probationService.list(query));
    }

    /** 获取转正详情（含审批进度） */
    @GetMapping("/{id}")
    @AuthCheck
    public BaseResponse<ProbationDetailVO> getDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(probationService.getDetail(id));
    }

    /** 创建转正申请（保存草稿或直接提交审批） */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> create(@Valid @RequestBody ProbationCreateDTO dto) {
        return ResultUtils.success(probationService.createApplication(dto));
    }

    /** 更新转正草稿 */
    @PutMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> updateDraft(@PathVariable Long id, @RequestBody ProbationUpdateDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.updateDraft(id, dto);
        return ResultUtils.success(null);
    }

    /** 删除转正草稿 */
    @DeleteMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> deleteDraft(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.deleteDraft(id);
        return ResultUtils.success(null);
    }

    /** 提交转正审批 */
    @PostMapping("/{id}/submit")
    @AuthCheck
    public BaseResponse<?> submitToApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.submitToApproval(id);
        return ResultUtils.success(null);
    }

    /** 撤回转正申请 */
    @PostMapping("/{id}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.cancel(id);
        return ResultUtils.success(null);
    }

    /** 处理转正结果（审批拒绝后HR决定延期/辞退） */
    @PostMapping("/{id}/handle-result")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<?> handleResult(@PathVariable Long id, @Valid @RequestBody ProbationHandleResultDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        probationService.handleResult(id, dto);
        return ResultUtils.success(null);
    }

    /** 查询待转正员工（试用期即将到期） */
    @GetMapping("/pending-employees")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<List<PendingEmployeeVO>> getPendingEmployees(@RequestParam(defaultValue = "7") Integer days) {
        return ResultUtils.success(probationService.getPendingEmployees(days));
    }
}
