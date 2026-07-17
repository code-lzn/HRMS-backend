package com.limou.hrms.interceptor;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器
 * <p>
 * 请求进入时从 session 取出登录用户并放入 UserContext，
 * 同时校验密码版本号（密码修改后强制旧登录态失效），
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
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检请求不携带 session，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 从 session 获取登录用户
        HttpSession session = request.getSession();
        Object userObj = session.getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 校验密码版本号：如果密码被修改过，此 session 的版本号将不匹配，强制下线
        String sessionVersion = (String) session.getAttribute("PASSWORD_VERSION");
        String redisVersion = stringRedisTemplate.opsForValue()
                .get(String.format("pwd:version:%d", currentUser.getId()));
        if (sessionVersion != null && redisVersion != null && !sessionVersion.equals(redisVersion)) {
            log.info("用户 {} 密码版本不匹配（session={}, redis={}），强制下线",
                    currentUser.getId(), sessionVersion, redisVersion);
            session.removeAttribute(UserConstant.USER_LOGIN_STATE);
            session.removeAttribute("PASSWORD_VERSION");
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "密码已修改，请重新登录");
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
