package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.resignation.ResignationAddRequest;
import com.limou.hrms.model.entity.HrResignation;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.ResignationVO;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.ResignationService;
import com.limou.hrms.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resignation")
public class ResignationController {

    @Resource
    private ResignationService resignationService;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private UserService userService;

    @GetMapping("/list")
    public BaseResponse<Page<ResignationVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResultUtils.success(resignationService.listResignation(keyword, statuses, page, size));
    }

    @GetMapping("/detail")
    public BaseResponse<ResignationVO> detail(@RequestParam Long id) {
        return ResultUtils.success(resignationService.getResignationDetail(id));
    }

    @GetMapping("/stats")
    public BaseResponse<Map<String, Long>> stats() {
        return ResultUtils.success(resignationService.getStats());
    }

    @PostMapping("/draft")
    public BaseResponse<HrResignation> saveDraft(@RequestBody ResignationAddRequest request,
                                                  HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        return ResultUtils.success(resignationService.addResignation(request, hrEmployeeId, false));
    }

    @PostMapping("/submit")
    public BaseResponse<HrResignation> submit(@RequestBody ResignationAddRequest request,
                                               HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        return ResultUtils.success(resignationService.addResignation(request, hrEmployeeId, true));
    }

    @PostMapping("/{id}/submit")
    public BaseResponse<Void> submitDraft(@PathVariable Long id, HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        resignationService.submitForApproval(id, hrEmployeeId);
        return ResultUtils.success(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody ResignationAddRequest request,
                                      HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        resignationService.updateResignation(id, request, hrEmployeeId);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        resignationService.deleteResignation(id, hrEmployeeId);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/revoke")
    public BaseResponse<Void> revoke(@PathVariable Long id, HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        resignationService.revokeResignation(id, hrEmployeeId);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/abandon")
    public BaseResponse<Void> abandon(@PathVariable Long id, HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        resignationService.abandonResignation(id, hrEmployeeId);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/confirm")
    public BaseResponse<Void> confirm(@PathVariable Long id, HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        resignationService.confirmResignation(id, hrEmployeeId);
        return ResultUtils.success(null);
    }

    @PutMapping("/{id}/resign-date")
    public BaseResponse<Void> updateResignDate(@PathVariable Long id,
                                                @RequestParam String resignDate,
                                                HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        Date date;
        try { date = new SimpleDateFormat("yyyy-MM-dd").parse(resignDate); }
        catch (Exception e) { throw new BusinessException(ErrorCode.PARAMS_ERROR, "日期格式错误，应为 yyyy-MM-dd"); }
        resignationService.updateResignDate(id, date, hrEmployeeId);
        return ResultUtils.success(null);
    }

    @PostMapping("/{id}/resubmit")
    public BaseResponse<Void> resubmit(@PathVariable Long id, HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        resignationService.resubmitResignation(id, hrEmployeeId);
        return ResultUtils.success(null);
    }

    private Long getLoginEmployeeId(HttpServletRequest httpReq) {
        User user = userService.getLoginUser(httpReq);
        var emp = employeeService.getByUserId(user.getId());
        if (emp == null) throw new BusinessException(ErrorCode.OPERATION_ERROR, "员工档案不存在");
        return emp.getId();
    }
}
