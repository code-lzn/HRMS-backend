package com.limou.hrms.context;

import com.limou.hrms.model.entity.User;

/**
 * 当前登录用户上下文（ThreadLocal）
 * <p>
 * 由 LoginInterceptor 在请求进入时设置，响应返回时清除。
 * Controller / Service 中通过 {@link #getCurrentUser()} 获取当前用户，
 * 无需再通过 HttpServletRequest 参数层层传递。
 */
public class UserContext {

    private static final ThreadLocal<User> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(User user) {
        USER_HOLDER.set(user);
    }

    public static User getCurrentUser() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
