package com.limou.hrms.model.dto.leave;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 更新请假草稿请求 — 所有字段可选
 */
@Data
public class LeaveUpdateDTO implements Serializable {

    private Integer leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private Long handoverEmployeeId;
    private String attachmentUrl;

    private static final long serialVersionUID = 1L;
}
