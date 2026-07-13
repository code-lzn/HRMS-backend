package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.ApprovalDelegation;
import com.limou.hrms.model.vo.ApprovalDelegationVO;

import java.util.Date;
import java.util.List;

public interface ApprovalDelegationService extends IService<ApprovalDelegation> {

    /**
     * 创建委托
     */
    ApprovalDelegation createDelegation(Long delegatorEmployeeId, String delegatorName,
                                         Long delegateEmployeeId, String delegateName,
                                         String businessTypes, Date startDate, Date endDate);

    /**
     * 取消委托
     */
    void cancelDelegation(Long delegationId, Long delegatorEmployeeId);

    /**
     * 获取我的委托列表
     */
    List<ApprovalDelegationVO> getMyDelegations(Long employeeId);

    /**
     * 检查是否有有效的委托关系（被委托人替委托人审批）
     */
    boolean isActiveDelegate(Long delegatorId, Long delegateId, String businessType);
}
