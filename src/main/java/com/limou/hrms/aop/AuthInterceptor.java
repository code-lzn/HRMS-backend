package com.limou.hrms.aop;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.UserRoleEnum;
import com.limou.hrms.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限校验 AOP — 同时承担数据权限上下文注入
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private DataScopeContext dataScopeContext;

    @Resource
    private DepartmentMapper departmentMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String[] mustRole = authCheck.mustRole();

        // 当前登录用户
        User loginUser = UserContext.getCurrentUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 不需要权限（空数组或仅含空字符串），放行
        if (mustRole.length == 0 || (mustRole.length == 1 && mustRole[0].isEmpty())) {
            // 注入数据权限上下文（无论是否需要权限，只要登录就注入）
            populateDataScope(loginUser);
            return joinPoint.proceed();
        }
        // 必须有该权限才通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 如果被封号，直接拒绝
        if (UserRoleEnum.BAN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 遍历必需角色，任一命中即放行
        for (String role : mustRole) {
            UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(role);
            if (mustRoleEnum == null) {
                continue;
            }
            if (mustRoleEnum.equals(userRoleEnum)) {
                // 注入数据权限上下文（无论是否需要权限，只要登录就注入）
                populateDataScope(loginUser);
                return joinPoint.proceed();
            }
        }
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
    }

    /**
     * 注入数据权限上下文
     */
    private void populateDataScope(User user) {
        dataScopeContext.setCurrentUserId(user.getId());
        dataScopeContext.setCurrentRole(user.getUserRole());

        // 查找当前用户对应的员工档案
        Employee employee = employeeMapper.selectByUserId(user.getId());

        if(employee==null) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);

        dataScopeContext.setCurrentEmployeeId(employee.getId());

        // 部门主管：查找管理的部门及下属部门
        if ("dept_head".equals(user.getUserRole())) {

            QueryWrapper<Department> wrapper = new QueryWrapper<>();
            wrapper.eq("manager_id", employee.getId());//查找部门主管所在的部门
            wrapper.eq("is_deleted", 0);
            Department managedDept = departmentMapper.selectOne(wrapper);
            if (managedDept != null) {
                dataScopeContext.setCurrentDepartmentId(managedDept.getId());
                Set<Long> deptIds = new HashSet<>();
                collectChildDeptIds(managedDept.getId(), deptIds);
                dataScopeContext.setManagedDepartmentIds(deptIds);
            } else {
                dataScopeContext.setManagedDepartmentIds(Collections.emptySet());
            }
        }
    }

    /**
     * 递归收集所有子部门 ID
     */
    private void collectChildDeptIds(Long parentId, Set<Long> result) {
        result.add(parentId);//将当前部门ID以及子部门ID添加到结果集合中
        QueryWrapper<Department> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", parentId);
        wrapper.eq("is_deleted", 0);
        List<Department> children = departmentMapper.selectList(wrapper);//查找当前部门下的所有子部门
        for (Department child : children) {
            collectChildDeptIds(child.getId(), result);
        }
    }
}

