package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.regularization.RegularizationAddRequest;
import com.limou.hrms.model.entity.HrRegularization;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.RegularizationVO;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.RegularizationService;
import com.limou.hrms.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/regularization")
public class RegularizationController {

    @Resource
    private RegularizationService regularizationService;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private UserService userService;

    @GetMapping("/list")
    public BaseResponse<Page<RegularizationVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<RegularizationVO> result = regularizationService.listRegularization(keyword, statuses, page, size);
        return ResultUtils.success(result);
    }

    @GetMapping("/detail")
    public BaseResponse<RegularizationVO> detail(@RequestParam Long id) {
        return ResultUtils.success(regularizationService.getRegularizationDetail(id));
    }

    @GetMapping("/stats")
    public BaseResponse<Map<String, Long>> stats() {
        return ResultUtils.success(regularizationService.getStats());
    }

    @PostMapping("/draft")
    public BaseResponse<HrRegularization> saveDraft(@RequestBody RegularizationAddRequest request,
                                                     HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        HrRegularization entity = regularizationService.addRegularization(request, hrEmployeeId, false);
        return ResultUtils.success(entity);
    }

    @PostMapping("/submit")
    public BaseResponse<HrRegularization> submit(@RequestBody RegularizationAddRequest request,
                                                  HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        HrRegularization entity = regularizationService.addRegularization(request, hrEmployeeId, true);
        return ResultUtils.success(entity);
    }

    @PostMapping("/{id}/submit")
    public BaseResponse<Void> submitDraft(@PathVariable Long id, HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        regularizationService.submitForApproval(id, hrEmployeeId);
        return ResultUtils.success(null);
    }

    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody RegularizationAddRequest request,
                                      HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        regularizationService.updateRegularization(id, request, hrEmployeeId);
        return ResultUtils.success(null);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpReq) {
        Long hrEmployeeId = getLoginEmployeeId(httpReq);
        regularizationService.deleteRegularization(id, hrEmployeeId);
        return ResultUtils.success(null);
    }

    private Long getLoginEmployeeId(HttpServletRequest httpReq) {
        User user = userService.getLoginUser(httpReq);
        var emp = employeeService.getByUserId(user.getId());
        if (emp == null) throw new BusinessException(ErrorCode.OPERATION_ERROR, "员工档案不存在");
        return emp.getId();
    }
}
