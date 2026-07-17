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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 调岗管理控制器
 */
@Api(tags = "调岗管理")
@RestController
@RequestMapping("/api/v1/transfers")
@Slf4j
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @ApiOperation("查询调岗列表（分页 + 角色路由）")
    @GetMapping
    @AuthCheck
    public BaseResponse<Page<TransferListVO>> list(TransferQuery query) {
        return ResultUtils.success(transferService.list(query));
    }

    @ApiOperation("获取调岗详情（含审批进度）")
    @GetMapping("/{id}")
    @AuthCheck
    public BaseResponse<TransferDetailVO> getDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(transferService.getDetail(id));
    }

    @ApiOperation("创建调岗申请（保存草稿或直接提交审批）")
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> create(@Valid @RequestBody TransferCreateDTO dto) {
        return ResultUtils.success(transferService.createApplication(dto));
    }

    @ApiOperation("更新调岗草稿（仅草稿状态可编辑）")
    @PutMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> updateDraft(@PathVariable Long id, @RequestBody TransferUpdateDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        transferService.updateDraft(id, dto);
        return ResultUtils.success(null);
    }

    @ApiOperation("删除调岗草稿")
    @DeleteMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> deleteDraft(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        transferService.deleteDraft(id);
        return ResultUtils.success(null);
    }

    @ApiOperation("提交调岗审批（草稿→审批中）")
    @PostMapping("/{id}/submit")
    @AuthCheck
    public BaseResponse<?> submitToApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        transferService.submitToApproval(id);
        return ResultUtils.success(null);
    }

    @ApiOperation("撤回调岗申请（回退为草稿）")
    @PostMapping("/{id}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        transferService.cancel(id);
        return ResultUtils.success(null);
    }

    @ApiOperation("查询员工调岗历史")
    @GetMapping("/history/{employeeId}")
    @AuthCheck
    public BaseResponse<Page<TransferHistoryVO>> getHistory(@PathVariable Long employeeId, PageRequest page) {
        ThrowUtils.throwIf(employeeId == null || employeeId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(transferService.getHistory(employeeId, page));
    }
}
