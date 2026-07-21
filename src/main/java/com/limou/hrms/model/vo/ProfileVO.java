package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 个人档案 VO — 聚合展示
 */
@Data
public class ProfileVO implements Serializable {

    private Long employeeId;

    private String employeeNo;

    private String userAvatar;

    private String name;

    private Integer gender;

    private String genderDesc;

    /**
     * 手机号（脱敏后）
     */
    private String phone;

    private String email;

    /**
     * 身份证号（脱敏后）
     */
    private String idCard;

    /**
     * 生日
     */
    private String birthday;

    /**
     * 现居住地址
     */
    private String address;

    /**
     * 紧急联系人姓名
     */
    private String emergencyContact;

    /**
     * 紧急联系人电话（脱敏后）
     */
    private String emergencyPhone;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 职位名称
     */
    private String positionName;

    /**
     * 职级
     */
    private String jobLevel;

    /**
     * 在职状态
     */
    private Integer status;

    private String statusDesc;

    /**
     * 入职日期
     */
    private String hireDate;

    /**
     * 可编辑字段列表
     */
    private List<String> editableFields;

    /**
     * 锁定字段列表
     */
    private List<String> lockedFields;

    private static final long serialVersionUID = 1L;
}
