package com.limou.hrms.service.salary.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.SalaryBatchMapper;
import com.limou.hrms.mapper.SalaryDetailMapper;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.entity.SalaryDetail;
import com.limou.hrms.model.enums.BatchStatusEnum;
import com.limou.hrms.model.vo.salary.CompositionVO;
import com.limou.hrms.model.vo.salary.CostTrendVO;
import com.limou.hrms.model.vo.salary.DeptDistributionVO;
import com.limou.hrms.model.vo.salary.SalaryItemAmountVO;
import com.limou.hrms.model.vo.salary.SocialComparisonVO;
import com.limou.hrms.model.vo.salary.VariationDistributionVO;
import com.limou.hrms.service.salary.SalaryStatisticsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 薪资统计服务实现（AntV 可视化数据源）
 */
@Service
@Slf4j
public class SalaryStatisticsServiceImpl implements SalaryStatisticsService {

    @Resource
    private SalaryBatchMapper salaryBatchMapper;

    @Resource
    private SalaryDetailMapper salaryDetailMapper;

    @Override
    public List<CostTrendVO> getCostTrend(int months) {
        // 查询最近N个已通过/已发放的批次
        QueryWrapper<SalaryBatch> wrapper = new QueryWrapper<>();
        wrapper.in("status", BatchStatusEnum.APPROVED.getValue(), BatchStatusEnum.PAID.getValue())
                .orderByDesc("salary_month")
                .last("LIMIT " + months);
        List<SalaryBatch> batches = salaryBatchMapper.selectList(wrapper);

        List<CostTrendVO> result = new ArrayList<>();
        for (int i = 0; i < batches.size(); i++) {
            SalaryBatch batch = batches.get(i);
            CostTrendVO vo = new CostTrendVO();
            vo.setMonth(batch.getSalary_month());
            vo.setTotal_cost(batch.getTotal_gross_pay());

            // 计算同比变化率
            if (i < batches.size() - 1) {
                BigDecimal lastCost = batches.get(i + 1).getTotal_gross_pay();
                if (lastCost != null && lastCost.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal change = batch.getTotal_gross_pay().subtract(lastCost)
                            .divide(lastCost, 4, RoundingMode.HALF_UP);
                    vo.setYoy_change_rate(change);
                }
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<DeptDistributionVO> getDeptDistribution(String month) {
        // 按部门聚合薪资数据
        // 实际应 JOIN department 表，这里返回基础结构
        return aggregateByDept(month);
    }

    @Override
    public List<CompositionVO> getComposition(String month) {
        // 汇总所有员工工资项，按类型聚合
        SalaryBatch batch = findBatchByMonth(month);
        if (batch == null) {
            return new ArrayList<>();
        }

        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_id", batch.getId());
        List<SalaryDetail> details = salaryDetailMapper.selectList(wrapper);

        BigDecimal totalGross = details.stream()
                .map(d -> d.getGross_pay() != null ? d.getGross_pay() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CompositionVO> result = new ArrayList<>();

        // 从工资项JSON中聚合各类型
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalAllowance = BigDecimal.ZERO;
        BigDecimal totalPerformance = BigDecimal.ZERO;
        BigDecimal totalOvertime = BigDecimal.ZERO;

        for (SalaryDetail detail : details) {
            if (detail.getSalary_items() != null) {
                List<SalaryItemAmountVO> items = JSONUtil.toList(
                        JSONUtil.parseArray(detail.getSalary_items()), SalaryItemAmountVO.class);
                for (SalaryItemAmountVO item : items) {
                    if (item.getAmount() == null) continue;
                    String name = item.getName();
                    if (name != null && name.contains("基本")) totalBase = totalBase.add(item.getAmount());
                    else if (name != null && name.contains("津贴")) totalAllowance = totalAllowance.add(item.getAmount());
                    else if (name != null && name.contains("绩效")) totalPerformance = totalPerformance.add(item.getAmount());
                    else if (name != null && name.contains("加班")) totalOvertime = totalOvertime.add(item.getAmount());
                }
            }
        }

        if (totalGross.compareTo(BigDecimal.ZERO) > 0) {
            addCompositionItem(result, "基本工资", totalBase, totalGross);
            addCompositionItem(result, "津贴", totalAllowance, totalGross);
            addCompositionItem(result, "绩效奖金", totalPerformance, totalGross);
            addCompositionItem(result, "加班费", totalOvertime, totalGross);
        }

        return result;
    }

    @Override
    public List<SocialComparisonVO> getSocialComparison(String month) {
        // 按部门聚合社保和公积金
        SalaryBatch batch = findBatchByMonth(month);
        if (batch == null) {
            return new ArrayList<>();
        }

        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_id", batch.getId());
        List<SalaryDetail> details = salaryDetailMapper.selectList(wrapper);

        // 简化：整体汇总（实际应按部门分组）
        SocialComparisonVO vo = new SocialComparisonVO();
        vo.setDepartment_name("全公司");
        vo.setTotal_social_security(details.stream()
                .map(d -> d.getSocial_security() != null ? d.getSocial_security() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setTotal_housing_fund(details.stream()
                .map(d -> d.getHousing_fund() != null ? d.getHousing_fund() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        List<SocialComparisonVO> result = new ArrayList<>();
        result.add(vo);
        return result;
    }

    @Override
    public List<VariationDistributionVO> getVariationDistribution(String month) {
        // 薪资变动分布：按实发金额区间统计人数
        SalaryBatch batch = findBatchByMonth(month);
        if (batch == null) {
            return new ArrayList<>();
        }

        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_id", batch.getId());
        List<SalaryDetail> details = salaryDetailMapper.selectList(wrapper);

        // 定义区间
        int[][] ranges = {{0, 5000}, {5000, 8000}, {8000, 12000}, {12000, 20000}, {20000, 999999}};
        List<VariationDistributionVO> result = new ArrayList<>();

        for (int[] range : ranges) {
            BigDecimal low = new BigDecimal(range[0]);
            BigDecimal high = new BigDecimal(range[1]);
            long count = details.stream()
                    .filter(d -> d.getNet_pay().compareTo(low) >= 0 && d.getNet_pay().compareTo(high) < 0)
                    .count();

            VariationDistributionVO vo = new VariationDistributionVO();
            vo.setRange_label(low + "-" + (range[1] == 999999 ? "以上" : high));
            vo.setRange_start(range[0]);
            vo.setRange_end(range[1]);
            vo.setEmployee_count((int) count);
            result.add(vo);
        }

        return result;
    }

    // ==================== 辅助方法 ====================

    private SalaryBatch findBatchByMonth(String month) {
        if (month == null || month.isEmpty()) {
            // 获取最新批次
            QueryWrapper<SalaryBatch> wrapper = new QueryWrapper<>();
            wrapper.in("status", BatchStatusEnum.APPROVED.getValue(), BatchStatusEnum.PAID.getValue())
                    .orderByDesc("salary_month")
                    .last("LIMIT 1");
            return salaryBatchMapper.selectOne(wrapper);
        }
        QueryWrapper<SalaryBatch> wrapper = new QueryWrapper<>();
        wrapper.eq("salary_month", month);
        return salaryBatchMapper.selectOne(wrapper);
    }

    private void addCompositionItem(List<CompositionVO> list, String name, BigDecimal amount, BigDecimal total) {
        CompositionVO vo = new CompositionVO();
        vo.setItem_name(name);
        vo.setAmount(amount);
        vo.setPercentage(amount.divide(total, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")));
        list.add(vo);
    }

    private List<DeptDistributionVO> aggregateByDept(String month) {
        // 简化实现：返回整体汇总
        SalaryBatch batch = findBatchByMonth(month);
        if (batch == null) {
            return new ArrayList<>();
        }

        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_id", batch.getId());
        List<SalaryDetail> details = salaryDetailMapper.selectList(wrapper);

        DeptDistributionVO vo = new DeptDistributionVO();
        vo.setDepartment_name("全公司");
        vo.setTotal_salary(details.stream()
                .map(d -> d.getNet_pay() != null ? d.getNet_pay() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setAvg_salary(details.isEmpty() ? BigDecimal.ZERO
                : vo.getTotal_salary().divide(new BigDecimal(details.size()), 2, RoundingMode.HALF_UP));
        vo.setEmployee_count(details.size());

        List<DeptDistributionVO> result = new ArrayList<>();
        result.add(vo);
        return result;
    }
}
