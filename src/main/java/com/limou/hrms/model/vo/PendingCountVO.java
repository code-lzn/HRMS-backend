package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 待办数量 VO — 用于顶部导航红点角标
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingCountVO implements Serializable {

    /**
     * 请假审批结果通知数（审批通过/拒绝的通知）
     */
    private Integer leaveApprovalResult;

    /**
     * 新工资条可查看数
     */
    private Integer newSalaryAvailable;

    /**
     * 总待办数
     */
    private Integer total;

    private static final long serialVersionUID = 1L;
}
