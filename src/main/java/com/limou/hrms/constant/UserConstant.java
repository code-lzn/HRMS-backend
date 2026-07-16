package com.limou.hrms.constant;

/**
 * 用户常量
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public interface UserConstant {


    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色（普通员工）
     */
    String DEFAULT_ROLE = "user";

    /**
     * 系统管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * HR专员角色
     */
    String HR_ROLE = "hr";

    /**
     * 部门主管角色
     */
    String DEPT_HEAD_ROLE = "dept_head";

    /**
     * 财务专员角色
     */
    String FINANCE_ROLE = "finance";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    // endregion
}
