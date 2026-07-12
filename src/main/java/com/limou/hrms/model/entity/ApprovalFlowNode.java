package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批节点定义
 * @TableName approval_flow_node
 */
@TableName(value = "approval_flow_node")
@Data
public class ApprovalFlowNode implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 审批流ID */
    private Long flowId;

    /** 节点名称 */
    private String nodeName;

    /** 节点顺序 */
    private Integer nodeOrder;

    /** 审批人类型: DEPT_MANAGER/HR_MANAGER/DIRECT_SUPERIOR/FINANCE/BOSS/SPECIFIED */
    private String approverType;

    /** 指定审批人ID */
    private Long approverId;

    /** 是否可选: 0=必选, 1=可选 */
    private Integer isOptional;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
