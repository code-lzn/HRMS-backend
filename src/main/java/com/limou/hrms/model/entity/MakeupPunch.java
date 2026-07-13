package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 补卡申请表
 * @TableName makeup_punch_request
 */
@TableName(value ="makeup_punch")
@Data
public class MakeupPunch implements Serializable {

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
     * 补卡日期
     */
    private Date punchDate;

    /**
     * 补卡类型：0=上班补卡 1=下班补卡
     */
    private Integer punchType;

    /**
     * 实际到岗/离岗时间
     */
    private Date punchTime;

    /**
     * 缺卡原因
     */
    private String reason;

    /**
     * 状态：0=待审批 1=已通过 2=已拒绝
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
    private Integer isDeleted;
}
