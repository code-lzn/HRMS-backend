package com.limou.hrms.service.salary;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.salary.SalaryAdjustRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchCreateRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchQueryRequest;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.vo.salary.AnomalyVO;
import com.limou.hrms.model.vo.salary.SalaryBatchPreviewVO;
import com.limou.hrms.model.vo.salary.SalaryBatchVO;
import com.limou.hrms.model.vo.salary.SalaryDetailVO;
import java.util.List;

/**
 * 薪资核算批次服务接口
 */
public interface SalaryBatchService extends IService<SalaryBatch> {

    /**
     * 新建核算批次
     */
    Long createBatch(SalaryBatchCreateRequest request, Long createBy);

    /**
     * 执行计算（异步）
     */
    void executeCalculate(Long batchId);

    /**
     * 预览核算结果（分页）
     */
    SalaryBatchPreviewVO preview(Long batchId, int page, int size);

    /**
     * 查看异常项
     */
    List<AnomalyVO> getAnomalies(Long batchId);

    /**
     * 手动调整
     */
    void adjust(SalaryAdjustRequest request);

    /**
     * 提交审批
     */
    void submitForApproval(Long batchId);

    /**
     * 审批通过
     */
    void approve(Long batchId);

    /**
     * 驳回
     */
    void reject(Long batchId, String reason);

    /**
     * 标记已发放
     */
    void markPaid(Long batchId);

    /**
     * 分页查询批次列表
     */
    Page<SalaryBatchVO> listBatches(SalaryBatchQueryRequest request);
}
