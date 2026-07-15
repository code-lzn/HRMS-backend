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
 * 薪资账套
 */
@TableName(value = "salary_account")
@Data
public class SalaryAccount implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账套名称，如"标准职员工资"
     */
    private String name;

    /**
     * 适用范围类型：1=部门, 2=职位, 3=职级
     */
    private Integer scopeType;

    /**
     * 适用范围 ID 列表，逗号分隔；NULL 表示全员适用
     */
    private String scopeIds;

    /**
     * 生效日期
     */
    private Date effectiveDate;

    /**
     * 逻辑删除：0=正常, 1=删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
