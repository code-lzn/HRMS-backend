package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工资条查看记录表
 * @TableName sal_payslip_view_log
 */
@TableName(value = "sal_payslip_view_log")
@Data
public class SalPayslipViewLog implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 薪资明细ID */
    @TableField("batch_detail_id")
    private Long batchDetailId;

    /** 查看员工ID */
    @TableField("employee_id")
    private Long employeeId;

    /** 二次验证码（短信验证） */
    @TableField("verify_code")
    private String verifyCode;

    /** 验证通过时间 */
    @TableField("verified_at")
    private Date verifiedAt;

    /** 查看时间 */
    @TableField("viewed_at")
    private Date viewedAt;

    /** 查看IP */
    @TableField("view_ip")
    private String viewIp;

    private static final long serialVersionUID = 1L;
}
