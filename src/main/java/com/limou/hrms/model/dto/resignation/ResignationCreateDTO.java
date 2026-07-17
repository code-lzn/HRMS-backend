package com.limou.hrms.model.dto.resignation;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class ResignationCreateDTO {
    @NotNull private Long employeeId;
    @NotNull private LocalDate resignationDate;
    @NotNull private Integer resignationType;
    @NotBlank private String reason;
    @NotNull private Long handoverToId;
    private String remark;
    private Boolean submitDirectly;
}
