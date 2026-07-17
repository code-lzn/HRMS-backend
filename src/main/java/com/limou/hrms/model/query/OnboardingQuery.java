package com.limou.hrms.model.query;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 入职申请分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnboardingQuery extends PageRequest {

    /** 状态筛选 */
    private Integer status;

    /** 部门ID筛选 */
    private Long departmentId;

    /** 关键词模糊搜索（姓名/手机号） */
    private String keyword;

    /** 预计入职日期起始 */
    private LocalDate hireDateStart;

    /** 预计入职日期截止 */
    private LocalDate hireDateEnd;
}
