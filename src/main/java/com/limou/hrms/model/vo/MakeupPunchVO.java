package com.limou.hrms.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 补卡记录视图
 */
@Data
public class MakeupPunchVO implements Serializable {

    /** 补卡ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名 */
    private String employeeName;

    /** 补卡日期 */
    private Date punchDate;

    /** 补卡类型：0=上班补卡 1=下班补卡 */
    private Integer punchType;

    /** 补卡类型文本 */
    private String punchTypeText;

    /** 实际时间 */
    private Date punchTime;

    /** 缺卡原因 */
    private String reason;

    /** 状态：0=待审批 1=已通过 2=已拒绝 */
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
