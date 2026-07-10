package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.salary.PayslipVerifyRequest;
import com.limou.hrms.model.vo.salary.PayslipListVO;
import com.limou.hrms.model.vo.salary.PayslipVO;
import com.limou.hrms.service.salary.PayslipService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工资条 Controller（员工视角）
 */
@Api(tags = "工资条（员工自助）")
@RestController
@RequestMapping("/v1/payslips")
@Slf4j
public class PayslipController {

    @Resource
    private PayslipService payslipService;

    @ApiOperation("我的工资条列表（按月倒序）")
    @GetMapping
    public BaseResponse<List<PayslipListVO>> getMyPayslips(HttpServletRequest request) {
        Long employeeId = getEmployeeId(request);
        List<PayslipListVO> payslips = payslipService.getMyPayslips(employeeId);
        return ResultUtils.success(payslips);
    }

    @ApiOperation("二次验证（短信验证码/密码）")
    @PostMapping("/{batchId}/verify")
    public BaseResponse<Boolean> verify(
            @ApiParam("批次ID") @PathVariable Long batchId,
            @RequestBody PayslipVerifyRequest verifyRequest,
            HttpServletRequest request) {
        if (batchId == null || verifyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long employeeId = getEmployeeId(request);
        boolean result = payslipService.verify(employeeId, batchId, verifyRequest);
        return ResultUtils.success(result);
    }

    @ApiOperation("工资条详情（需先验证通过）")
    @GetMapping("/{batchId}")
    public BaseResponse<PayslipVO> getPayslipDetail(
            @ApiParam("批次ID") @PathVariable Long batchId,
            HttpServletRequest request) {
        if (batchId == null || batchId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long employeeId = getEmployeeId(request);
        PayslipVO vo = payslipService.getPayslipDetail(employeeId, batchId);
        return ResultUtils.success(vo);
    }

    @ApiOperation("个人薪资趋势图（近N个月实发工资折线图）")
    @GetMapping("/trend")
    public BaseResponse<List<PayslipListVO>> getSalaryTrend(
            @ApiParam("月数，默认6") @RequestParam(defaultValue = "6") int months,
            HttpServletRequest request) {
        Long employeeId = getEmployeeId(request);
        List<PayslipListVO> trend = payslipService.getSalaryTrend(employeeId, months);
        return ResultUtils.success(trend);
    }

    private Long getEmployeeId(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute("user_login");
        if (userObj != null) {
            try {
                return (Long) userObj.getClass().getMethod("getId").invoke(userObj);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
        }
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }
}
