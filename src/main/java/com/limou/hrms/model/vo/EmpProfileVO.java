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

    private Long id;
    private String employeeName;
    private String employeeNo;
    private Integer status;
    private Integer gender;
    private String phone;
    private String email;
    private String idCard;
    private Date hireDate;
    private Integer hireType;
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
    private String emergencyContactName;
    private String emergencyContactPhone;
    private Date createTime;
    private Date updateTime;

    /**
     * 可编辑的字段名集合（不在这个集合里的字段均为锁定状态，前端提示"如需修改请联系 HR"）
     */
    private Set<String> editableFields;

    private static final long serialVersionUID = 1L;
}
