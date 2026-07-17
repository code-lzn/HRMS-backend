package com.limou.hrms.model.dto.probation;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新转正草稿请求体
 */
@Data
public class ProbationUpdateDTO {

    private String performanceReview;
    private BigDecimal salaryAdjustment;
    private String remark;
}
