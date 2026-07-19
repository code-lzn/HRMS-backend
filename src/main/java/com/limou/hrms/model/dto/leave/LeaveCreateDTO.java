package com.limou.hrms.model.dto.leave;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 创建请假草稿请求
 */
@Data
public class LeaveCreateDTO implements Serializable {

    @NotNull(message = "请假类型不能为空")
    private Integer leaveType;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private String reason;

    /** 工作交接人ID */
    private Long handoverEmployeeId;

    /** 附件URL */
    private String attachmentUrl;

    /** 是否直接提交审批 */
    private Boolean submitDirectly;

    private static final long serialVersionUID = 1L;
}
