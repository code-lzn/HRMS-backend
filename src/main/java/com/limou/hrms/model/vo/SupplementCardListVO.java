package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请列表 VO
 */
@Data
public class SupplementCardListVO implements Serializable {

    /** 申请ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名 */
    private String employeeName;

    /** 部门名称 */
    private String departmentName;

    /** 补卡日期 */
    private LocalDate attendanceDate;

    /** 补卡类型：1=上班卡 2=下班卡 */
    private Integer cardType;

    /** 补卡类型描述：上班卡 / 下班卡 */
    private String cardTypeDesc;

    /** 补卡事由 */
    private String reason;

    /** 状态：1=草稿 2=审批中 3=已通过 4=已拒绝 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 申请时间 */
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}