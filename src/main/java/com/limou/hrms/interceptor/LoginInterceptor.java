package com.limou.hrms.interceptor;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.UserService;
import com.limou.hrms.utils.JwtUtils;
import io.jsonwebtoken.Claims;
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
 *
 * <p>双模认证：优先从 Authorization 头读取 JWT Token，
 * Token 无效或不存在时回退到 Session 方式（兼容旧版）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检请求不携带 session，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // ---- 1. 优先从 Authorization 头读取 JWT Token ----
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims = jwtUtils.parseToken(token);
            if (claims != null) {
                // JWT 有效，构造轻量 User 对象放入 UserContext
                User jwtUser = new User();
                jwtUser.setId(claims.get("userId", Long.class));
                jwtUser.setUserName(claims.get("userName", String.class));
                jwtUser.setUserRole(claims.get("userRole", String.class));
                UserContext.set(jwtUser);
                return true;
            }
            // JWT 无效，继续尝试 Session 方式（不直接抛异常）
        }

        // ---- 2. 回退：从 Session 获取登录用户（兼容旧版） ----
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

        UserContext.set(currentUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
