package com.limou.hrms.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ResignationListVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private String departmentName;
    private LocalDate resignationDate;
    private Integer resignationType;
    private String resignationTypeDesc;
    private Integer status;
    private String statusDesc;
    private String applicantName;
    private LocalDateTime createTime;
}
