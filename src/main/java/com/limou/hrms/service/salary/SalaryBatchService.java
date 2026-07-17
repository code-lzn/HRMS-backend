package com.limou.hrms.service.salary;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.salary.SalaryBatchCreateRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchQueryRequest;
import com.limou.hrms.model.dto.salary.SalaryDetailAdjustRequest;
import com.limou.hrms.model.dto.salary.SalaryDetailQueryRequest;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.vo.salary.SalaryBatchVO;
import com.limou.hrms.model.vo.salary.SalaryDetailVO;

/**
 * 薪资核算批次服务接口
 */
public interface SalaryBatchService extends IService<SalaryBatch> {

    /**
     * 创建核算批次
     */
    Long createBatch(SalaryBatchCreateRequest request, Long createBy);

    /**
     * 执行异步计算
     */
    void executeCalculate(Long batchId);

    /**
     * 查询批次列表
     */
    Page<SalaryBatchVO> listBatches(SalaryBatchQueryRequest request);

    /**
     * 获取批次详情
     */
    SalaryBatchVO getBatchDetail(Long batchId);

    /**
     * 查询批次下的薪资明细
     */
    Page<SalaryDetailVO> listDetails(Long batchId, SalaryDetailQueryRequest request);

    /**
     * 手动调整某员工的工资条
     */
    void adjustDetail(Long detailId, SalaryDetailAdjustRequest request);

    /**
     * 提交审批（创建审批实例，由 SalaryBatchNodeBuilder 构建节点链）
     */
    void submitForApproval(Long batchId);

    /**
     * 标记已发放
     */
    void markAsPaid(Long batchId);
}
