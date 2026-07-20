package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.salary.PayslipVerifyRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.salary.PayslipVO;
import com.limou.hrms.service.UserService;
import com.limou.hrms.service.salary.SalaryDetailService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工资条 Controller（员工自助）
 */
@Api(tags = "工资条（员工自助）")
@RestController
@RequestMapping("/v1/payslips")
@Slf4j
public class SalaryDetailController {

    @Resource
    private SalaryDetailService salaryDetailService;

    @Resource
    private UserService userService;

    @Resource
    private EmployeeMapper employeeMapper;

    @ApiOperation("获取我的工资条列表")
    @GetMapping("/my")
    @AuthCheck(mustRole = {UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE, UserConstant.DEFAULT_ROLE})
    public BaseResponse<List<PayslipVO>> getMyPayslips(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long employeeId = getEmployeeId(loginUser);
        List<PayslipVO> list = salaryDetailService.getMyPayslips(employeeId);
        return ResultUtils.success(list);
    }

    @ApiOperation("查看工资条详情")
    @GetMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE, UserConstant.DEFAULT_ROLE})
    public BaseResponse<PayslipVO> getPayslipDetail(
            @ApiParam("工资条ID") @PathVariable Long id,
            HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long employeeId = getEmployeeId(loginUser);
        PayslipVO vo = salaryDetailService.getPayslipDetail(employeeId, id);
        return ResultUtils.success(vo);
    }

    @ApiOperation("二次验证")
    @PostMapping("/{id}/verify")
    @AuthCheck(mustRole = {UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE, UserConstant.DEFAULT_ROLE})
    public BaseResponse<Boolean> verifyPayslip(
            @PathVariable Long id,
            @RequestBody PayslipVerifyRequest verifyRequest,
            HttpServletRequest request) {
        if (id == null || id <= 0 || verifyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long employeeId = getEmployeeId(loginUser);
        boolean result = salaryDetailService.verifyPayslip(employeeId, id, verifyRequest);
        return ResultUtils.success(result);
    }

    /**
     * 根据登录用户获取对应的员工 ID
     */
    private Long getEmployeeId(User loginUser) {
        Employee employee = employeeMapper.selectByUserId(loginUser.getId());
        if (employee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到关联的员工档案");
        }
        return employee.getId();
    }
}
