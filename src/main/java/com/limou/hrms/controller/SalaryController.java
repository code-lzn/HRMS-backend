package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.salary.VerifyPasswordRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.SalarySlipDetailVO;
import com.limou.hrms.model.vo.SalarySlipVO;
import com.limou.hrms.model.vo.SalaryTrendVO;
import com.limou.hrms.service.SalarySlipService;
import com.limou.hrms.service.UserService;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 我的薪资接口
 */
@RestController
@RequestMapping("/salary")
@Slf4j
public class SalaryController {

    @Resource
    private SalarySlipService salarySlipService;

    @Resource
    private UserService userService;

    /**
     * 获取我的工资条列表（按月倒序）
     */
    @GetMapping("/slips")
    public BaseResponse<List<SalarySlipVO>> getMySalarySlips(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<SalarySlipVO> list = salarySlipService.getMySalarySlips(loginUser.getId());
        return ResultUtils.success(list);
    }

    /**
     * 查看工资条详情（需二次密码验证）
     */
    @PostMapping("/slip/{id}")
    public BaseResponse<SalarySlipDetailVO> getSalarySlipDetail(
            @PathVariable Long id,
            @RequestBody VerifyPasswordRequest verifyRequest,
            HttpServletRequest request) {
        if (verifyRequest == null || verifyRequest.getPassword() == null
                || verifyRequest.getPassword().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入密码进行验证");
        }
        User loginUser = userService.getLoginUser(request);
        SalarySlipDetailVO vo = salarySlipService.getSalarySlipDetail(
                id, loginUser.getId(), verifyRequest.getPassword());
        return ResultUtils.success(vo);
    }

    /**
     * 获取近6个月薪资趋势
     */
    @GetMapping("/trend")
    public BaseResponse<List<SalaryTrendVO>> getMySalaryTrend(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<SalaryTrendVO> list = salarySlipService.getMySalaryTrend(loginUser.getId());
        return ResultUtils.success(list);
    }
}
