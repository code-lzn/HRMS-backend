package com.limou.hrms.model.dto.leave;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提交请假申请 DTO
 */
@Data
public class LeaveRequestSubmitDTO implements Serializable {

    /**
     * 请假类型：1=年假 2=病假 3=事假 4=婚假 5=产假 6=丧假 7=调休
     */
    @NotNull(message = "请假类型不能为空")
    private Integer leaveType;

    /**
     * 开始时间（日期+时段）
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间（日期+时段）
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 请假事由
     */
    @NotBlank(message = "请假事由不能为空")
    private String reason;

    /**
     * 工作交接人ID
     */
    private Long handoverEmployeeId;

    /**
     * 附件URL（病假>1天/婚假/产假必传）
     */
    private String attachmentUrl;

    private static final long serialVersionUID = 1L;
}
