package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.entity.EmpSalaryProfile;
import com.limou.hrms.model.vo.*;

import java.util.List;

/**
 * 薪资核心业务编排服务（核算计算、批次管理、审批对接）
 */
public interface SalaryBizService {

    // ========== 员工薪资档案 ==========

    /**
     * 查询员工薪资档案
     */
    EmployeeSalaryVO getEmployeeSalary(Long employeeId);

    /**
     * 更新员工薪资档案（自动记录调薪历史）
     */
    void updateEmployeeSalary(Long employeeId, EmpSalaryProfile profile, Long operatorId);

    /**
     * 查询员工调薪历史
     */
    List<SalaryChangeLogVO> getEmployeeSalaryHistory(Long employeeId);

    // ========== 核算批次管理 ==========

    /**
     * 新建核算批次（草稿状态）
     */
    SalaryBatchVO createBatch(String salaryMonth, Long operatorId);

    /**
     * 执行薪资计算（异步，状态→计算中→待确认）
     */
    void calculateBatch(Long batchId);

    /**
     * 预览核算结果（分页）
     */
    SalaryBatchPreviewVO previewBatch(Long batchId, long current, long size);

    /**
     * 查看异常项
     */
    List<SalaryDetailVO> getAnomalies(Long batchId);

    /**
     * 导出批次核算明细为 Excel
     */
    List<SalaryDetailExcelVO> exportBatch(Long batchId);

    /**
     * 手动调整单条明细
     */
    void adjustDetail(Long batchId, Long employeeId, java.math.BigDecimal manualAdjust,
                      String adjustReason, Long operatorId);

    /**
     * 提交审批
     */
    void submitForApproval(Long batchId, Long operatorId);

    // ========== 审批操作 ==========

    /**
     * 审批通过
     */
    void approveBatch(Long batchId, Long operatorId);

    /**
     * 审批驳回
     */
    void rejectBatch(Long batchId, String reason, Long operatorId);

    /**
     * 标记已发放
     */
    void markPaid(Long batchId, Long operatorId);

    // ========== 查询 ==========

    /**
     * 批次列表
     */
    List<SalaryBatchVO> listBatches();

    /**
     * 批次详情
     */
    SalaryBatchVO getBatchDetail(Long batchId);
}
