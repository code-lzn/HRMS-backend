package com.limou.hrms.model.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 请假记录视图
 */
@Data
public class LeaveVO implements Serializable {

    /** 请假ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名 */
    private String employeeName;

    /** 请假类型 */
    private Integer leaveType;

    /** 请假类型文本 */
    private String leaveTypeText;

    /** 开始日期 */
    private Date startDate;

    /** 结束日期 */
    private Date endDate;

    /** 请假天数 */
    private BigDecimal totalDays;

    /** 请假原因 */
    private String reason;

    /** 状态：0=待审批 1=已通过 2=已拒绝 3=已撤销 */
    private Integer status;

    /** 状态文本 */
    private String statusText;

    /** 审批人ID */
    private Long approverId;

    /** 审批时间 */
    private Date approveTime;

    /** 审批意见 */
    private String approveComment;

    /** 创建时间 */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
