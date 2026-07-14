package com.limou.hrms.service;

import com.limou.hrms.model.entity.ApprovalDelegate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 委托审批服务接口
 */
public interface ApprovalDelegateService {

    /**
     * 设置委托
     * @param delegatorId 委托人 employee.id
     * @param delegateId  被委托人 employee.id
     * @param startTime   委托开始时间
     * @param endTime     委托结束时间
     * @return 委托记录
     */
    ApprovalDelegate createDelegate(Long delegatorId, Long delegateId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 取消委托
     * @param delegateId  委托记录ID
     * @param delegatorId 委托人 employee.id（校验用）
     */
    void cancelDelegate(Long delegateId, Long delegatorId);

    /**
     * 查询我的委托关系
     * @param userId 当前用户 employee.id
     * @return asDelegator（我委托别人） + asDelegate（别人委托我）
     */
    Map<String, List<ApprovalDelegate>> getMyDelegates(Long userId);

    /**
     * 解析实际审批人：若有委托则返回被委托人ID，否则返回本人
     * @param originalApproverId 原始审批人 employee.id
     * @return 实际执行审批的人 employee.id
     */
    Long resolveApprover(Long originalApproverId);
}
