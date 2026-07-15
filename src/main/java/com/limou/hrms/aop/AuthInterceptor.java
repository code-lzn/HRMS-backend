package com.limou.hrms.aop;



import com.limou.hrms.annotation.AuthCheck;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.entity.Role;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.PermissionService;
import com.limou.hrms.service.RoleService;
import com.limou.hrms.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验 AOP
 * <p>
 * 基于新版 RBAC：通过 roleId 关联 role 表，由 PermissionService 校验权限。
 * 当 @AuthCheck(mustRole = "admin") 时，检查用户角色是否拥有管理员等效权限。
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private PermissionService permissionService;

    @Lazy
    @Resource
    private RoleService roleService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限注解
     * @return 方法返回值
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 检查用户是否有 roleId
        if (loginUser.getRoleId() == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未分配角色");
        }

        // 检查角色是否被禁用
        Role role = roleService.getRoleById(loginUser.getRoleId());
        if (role == null || role.getStatus() == null || role.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "角色已被禁用");
        }

        // @AuthCheck(mustRole = "admin") → 检查是否拥有管理员等效权限
        if ("admin".equals(mustRole)) {
            if (permissionService.hasPermission(loginUser.getId(), "*:*:*") ||
                permissionService.hasPermission(loginUser.getId(), "role:manage")) {
                return joinPoint.proceed();
            }
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
