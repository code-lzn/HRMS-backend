package com.limou.hrms.model.vo.salary;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 工资项目 VO
 */
@Data
public class SalaryItemVO implements Serializable {

    private Long id;

    private Long accountId;

    private String name;

    private Integer itemType;

    private String itemTypeLabel;

    private String formula;

    private Integer sortOrder;

    private Integer isTaxable;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
