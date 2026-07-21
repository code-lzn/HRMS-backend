package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName(value = "emp_onboarding")
@Data
public class HrOnboarding implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 入职单号，ON+年月日+流水号 */
    private String businessNo;

    /** 关联审批流ID(approval_flow.id) */
    private Long flowId;

    /** 审批实例ID(approval_record.id) */
    private Long recordId;

    /** 入职部门ID(department.id) */
    private Long deptId;

    /** 入职职位ID(position.id) */
    private Long positionId;

    /** 预定入职日期 */
    private Date hireDate;

    /** 试用期月数 */
    private Integer probationMonth;

    /** 录用类型:FULL_TIME全职/PART_TIME兼职 */
    private String employmentType;

    /** 合同类型:1固定期限/2无固定期限/3实习 */
    private Integer contractType;

    /** 合同到期日 */
    private Date contractExpireDate;

    /** 约定基本工资 */
    private BigDecimal baseSalary;

    /** 社保缴纳基数 */
    private BigDecimal socialInsuranceBase;

    /** 公积金缴纳基数 */
    private BigDecimal housingFundBase;

    /** 工资卡账号(加密存储) */
    private String bankAccount;

    /** 开户行名称 */
    private String bankName;

    /** 候选人姓名 */
    private String candidateName;

    /** 候选人性別: MALE/FEMALE */
    private String gender;

    /** 试用期薪资比例(%) */
    private Integer probationSalaryRatio;

    /** 联系手机号 */
    private String phone;

    /** 身份证号(加密存储) */
    private String idCard;

    /** 候选人邮箱 */
    private String email;

    /** 紧急联系人姓名 */
    private String emergencyContactName;

    /** 紧急联系电话 */
    private String emergencyContactPhone;

    /** 生日 */
    private Date birthday;

    /** 户籍地址 */
    private String registeredAddress;

    /** 现居住地址 */
    private String currentAddress;

    /** 工作地点 */
    private String workLocation;

    /** 审批通过后生成员工ID(employee.id) */
    private Long employeeId;

    /** 操作HR员工ID */
    private Long operatorId;

    /** 审批人ID（部门负责人，关联 employee.id） */
    private Long approverId;

    /** 直接汇报人ID */
    private Long directReportId;

    /** 单据备注 */
    private String remark;

    private Date createTime;

    private Date updateTime;

    /** 逻辑删除:0=否,1=是 */
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Date getHireDate() {
        return hireDate;
    }

    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
    }

    public Integer getProbationMonth() {
        return probationMonth;
    }

    public void setProbationMonth(Integer probationMonth) {
        this.probationMonth = probationMonth;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public Integer getContractType() {
        return contractType;
    }

    public void setContractType(Integer contractType) {
        this.contractType = contractType;
    }

    public Date getContractExpireDate() {
        return contractExpireDate;
    }

    public void setContractExpireDate(Date contractExpireDate) {
        this.contractExpireDate = contractExpireDate;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public BigDecimal getSocialInsuranceBase() {
        return socialInsuranceBase;
    }

    public void setSocialInsuranceBase(BigDecimal socialInsuranceBase) {
        this.socialInsuranceBase = socialInsuranceBase;
    }

    public BigDecimal getHousingFundBase() {
        return housingFundBase;
    }

    public void setHousingFundBase(BigDecimal housingFundBase) {
        this.housingFundBase = housingFundBase;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }


}
