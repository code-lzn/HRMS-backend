package com.limou.hrms.service.salary.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.EmployeeSalaryMapper;
import com.limou.hrms.mapper.IncomeTaxCumulativeMapper;
import com.limou.hrms.mapper.SalaryBatchMapper;
import com.limou.hrms.mapper.SalaryDetailMapper;
import com.limou.hrms.mapper.SalaryItemMapper;
import com.limou.hrms.model.dto.salary.SalaryBatchCreateRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchQueryRequest;
import com.limou.hrms.model.dto.salary.SalaryDetailAdjustRequest;
import com.limou.hrms.model.dto.salary.SalaryDetailQueryRequest;
import com.limou.hrms.model.entity.EmployeeSalary;
import com.limou.hrms.model.entity.IncomeTaxCumulative;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.entity.SalaryDetail;
import com.limou.hrms.model.entity.SalaryItem;
import com.limou.hrms.model.enums.AbnormalLevelEnum;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.BatchStatusEnum;
import com.limou.hrms.model.vo.salary.SalaryBatchVO;
import com.limou.hrms.model.vo.salary.SalaryDetailVO;
import com.limou.hrms.model.vo.salary.SalaryItemDetailVO;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.salary.SalaryBatchService;
import com.limou.hrms.service.salary.calculator.SalaryCalculationContext;
import com.limou.hrms.service.salary.calculator.SalaryCalculatorEngine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 薪资核算批次服务实现
 */
