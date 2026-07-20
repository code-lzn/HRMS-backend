package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 员工个人信息控制器（前端 /api/employee/* 路径）
 */
@RestController
@RequestMapping("/employee")
@Slf4j
@RequiredArgsConstructor
public class EmployeeProfileController {

    private final UserService userService;

    /**
     * 获取当前登录用户的员工档案
     */
    @GetMapping("/profile")
    public BaseResponse<Map<String, Object>> getMyProfile(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("employeeName", user.getUserName());
        profile.put("employeeNo", "");
        profile.put("departmentName", "");
        profile.put("positionName", "");
        profile.put("phone", "");
        profile.put("email", "");
        profile.put("hireDate", "");
        profile.put("status", 1);
        return ResultUtils.success(profile);
    }
}
