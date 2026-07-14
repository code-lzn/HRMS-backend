package com.limou.hrms.model.dto.employee;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工新增请求
 */
@Data
public class EmployeeAddRequest implements Serializable {

    /** 姓名 */
    private String employeeName;

    /** 性别：0=女 1=男 */
    private Integer gender;

    /** 手机号（同时作为系统账号） */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 部门ID */
    private Long departmentId;

    /** 职位ID */
    private Long positionId;

    /** 职级 */
    private String jobLevel;

    /** 入职日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date hireDate;

    /** 入职类型 */
    private Integer hireType;

    /** 录用类型：FULL_TIME/PART_TIME/INTERN */
    private String employmentType;

    // ---- 详情表字段 ----

    /** 身份证号 */
    private String idCard;

    /** 生日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;

    /** 户籍地址 */
    private String registeredAddress;

    /** 现居住地址 */
    private String currentAddress;

    /** 直接汇报人ID */
    private Long directReportId;

    /** 工作地点 */
    private String workLocation;

    /** 合同类型：1=固定期限 2=无固定期限 3=劳务合同 */
    private Integer contractType;

    /** 合同到期日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date contractExpireDate;

    /** 试用期待遇比例 */
    private BigDecimal probationRatio;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 银行账号 */
    private String bankAccount;

    /** 开户行 */
    private String bankName;

    /** 紧急联系人姓名 */
    private String emergencyContactName;

    /** 紧急联系人电话 */
    private String emergencyContactPhone;

    private static final long serialVersionUID = 1L;
}
