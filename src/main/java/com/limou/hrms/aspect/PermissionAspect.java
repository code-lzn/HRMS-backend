package com.limou.hrms.aspect;

import cn.hutool.core.util.StrUtil;
import com.limou.hrms.annotation.RequirePermission;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.PermissionService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Resource
    private UserService userService;

    @Resource
    private PermissionService permissionService;

//    @Around("@annotation(com.limou.hrms.annotation.RequirePermission)")
//    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
//        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//        if (attributes == null) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法获取请求上下文");
//        }
//        HttpServletRequest request = attributes.getRequest();
//        User currentUser = userService.getLoginUserPermitNull(request);
//
//        if (currentUser == null) {
//            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
//        }
//
//        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//        Method method = signature.getMethod();
//        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
//
//        if (requirePermission != null) {
//            String permissionCode = requirePermission.value();
//            if (StrUtil.isNotBlank(permissionCode)) {
//                boolean hasPermission = permissionService.hasPermission(currentUser.getId(), permissionCode);
//                if (!hasPermission) {
//                    log.warn("用户 {} 无权限访问: {}", currentUser.getUserAccount(), permissionCode);
//                    throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "无权限访问：" + permissionCode);
//                }
//            }
//        }
//
//        return joinPoint.proceed();
//    }
}