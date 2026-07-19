package com.limou.hrms.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 已登录用户视图（脱敏）
 *
 **/
@Data
public class LoginUserVO implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    /**
     * 是否需要重置密码（1=首次登录需强制修改密码）
     */
    private Integer pwdReset;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * JWT Token（多标签页独立登录用）
     */
    private String token;

    private static final long serialVersionUID = 1L;
}