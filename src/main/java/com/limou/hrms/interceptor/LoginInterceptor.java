package com.limou.hrms.interceptor;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 * <p>
 * 请求进入时从 session 取出登录用户并放入 UserContext，
 * 响应返回后清理 ThreadLocal 防止内存泄漏。
 * <p>
 * 注意：此拦截器仅负责"登录态校验"，不校验角色权限。
 * 角色权限由 {@link com.limou.hrms.annotation.AuthCheck} AOP 负责。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从 session 获取登录用户
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库获取最新用户信息（保证角色等字段为最新值）
        User latestUser = userService.getById(currentUser.getId());
        if (latestUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        UserContext.set(latestUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
