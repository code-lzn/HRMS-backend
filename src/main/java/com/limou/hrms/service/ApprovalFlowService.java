package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.entity.ApprovalInstance;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.ApprovalInstanceVO;
import com.limou.hrms.model.vo.PendingItemVO;
import com.limou.hrms.model.vo.ProcessedItemVO;

/**
 * 审批流引擎 — 核心服务接口。
 * 操作人信息由 Service 内部从当前请求解析，无需调用方传入。
 */
public interface ApprovalFlowService {

    /**
     * 创建审批实例 + 节点链
     */
    ApprovalInstance createInstance(ApprovalBizType bizType, Long bizId, Long applicantId);

    /**
     * 审批通过
     */
    void approve(Long nodeId, String comment);

    /**
     * 审批拒绝
     */
    void reject(Long nodeId, String comment);

    /**
     * 审批转交
     */
    void transfer(Long nodeId, Long toEmployeeId);

    /**
     * 撤回申请
     */
    void cancel(Long instanceId);

    /**
     * 获取待办数量（含委托）
     */
    long getPendingCount();

    /**
     * 查询待办列表。角色路由由 Service 内部完成。
     */
    Page<PendingItemVO> getPendingList(ApprovalQuery query);

    /**
     * 查询已办列表。角色路由由 Service 内部完成。
     */
    Page<ProcessedItemVO> getProcessedList(ApprovalQuery query);

    /**
     * 获取审批详情（含节点时间线）
     */
    ApprovalInstanceVO getDetail(Long instanceId);
}
