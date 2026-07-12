package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.account.BindPhoneRequest;
import com.limou.hrms.model.dto.account.ChangePasswordRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.LoginLogVO;
import com.limou.hrms.service.LoginLogService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 账号安全接口
 */
@RestController
@RequestMapping("/account")
@Slf4j
public class AccountSecurityController {

    @Resource
    private UserService userService;

    @Resource
    private LoginLogService loginLogService;

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    public BaseResponse<Boolean> changePassword(@RequestBody ChangePasswordRequest request,
                                                 HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        userService.changePassword(loginUser.getId(),
                request.getOldPassword(), request.getNewPassword(), request.getConfirmPassword());
        return ResultUtils.success(true);
    }

    /**
     * 绑定/解绑手机
     */
    @PostMapping("/bindPhone")
    public BaseResponse<Boolean> bindPhone(@RequestBody BindPhoneRequest request,
                                            HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        userService.updatePhone(loginUser.getId(), request.getPhone());
        return ResultUtils.success(true);
    }

    /**
     * 获取登录日志（最近30条）
     */
    @GetMapping("/loginLogs")
    public BaseResponse<List<LoginLogVO>> getLoginLogs(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<LoginLogVO> list = loginLogService.getMyLoginLogs(loginUser.getId());
        return ResultUtils.success(list);
    }
}
