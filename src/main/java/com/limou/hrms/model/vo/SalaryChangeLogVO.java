package com.limou.hrms.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 调薪历史视图
 */
@Data
public class SalaryChangeLogVO {

    /** 主键ID */
    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 变更类型 */
    private Integer changeType;
    /** 变更类型文本 */
    private String changeTypeText;
    /** 变更前值 */
    private String oldValue;
    /** 变更后值 */
    private String newValue;
    /** 生效日期 */
    private Date effectiveDate;
    /** 操作人ID */
    private Long operatorId;
    /** 操作人姓名 */
    private String operatorName;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private Date createTime;
}
