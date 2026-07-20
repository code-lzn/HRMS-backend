package com.limou.hrms.service;

import com.limou.hrms.model.dto.approval.DelegateSettingDTO;
import com.limou.hrms.model.entity.ApprovalDelegate;
import com.limou.hrms.model.vo.MyDelegatesVO;

/**
 * 委托审批服务接口。
 * 操作人信息由 Service 内部解析，无需调用方传入。
 */
public interface ApprovalDelegateService {

    /**
     * 设置委托审批
     */
    ApprovalDelegate createDelegate(DelegateSettingDTO dto);

    /**
     * 取消委托
     */
    void cancelDelegate(Long delegateId);

    /**
     * 查询我的委托关系
     */
    MyDelegatesVO getMyDelegates();

    /**
     * 解析实际审批人（有委托返回被委托人ID，无委托返回本人）
     */
    Long resolveApprover(Long originalApproverId);
}
