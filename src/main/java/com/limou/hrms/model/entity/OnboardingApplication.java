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

@TableName("onboarding_application")
@Data
public class OnboardingApplication implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String idCard;
    private LocalDate expectedHireDate;
    private Long departmentId;
    private Long positionId;
    private Integer hireType;
    private Integer defaultProbationMonths;
    private BigDecimal probationRatio;
    private Long directReportId;
    /** 状态：1=草稿 2=审批中 3=已批准待入职 4=已入职 5=已拒绝 */
    private Integer status;
    private Long approvalInstanceId;
    private Long employeeId;
    private Long applicantId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
