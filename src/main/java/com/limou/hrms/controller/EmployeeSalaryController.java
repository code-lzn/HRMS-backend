package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.salary.EmployeeSalaryUpdateRequest;
import com.limou.hrms.model.vo.salary.EmployeeSalaryVO;
import com.limou.hrms.model.vo.salary.SalaryChangeHistoryVO;
import com.limou.hrms.service.salary.EmployeeSalaryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
@Api(tags = "员工薪资档案")
@RestController
@RequestMapping("/v1/employee-salaries")
@Slf4j
public class EmployeeSalaryController {

    @Resource
    private EmployeeSalaryService employeeSalaryService;

    @ApiOperation("查询员工薪资档案")
    @GetMapping("/{employeeId}")
    public BaseResponse<EmployeeSalaryVO> getEmployeeSalary(
            @ApiParam("员工ID") @PathVariable Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        EmployeeSalaryVO vo = employeeSalaryService.getEmployeeSalary(employeeId);
        return ResultUtils.success(vo);
    }

    @ApiOperation("更新员工薪资档案（自动记录调薪历史）")
    @PutMapping("/{employeeId}")
    public BaseResponse<Boolean> updateEmployeeSalary(
            @ApiParam("员工ID") @PathVariable Long employeeId,
            @RequestBody EmployeeSalaryUpdateRequest request,
            HttpServletRequest httpRequest) {
        if (request == null || employeeId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long operatorId = getOperatorId(httpRequest);
        employeeSalaryService.updateEmployeeSalary(employeeId, request, operatorId);
        return ResultUtils.success(true);
    }

    @ApiOperation("查询员工调薪历史")
    @GetMapping("/{employeeId}/history")
    public BaseResponse<List<SalaryChangeHistoryVO>> getSalaryHistory(
            @ApiParam("员工ID") @PathVariable Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<SalaryChangeHistoryVO> history = employeeSalaryService.getSalaryHistory(employeeId);
        return ResultUtils.success(history);
    }

    private Long getOperatorId(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute("user_login");
        if (userObj != null) {
            try {
                return (Long) userObj.getClass().getMethod("getId").invoke(userObj);
            } catch (Exception e) {
                return 1L;
            }
        }
        return 1L;
    }
}
