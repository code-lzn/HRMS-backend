package com.limou.hrms.aop;



import com.limou.hrms.annotation.AuthCheck;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.UserRoleEnum;
import com.limou.hrms.service.PermissionService;
import com.limou.hrms.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 权限校验 AOP
 * <p>
 * 兼容旧版权限（userRole 字段 + UserRoleEnum）和新版 RBAC（roleId 关联 role 表）。
 * 当旧版检查不通过时，会尝试通过 PermissionService 检查新版 RBAC 权限。
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private PermissionService permissionService;

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
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        // 必须有该权限才通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 如果被封号，直接拒绝
        if (UserRoleEnum.BAN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 必须有管理员权限
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum)) {
            // 先检查旧版 userRole 是否为 admin
            if (UserRoleEnum.ADMIN.equals(userRoleEnum)) {
                return joinPoint.proceed();
            }
            // 旧版不通过时，检查新版 RBAC：用户是否通过 roleId 关联了拥有管理员权限的角色
            if (loginUser.getRoleId() != null) {
                // 拥有超级权限或角色管理权限的用户视为管理员等效
                if (permissionService.hasPermission(loginUser.getId(), "*:*:*") ||
                    permissionService.hasPermission(loginUser.getId(), "role:manage")) {
                    return joinPoint.proceed();
                }
            }
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
