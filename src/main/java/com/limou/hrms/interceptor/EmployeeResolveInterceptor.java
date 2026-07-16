package com.limou.hrms.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 员工 ID 解析拦截器 — 在请求进入 Controller 前解析 employee.id，
 * 写入 {@code request.attribute("currentEmployeeId")}，后续直接读取。
 */
@Component
@Slf4j
public class EmployeeResolveInterceptor implements HandlerInterceptor {

    public static final String CURRENT_EMPLOYEE_ID = "currentEmployeeId";
    /** 当前用户角色（user.user_role） */
    public static final String CURRENT_USER_ROLE = "currentUserRole";

    @Resource
    private UserService userService;
    @Resource
    private EmployeeMapper employeeMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            User loginUser = userService.getLoginUserPermitNull(request);
            if (loginUser == null) {
                // 未登录的请求放行，Controller 自己处理权限
                return true;
            }
            Employee employee = employeeMapper.selectOne(
                    new QueryWrapper<Employee>().eq("user_id", loginUser.getId()));
            if (employee == null) {
                log.warn("用户 {} 未关联员工档案", loginUser.getId());
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.getWriter().write("{\"code\":40101,\"message\":\"当前用户未关联员工档案\"}");
                return false;
            }
            request.setAttribute(CURRENT_EMPLOYEE_ID, employee.getId());
            request.setAttribute(CURRENT_USER_ROLE, loginUser.getUserRole());
        } catch (Exception e) {
            log.error("解析员工 ID 失败", e);
            // 放行，让 Controller 兜底
        }
        return true;
    }
}
