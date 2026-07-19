package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户
 */
@TableName(value = "user")
@Data
public class User implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String userAccount;

    private String userPassword;

    private String unionId;

    private String mpOpenId;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String userRole;

    /** 首次登录是否需要重置密码：0=否 1=是 */
    private Integer pwdReset;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
