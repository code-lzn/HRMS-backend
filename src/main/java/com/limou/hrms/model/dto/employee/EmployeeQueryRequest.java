package com.limou.hrms.model.dto.employee;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 员工列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeQueryRequest extends PageRequest implements Serializable {

    /**
     * 模糊搜索关键词（姓名/工号/手机号）
     */
    private String keyword;

    /**
     * 部门ID列表（部门树多选）
     */
    private Long[] departmentIds;

    /**
     * 职位ID列表（下拉多选）
     */
    private Long[] positionIds;

    /**
     * 在职状态列表：1=试用期 2=正式 3=待离职 4=已离职
     */
    private Integer[] statuses;

    /**
     * 职级列表，如 P5、M2
     */
    private String[] jobLevels;

    /**
     * 入职日期起始
     */
    private LocalDate hireDateStart;

    /**
     * 入职日期截止
     */
    private LocalDate hireDateEnd;

    private static final long serialVersionUID = 1L;
}
