package com.limou.hrms.model.dto.account;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordRequest implements Serializable {

    /** 原密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;

    /** 确认新密码 */
    private String confirmPassword;

    private static final long serialVersionUID = 1L;
}
