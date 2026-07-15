package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工号序列表实体
 * <p>
 * 按 (year, dept_code) 联合唯一键自增，用于生成工号。
 */
@TableName("employee_no_sequence")
@Data
public class EmployeeNoSequence implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 年份，如 2026
     */
    private Integer year;

    /**
     * 部门编码，2位
     */
    private String deptCode;

    /**
     * 当前序号
     */
    private Integer currentSeq;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
