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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 离职管理控制器
 */
@Api(tags = "离职管理")
@RestController
@RequestMapping("/api/v1/resignations")
@Slf4j
@RequiredArgsConstructor
public class ResignationController {

    private final ResignationService resignationService;

    @ApiOperation("查询离职列表（分页 + 角色路由）")
    @GetMapping
    @AuthCheck
    public BaseResponse<Page<ResignationListVO>> list(ResignationQuery query) {
        return ResultUtils.success(resignationService.list(query));
    }

    @ApiOperation("获取离职详情（含审批进度）")
    @GetMapping("/{id}")
    @AuthCheck
    public BaseResponse<ResignationDetailVO> getDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(resignationService.getDetail(id));
    }

    @ApiOperation("创建离职申请（保存草稿或直接提交审批）")
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> create(@Valid @RequestBody ResignationCreateDTO dto) {
        return ResultUtils.success(resignationService.createApplication(dto));
    }

    @ApiOperation("更新离职草稿（仅草稿状态可编辑）")
    @PutMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> updateDraft(@PathVariable Long id, @RequestBody ResignationUpdateDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        resignationService.updateDraft(id, dto);
        return ResultUtils.success(null);
    }

    @ApiOperation("删除离职草稿")
    @DeleteMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> deleteDraft(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        resignationService.deleteDraft(id);
        return ResultUtils.success(null);
    }

    @ApiOperation("提交离职审批（草稿→审批中）")
    @PostMapping("/{id}/submit")
    @AuthCheck
    public BaseResponse<?> submitToApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        resignationService.submitToApproval(id);
        return ResultUtils.success(null);
    }

    @ApiOperation("撤回离职申请（回退为草稿）")
    @PostMapping("/{id}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        resignationService.cancel(id);
        return ResultUtils.success(null);
    }
}
