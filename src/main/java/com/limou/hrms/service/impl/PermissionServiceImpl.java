package com.limou.hrms.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.PermissionConstant;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.entity.Role;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.DataScopeEnum;
import com.limou.hrms.model.vo.UserPermissionVO;
import com.limou.hrms.service.PermissionService;
import com.limou.hrms.service.UserService;
import com.limou.hrms.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private UserService userService;
    @Resource
    private RoleService roleService;

    @Override
    public UserPermissionVO getUserPermissions(Long userId) {
        UserPermissionVO vo = new UserPermissionVO();

        try {
            User user = userService.getById(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            }

            vo.setUserId(user.getId());
            vo.setUserAccount(user.getUserAccount());
            vo.setUserName(user.getUserName());

            // 兼容旧管理员账号
            if (user.getRoleId() == null && UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
                vo.setDataScope(DataScopeEnum.ALL.getCode());
                vo.setDataScopeDesc(DataScopeEnum.ALL.getDesc());
                vo.setPermissionCodes(Collections.singletonList("*:*:*"));
                return vo;
            }

            if (user.getRoleId() != null) {
                Role role = roleService.getRoleById(user.getRoleId());
                if (role != null) {
                    vo.setRoleId(role.getId());
                    vo.setRoleName(role.getRoleName());
                    vo.setRoleCode(role.getRoleCode());
                    if (isRoleActive(role)) {
                        vo.setDataScope(role.getDataScope());
                        vo.setDataScopeDesc(getDataScopeDesc(role.getDataScope()));
                        vo.setPermissions(role.getPermissions());
                        vo.setPermissionCodes(parsePermissionCodes(role.getPermissions()));
                        vo.setFieldPermissions(role.getFieldPermissions());
                    } else {
                        vo.setDataScope(DataScopeEnum.SELF.getCode());
                        vo.setDataScopeDesc(DataScopeEnum.SELF.getDesc());
                        vo.setPermissionCodes(Collections.emptyList());
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取用户权限失败, userId: {}", userId, e);
            vo.setDataScope(DataScopeEnum.SELF.getCode());
            vo.setDataScopeDesc(DataScopeEnum.SELF.getDesc());
            vo.setPermissionCodes(Collections.emptyList());
        }

        return vo;
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        if (StrUtil.isBlank(permissionCode)) {
            return true;
        }

        List<String> permissions = getUserPermissionCodes(userId);
        if (CollUtil.isEmpty(permissions)) {
            return false;
        }

        if (permissions.contains("*:*:*")) {
            return true;
        }

        return permissions.contains(permissionCode);
    }

    @Override
    public Integer getUserDataScope(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return DataScopeEnum.SELF.getCode();
        }
        // 兼容旧管理员账号：有 admin 角色字符串但未关联新角色表
        if (user.getRoleId() == null && UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            return DataScopeEnum.ALL.getCode();
        }
        if (user.getRoleId() == null) {
            return DataScopeEnum.SELF.getCode();
        }
        Role role = roleService.getRoleById(user.getRoleId());
        if (role == null || !isRoleActive(role) || role.getDataScope() == null) {
            return DataScopeEnum.SELF.getCode();
        }
        return role.getDataScope();
    }

    @Override
    public List<String> getUserPermissionCodes(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return Collections.emptyList();
        }
        // 兼容旧管理员账号：有 admin 角色字符串但未关联新角色表
        if (user.getRoleId() == null && UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            return Collections.singletonList("*:*:*");
        }
        if (user.getRoleId() == null) {
            return Collections.emptyList();
        }
        Role role = roleService.getRoleById(user.getRoleId());
        if (role == null || !isRoleActive(role) || StrUtil.isBlank(role.getPermissions())) {
            return Collections.emptyList();
        }
        try {
            JSONArray jsonArray = JSONUtil.parseArray(role.getPermissions());
            return jsonArray.toList(String.class);
        } catch (Exception e) {
            log.warn("解析用户权限JSON失败, userId: {}, permissions: {}", userId, role.getPermissions(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getFieldPermissions(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return null;
        }
        // 兼容旧管理员账号
        if (user.getRoleId() == null && UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            return null;
        }
        if (user.getRoleId() == null) {
            return null;
        }
        Role role = roleService.getRoleById(user.getRoleId());
        if (role == null || !isRoleActive(role)) {
            return null;
        }
        return role.getFieldPermissions();
    }

    @Override
    public List<String> getAllPermissionCodes() {
        return PermissionConstant.ALL_CODES;
    }

    /**
     * 解析权限 JSON 为字符串列表
     */
    private List<String> parsePermissionCodes(String permissionsJson) {
        if (StrUtil.isBlank(permissionsJson)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.parseArray(permissionsJson).toList(String.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 判断角色是否启用
     */
    private boolean isRoleActive(Role role) {
        return role != null && role.getStatus() != null && role.getStatus() == 1;
    }

    private String getDataScopeDesc(Integer dataScope) {
        if (dataScope == null) {
            return DataScopeEnum.SELF.getDesc();
        }
        return DataScopeEnum.getDescByCode(dataScope);
    }
}
