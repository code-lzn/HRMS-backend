package com.limou.hrms.builder;

import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.enums.ApprovalBizType;
import java.util.List;

/**
 * 审批节点构建器 — 策略接口
 */
public interface ApprovalNodeBuilder {

    /**
     * 构建审批节点链
     * @param bizId       业务主键ID（如 onboarding_application.id）
     * @param applicantId 申请人 employee.id
     * @return 审批节点列表（按 nodeOrder 升序）
     */
    List<ApprovalNode> build(Long bizId, Long applicantId);

    /**
     * @return 该 Builder 支持的业务类型
     */
    ApprovalBizType supportedBizType();
}
