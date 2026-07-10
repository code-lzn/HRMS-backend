package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工资条二次验证请求
 */
@ApiModel("工资条二次验证请求")
@Data
public class PayslipVerifyRequest {

    @ApiModelProperty("验证方式：1=短信验证码 2=登录密码")
    private Integer verify_type;

    @ApiModelProperty("验证码/密码")
    private String verify_code;
}
