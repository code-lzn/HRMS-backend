package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 密码历史
 * @TableName password_history
 */
@TableName(value = "password_history")
@Data
public class PasswordHistory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 历史密码哈希 */
    private String passwordHash;

    /** 创建时间 */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
