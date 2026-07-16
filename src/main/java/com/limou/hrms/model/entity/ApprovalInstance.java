package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("approval_instance")
@Data
public class ApprovalInstance implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务类型 */
    private String bizType;
    /** 业务主键ID */
    private Long bizId;
    /** 审批标题 */
    private String title;
    /** 审批状态：1=审批中 2=已通过 3=已拒绝 4=已撤回 */
    private Integer status;
    /** 申请人ID，关联employee.id */
    private Long applicantId;
    /** 当前审批节点序号 */
    private Integer currentNodeOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
