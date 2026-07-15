package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

/**
 * 工资条二次验证请求
 */
@ApiModel("工资条二次验证请求")
@Data
public class PayslipVerifyRequest implements Serializable {

    @ApiModelProperty("验证类型：1=短信验证码, 2=登录密码")
    private Integer verifyType;

    @ApiModelProperty("验证码或密码")
    private String verifyCode;

    private static final long serialVersionUID = 1L;
}
