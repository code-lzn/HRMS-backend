package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("leave_request")
@Data
public class LeaveRequest implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    /** 请假类型：1=年假 2=病假 3=事假 4=婚假 5=产假 6=丧假 7=调休 */
    private Integer leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal leaveDays;
    private String reason;
    private Long handoverEmployeeId;
    private String attachmentUrl;
    /** 状态：1=草稿 2=审批中 3=已通过 4=已拒绝 5=已取消 */
    private Integer status;
    private Long approvalInstanceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
