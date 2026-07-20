package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("approval_node")
@Data
public class ApprovalNode implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 审批实例ID */
    private Long instanceId;
    /** 节点名称 */
    private String nodeName;
    /** 节点顺序号 */
    private Integer nodeOrder;
    /** 当前审批人ID，关联employee.id */
    private Long approverId;
    /** 原审批人ID（转交场景记录） */
    private Long originalApproverId;
    /** 是否转交（true=转交，false/null=委托或正常） */
    private Boolean transferred;
    /** 节点状态：1=待审批 2=已通过 3=已拒绝 4=已转交 */
    private Integer status;
    /** 审批意见 */
    private String comment;
    /** 操作时间 */
    private LocalDateTime operateTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
