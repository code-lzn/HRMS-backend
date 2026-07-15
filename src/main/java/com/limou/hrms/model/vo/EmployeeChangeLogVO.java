package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工变更日志 VO
 */
@Data
public class EmployeeChangeLogVO implements Serializable {

    /** 主键ID */
    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 变更字段名 */
    private String fieldName;
    /** 变更字段描述 */
    private String fieldDesc;
    /** 变更前值 */
    private String oldValue;
    /** 变更后值 */
    private String newValue;
    /** 变更类型 */
    private String changeType;
    /** 变更类型描述 */
    private String changeTypeDesc;
    /** 操作人姓名 */
    private String operatorName;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
