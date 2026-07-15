package com.limou.hrms.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 薪资账套视图
 */
@Data
public class SalaryAccountVO {

    /** 主键ID */
    private Long id;
    /** 账套名称 */
    private String name;
    /** 适用范围类型 */
    private Integer scopeType;
    /** 适用范围文本 */
    private String scopeTypeText;
    /** 适用范围ID列表 */
    private String scopeIds;
    /** 生效日期 */
    private Date effectiveDate;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;
    /** 账套下的工资项目列表 */
    private List<SalaryItemVO> items;
}
