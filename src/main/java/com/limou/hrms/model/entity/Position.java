package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 职位表
 * @TableName position
 */
@TableName(value ="position")
@Data
public class Position implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 职位名称，如Java开发工程师
     */
    private String name;

    /**
     * 职位序列：1=M管理 2=P专业 3=S支持
     */
    private Integer sequence;

    /**
     * 所属部门ID，空表示全公司通用
     */
    private Long departmentId;

    /**
     * 职级下限，如P1
     */
    private String levelMin;

    /**
     * 职级上限，如P10
     */
    private String levelMax;

    /**
     * 默认试用期月数
     */
    private Integer defaultProbationMonths;

    /**
     * 职位描述/岗位职责
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /** 逻辑删除：0=否 1=是 */
    @TableLogic
    private Integer isDeleted;
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}