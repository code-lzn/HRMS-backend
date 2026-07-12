package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.employee.EmpProfileUpdateRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.EmpProfileVO;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工个人中心接口
 */
@RestController
@RequestMapping("/employee")
@Slf4j
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private UserService userService;

    /**
     * 查看我的档案（含敏感字段脱敏、锁定字段标记）
     */
    @GetMapping("/profile")
    public BaseResponse<EmpProfileVO> getMyProfile(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        EmpProfileVO vo = employeeService.getProfile(loginUser.getId());
        return ResultUtils.success(vo);
    }

    /**
     * 编辑我的档案（仅可编辑邮箱/现居住地址/紧急联系人）
     */
    @PostMapping("/profileUpdate")
    public BaseResponse<Boolean> updateMyProfile(@RequestBody EmpProfileUpdateRequest updateRequest,
                                                  HttpServletRequest request) {
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        employeeService.updateProfile(loginUser.getId(), updateRequest);
        return ResultUtils.success(true);
    }
}
