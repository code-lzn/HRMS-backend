package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工列表响应（脱敏后，仅展示默认字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeListVO {

    private Long id;
    private String employeeNo;
    private String name;
    private String departmentName;
    private String positionName;
    private String jobLevel;
    private Integer status;
    private String statusDesc;
    private String hireDate;
}
