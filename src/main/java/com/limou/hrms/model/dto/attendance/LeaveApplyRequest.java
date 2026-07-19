package com.limou.hrms.model.dto.attendance;

import java.io.Serializable;
import lombok.Data;

/**
 * 请假申请请求
 */
@Data
public class LeaveApplyRequest implements Serializable {

    /** 请假类型：0=事假 1=病假 2=年假 3=婚假 4=产假 5=丧假 6=调休 */
    private Integer leaveType;

    /** 开始日期 yyyy-MM-dd */
    private String startDate;

    /** 结束日期 yyyy-MM-dd */
    private String endDate;

    /** 时段：0=全天 1=上午 2=下午 */
    private Integer timeSlot;

    /** 请假原因 */
    private String reason;

    private static final long serialVersionUID = 1L;
}
