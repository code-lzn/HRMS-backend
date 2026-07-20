package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体
 */
@TableName("login_log")
@Data
public class LoginLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID → user.id
     */
    private Long userId;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * IP 地址
     */
    private String ipAddress;

    /**
     * 设备信息
     */
    private String device;

    /**
     * 登录结果：1=成功 0=失败
     */
    private Integer result;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
