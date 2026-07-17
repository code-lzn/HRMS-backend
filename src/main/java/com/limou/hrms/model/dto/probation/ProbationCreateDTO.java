package com.limou.hrms.model.dto.probation;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建转正申请请求体
 */
@Data
public class ProbationCreateDTO {

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @NotBlank(message = "试用期表现评价不能为空")
    private String performanceReview;

    /** 转正后薪资调整（可选） */
    private BigDecimal salaryAdjustment;

    /** 备注 */
    private String remark;

    /** 是否直接提交审批，默认false=保存草稿 */
    private Boolean submitDirectly;
}
