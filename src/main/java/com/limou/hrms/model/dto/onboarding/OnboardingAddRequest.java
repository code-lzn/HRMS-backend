package com.limou.hrms.model.dto.onboarding;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class OnboardingAddRequest implements Serializable {

    /** 预定入职日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date hireDate;

    /** 合同到期日 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date contractExpireDate;

    /** 入职部门ID */
    private Long deptId;

    /** 入职职位ID */
    private Long positionId;

    /** 审批流ID */
    private Long flowId;

    /** 试用期月数 */
    private Integer probationMonth;

    /** 录用类型:FULL_TIME/PART_TIME */
    private String employmentType;

    /** 合同类型:1固定期限/2无固定期限/3实习 */
    private Integer contractType;

    /** 约定基本工资 */
    private BigDecimal baseSalary;

    /** 社保缴纳基数 */
    private BigDecimal socialInsuranceBase;

    /** 公积金缴纳基数 */
    private BigDecimal housingFundBase;

    /** 工资卡账号 */
    private String bankAccount;

    /** 开户行名称 */
    private String bankName;

    /** 候选人姓名 */
    private String candidateName;

    /** 直接汇报人ID */
    private Long directReportId;

    /** 联系手机号 */
    private String phone;

    /** 身份证号 */
    private String idCard;

    /** 候选人邮箱 */
    private String email;

    /** 紧急联系人姓名 */
    private String emergencyContactName;

    /** 紧急联系电话 */
    private String emergencyContactPhone;

    /** 单据备注 */
    private String remark;

    private static final long serialVersionUID = 1L;
}
