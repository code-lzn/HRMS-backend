package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工个人信息实体
 */
@TableName("employee_personal_info")
@Data
public class EmployeePersonalInfo implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID，关联 employee.id
     */
    private Long employeeId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 性别：1=男 2=女
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 身份证号（AES-256 加密存储）
     */
    private String idCard;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 户籍地址
     */
    private String registeredAddress;

    /**
     * 现居住地址
     */
    private String currentAddress;

    /**
     * 紧急联系人姓名
     */
    private String emergencyContactName;

    /**
     * 紧急联系人电话
     */
    private String emergencyContactPhone;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
