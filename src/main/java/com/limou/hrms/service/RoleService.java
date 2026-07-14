package com.limou.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.role.RoleQueryRequest;
import com.limou.hrms.model.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {

    List<Role> getAllRoles();

    List<Role> getAllEnabledRoles();

    Role getRoleById(Long id);

    Role getRoleByCode(String code);

    boolean addRole(Role role);

    boolean updateRole(Role role);

    boolean deleteRole(Long id);

    boolean assignRoleToUser(Long userId, Long roleId);

    QueryWrapper<Role> getQueryWrapper(RoleQueryRequest roleQueryRequest);
}