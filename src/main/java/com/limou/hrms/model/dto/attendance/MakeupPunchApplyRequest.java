package com.limou.hrms.model.dto.attendance;

import java.io.Serializable;
import lombok.Data;

/**
 * 补卡申请请求
 */
@Data
public class MakeupPunchApplyRequest implements Serializable {

    /** 补卡日期 yyyy-MM-dd */
    private String punchDate;

    /** 补卡类型：0=上班补卡 1=下班补卡 */
    private Integer punchType;

    /** 实际到岗/离岗时间 yyyy-MM-dd HH:mm:ss */
    private String punchTime;

    /** 缺卡原因 */
    private String reason;

    private static final long serialVersionUID = 1L;
}
