package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("transfer_application")
@Data
public class TransferApplication implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private Long fromDepartmentId;
    private Long toDepartmentId;
    private Long fromPositionId;
    private Long toPositionId;
    private String fromJobLevel;
    private String toJobLevel;
    private Long fromDirectReportId;
    private Long toDirectReportId;
    private BigDecimal salaryAdjustment;
    private String reason;
    /** 状态：1=草稿 2=审批中 3=已生效 4=已拒绝 */
    private Integer status;
    private Long approvalInstanceId;
    private Long applicantId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
