package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 操作日志
 * @TableName oper_log
 */
@TableName(value = "oper_log")
@Data
public class OperLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作模块 */
    private String module;

    /** 操作类型 */
    private String action;

    /** 操作描述 */
    private String description;

    /** 操作时间 */
    private Date operateTime;

    private static final long serialVersionUID = 1L;
}
