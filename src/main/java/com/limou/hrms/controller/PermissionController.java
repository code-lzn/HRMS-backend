package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.UserPermissionVO;
import com.limou.hrms.service.PermissionService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/permission")
@Slf4j
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    @Resource
    private UserService userService;

    @GetMapping("/current")
    public BaseResponse<UserPermissionVO> getCurrentPermissions(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        UserPermissionVO vo = permissionService.getUserPermissions(currentUser.getId());
        return ResultUtils.success(vo);
    }

    @GetMapping("/check")
    public BaseResponse<Boolean> checkPermission(HttpServletRequest request, @RequestParam String code) {
        User currentUser = userService.getLoginUser(request);
        boolean has = permissionService.hasPermission(currentUser.getId(), code);
        return ResultUtils.success(has);
    }

    @GetMapping("/data-scope")
    public BaseResponse<Integer> getDataScope(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        Integer dataScope = permissionService.getUserDataScope(currentUser.getId());
        return ResultUtils.success(dataScope);
    }

    @GetMapping("/codes")
    public BaseResponse<List<String>> getAllPermissionCodes() {
        List<String> codes = permissionService.getAllPermissionCodes();
        return ResultUtils.success(codes);
    }
}