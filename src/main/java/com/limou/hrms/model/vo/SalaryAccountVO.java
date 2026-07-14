package com.limou.hrms.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 薪资账套视图
 */
@Data
public class SalaryAccountVO {

    private Long id;
    private String name;
    private Integer scopeType;
    private String scopeTypeText;
    private String scopeIds;
    private Date effectiveDate;
    private Date createTime;
    private Date updateTime;
    /** 账套下的工资项目列表 */
    private List<SalaryItemVO> items;
}
