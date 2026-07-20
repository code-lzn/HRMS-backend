package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.SalaryStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 薪资统计可视化服务实现
 */
@Service
@Slf4j
public class SalaryStatisticsServiceImpl implements SalaryStatisticsService {

    @Resource
    private SalaryBatchMapper salaryBatchMapper;
    @Resource
    private SalarySlipMapper salarySlipMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public List<SalaryMonthlyTrendVO> getMonthlyTrend(int months) {
        // 查最近 N 个批次的汇总数据（按月正序）
        List<SalaryBatch> batches = salaryBatchMapper.selectList(
                new LambdaQueryWrapper<SalaryBatch>()
                        .in(SalaryBatch::getStatus, "APPROVED", "PAID")
                        .orderByAsc(SalaryBatch::getSalaryMonth)
                        .last("LIMIT " + Math.min(months, 12))
        );

        return batches.stream().map(batch -> {
            SalaryMonthlyTrendVO vo = new SalaryMonthlyTrendVO();
            vo.setMonth(batch.getSalaryMonth());
            vo.setGrossTotal(batch.getTotalGross());
            vo.setNetTotal(batch.getTotalNet());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<SalaryDeptDistributionVO> getDeptDistribution(Long batchId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");

        // 查询批次下所有明细
        List<SalarySlip> slips = salarySlipMapper.selectList(
                new LambdaQueryWrapper<SalarySlip>()
                        .eq(SalarySlip::getBatchId, batchId)
        );

        // 按员工ID分组，再按部门汇总
        Map<Long, Long> empIdToSlipId = new HashMap<>();
        Map<Long, BigDecimal> empIdToGross = new HashMap<>();
        Map<Long, BigDecimal> empIdToNet = new HashMap<>();

        for (SalarySlip slip : slips) {
            empIdToSlipId.put(slip.getEmployeeId(), slip.getId());
            empIdToGross.merge(slip.getEmployeeId(), slip.getGrossSalary(), BigDecimal::add);
            empIdToNet.merge(slip.getEmployeeId(), slip.getNetSalary(), BigDecimal::add);
        }

        // 查员工信息，按部门分组
        Set<Long> allEmpIds = empIdToSlipId.keySet();
        if (allEmpIds.isEmpty()) return Collections.emptyList();

        List<Employee> employees = employeeMapper.selectBatchIds(allEmpIds);
        Map<Long, Long> empIdToDeptId = employees.stream()
                .collect(Collectors.toMap(Employee::getId, e -> e.getDepartmentId() != null ? e.getDepartmentId() : 0L));

        Set<Long> deptIds = new HashSet<>(empIdToDeptId.values());
        deptIds.remove(0L);
        Map<Long, String> deptIdToName = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<Department> depts = departmentMapper.selectBatchIds(deptIds);
            for (Department dept : depts) {
                deptIdToName.put(dept.getId(), dept.getDeptName());
            }
        }

        // 按部门汇总
        Map<Long, BigDecimal> deptGross = new LinkedHashMap<>();
        Map<Long, BigDecimal> deptNet = new LinkedHashMap<>();
        Map<Long, Integer> deptCount = new LinkedHashMap<>();

        for (Employee emp : employees) {
            Long deptId = emp.getDepartmentId() != null ? emp.getDepartmentId() : 0L;
            deptGross.merge(deptId, empIdToGross.getOrDefault(emp.getId(), BigDecimal.ZERO), BigDecimal::add);
            deptNet.merge(deptId, empIdToNet.getOrDefault(emp.getId(), BigDecimal.ZERO), BigDecimal::add);
            deptCount.merge(deptId, 1, Integer::sum);
        }

        // 构建VO
        List<SalaryDeptDistributionVO> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : deptGross.entrySet()) {
            Long deptId = entry.getKey();
            SalaryDeptDistributionVO vo = new SalaryDeptDistributionVO();
            vo.setDepartmentName(deptId == 0L ? "未分配部门" : deptIdToName.getOrDefault(deptId, "未知部门"));
            vo.setEmployeeCount(deptCount.getOrDefault(deptId, 0));
            vo.setGrossTotal(entry.getValue().setScale(2, RoundingMode.HALF_UP));
            vo.setNetTotal(deptNet.getOrDefault(deptId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
            result.add(vo);
        }
        // 按应发总额降序
        result.sort((a, b) -> b.getGrossTotal().compareTo(a.getGrossTotal()));
        return result;
    }

    @Override
    public List<SalaryCompositionVO> getComposition(Long batchId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");

        List<SalarySlip> slips = salarySlipMapper.selectList(
                new LambdaQueryWrapper<SalarySlip>()
                        .eq(SalarySlip::getBatchId, batchId)
        );

        BigDecimal totalBaseSalary = BigDecimal.ZERO;
        BigDecimal totalAllowance = BigDecimal.ZERO;
        BigDecimal totalPerformance = BigDecimal.ZERO;
        BigDecimal totalOvertime = BigDecimal.ZERO;

        for (SalarySlip slip : slips) {
            totalBaseSalary = totalBaseSalary.add(slip.getBaseSalary() != null ? slip.getBaseSalary() : BigDecimal.ZERO);
            totalAllowance = totalAllowance.add(slip.getAllowance() != null ? slip.getAllowance() : BigDecimal.ZERO);
            totalPerformance = totalPerformance.add(slip.getPerformanceBonus() != null ? slip.getPerformanceBonus() : BigDecimal.ZERO);
            totalOvertime = totalOvertime.add(slip.getOvertimePay() != null ? slip.getOvertimePay() : BigDecimal.ZERO);
        }

        List<SalaryCompositionVO> result = new ArrayList<>();
        addCompositionItem(result, "基本工资", totalBaseSalary);
        addCompositionItem(result, "岗位津贴", totalAllowance);
        addCompositionItem(result, "绩效奖金", totalPerformance);
        addCompositionItem(result, "加班费", totalOvertime);
        // 按金额降序
        result.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return result;
    }

    private void addCompositionItem(List<SalaryCompositionVO> list, String name, BigDecimal amount) {
        SalaryCompositionVO vo = new SalaryCompositionVO();
        vo.setItemName(name);
        vo.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        list.add(vo);
    }

    @Override
    public List<SalarySocialSecurityVO> getSocialSecurityComparison(Long batchId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");

        List<SalarySlip> slips = salarySlipMapper.selectList(
                new LambdaQueryWrapper<SalarySlip>()
                        .eq(SalarySlip::getBatchId, batchId)
        );

        BigDecimal totalPension = BigDecimal.ZERO;
        BigDecimal totalMedical = BigDecimal.ZERO;
        BigDecimal totalUnemployment = BigDecimal.ZERO;
        BigDecimal totalHousingFund = BigDecimal.ZERO;

        for (SalarySlip slip : slips) {
            totalPension = totalPension.add(slip.getSocialPension() != null ? slip.getSocialPension() : BigDecimal.ZERO);
            totalMedical = totalMedical.add(slip.getSocialMedical() != null ? slip.getSocialMedical() : BigDecimal.ZERO);
            totalUnemployment = totalUnemployment.add(slip.getSocialUnemployment() != null ? slip.getSocialUnemployment() : BigDecimal.ZERO);
            totalHousingFund = totalHousingFund.add(slip.getHousingFund() != null ? slip.getHousingFund() : BigDecimal.ZERO);
        }

        // 企业比例：养老16%、医疗8%、失业0.5%、公积金12%
        List<SalarySocialSecurityVO> result = new ArrayList<>();
        result.add(buildSsVo("养老保险", totalPension, BigDecimal.valueOf(0.16), BigDecimal.valueOf(0.08)));
        result.add(buildSsVo("医疗保险", totalMedical, BigDecimal.valueOf(0.08), BigDecimal.valueOf(0.02)));
        result.add(buildSsVo("失业保险", totalUnemployment, BigDecimal.valueOf(0.005), BigDecimal.valueOf(0.005)));
        result.add(buildSsVo("住房公积金", totalHousingFund, BigDecimal.valueOf(0.12), BigDecimal.valueOf(0.12)));
        return result;
    }

    private SalarySocialSecurityVO buildSsVo(String name, BigDecimal personalAmount,
                                              BigDecimal companyRate, BigDecimal personalRate) {
        SalarySocialSecurityVO vo = new SalarySocialSecurityVO();
        vo.setItemName(name);
        vo.setPersonalAmount(personalAmount.setScale(2, RoundingMode.HALF_UP));
        // 企业缴纳 = 个人缴纳 / 个人比例 * 企业比例（同基数）
        if (personalRate.compareTo(BigDecimal.ZERO) > 0) {
            vo.setCompanyAmount(personalAmount.divide(personalRate, 4, RoundingMode.HALF_UP)
                    .multiply(companyRate).setScale(2, RoundingMode.HALF_UP));
        } else {
            vo.setCompanyAmount(BigDecimal.ZERO);
        }
        return vo;
    }

    @Override
    public List<SalaryChangeDistributionVO> getChangeDistribution(Long batchId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");

        // 计算上个月份
        String salaryMonth = batch.getSalaryMonth();
        String[] parts = salaryMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int lastMonth = month - 1;
        int lastYear = year;
        if (lastMonth == 0) {
            lastMonth = 12;
            lastYear--;
        }
        String lastSalaryMonth = String.format("%04d-%02d", lastYear, lastMonth);

        // 找上月批次
        SalaryBatch lastBatch = salaryBatchMapper.selectOne(
                new LambdaQueryWrapper<SalaryBatch>().eq(SalaryBatch::getSalaryMonth, lastSalaryMonth)
        );
        if (lastBatch == null) return Collections.emptyList();

        // 查当月所有明细
        List<SalarySlip> currentSlips = salarySlipMapper.selectList(
                new LambdaQueryWrapper<SalarySlip>().eq(SalarySlip::getBatchId, batchId)
        );
        // 查上月所有明细
        List<SalarySlip> lastSlips = salarySlipMapper.selectList(
                new LambdaQueryWrapper<SalarySlip>().eq(SalarySlip::getBatchId, lastBatch.getId())
        );

        Map<Long, BigDecimal> lastNetMap = new HashMap<>();
        for (SalarySlip slip : lastSlips) {
            lastNetMap.put(slip.getEmployeeId(), slip.getNetSalary() != null ? slip.getNetSalary() : BigDecimal.ZERO);
        }

        // 定义区间
        String[] ranges = {"-30%以下", "-30%~-10%", "-10%~0%", "0%~+10%", "+10%~+30%", "+30%以上"};
        Map<String, Integer> rangeCount = new LinkedHashMap<>();
        for (String range : ranges) {
            rangeCount.put(range, 0);
        }

        for (SalarySlip slip : currentSlips) {
            BigDecimal lastNet = lastNetMap.get(slip.getEmployeeId());
            if (lastNet == null || lastNet.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal currentNet = slip.getNetSalary() != null ? slip.getNetSalary() : BigDecimal.ZERO;
            BigDecimal changeRate = currentNet.subtract(lastNet)
                    .divide(lastNet, 4, RoundingMode.HALF_UP);

            String range;
            if (changeRate.compareTo(BigDecimal.valueOf(-0.3)) < 0) {
                range = "-30%以下";
            } else if (changeRate.compareTo(BigDecimal.valueOf(-0.1)) < 0) {
                range = "-30%~-10%";
            } else if (changeRate.compareTo(BigDecimal.ZERO) < 0) {
                range = "-10%~0%";
            } else if (changeRate.compareTo(BigDecimal.valueOf(0.1)) < 0) {
                range = "0%~+10%";
            } else if (changeRate.compareTo(BigDecimal.valueOf(0.3)) < 0) {
                range = "+10%~+30%";
            } else {
                range = "+30%以上";
            }
            rangeCount.merge(range, 1, Integer::sum);
        }

        List<SalaryChangeDistributionVO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : rangeCount.entrySet()) {
            SalaryChangeDistributionVO vo = new SalaryChangeDistributionVO();
            vo.setRangeLabel(entry.getKey());
            vo.setCount(entry.getValue());
            result.add(vo);
        }
        return result;
    }
}
