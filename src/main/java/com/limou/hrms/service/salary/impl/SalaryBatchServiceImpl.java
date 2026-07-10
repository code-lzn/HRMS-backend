package com.limou.hrms.service.salary.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.EmployeeSalaryMapper;
import com.limou.hrms.mapper.IncomeTaxCumulativeMapper;
import com.limou.hrms.mapper.SalaryBatchMapper;
import com.limou.hrms.mapper.SalaryDetailMapper;
import com.limou.hrms.model.dto.salary.SalaryAdjustRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchCreateRequest;
import com.limou.hrms.model.dto.salary.SalaryBatchQueryRequest;
import com.limou.hrms.model.entity.EmployeeSalary;
import com.limou.hrms.model.entity.IncomeTaxCumulative;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.entity.SalaryDetail;
import com.limou.hrms.model.enums.AbnormalLevelEnum;
import com.limou.hrms.model.enums.BatchStatusEnum;
import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import com.limou.hrms.model.vo.salary.AnomalyVO;
import com.limou.hrms.model.vo.salary.SalaryBatchPreviewVO;
import com.limou.hrms.model.vo.salary.SalaryBatchVO;
import com.limou.hrms.model.vo.salary.SalaryDetailVO;
import com.limou.hrms.model.vo.salary.SalaryItemAmountVO;
import com.limou.hrms.service.salary.SalaryBatchService;
import com.limou.hrms.service.salary.anomaly.AnomalyDetector;
import com.limou.hrms.service.salary.anomaly.AnomalyResult;
import com.limou.hrms.service.salary.calculator.SalaryCalculationContext;
import com.limou.hrms.service.salary.calculator.SalaryCalculatorEngine;
import com.limou.hrms.service.salary.tax.IncomeTaxCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 薪资核算批次服务实现
 */
