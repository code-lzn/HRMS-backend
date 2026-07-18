package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 请假申请表
 */
@TableName(value ="leave_record")
@Data
public class Leave implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 请假类型：0=事假 1=病假 2=年假 3=婚假 4=产假 5=丧假 6=调休
     */
    private Integer leaveType;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 结束日期
     */
    private Date endDate;

    /**
     * 请假总天数
     */
    private BigDecimal totalDays;

    /**
     * 请假原因
     */
    private String reason;

    /**
     * 状态：0=待审批 1=已通过 2=已拒绝 3=已撤销
     */
    private Integer status;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批时间
     */
    private Date approveTime;

    /**
     * 审批意见
     */
    private String approveComment;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除：0=否 1=是
     */
    @TableLogic
    private Integer isDeleted;
}
