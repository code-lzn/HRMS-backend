package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 薪资明细（工资条）
 */
@TableName(value = "salary_detail")
@Data
public class SalaryDetail implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 批次 ID → salary_batch.id
     */
    private Long batchId;

    /**
     * 员工 ID → user.id
     */
    private Long employeeId;

    /**
     * 工资项目明细 JSON
     */
    private String salaryItems;

    /**
     * 应发工资
     */
    private BigDecimal grossPay;

    /**
     * 社保个人部分
     */
    private BigDecimal socialSecurity;

    /**
     * 公积金个人部分
     */
    private BigDecimal housingFund;

    /**
     * 个人所得税
     */
    private BigDecimal incomeTax;

    /**
     * 扣款合计
     */
    private BigDecimal totalDeductions;

    /**
     * 实发工资
     */
    private BigDecimal netPay;

    /**
     * 异常标记：0=正常, 1=黄色预警, 2=红色预警, 3=红色阻断
     */
    private Integer isAbnormal;

    /**
     * 异常原因
     */
    private String abnormalReason;

    /**
     * 手动调整金额（正=补发, 负=扣回）
     */
    private BigDecimal manualAdjustment;

    /**
     * 手动调整原因
     */
    private String adjustmentReason;

    /**
     * 工资条查看状态：0=未查看, 1=已验证, 2=已查看
     */
    private Integer payslipViewed;

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