@Service
@Slf4j
public class SalaryBatchServiceImpl extends ServiceImpl<SalaryBatchMapper, SalaryBatch>
        implements SalaryBatchService {

    @Resource
    private SalaryDetailMapper salaryDetailMapper;

    @Resource
    private EmployeeSalaryMapper employeeSalaryMapper;

    @Resource
    private IncomeTaxCumulativeMapper incomeTaxCumulativeMapper;

    @Resource
    private SalaryCalculatorEngine calculatorEngine;

    @Resource
    private IncomeTaxCalculator incomeTaxCalculator;

    @Resource
    private AnomalyDetector anomalyDetector;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBatch(SalaryBatchCreateRequest request, Long createBy) {
        if (request == null || request.getSalary_month() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "核算月份不能为空");
        }

        // 检查该月份是否已有批次
        QueryWrapper<SalaryBatch> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("salary_month", request.getSalary_month());
        Long count = this.baseMapper.selectCount(checkWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该月份已存在核算批次");
        }

        // 生成批次号
        String batchNo = "SAL" + request.getSalary_month().replace("-", "") + "001";

        SalaryBatch batch = new SalaryBatch();
        batch.setBatch_no(batchNo);
        batch.setSalary_month(request.getSalary_month());
        batch.setStatus(BatchStatusEnum.DRAFT.getValue());
        batch.setTotal_employees(0);
        batch.setCreate_by(createBy);
        this.save(batch);

        log.info("创建核算批次：{}，月份：{}", batchNo, request.getSalary_month());
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Async
    public void executeCalculate(Long batchId) {
        SalaryBatch batch = this.getById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }

        // 更新状态为计算中
        batch.setStatus(BatchStatusEnum.CALCULATING.getValue());
        this.updateById(batch);

        try {
            // 解析月份
            String[] parts = batch.getSalary_month().split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            // 查询所有在职员工薪资档案
            QueryWrapper<EmployeeSalary> salaryWrapper = new QueryWrapper<>();
            salaryWrapper.eq("is_deleted", 0)
                    .orderByDesc("effective_date");
            List<EmployeeSalary> allSalaries = employeeSalaryMapper.selectList(salaryWrapper);

            // 按 employee_id 分组，取每个员工最新的档案
            Map<Long, EmployeeSalary> latestSalaryMap = allSalaries.stream()
                    .collect(Collectors.toMap(
                            EmployeeSalary::getEmployee_id,
                            s -> s,
                            (s1, s2) -> s1.getEffective_date().after(s2.getEffective_date()) ? s1 : s2
                    ));

            // 逐员工计算
            List<SalaryDetail> details = new ArrayList<>();
            for (Map.Entry<Long, EmployeeSalary> entry : latestSalaryMap.entrySet()) {
                Long employeeId = entry.getKey();
                EmployeeSalary salary = entry.getValue();

                try {
                    SalaryDetail detail = calculateOneEmployee(batchId, employeeId, salary, year, month);
                    details.add(detail);
                } catch (Exception e) {
                    log.error("员工 {} 薪资计算失败：{}", employeeId, e.getMessage());
                    // 创建异常明细
                    SalaryDetail errorDetail = new SalaryDetail();
                    errorDetail.setBatch_id(batchId);
                    errorDetail.setEmployee_id(employeeId);
                    errorDetail.setSalary_items("[]");
                    errorDetail.setGross_pay(BigDecimal.ZERO);
                    errorDetail.setNet_pay(BigDecimal.ZERO);
                    errorDetail.setIs_abnormal(AbnormalLevelEnum.BLOCKED.getValue());
                    errorDetail.setAbnormal_reason("计算失败：" + e.getMessage());
                    details.add(errorDetail);
                }
            }

            // 批量保存明细
            for (SalaryDetail detail : details) {
                salaryDetailMapper.insert(detail);
            }

            // 汇总批次统计
            BigDecimal totalGross = details.stream()
                    .map(d -> d.getGross_pay() != null ? d.getGross_pay() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalNet = details.stream()
                    .map(d -> d.getNet_pay() != null ? d.getNet_pay() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalTax = details.stream()
                    .map(d -> d.getIncome_tax() != null ? d.getIncome_tax() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            batch.setTotal_employees(details.size());
            batch.setTotal_gross_pay(totalGross);
            batch.setTotal_net_pay(totalNet);
            batch.setTotal_tax(totalTax);
            batch.setStatus(BatchStatusEnum.CONFIRMING.getValue());
            this.updateById(batch);

            log.info("批次 {} 计算完成，共 {} 人，应发合计：{}，实发合计：{}",
                    batch.getBatch_no(), details.size(), totalGross, totalNet);

        } catch (Exception e) {
            log.error("批次 {} 计算异常", batchId, e);
            batch.setStatus(BatchStatusEnum.DRAFT.getValue());
            this.updateById(batch);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "薪资计算异常：" + e.getMessage());
        }
    }

    /**
     * 计算单个员工的薪资
     */
    private SalaryDetail calculateOneEmployee(Long batchId, Long employeeId, EmployeeSalary salary,
                                               int year, int month) {
        // 构建计算上下文
        SalaryCalculationContext ctx = SalaryCalculationContext.builder()
                .employeeId(employeeId)
                .baseSalary(salary.getBase_salary())
                .allowanceBase(salary.getAllowance_base())
                .socialSecurityBase(salary.getSocial_security_base())
                .housingFundBase(salary.getHousing_fund_base())
                .performanceBase(salary.getPerformance_base())
                .performanceCoefficient(BigDecimal.ONE) // 默认绩效系数1.0，实际应从绩效系统获取
                .month(month)
                .build();

        // 计算各项（不含个税）
        Map<SalaryItemTypeEnum, BigDecimal> itemResults = calculatorEngine.calculateAllExceptTax(ctx);

        // 应发合计 = 固定收入 + 变动收入 + 考勤扣款（已为负值）
        BigDecimal grossPay = itemResults.getOrDefault(SalaryItemTypeEnum.FIXED_INCOME, BigDecimal.ZERO)
                .add(itemResults.getOrDefault(SalaryItemTypeEnum.VARIABLE_INCOME, BigDecimal.ZERO))
                .add(itemResults.getOrDefault(SalaryItemTypeEnum.ATTENDANCE_DEDUCT, BigDecimal.ZERO));

        // 社保和公积金（绝对值，用于个税计算）
        BigDecimal socialSecurityAbs = itemResults.getOrDefault(SalaryItemTypeEnum.SOCIAL_SECURITY, BigDecimal.ZERO).abs();
        BigDecimal housingFundAbs = itemResults.getOrDefault(SalaryItemTypeEnum.HOUSING_FUND, BigDecimal.ZERO).abs();

        // 计算个税
        BigDecimal incomeTax = BigDecimal.ZERO;
        try {
            IncomeTaxCumulative taxRecord = incomeTaxCalculator.calculateMonthlyTax(
                    employeeId, year, month, grossPay, socialSecurityAbs, housingFundAbs, BigDecimal.ZERO);
            incomeTax = taxRecord.getCurrent_month_tax();
            // 保存个税累计记录
            QueryWrapper<IncomeTaxCumulative> checkExists = new QueryWrapper<>();
            checkExists.eq("employee_id", employeeId)
                    .eq("tax_year", year)
                    .eq("tax_month", month);
            IncomeTaxCumulative existing = incomeTaxCumulativeMapper.selectOne(checkExists);
            if (existing != null) {
                incomeTaxCumulativeMapper.deleteById(existing.getId());
            }
            incomeTaxCumulativeMapper.insert(taxRecord);
        } catch (Exception e) {
            log.error("员工 {} 个税计算失败：{}", employeeId, e.getMessage());
        }

        // 扣除合计
        BigDecimal totalDeductions = socialSecurityAbs.add(housingFundAbs).add(incomeTax);

        // 实发合计 = 应发 - 社保 - 公积金 - 个税
        BigDecimal netPay = grossPay.subtract(totalDeductions);

        // 异常检测
        List<AnomalyResult> anomalies = anomalyDetector.detect(ctx);
        int abnormalLevel = AbnormalLevelEnum.NORMAL.getValue();
        String abnormalReason = null;
        if (!anomalies.isEmpty()) {
            // 取最高等级
            for (AnomalyResult anomaly : anomalies) {
                if (anomaly.getLevel().getValue() > abnormalLevel
                        && anomaly.getLevel() != AbnormalLevelEnum.NORMAL) {
                    abnormalLevel = anomaly.getLevel().getValue();
                    abnormalReason = anomaly.getReason();
                }
            }
        }

        // 构建工资项JSON
        List<SalaryItemAmountVO> itemAmounts = new ArrayList<>();
        for (Map.Entry<SalaryItemTypeEnum, BigDecimal> entry : itemResults.entrySet()) {
            SalaryItemAmountVO amountVO = new SalaryItemAmountVO();
            amountVO.setName(entry.getKey().getLabel());
            amountVO.setType(entry.getKey().getValue());
            amountVO.setAmount(entry.getValue());
            itemAmounts.add(amountVO);
        }
        // 加上个税
        SalaryItemAmountVO taxAmount = new SalaryItemAmountVO();
        taxAmount.setName(SalaryItemTypeEnum.INCOME_TAX.getLabel());
        taxAmount.setType(SalaryItemTypeEnum.INCOME_TAX.getValue());
        taxAmount.setAmount(incomeTax.negate());
        itemAmounts.add(taxAmount);

        // 创建明细
        SalaryDetail detail = new SalaryDetail();
        detail.setBatch_id(batchId);
        detail.setEmployee_id(employeeId);
        detail.setSalary_items(JSONUtil.toJsonStr(itemAmounts));
        detail.setGross_pay(grossPay);
        detail.setSocial_security(socialSecurityAbs);
        detail.setHousing_fund(housingFundAbs);
        detail.setIncome_tax(incomeTax);
        detail.setTotal_deductions(totalDeductions);
        detail.setNet_pay(netPay);
        detail.setIs_abnormal(abnormalLevel);
        detail.setAbnormal_reason(abnormalReason);
        detail.setManual_adjustment(BigDecimal.ZERO);

        return detail;
    }

    @Override
    public SalaryBatchPreviewVO preview(Long batchId, int page, int size) {
        SalaryBatch batch = this.getById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }

        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_id", batchId);
        long total = salaryDetailMapper.selectCount(wrapper);

        Page<SalaryDetail> pageQuery = new Page<>(page, size);
        Page<SalaryDetail> detailPage = salaryDetailMapper.selectPage(pageQuery, wrapper);

        List<SalaryDetailVO> recordVOs = detailPage.getRecords().stream()
                .map(this::toDetailVO)
                .collect(Collectors.toList());

        SalaryBatchPreviewVO preview = new SalaryBatchPreviewVO();
        preview.setBatch(toBatchVO(batch));
        preview.setRecords(recordVOs);
        preview.setTotal(total);

        return preview;
    }

    @Override
    public List<AnomalyVO> getAnomalies(Long batchId) {
        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_id", batchId)
                .gt("is_abnormal", 0);
        List<SalaryDetail> anomalies = salaryDetailMapper.selectList(wrapper);
        return anomalies.stream().map(this::toAnomalyVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(SalaryAdjustRequest request) {
        if (request == null || request.getDetail_id() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SalaryDetail detail = salaryDetailMapper.selectById(request.getDetail_id());
        if (detail == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "薪资明细不存在");
        }
        // 应用调整
        detail.setManual_adjustment(request.getAdjustment_amount());
        detail.setAdjustment_reason(request.getAdjustment_reason());
        detail.setNet_pay(detail.getNet_pay().add(request.getAdjustment_amount()));
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
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前状态不允许提交审批");
        }
        batch.setStatus(BatchStatusEnum.APPROVING.getValue());
        this.updateById(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long batchId) {
        SalaryBatch batch = this.getById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        batch.setStatus(BatchStatusEnum.APPROVED.getValue());
        this.updateById(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long batchId, String reason) {
        SalaryBatch batch = this.getById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        batch.setStatus(BatchStatusEnum.REJECTED.getValue());
        this.updateById(batch);
        log.info("批次 {} 审批驳回，原因：{}", batch.getBatch_no(), reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long batchId) {
        SalaryBatch batch = this.getById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        batch.setStatus(BatchStatusEnum.PAID.getValue());
        this.updateById(batch);
    }

    @Override
    public Page<SalaryBatchVO> listBatches(SalaryBatchQueryRequest request) {
        QueryWrapper<SalaryBatch> wrapper = new QueryWrapper<>();
        if (request.getSalary_month() != null) {
            wrapper.eq("salary_month", request.getSalary_month());
        }
        if (request.getStatus() != null) {
            wrapper.eq("status", request.getStatus());
        }
        wrapper.orderByDesc("create_time");

        Page<SalaryBatch> page = this.page(
                new Page<>(request.getCurrent(), request.getPageSize()), wrapper);

        Page<SalaryBatchVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toBatchVO).collect(Collectors.toList()));
        return voPage;
    }

    // ==================== VO 转换 ====================

    private SalaryBatchVO toBatchVO(SalaryBatch batch) {
        SalaryBatchVO vo = new SalaryBatchVO();
        vo.setId(batch.getId());
        vo.setBatch_no(batch.getBatch_no());
        vo.setSalary_month(batch.getSalary_month());
        vo.setStatus(batch.getStatus());
        BatchStatusEnum statusEnum = BatchStatusEnum.fromValue(batch.getStatus());
        vo.setStatus_label(statusEnum != null ? statusEnum.getLabel() : "");
        vo.setTotal_employees(batch.getTotal_employees());
        vo.setTotal_gross_pay(batch.getTotal_gross_pay());
        vo.setTotal_net_pay(batch.getTotal_net_pay());
        vo.setTotal_tax(batch.getTotal_tax());
        vo.setCreate_by(batch.getCreate_by());
        vo.setCreate_time(batch.getCreate_time());
        vo.setUpdate_time(batch.getUpdate_time());
        return vo;
    }

    private SalaryDetailVO toDetailVO(SalaryDetail detail) {
        SalaryDetailVO vo = new SalaryDetailVO();
        vo.setId(detail.getId());
        vo.setEmployee_id(detail.getEmployee_id());
        vo.setGross_pay(detail.getGross_pay());
        vo.setSocial_security(detail.getSocial_security());
        vo.setHousing_fund(detail.getHousing_fund());
        vo.setIncome_tax(detail.getIncome_tax());
        vo.setTotal_deductions(detail.getTotal_deductions());
        vo.setNet_pay(detail.getNet_pay());
        vo.setIs_abnormal(detail.getIs_abnormal());
        vo.setAbnormal_reason(detail.getAbnormal_reason());
        vo.setManual_adjustment(detail.getManual_adjustment());
        vo.setAdjustment_reason(detail.getAdjustment_reason());
        // 解析工资项JSON
        if (detail.getSalary_items() != null) {
            vo.setSalary_items(JSONUtil.toList(
                    JSONUtil.parseArray(detail.getSalary_items()), SalaryItemAmountVO.class));
        }
        return vo;
    }

    private AnomalyVO toAnomalyVO(SalaryDetail detail) {
        AnomalyVO vo = new AnomalyVO();
        vo.setDetail_id(detail.getId());
        vo.setEmployee_id(detail.getEmployee_id());
        vo.setIs_abnormal(detail.getIs_abnormal());
        vo.setAbnormal_reason(detail.getAbnormal_reason());
        AbnormalLevelEnum level = AbnormalLevelEnum.fromValue(detail.getIs_abnormal());
        vo.setAbnormal_level_label(level != null ? level.getLabel() : "");
        return vo;
    }
}
