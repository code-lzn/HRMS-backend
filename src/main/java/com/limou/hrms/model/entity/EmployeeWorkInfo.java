package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("employee_work_info")
@Data
public class EmployeeWorkInfo implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    /** 所属部门ID */
    private Long departmentId;
    private Long positionId;
    private String jobLevel;
    /** 直接汇报人ID，关联employee.id */
    private Long directReportId;
    private String workLocation;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
