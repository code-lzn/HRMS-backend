package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.ApprovalRecord;
import com.limou.hrms.model.vo.ApprovalDetailVO;
import com.limou.hrms.model.vo.ApprovalPendingVO;

import java.util.List;

/**
 * 审批中心服务
 */
public interface ApprovalService extends IService<ApprovalRecord> {

    /**
     * 获取我的待审批列表
     */
    List<ApprovalPendingVO> getPendingList(Long employeeId);

    /**
     * 获取审批详情（含节点历史）
     */
    ApprovalDetailVO getApprovalDetail(Long recordId);

    /**
     * 通过审批
     */
    void approve(Long detailId, Long employeeId, String comment);

    /**
     * 拒绝审批
     */
    void reject(Long detailId, Long employeeId, String comment);

    /**
     * 转交审批
     */
    void transfer(Long detailId, Long employeeId, Long targetEmployeeId, String comment);

    /**
     * 启动审批流程（创建审批实例和明细节点）
     */
    ApprovalRecord startApproval(String businessType, Long businessId, Long applicantEmployeeId, String applicantName);
}
