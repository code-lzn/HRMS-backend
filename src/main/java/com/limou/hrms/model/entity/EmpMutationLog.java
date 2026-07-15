package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "emp_mutation_log")
@Data
public class EmpMutationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessType;

    private Long businessId;

    private String businessNo;

    private Long employeeId;

    private String employeeName;

    private Long deptId;

    private String deptName;

    private Date effectDate;

    private String approvalStatus;

    private Long operatorId;

    private String operatorName;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
