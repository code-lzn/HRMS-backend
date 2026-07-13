package com.limou.hrms.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.limou.hrms.model.entity.Role;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.DataScopeEnum;
import com.limou.hrms.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static com.limou.hrms.constant.UserConstant.USER_LOGIN_STATE;

@Component
public class PermissionUtils {

    private static PermissionUtils instance;

    @Autowired
    private RoleService roleService;

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 从 Session 获取当前用户
     */
    public static User getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            return null;
        }
        return (User) userObj;
    }

    /**
     * 根据用户获取角色
     */
    public static Role getRoleByUser(User user) {
        if (user == null || user.getRoleId() == null) {
            return null;
        }
        if (instance == null || instance.roleService == null) {
            return null;
        }
        return instance.roleService.getById(user.getRoleId());
    }

    /**
     * 解析权限列表 JSON
     */
    public static List<String> parsePermissions(String permissionsJson) {
        if (StrUtil.isBlank(permissionsJson)) {
            return new ArrayList<>();
        }
        try {
            JSONArray jsonArray = JSONUtil.parseArray(permissionsJson);
            return jsonArray.toList(String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 检查用户是否有某权限
     */
    public static boolean hasPermission(User user, String permissionCode) {
        if (user == null || StrUtil.isBlank(permissionCode)) {
            return false;
        }

        Role role = getRoleByUser(user);
        if (role == null || StrUtil.isBlank(role.getPermissions())) {
            return false;
        }

        List<String> permissions = parsePermissions(role.getPermissions());
        if (permissions.contains("*:*:*")) {
            return true;
        }
        return permissions.contains(permissionCode);
    }

    /**
     * 获取用户数据范围
     */
    public static Integer getUserDataScope(User user) {
        if (user == null || user.getRoleId() == null) {
            return DataScopeEnum.SELF.getCode();
        }

        Role role = getRoleByUser(user);
        if (role == null) {
            return DataScopeEnum.SELF.getCode();
        }

        Integer dataScope = role.getDataScope();
        return dataScope != null ? dataScope : DataScopeEnum.SELF.getCode();
    }

    /**
     * 获取用户字段权限
     */
    public static String getFieldPermissions(User user) {
        if (user == null || user.getRoleId() == null) {
            return null;
        }

        Role role = getRoleByUser(user);
        if (role == null) {
            return null;
        }

        return role.getFieldPermissions();
    }
}