package com.limou.hrms.model.dto.profile;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 修改手机号请求
 */
@Data
public class PhoneChangeDTO implements Serializable {

    @NotBlank(message = "新手机号不能为空")
    private String newPhone;

    @NotBlank(message = "验证码不能为空")
    private String verifyCode;

    private static final long serialVersionUID = 1L;
}
