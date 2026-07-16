package com.limou.hrms.aop;

import cn.hutool.json.JSONUtil;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.enums.PermissionUrlEnum;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.PermissionService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 权限拦截器 —— 遍历 {@link PermissionUrlEnum} 匹配 URL，强制校验权限码
 * <p>
 * 未命中任何枚举值的 URL 默认放行；命中则必须拥有对应权限码。
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PermissionService permissionService;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        // OPTIONS 预检请求不带 Cookie，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // 遍历枚举，查找该 URI 对应的权限码
        String requiredPermission = matchPermission(uri);
        if (requiredPermission == null) {
            // 未命中枚举 → 自服务接口或公开接口，放行
            return true;
        }

        // 获取当前登录用户（未登录会抛 BusinessException，由 GlobalExceptionHandler 统一处理）
        User currentUser = userService.getLoginUser(request);

        // 检查用户是否拥有该权限
        if (!permissionService.hasPermission(currentUser.getId(), requiredPermission)) {
            log.warn("权限拦截: 用户 {} 无权限 {} 访问 {}", currentUser.getUserAccount(),
                    requiredPermission, uri);
            writeJson(response, ErrorCode.FORBIDDEN_ERROR,
                    "无权限访问，需要权限：" + requiredPermission);
            return false;
        }

        return true;
    }

    /**
     * 遍历 {@link PermissionUrlEnum}，用 AntPathMatcher 匹配请求 URI，
     * 命中返回所需权限码，未命中返回 null
     */
    private String matchPermission(String uri) {
        for (PermissionUrlEnum entry : PermissionUrlEnum.values()) {
            if (PATH_MATCHER.match(entry.getUrlPattern(), uri)) {
                return entry.getPermissionCode();
            }
        }
        return null;
    }

    /**
     * 向响应写入标准 JSON 格式的错误信息
     */
    private void writeJson(HttpServletResponse response, ErrorCode errorCode, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        BaseResponse<?> errorResponse = ResultUtils.error(errorCode, message);
        response.getWriter().write(JSONUtil.toJsonStr(errorResponse));
    }
}
