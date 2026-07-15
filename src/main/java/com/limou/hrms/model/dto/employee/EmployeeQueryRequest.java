package com.limou.hrms.model.dto.employee;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 员工列表查询请求
 */
@Data
public class EmployeeQueryRequest implements Serializable {

    /** 关键词（姓名/工号/手机号模糊） */
    private String keyword;

    /** 部门ID列表 */
    private List<Long> departmentIds;

    /** 职位ID列表 */
    private List<Long> positionIds;

    /** 状态列表 */
    private List<Integer> statuses;

    /** 职级列表 */
    private List<String> jobLevels;

    /** 入职日期起始 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date hireDateStart;

    /** 入职日期截止 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date hireDateEnd;

    /** 页码 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 20;

    private static final long serialVersionUID = 1L;
}
