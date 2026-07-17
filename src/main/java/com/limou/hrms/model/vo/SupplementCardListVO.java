package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请列表 VO
 */
@Data
public class SupplementCardListVO implements Serializable {

    private Long id;

    private Long employeeId;

    private String employeeName;

    private String departmentName;

    private LocalDate attendanceDate;

    private Integer cardType;

    private String cardTypeDesc;

    private String reason;

    private Integer status;

    private String statusDesc;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
