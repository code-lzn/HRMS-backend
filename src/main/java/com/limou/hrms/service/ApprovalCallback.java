package com.limou.hrms.service;

import com.limou.hrms.model.enums.ApprovalBizType;

/**
 * 审批回调接口 — 各业务模块实现此接口接收审批结果
 */
public interface ApprovalCallback {

    /**
     * 审批全部通过后的业务处理
     */
    void onApproved(ApprovalBizType bizType, Long bizId);

    /**
     * 审批被拒绝后的业务处理
     */
    void onRejected(ApprovalBizType bizType, Long bizId);
}
