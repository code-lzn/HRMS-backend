package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("probation_application")
@Data
public class ProbationApplication implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private LocalDate probationStartDate;
    private LocalDate probationEndDate;
    private String performanceReview;
    private BigDecimal salaryAdjustment;
    /** 转正结果：1=通过 2=延长试用 3=不通过 */
    private Integer result;
    private LocalDate extendedEndDate;
    /** 状态：1=草稿 2=审批中 3=已完成 4=已拒绝 */
    private Integer status;
    private Long approvalInstanceId;
    private Long applicantId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
