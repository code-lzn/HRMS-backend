package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.salary.EmployeeSalaryUpdateRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.salary.EmployeeSalaryVO;
import com.limou.hrms.model.vo.salary.SalaryChangeHistoryVO;
import com.limou.hrms.service.UserService;
import com.limou.hrms.service.salary.EmployeeSalaryService;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工薪资档案 Controller
 */
@RestController
@RequestMapping("/employee-salaries")
@Slf4j
public class EmployeeSalaryController {

    @Resource
    private EmployeeSalaryService employeeSalaryService;

    @Resource
    private UserService userService;

    @GetMapping("/{employeeId}")
    public BaseResponse<EmployeeSalaryVO> getEmployeeSalary(
            @PathVariable Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        EmployeeSalaryVO vo = employeeSalaryService.getEmployeeSalary(employeeId);
        return ResultUtils.success(vo);
    }

    @PutMapping("/{employeeId}")
    public BaseResponse<Boolean> updateEmployeeSalary(
            @PathVariable Long employeeId,
            @RequestBody EmployeeSalaryUpdateRequest request,
            HttpServletRequest httpRequest) {
        if (request == null || employeeId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        employeeSalaryService.updateEmployeeSalary(employeeId, request, loginUser.getId());
        return ResultUtils.success(true);
    }

    @GetMapping("/{employeeId}/history")
    public BaseResponse<List<SalaryChangeHistoryVO>> getSalaryHistory(
            @PathVariable Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<SalaryChangeHistoryVO> history = employeeSalaryService.getSalaryHistory(employeeId);
        return ResultUtils.success(history);
    }
}
