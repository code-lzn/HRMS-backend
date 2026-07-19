package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.model.dto.leave.LeaveCreateDTO;
import com.limou.hrms.model.dto.leave.LeaveUpdateDTO;
import com.limou.hrms.model.query.LeaveQuery;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.model.vo.LeaveRequestVO;
import com.limou.hrms.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 请假管理控制器 — 请假 CRUD + 假期余额
 */
@RestController
@RequestMapping("/leave")
@Slf4j
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // ==================== 请假 CRUD ====================

    /** 创建请假申请（草稿，可一键提交） */
    @PostMapping("/requests")
    @AuthCheck
    public BaseResponse<Long> createApplication(@Valid @RequestBody LeaveCreateDTO dto) {
        log.info("{} 创建请假申请", UserContext.getCurrentUser());
        Long id = leaveService.createApplication(dto);
        return ResultUtils.success(id);
    }

    /** 更新草稿 */
    @PutMapping("/requests/{id}")
    @AuthCheck
    public BaseResponse<Boolean> updateDraft(@PathVariable Long id, @RequestBody LeaveUpdateDTO dto) {
        log.info("{} 更新请假草稿, id={}", UserContext.getCurrentUser(), id);
        leaveService.updateDraft(id, dto);
        return ResultUtils.success(true);
    }

    /** 删除草稿 */
    @DeleteMapping("/requests/{id}")
    @AuthCheck
    public BaseResponse<Boolean> deleteDraft(@PathVariable Long id) {
        log.info("{} 删除请假草稿, id={}", UserContext.getCurrentUser(), id);
        leaveService.deleteDraft(id);
        return ResultUtils.success(true);
    }

    /** 提交审批 */
    @PostMapping("/requests/{id}/submit")
    @AuthCheck
    public BaseResponse<Boolean> submitToApproval(@PathVariable Long id) {
        log.info("{} 提交请假审批, id={}", UserContext.getCurrentUser(), id);
        leaveService.submitToApproval(id);
        return ResultUtils.success(true);
    }

    /** 撤回申请 */
    @PostMapping("/requests/{id}/cancel")
    @AuthCheck
    public BaseResponse<Boolean> cancel(@PathVariable Long id) {
        log.info("{} 撤回请假申请, id={}", UserContext.getCurrentUser(), id);
        leaveService.cancel(id);
        return ResultUtils.success(true);
    }

    // ==================== 查询 ====================

    /** 请假列表（分页 + 关键字搜索 + 数据权限） */
    @GetMapping("/requests")
    @AuthCheck
    public BaseResponse<Page<LeaveRequestVO>> queryRequests(LeaveQuery query) {
        log.info("{} 查询请假列表, keyword={}", UserContext.getCurrentUser(), query.getKeyword());
        Page<LeaveRequestVO> result = leaveService.queryRequests(query);
        return ResultUtils.success(result);
    }

    /** 请假详情 */
    @GetMapping("/requests/{id}")
    @AuthCheck
    public BaseResponse<LeaveRequestVO> getRequestDetail(@PathVariable Long id) {
        log.info("{} 查询请假详情, id={}", UserContext.getCurrentUser(), id);
        LeaveRequestVO vo = leaveService.getRequestDetail(id);
        return ResultUtils.success(vo);
    }

    /** 假期余额 */
    @GetMapping("/balances")
    @AuthCheck
    public BaseResponse<LeaveBalanceVO> getBalances(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year) {
        log.info("{} 查询假期余额, employeeId={}, year={}", UserContext.getCurrentUser(), employeeId, year);
        LeaveBalanceVO vo = leaveService.getBalances(employeeId, year);
        return ResultUtils.success(vo);
    }
}
