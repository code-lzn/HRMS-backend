package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录日志视图
 */
@Data
public class LoginLogVO implements Serializable {

    /** 日志ID */
    private Long id;

    /** 登录时间 */
    private Date loginTime;

    /** 登录IP */
    private String ip;

    /** 设备信息 */
    private String device;

    /** 登录方式：1=密码登录 2=短信验证码登录 */
    private Integer loginType;

    /** 登录方式文本 */
    private String loginTypeText;

    /** 是否成功：0=失败 1=成功 */
    private Integer isSuccess;

    /** 失败原因 */
    private String failReason;

    private static final long serialVersionUID = 1L;
}
