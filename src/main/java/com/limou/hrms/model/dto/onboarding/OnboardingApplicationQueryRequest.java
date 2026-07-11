package com.limou.hrms.model.dto.onboarding;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 入职申请查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OnboardingApplicationQueryRequest extends PageRequest implements Serializable {

    private Long id;
    private Integer status;
    private String keyword;
    private Long departmentId;
    private String sortField;
    private String sortOrder;

    private static final long serialVersionUID = 1L;
}
