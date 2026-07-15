package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工工作信息实体
 */
@TableName("employee_work_info")
@Data
public class EmployeeWorkInfo implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 职位ID
     */
    private Long positionId;

    /**
     * 职级（如 P5）
     */
    private String jobLevel;

    /**
     * 直接汇报人ID
     */
    private Long directReportId;

    /**
     * 工作地点
     */
    private String workLocation;

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
