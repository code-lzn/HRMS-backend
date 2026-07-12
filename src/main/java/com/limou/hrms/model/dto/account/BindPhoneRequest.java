package com.limou.hrms.model.dto.account;

import lombok.Data;

import java.io.Serializable;

/**
 * 绑定/解绑手机请求
 */
@Data
public class BindPhoneRequest implements Serializable {

    /** 手机号（传空字符串表示解绑） */
    private String phone;

    private static final long serialVersionUID = 1L;
}
