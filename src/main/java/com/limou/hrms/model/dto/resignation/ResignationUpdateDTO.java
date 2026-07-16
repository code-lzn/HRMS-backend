package com.limou.hrms.model.dto.resignation;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ResignationUpdateDTO {
    private LocalDate resignationDate;
    private Integer resignationType;
    private String reason;
    private Long handoverToId;
    private String remark;
}
