package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.entity.ApprovalInstance;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.ApprovalInstanceVO;
import com.limou.hrms.model.vo.PendingItemVO;
import com.limou.hrms.model.vo.ProcessedItemVO;

/**
 * 审批流引擎 — 核心服务接口
 */
public interface ApprovalFlowService {

    /**
     * 创建审批实例 + 节点链
     * @return 审批实例
     */
    ApprovalInstance createInstance(ApprovalBizType bizType, Long bizId, Long applicantId);

    /**
     * 审批通过
     * @param nodeId   审批节点ID
     * @param employeeId 当前操作人的 employee.id
     * @param comment  审批意见（可选）
     */
    void approve(Long nodeId, Long employeeId, String comment);

    /**
     * 审批拒绝
     * @param nodeId   审批节点ID
     * @param employeeId 当前操作人的 employee.id
     * @param comment  拒绝理由（必填）
     */
    void reject(Long nodeId, Long employeeId, String comment);

    /**
     * 审批转交
     * @param nodeId       审批节点ID
     * @param fromEmployeeId 当前审批人 employee.id
     * @param toEmployeeId   新审批人 employee.id
     */
    void transfer(Long nodeId, Long fromEmployeeId, Long toEmployeeId);

    /**
     * 撤回申请（仅申请人和第一节点可撤回）
     * @param instanceId 审批实例ID
     * @param operatorId 操作人 employee.id
     */
    void cancel(Long instanceId, Long operatorId);

    /**
     * 获取待办数量（含委托）
     */
    long getPendingCount(Long employeeId);

    /**
     * 查询待办列表
     */
    Page<PendingItemVO> getPendingList(Long employeeId, ApprovalQuery query);

    /**
     * 查询已办列表
     */
    Page<ProcessedItemVO> getProcessedList(Long employeeId, ApprovalQuery query);

    /**
     * 获取审批详情（含节点时间线）
     */
    ApprovalInstanceVO getDetail(Long instanceId);
}
