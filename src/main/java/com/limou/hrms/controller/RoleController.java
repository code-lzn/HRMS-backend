package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.annotation.RequirePermission;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.PermissionConstant;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.role.RoleAddRequest;
import com.limou.hrms.model.dto.role.RoleAssignRequest;
import com.limou.hrms.model.dto.role.RoleQueryRequest;
import com.limou.hrms.model.dto.role.RoleUpdateRequest;
import com.limou.hrms.model.entity.Role;
import com.limou.hrms.model.vo.RoleVO;
import com.limou.hrms.model.enums.DataScopeEnum;
import com.limou.hrms.service.RoleService;
import com.limou.hrms.utils.PermissionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/role")
@Slf4j
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping("/list/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RequirePermission(PermissionConstant.ROLE_MANAGE)
    public BaseResponse<List<RoleVO>> listAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        List<RoleVO> voList = roles.stream().map(this::toVO).collect(Collectors.toList());
        return ResultUtils.success(voList);
    }

    @GetMapping("/list/enabled")
    public BaseResponse<List<RoleVO>> listEnabledRoles() {
        List<Role> roles = roleService.getAllEnabledRoles();
        List<RoleVO> voList = roles.stream().map(this::toVO).collect(Collectors.toList());
        return ResultUtils.success(voList);
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RequirePermission(PermissionConstant.ROLE_MANAGE)
    public BaseResponse<RoleVO> getRoleById(@RequestParam Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Role role = roleService.getRoleById(id);
        ThrowUtils.throwIf(role == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(toVO(role));
    }

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RequirePermission(PermissionConstant.ROLE_MANAGE)
    public BaseResponse<Long> addRole(@RequestBody RoleAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Role role = new Role();
        BeanUtils.copyProperties(request, role);
        boolean result = roleService.addRole(role);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(role.getId());
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RequirePermission(PermissionConstant.ROLE_MANAGE)
    public BaseResponse<Boolean> updateRole(@RequestBody RoleUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Role role = new Role();
        BeanUtils.copyProperties(request, role);
        boolean result = roleService.updateRole(role);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RequirePermission(PermissionConstant.ROLE_MANAGE)
    public BaseResponse<Boolean> deleteRole(@RequestParam Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = roleService.deleteRole(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/assign")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RequirePermission(PermissionConstant.ROLE_MANAGE)
    public BaseResponse<Boolean> assignRole(@RequestBody RoleAssignRequest request) {
        if (request == null || request.getUserId() == null || request.getRoleId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = roleService.assignRoleToUser(request.getUserId(), request.getRoleId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @RequirePermission(PermissionConstant.ROLE_MANAGE)
    public BaseResponse<Page<RoleVO>> listRoleByPage(@RequestBody RoleQueryRequest request) {
        long current = request.getCurrent();
        long size = request.getPageSize();
        Page<Role> page = roleService.page(new Page<>(current, size), roleService.getQueryWrapper(request));
        Page<RoleVO> voPage = new Page<>(current, size, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return ResultUtils.success(voPage);
    }

    private RoleVO toVO(Role role) {
        if (role == null) {
            return null;
        }
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        vo.setDataScopeDesc(DataScopeEnum.getDescByCode(role.getDataScope()));
        vo.setPermissionCodes(PermissionUtils.parsePermissions(role.getPermissions()));
        return vo;
    }
}
