package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import java.io.Serializable;

@Data
public class HRAttendanceQueryRequest implements Serializable {

    private String employeeName;
    private String employeeNo;
    private Long departmentId;
    private String month;
    private Integer status;
    private Integer punchType;
    private Long positionId;
    private Integer teamId;
    private String punchLocation;
    private Integer pageNum;
    private Integer pageSize;
    private String sortField;
    private String sortOrder;

    private static final long serialVersionUID = 1L;
}