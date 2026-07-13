package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.CommonConstant;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.RoleMapper;
import com.limou.hrms.model.dto.role.RoleQueryRequest;
import com.limou.hrms.model.entity.Role;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.RoleService;
import com.limou.hrms.service.UserService;
import com.limou.hrms.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Resource
    private UserService userService;

    @Override
    public List<Role> getAllRoles() {
        return baseMapper.selectAll();
    }

    @Override
    public List<Role> getAllEnabledRoles() {
        return baseMapper.selectAllEnabled();
    }

    @Override
    public Role getRoleById(Long id) {
        return this.getById(id);
    }

    @Override
    public Role getRoleByCode(String code) {
        return baseMapper.selectByCode(code);
    }

    @Override
    @Transactional
    public boolean addRole(Role role) {
        Role existRole = this.getRoleByCode(role.getRoleCode());
        if (existRole != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色编码已存在");
        }
        role.setStatus(1);
        return this.save(role);
    }

    @Override
    @Transactional
    public boolean updateRole(Role role) {
        return this.updateById(role);
    }

    @Override
    @Transactional
    public boolean deleteRole(Long id) {
        return this.removeById(id);
    }

    @Override
    @Transactional
    public boolean assignRoleToUser(Long userId, Long roleId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        Role role = this.getRoleById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "角色不存在");
        }
        user.setRoleId(roleId);
        // 同步更新 userRole 字段，保持与 AuthInterceptor / isAdmin() 的向后兼容
        if (UserConstant.ADMIN_ROLE.equals(role.getRoleCode())) {
            user.setUserRole(UserConstant.ADMIN_ROLE);
        } else {
            user.setUserRole(UserConstant.DEFAULT_ROLE);
        }
        return userService.updateById(user);
    }

    @Override
    public QueryWrapper<Role> getQueryWrapper(RoleQueryRequest roleQueryRequest) {
        if (roleQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = roleQueryRequest.getId();
        String roleName = roleQueryRequest.getRoleName();
        String roleCode = roleQueryRequest.getRoleCode();
        Integer dataScope = roleQueryRequest.getDataScope();
        Integer status = roleQueryRequest.getStatus();
        String sortField = roleQueryRequest.getSortField();
        String sortOrder = roleQueryRequest.getSortOrder();

        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(roleCode), "roleCode", roleCode);
        queryWrapper.eq(dataScope != null, "dataScope", dataScope);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.like(StringUtils.isNotBlank(roleName), "roleName", roleName);
        boolean isAsc = sortOrder == null || CommonConstant.SORT_ORDER_ASC.equals(sortOrder);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), isAsc, sortField);
        return queryWrapper;
    }
}