@Service
@Slf4j
public class SalaryBatchServiceImpl extends ServiceImpl<SalaryBatchMapper, SalaryBatch>
        implements SalaryBatchService, ApprovalCallback {

    @Resource
    private SalaryDetailMapper salaryDetailMapper;
    @Resource
    private EmployeeSalaryMapper employeeSalaryMapper;
    @Resource
    private SalaryItemMapper salaryItemMapper;
    @Resource
    private IncomeTaxCumulativeMapper incomeTaxCumulativeMapper;
    @Resource
    private SalaryCalculatorEngine calculatorEngine;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private DataScopeContext dataScopeContext;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBatch(SalaryBatchCreateRequest request, Long createBy) {
        if (request == null || StringUtils.isBlank(request.getSalaryMonth())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "薪资月份不能为空");
        }
        // 生成批次号
        String batchNo = "SAL" + request.getSalaryMonth().replace("-", "");
        // 检查批次号唯一性
        QueryWrapper<SalaryBatch> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_no", batchNo);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该月份批次已存在");
        }
        SalaryBatch batch = new SalaryBatch();
        batch.setBatchNo(batchNo);
        batch.setSalaryMonth(request.getSalaryMonth());
        batch.setStatus(BatchStatusEnum.DRAFT.getValue());
        batch.setCreateBy(createBy);
        batch.setTotalEmployees(0);
        batch.setTotalGrossPay(BigDecimal.ZERO);
        batch.setTotalNetPay(BigDecimal.ZERO);
        batch.setTotalTax(BigDecimal.ZERO);
        this.save(batch);
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Async("salaryTaskExecutor")
    public void executeCalculate(Long batchId) {
        log.info("开始异步薪资核算，批次ID：{}", batchId);
        SalaryBatch batch = this.getById(batchId);
        if (batch == null) {
            log.error("批次不存在，ID：{}", batchId);
            return;
        }
        if (batch.getStatus() != BatchStatusEnum.DRAFT.getValue()) {
            log.error("批次状态非草稿，无法计算，ID：{}，状态：{}", batchId, batch.getStatus());
            return;
        }
        // 更新状态为计算中
        batch.setStatus(BatchStatusEnum.CALCULATING.getValue());
        this.updateById(batch);

        try {
            // 拉取所有在职员工薪资档案
            QueryWrapper<EmployeeSalary> esWrapper = new QueryWrapper<>();
            esWrapper.eq("is_deleted", 0).orderByDesc("effective_date");
            List<EmployeeSalary> salaryList = employeeSalaryMapper.selectList(esWrapper);

            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;
            BigDecimal totalTax = BigDecimal.ZERO;
            int employeeCount = 0;

            // 遍历每个员工
            for (EmployeeSalary employeeSalary : salaryList) {
                try {
                    // 获取该员工的薪资项目
                    List<SalaryItem> items = salaryItemMapper.selectList(
                            new QueryWrapper<SalaryItem>().eq("account_id", employeeSalary.getAccountId())
                                    .orderByAsc("sort_order"));

                    // 构建计算上下文
                    SalaryCalculationContext context = new SalaryCalculationContext();
                    context.setEmployeeId(employeeSalary.getEmployeeId());
                    context.setEmployeeSalary(employeeSalary);
                    context.setSalaryItems(items);
                    context.setSalaryMonth(batch.getSalaryMonth());

                    // 调用计算引擎逐项计算
                    BigDecimal grossPay = BigDecimal.ZERO;
                    BigDecimal socialSecurity = BigDecimal.ZERO;
                    BigDecimal housingFund = BigDecimal.ZERO;
                    BigDecimal incomeTax = BigDecimal.ZERO;
                    BigDecimal totalDeductions = BigDecimal.ZERO;
                    List<SalaryItemDetailVO> itemDetails = new ArrayList<>();

                    for (SalaryItem item : items) {
                        BigDecimal amount = calculatorEngine.calculate(item.getItemType(), context);
                        if (amount == null) {
                            amount = BigDecimal.ZERO;
                        }
                        SalaryItemDetailVO detail = new SalaryItemDetailVO();
                        detail.setName(item.getName());
                        detail.setAmount(amount);
                        detail.setType(item.getItemType());
                        itemDetails.add(detail);

                        // 汇总
                        if (item.getItemType() <= 2) {
                            grossPay = grossPay.add(amount);
                        } else {
                            totalDeductions = totalDeductions.add(amount.abs());
                        }
                        if (item.getItemType() == 4) {
                            socialSecurity = socialSecurity.add(amount.abs());
                        }
                        if (item.getItemType() == 5) {
                            housingFund = housingFund.add(amount.abs());
                        }
                        if (item.getItemType() == 6) {
                            incomeTax = incomeTax.add(amount.abs());
                        }
                    }

                    BigDecimal netPay = grossPay.subtract(totalDeductions);

                    // 异常检测
                    int isAbnormal = AbnormalLevelEnum.NORMAL.getValue();
                    String abnormalReason = null;
                    if (grossPay.compareTo(BigDecimal.ZERO) == 0) {
                        isAbnormal = AbnormalLevelEnum.BLOCKED.getValue();
                        abnormalReason = "未设置薪资档案或无薪资项目";
                    }

                    // 写入 salary_detail
                    SalaryDetail detail = new SalaryDetail();
                    detail.setBatchId(batchId);
                    detail.setEmployeeId(employeeSalary.getEmployeeId());
                    detail.setSalaryItems(JSONUtil.toJsonStr(itemDetails));
                    detail.setGrossPay(grossPay);
                    detail.setSocialSecurity(socialSecurity);
                    detail.setHousingFund(housingFund);
                    detail.setIncomeTax(incomeTax);
                    detail.setTotalDeductions(totalDeductions);
                    detail.setNetPay(netPay);
                    detail.setIsAbnormal(isAbnormal);
                    detail.setAbnormalReason(abnormalReason);
                    detail.setManualAdjustment(BigDecimal.ZERO);
                    detail.setPayslipViewed(0);
                    salaryDetailMapper.insert(detail);

                    totalGross = totalGross.add(grossPay);
                    totalNet = totalNet.add(netPay);
                    totalTax = totalTax.add(incomeTax);
                    employeeCount++;

                } catch (Exception e) {
                    log.error("计算员工 {} 薪资异常", employeeSalary.getEmployeeId(), e);
                }
            }

            // 更新批次汇总
            batch.setStatus(BatchStatusEnum.CONFIRMING.getValue());
            batch.setTotalEmployees(employeeCount);
            batch.setTotalGrossPay(totalGross);
            batch.setTotalNetPay(totalNet);
            batch.setTotalTax(totalTax);
            this.updateById(batch);

            log.info("薪资核算完成，批次ID：{}，参与人数：{}，应发合计：{}", batchId, employeeCount, totalGross);

        } catch (Exception e) {
            log.error("薪资核算异常，批次ID：{}", batchId, e);
            batch.setStatus(BatchStatusEnum.DRAFT.getValue());
            this.updateById(batch);
        }
    }

    @Override
    public Page<SalaryBatchVO> listBatches(SalaryBatchQueryRequest request) {
        QueryWrapper<SalaryBatch> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(request.getSalaryMonth())) {
            wrapper.eq("salary_month", request.getSalaryMonth());
        }
        if (request.getStatus() != null) {
            wrapper.eq("status", request.getStatus());
        }
        wrapper.orderByDesc("create_time");
        Page<SalaryBatch> page = this.page(new Page<>(request.getCurrent(), request.getPageSize()), wrapper);
        Page<SalaryBatchVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public SalaryBatchVO getBatchDetail(Long batchId) {
        SalaryBatch batch = this.getById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        return toVO(batch);
    }

    @Override
    public Page<SalaryDetailVO> listDetails(Long batchId, SalaryDetailQueryRequest request) {
        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_id", batchId);
        if (request.getEmployeeId() != null) {
            wrapper.eq("employee_id", request.getEmployeeId());
        }
        if (request.getIsAbnormal() != null) {
            wrapper.eq("is_abnormal", request.getIsAbnormal());
        }
        wrapper.orderByAsc("employee_id");
        Page<SalaryDetail> page = salaryDetailMapper.selectPage(
                new Page<>(request.getCurrent(), request.getPageSize()), wrapper);
        Page<SalaryDetailVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toDetailVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustDetail(Long detailId, SalaryDetailAdjustRequest request) {
        SalaryDetail detail = salaryDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "工资条不存在");
        }
        detail.setManualAdjustment(request.getAdjustment());
        detail.setAdjustmentReason(request.getReason());
        // 重新计算实发
        detail.setNetPay(detail.getGrossPay().subtract(detail.getTotalDeductions()).add(request.getAdjustment()));
        salaryDetailMapper.updateById(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long batchId) {
        SalaryBatch batch = this.getById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        if (batch.getStatus() != BatchStatusEnum.CONFIRMING.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅待确认状态的批次可以提交审批");
        }

        // 创建审批实例（由 SalaryBatchNodeBuilder 构建节点链：财务专员→老板）
        Long applicantId = dataScopeContext.getCurrentEmployeeId();
        approvalFlowService.createInstance(ApprovalBizType.SALARY_BATCH, batchId, applicantId);

        batch.setStatus(BatchStatusEnum.APPROVING.getValue());
        this.updateById(batch);

        log.info("薪资批次已提交审批: batchId={}, applicantId={}", batchId, applicantId);
    }

    // ==================== 审批回调 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.SALARY_BATCH) return;
        SalaryBatch batch = this.getById(bizId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        batch.setStatus(BatchStatusEnum.APPROVED.getValue());
        this.updateById(batch);
        log.info("薪资批次审批通过: batchId={}", bizId);
    }

    @Override
    public void onRejected(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.SALARY_BATCH) return;
        SalaryBatch batch = this.getById(bizId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        batch.setStatus(BatchStatusEnum.REJECTED.getValue());
        this.updateById(batch);
        log.info("薪资批次审批驳回: batchId={}", bizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsPaid(Long batchId) {
        SalaryBatch batch = this.getById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        if (batch.getStatus() != BatchStatusEnum.APPROVED.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅已通过状态的批次可以标记发放");
        }
        batch.setStatus(BatchStatusEnum.PAID.getValue());
        this.updateById(batch);
    }

    // ==================== VO 转换 ====================

    private SalaryBatchVO toVO(SalaryBatch batch) {
        SalaryBatchVO vo = new SalaryBatchVO();
        BeanUtils.copyProperties(batch, vo);
        BatchStatusEnum statusEnum = BatchStatusEnum.fromValue(batch.getStatus());
        vo.setStatusLabel(statusEnum != null ? statusEnum.getLabel() : "");
        return vo;
    }

    private SalaryDetailVO toDetailVO(SalaryDetail detail) {
        SalaryDetailVO vo = new SalaryDetailVO();
        BeanUtils.copyProperties(detail, vo);
        AbnormalLevelEnum abnormalEnum = AbnormalLevelEnum.fromValue(detail.getIsAbnormal());
        vo.setAbnormalLabel(abnormalEnum != null ? abnormalEnum.getLabel() : "");
        // 解析 JSON 工资项目
        if (StringUtils.isNotBlank(detail.getSalaryItems())) {
            vo.setSalaryItems(JSONUtil.toList(detail.getSalaryItems(), SalaryItemDetailVO.class));
        }
        return vo;
    }
}
