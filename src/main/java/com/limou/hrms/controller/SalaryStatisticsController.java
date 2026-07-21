package com.limou.hrms.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.mapper.SalaryBatchMapper;
import com.limou.hrms.mapper.SalaryDetailMapper;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.entity.SalaryDetail;
import com.limou.hrms.model.enums.BatchStatusEnum;
import com.limou.hrms.model.vo.salary.SalaryItemDetailVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪资统计 Controller（AntV 可视化数据源）
 */
@RestController
@RequestMapping("/salary-statistics")
@Slf4j
public class SalaryStatisticsController {

    @Resource
    private SalaryBatchMapper salaryBatchMapper;

    @Resource
    private SalaryDetailMapper salaryDetailMapper;

    @Resource
    private EmployeeWorkInfoMapper employeeWorkInfoMapper;

    @Resource
    private DepartmentMapper departmentMapper;

    @GetMapping("/monthly-trend")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<List<Map<String, Object>>> monthlyTrend() {
        // 查询已通过和已发放的批次，按月份倒序取最近6个月
        QueryWrapper<SalaryBatch> wrapper = new QueryWrapper<>();
        wrapper.in("status", BatchStatusEnum.APPROVED.getValue(), BatchStatusEnum.PAID.getValue())
                .orderByDesc("salary_month")
                .last("LIMIT 6");
        List<SalaryBatch> batches = salaryBatchMapper.selectList(wrapper);
        // 翻转为时间正序
        Collections.reverse(batches);

        List<Map<String, Object>> data = new ArrayList<>();
        BigDecimal prevGross = null;
        for (SalaryBatch batch : batches) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", batch.getSalaryMonth());
            item.put("grossPay", batch.getTotalGrossPay() != null ? batch.getTotalGrossPay() : BigDecimal.ZERO);
            item.put("netPay", batch.getTotalNetPay() != null ? batch.getTotalNetPay() : BigDecimal.ZERO);
            // 环比变化率
            String changeRate = "0%";
            if (prevGross != null && prevGross.compareTo(BigDecimal.ZERO) > 0
                    && batch.getTotalGrossPay() != null) {
                BigDecimal diff = batch.getTotalGrossPay().subtract(prevGross);
                BigDecimal rate = diff.divide(prevGross, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
                changeRate = (rate.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + rate + "%";
            }
            item.put("changeRate", changeRate);
            data.add(item);
            prevGross = batch.getTotalGrossPay();
        }
        return ResultUtils.success(data);
    }

    @GetMapping("/department-distribution")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<List<Map<String, Object>>> departmentDistribution(
            @RequestParam(required = false) String salaryMonth) {
        SalaryBatch targetBatch = getTargetBatch(salaryMonth);
        if (targetBatch == null) {
            return ResultUtils.success(new ArrayList<>());
        }

        // 查询该批次所有明细
        QueryWrapper<SalaryDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("batch_id", targetBatch.getId());
        List<SalaryDetail> details = salaryDetailMapper.selectList(detailWrapper);

        // 加载所有部门
        List<Department> departments = departmentMapper.selectList(new QueryWrapper<>());
        Map<Long, String> deptNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));

        // 按部门汇总
        Map<Long, BigDecimal[]> deptAgg = new LinkedHashMap<>();
        for (SalaryDetail detail : details) {
            EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                    new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", detail.getEmployeeId()));
            Long deptId = workInfo != null ? workInfo.getDepartmentId() : 0L;
            BigDecimal[] agg = deptAgg.computeIfAbsent(deptId,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            agg[0] = agg[0].add(detail.getGrossPay() != null ? detail.getGrossPay() : BigDecimal.ZERO);
            agg[1] = agg[1].add(detail.getNetPay() != null ? detail.getNetPay() : BigDecimal.ZERO);
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal[]> entry : deptAgg.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", deptNameMap.getOrDefault(entry.getKey(), "未分配"));
            item.put("grossPay", entry.getValue()[0]);
            item.put("netPay", entry.getValue()[1]);
            data.add(item);
        }
        return ResultUtils.success(data);
    }

