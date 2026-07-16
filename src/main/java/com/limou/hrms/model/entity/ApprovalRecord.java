package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批实例
 * @TableName approval_record
 */
@TableName(value = "approval_record")
@Data
public class ApprovalRecord implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 审批流定义ID */
    private Long flowId;

    /** 业务类型     ONBOARDING("ONBOARDING", "入职审批"),
     REGULARIZATION("REGULARIZATION", "转正审批"),
     TRANSFER("TRANSFER", "调岗审批"),
     RESIGNATION("RESIGNATION", "离职审批"),
     LEAVE("LEAVE", "请假审批"),
     PATCH_CLOCK("PATCH_CLOCK", "补卡审批"),
     SALARY_BATCH("SALARY_BATCH", "薪资批次审批");*/
    private String businessType;


    /** 关联业务表记录ID
     * 对，businessId 是一个多态外键。它存的是不同业务表的主键 ID，具体是哪张表由 businessType 决定。
     *
     * businessType = "LEAVE"        → businessId → leave 表的 id
     * businessType = "PATCH_CLOCK"  → businessId → makeup_punch 表的 id
     *
     * 在代码里就是这么用的：
     *
     * // LeaveServiceImpl.apply() — 传入请假记录ID
     * approvalService.startApproval("LEAVE", request.getId(), ...);
     *
     * // MakeupPunchServiceImpl.apply() — 传入补卡记录ID
     * approvalService.startApproval("PATCH_CLOCK", request.getId(), ...);
     *
     * 审批通过后回写业务表时，也是靠这个组合查找：
     *
     * if ("LEAVE".equals(record.getBusinessType())) {
     * Leave leave = leaveMapper.selectById(record.getBusinessId());  // 查 leave 表
     * }
     * */
    private Long businessId;

    /** 申请人ID（employeeId） */
    private Long applicantId;

    /** 申请人姓名（冗余） */
    private String applicantName;

    /** 当前审批步骤 */
    private Integer currentStep;

    /** 总步骤数 */
    private Integer totalSteps;

    /** 审批状态: APPROVING=审批中, APPROVED=已通过, REJECTED=已拒绝, WITHDRAWN=已撤回 */
    private String status;

    /** 目标部门ID（用于部门负责人权限匹配） */
    private Long departmentId;

    /** 审批完成时间 */
    private Date finishedAt;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
