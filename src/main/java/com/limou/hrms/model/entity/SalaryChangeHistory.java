package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import lombok.Data;

/**
 * 调薪历史
 */
@TableName(value = "salary_change_history")
@Data
public class SalaryChangeHistory implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 员工 ID → user.id
     */
    private Long employeeId;

    /**
     * 变更类型：1=调薪, 2=账套变更, 3=基数调整, 4=转正调薪, 5=调岗调薪
     */
    private Integer changeType;

    /**
     * 变更前 JSON 快照
     */
    private String oldValue;

    /**
     * 变更后 JSON 快照
     */
    private String newValue;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 操作人 ID（HR）→ user.id
     */
    private Long operatorId;

    /**
     * 备注
     */
    private String remark;

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
