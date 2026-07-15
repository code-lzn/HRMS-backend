package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工详情表（补充信息，与 employee 一对一）
 */
@TableName(value = "employee_detail")
@Data
public class EmployeeDetail implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工ID，关联employee.id */
    private Long employeeId;

    /** 系统账号（=手机号） */
    private String account;

    /** 身份证号（加密存储） */
    private String idCard;

    /** 生日 */
    private Date birthday;

    /** 户籍地址 */
    private String registeredAddress;

    /** 现居住地址 */
    private String currentAddress;

    /** 职级，如P5、M2 */
    private String jobLevel;

    /** 直接汇报人ID */
    private Long directReportId;

    /** 工作地点 */
    private String workLocation;

    /** 合同类型：1=固定期限 2=无固定期限 3=劳务合同 */
    private Integer contractType;

    /** 合同到期日 */
    private Date contractExpireDate;

    /** 试用期待遇比例 0.8000~1.0000 */
    private BigDecimal probationRatio;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 银行账号（加密存储） */
    private String bankAccount;

    /** 开户行 */
    private String bankName;

    /** 紧急联系人姓名 */
    private String emergencyContactName;

    /** 紧急联系人电话 */
    private String emergencyContactPhone;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
