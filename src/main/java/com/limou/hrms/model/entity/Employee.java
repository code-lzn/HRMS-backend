package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("employee")
@Data
public class Employee implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联user.id */
    private Long userId;
    private String employeeNo;
    /** 在职状态：1=试用期 2=正式 3=待离职 4=已离职 */
    private Integer status;
    private LocalDate hireDate;
    private Integer hireType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
