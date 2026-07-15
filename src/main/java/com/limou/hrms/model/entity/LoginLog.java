package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录日志
 * @TableName login_log
 */
@TableName(value = "login_log")
@Data
public class LoginLog implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 登录时间 */
    private Date loginTime;

    /** 登录IP */
    private String ip;

    /** 设备信息 */
    private String device;

    /** 登录方式：1=密码登录 2=短信验证码登录 */
    private Integer loginType;

    /** 是否成功：0=失败 1=成功 */
    private Integer isSuccess;

    /** 失败原因 */
    private String failReason;

    private static final long serialVersionUID = 1L;
}
