package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 员工
 * @TableName employee
 */
@TableName(value ="employee")
@Data
public class Employee {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 员工名称（真实姓名）
     */
    private String employeeName;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 工号, 格式: 年份(4)+部门编码(2)+序号(3)
     */
    private String employeeNo;

    /**
     * 状态：0-离职，1-在职，2-试用期
     */
    private Integer status;

    /**
     * 性别: 0=女, 1=男
     */
    private Integer gender;

    /**
     * 身份证号（加密存储）
     */
    private String idCard;

    /**
     * 入职日期
     */
    private Date hireDate;

    /**
     * 入职类型
     */
    private Integer hireType;

    /**
     * 联系人电话
     */
    private String phone;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 职位ID
     */
    private Long positionId;
//    对应的薪资档案id

    private Long salaryProfileId;


    /**
     * 录用类型: FULL_TIME=全职, PART_TIME=兼职, INTERN=实习
     */
    private String employmentType;

    /**
     * 邮箱
     */
    private String email;

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
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDeleted;
}