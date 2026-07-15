package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 薪资账套表
 * @TableName sal_account
 */
@TableName(value = "sal_account")
@Data
public class SalAccount implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 账套名称 */
    private String name;

    /** 适用范围类型：1=部门 2=职位 3=职级 */
    @TableField("scope_type")
    private Integer scopeType;

    /** 适用范围ID集合(JSON数组) */
    @TableField("scope_ids")
    private String scopeIds;

    /** 生效日期 */
    @TableField("effective_date")
    private Date effectiveDate;

    /** 逻辑删除：0=否 1=是 */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

    /** 更新时间 */
    @TableField("update_time")
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
