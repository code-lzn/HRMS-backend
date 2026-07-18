package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工主表（字段严格匹配数据库已有列）
 */
@TableName(value = "employee")
@Data
public class Employee implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工姓名 */
    private String employeeName;

    /** 工号，格式: 年份(4)+部门编码(2)+序号(3) */
    private String employeeNo;

    /** 系统账号（=手机号） */
    @TableField("account")
    private String account;

    /** 关联用户ID */
    private Long userId;

    /** 在职状态：1=试用期 2=正式 3=待离职 4=已离职 */
    private Integer status;

    /** 性别: 0=女 1=男 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 部门ID */
    private Long departmentId;

    /** 职位ID */
    private Long positionId;

    /** 职级（DB暂无，后续扩展） */
    @TableField(exist = false)
    private String jobLevel;

    /** 入职日期 */
    private Date hireDate;


    /** 头像URL */
    private String avatar;

    /** 录用类型 */
    private String employmentType;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
