package com.limou.hrms.model.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

import com.limou.hrms.model.entity.EmpSalaryProfile;
import lombok.Data;

/**
 * 员工档案视图（个人中心 - 我的档案）
 */
@Data
public class EmpProfileVO implements Serializable {

    /** 主键ID */
    private Long id;
    /** 员工姓名 */
    private String employeeName;
    /** 工号 */
    private String employeeNo;
    /** 在职状态 */
    private Integer status;
    /** 性别 */
    private Integer gender;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 身份证号 */
    private String idCard;
    /** 入职日期 */
    private Date hireDate;
    /** 录用类型 */
    private Integer hireType;
    /** 录用类型代码 */
    private String employmentType;
    /**
     * 部门名称
     */
    private String departmentName;
    /**
     * 职位名称
     */
    private String positionName;
   /**
     * 薪资
     */
    private BigDecimal baseSalary;
    /**
     * 当前地址
     */
    private String currentAddress;
    /** 紧急联系人姓名 */
    private String emergencyContactName;
    /** 紧急联系人电话 */
    private String emergencyContactPhone;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

    /**
     * 可编辑的字段名集合（不在这个集合里的字段均为锁定状态，前端提示"如需修改请联系 HR"）
     */
    private Set<String> editableFields;

    private static final long serialVersionUID = 1L;
}
