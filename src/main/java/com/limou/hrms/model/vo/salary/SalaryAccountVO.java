package com.limou.hrms.model.vo.salary;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 薪资账套详情 VO
 */
@Data
public class SalaryAccountVO implements Serializable {

    private Long id;

    private String name;

    private Integer scopeType;

    private String scopeTypeLabel;

    private String scopeIds;

    private Date effectiveDate;

    private Date createTime;

    private Date updateTime;

    /**
     * 工资项目列表
     */
    private List<SalaryItemVO> items;

    private static final long serialVersionUID = 1L;
}