    @GetMapping("/composition")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<List<Map<String, Object>>> composition(
            @RequestParam(required = false) String salaryMonth) {
        SalaryBatch targetBatch = getTargetBatch(salaryMonth);
        if (targetBatch == null) {
            return ResultUtils.success(new ArrayList<>());
        }

        // 查询该批次所有明细
        QueryWrapper<SalaryDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("batch_id", targetBatch.getId());
        List<SalaryDetail> details = salaryDetailMapper.selectList(detailWrapper);

        // 汇总各工资项目金额（按名称归并）
        Map<String, BigDecimal> itemAgg = new LinkedHashMap<>();
        for (SalaryDetail detail : details) {
            if (StringUtils.isBlank(detail.getSalaryItems())) {
                continue;
            }
            List<SalaryItemDetailVO> items = JSONUtil.toList(detail.getSalaryItems(), SalaryItemDetailVO.class);
            for (SalaryItemDetailVO item : items) {
                if (item.getName() == null || item.getAmount() == null) {
                    continue;
                }
                BigDecimal absAmount = item.getAmount().abs();
                itemAgg.merge(item.getName(), absAmount, BigDecimal::add);
            }
        }

        // 按金额降序排列
        List<Map.Entry<String, BigDecimal>> sorted = itemAgg.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sorted) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            data.add(item);
        }
        return ResultUtils.success(data);
    }

    @GetMapping("/variation-distribution")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<List<Map<String, Object>>> variationDistribution(
            @RequestParam(required = false) String salaryMonth) {
        SalaryBatch targetBatch = getTargetBatch(salaryMonth);
        if (targetBatch == null) {
            return ResultUtils.success(new ArrayList<>());
        }

        // 查询该批次所有明细
        QueryWrapper<SalaryDetail> detailWrapper = new QueryWrapper<>();
        detailWrapper.eq("batch_id", targetBatch.getId());
        List<SalaryDetail> details = salaryDetailMapper.selectList(detailWrapper);

        // 按实发工资区间分布统计
        // 区间：<3000, 3000-5000, 5000-8000, 8000-12000, 12000-20000, >20000
        String[] ranges = {"<3000", "3000-5000", "5000-8000", "8000-12000", "12000-20000", ">20000"};
        int[] thresholds = {3000, 5000, 8000, 12000, 20000};
        int[] counts = new int[ranges.length];

        for (SalaryDetail detail : details) {
            BigDecimal netPay = detail.getNetPay() != null ? detail.getNetPay() : BigDecimal.ZERO;
            int net = netPay.intValue();
            boolean placed = false;
            for (int i = 0; i < thresholds.length; i++) {
                if (net < thresholds[i]) {
                    counts[i]++;
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                counts[counts.length - 1]++;
            }
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < ranges.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("range", ranges[i]);
            item.put("count", counts[i]);
            data.add(item);
        }
        return ResultUtils.success(data);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取目标批次：指定月份或最新已通过/已发放批次
     */
    private SalaryBatch getTargetBatch(String salaryMonth) {
        QueryWrapper<SalaryBatch> wrapper = new QueryWrapper<>();
        wrapper.in("status", BatchStatusEnum.APPROVED.getValue(), BatchStatusEnum.PAID.getValue());
        if (StringUtils.isNotBlank(salaryMonth)) {
            wrapper.eq("salary_month", salaryMonth);
        }
        wrapper.orderByDesc("salary_month").last("LIMIT 1");
        return salaryBatchMapper.selectOne(wrapper);
    }

    /**
     * 获取所有可统计的月份列表（已通过/已发放的批次月份）
     */
    @GetMapping("/available-months")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<List<String>> availableMonths() {
        QueryWrapper<SalaryBatch> wrapper = new QueryWrapper<>();
        wrapper.in("status", BatchStatusEnum.APPROVED.getValue(), BatchStatusEnum.PAID.getValue())
                .orderByDesc("salary_month");
        wrapper.select("DISTINCT salary_month");
        List<SalaryBatch> batches = salaryBatchMapper.selectList(wrapper);
        List<String> months = batches.stream()
                .map(SalaryBatch::getSalaryMonth)
                .collect(Collectors.toList());
        return ResultUtils.success(months);
    }
}
