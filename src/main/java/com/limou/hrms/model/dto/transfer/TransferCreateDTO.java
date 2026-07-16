package com.limou.hrms.model.dto.transfer;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建调岗申请请求体
 */
@Data
public class TransferCreateDTO {

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @NotNull(message = "新部门不能为空")
    private Long toDepartmentId;

    /** 新职位（可选） */
    private Long toPositionId;

    /** 新职级（可选） */
    private String toJobLevel;

    /** 新汇报人（可选） */
    private Long toDirectReportId;

    /** 薪资调整（可选） */
    private BigDecimal salaryAdjustment;

    @NotBlank(message = "调岗原因不能为空")
    private String reason;

    /** 是否直接提交审批 */
    private Boolean submitDirectly;
}
