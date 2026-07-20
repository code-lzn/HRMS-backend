package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位实体
 */
@TableName("position")
@Data
public class Position implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 职位名称
     */
    private String name;

    /**
     * 职位序列：1=管理序列M 2=专业序列P 3=支持序列S
     */
    private Integer sequence;

    /**
     * 所属部门ID，NULL表示全公司通用
     */
    private Long departmentId;

    /**
     * 职级下限
     */
    private Integer levelMin;

    /**
     * 职级上限
     */
    private Integer levelMax;

    /**
     * 职级前缀（如 P、M）
     */
    private String levelPrefix;

    /**
     * 默认试用期（月）
     */
    private Integer defaultProbationMonths;

    /**
     * 职位描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
