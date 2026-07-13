package com.limou.hrms.model.dto.salary;

import lombok.Data;

import java.io.Serializable;

/**
 * 二次验证请求
 */
@Data
public class VerifyPasswordRequest implements Serializable {

    /** 需要验证的密码 */
    private String password;

    private static final long serialVersionUID = 1L;
}
