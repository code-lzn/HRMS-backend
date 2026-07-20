package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.model.dto.leave.LeaveRequestSubmitDTO;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.model.vo.LeaveRequestVO;
import com.limou.hrms.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 请假管理控制器 — 请假申请 / 假期余额
 */
@RestController
@RequestMapping("/leave")
@Slf4j
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    /**
     * GET /api/leave/requests — 查询请假申请列表（分页 + 数据权限）
     */
    @GetMapping("/requests")
    @AuthCheck
    public BaseResponse<Page<LeaveRequestVO>> queryRequests(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer leaveType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("{} 查询请假申请列表", UserContext.getCurrentUser());
        Page<LeaveRequestVO> result = leaveService.queryRequests(
                employeeId, leaveType, status, startDate, endDate, page, size);
        return ResultUtils.success(result);
    }

    /**
     * GET /api/leave/requests/{id} — 查询请假申请详情（含权限校验）
     */
    @GetMapping("/requests/{id}")
    @AuthCheck
    public BaseResponse<LeaveRequestVO> getRequestDetail(@PathVariable Long id) {
        log.info("{} 查询请假申请详情, id={}", UserContext.getCurrentUser(), id);
        LeaveRequestVO vo = leaveService.getRequestDetail(id);
        return ResultUtils.success(vo);
    }

    /**
     * GET /api/leave/balances — 查询假期余额（年假/调休）
     */
    @GetMapping("/balances")
    @AuthCheck
    public BaseResponse<LeaveBalanceVO> getBalances(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer year) {
        log.info("{} 查询假期余额, employeeId={}, year={}", UserContext.getCurrentUser(), employeeId, year);
        LeaveBalanceVO vo = leaveService.getBalances(employeeId, year);
        return ResultUtils.success(vo);
    }

    /**
     * POST /api/leave/requests — 提交请假申请（计算天数 + 余额校验 + 附件校验）
     */
    @PostMapping("/requests")
    @AuthCheck
    public BaseResponse<LeaveRequestVO> submitLeaveRequest(@Valid @RequestBody LeaveRequestSubmitDTO dto) {
        log.info("{} 提交请假申请, leaveType={}, startTime={}", UserContext.getCurrentUser(), dto.getLeaveType(), dto.getStartTime());
        LeaveRequestVO vo = leaveService.submitLeaveRequest(dto);
        return ResultUtils.success(vo);
    }
}