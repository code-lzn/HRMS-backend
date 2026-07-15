package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数据分析
 */
@RestController
@RequestMapping("/analytics")
@Slf4j
public class AnalyticsController {

    /**
     * 获取核心运营数据汇总
     */
    @GetMapping("/summary")
    public BaseResponse<AnalyticsSummaryVO> getSummary(@RequestParam(defaultValue = "7days") String range) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long base = "today".equals(range) ? 50 : "7days".equals(range) ? 350 : 1500;

        AnalyticsSummaryVO vo = new AnalyticsSummaryVO();
        vo.setTotalUsers(base + r.nextLong(-base / 10, base / 10));
        vo.setTotalUsersChange(randomChange());
        vo.setActiveUsers(base / 3 + r.nextLong(-base / 30, base / 30));
        vo.setActiveUsersChange(randomChange());
        vo.setTotalRevenue(BigDecimal.valueOf(r.nextDouble(base * 10, base * 100))
                .setScale(2, RoundingMode.HALF_UP));
        vo.setTotalRevenueChange(randomChange());
        vo.setAvgConversionRate(BigDecimal.valueOf(r.nextDouble(1.5, 8.0))
                .setScale(1, RoundingMode.HALF_UP));
        vo.setAvgConversionRateChange(randomChange());
        vo.setRetentionRate(BigDecimal.valueOf(r.nextDouble(60.0, 95.0))
                .setScale(1, RoundingMode.HALF_UP));
        vo.setRetentionRateChange(randomChange());
        return ResultUtils.success(vo);
    }

    /**
     * 获取用户增长与收入趋势（折线图）
     */
    @GetMapping("/growth-trend")
    public BaseResponse<List<GrowthTrendVO>> getGrowthTrend(@RequestParam(defaultValue = "7days") String range) {
        int days = resolveDays(range);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        LocalDate today = LocalDate.now();

        List<GrowthTrendVO> list = new ArrayList<>();
        long users = 800;
        BigDecimal revenue = BigDecimal.valueOf(50000);
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            users += r.nextLong(-30, 80);
            if (users < 100) users = 100;
            revenue = revenue.add(BigDecimal.valueOf(r.nextDouble(-2000, 5000)))
                    .setScale(2, RoundingMode.HALF_UP);
            if (revenue.compareTo(BigDecimal.ZERO) < 0) revenue = BigDecimal.valueOf(1000);
            list.add(new GrowthTrendVO(date, users, revenue));
        }
        return ResultUtils.success(list);
    }

    /**
     * 获取用户来源分布（环形图）
     */
    @GetMapping("/source-distribution")
    public BaseResponse<List<SourceDistributionVO>> getSourceDistribution(@RequestParam(defaultValue = "7days") String range) {
        long multiplier = "today".equals(range) ? 1L : "7days".equals(range) ? 7L : 30L;
        ThreadLocalRandom r = ThreadLocalRandom.current();

        List<SourceDistributionVO> list = Arrays.asList(
                new SourceDistributionVO("直接访问", r.nextLong(100, 500) * multiplier, BigDecimal.ZERO),
                new SourceDistributionVO("社交媒体", r.nextLong(50, 300) * multiplier, BigDecimal.ZERO),
                new SourceDistributionVO("搜索引擎", r.nextLong(80, 400) * multiplier, BigDecimal.ZERO),
                new SourceDistributionVO("邮件营销", r.nextLong(20, 150) * multiplier, BigDecimal.ZERO),
                new SourceDistributionVO("外部链接", r.nextLong(30, 200) * multiplier, BigDecimal.ZERO)
        );

        long total = list.stream().mapToLong(SourceDistributionVO::getCount).sum();
        for (SourceDistributionVO vo : list) {
            vo.setPercentage(BigDecimal.valueOf(vo.getCount() * 100.0 / total)
                    .setScale(1, RoundingMode.HALF_UP));
        }
        return ResultUtils.success(list);
    }

    /**
     * 获取各渠道转化率（柱状图）
     */
    @GetMapping("/conversion-rate")
    public BaseResponse<List<ConversionRateVO>> getConversionRate(@RequestParam(defaultValue = "7days") String range) {
        long multiplier = "today".equals(range) ? 1L : "7days".equals(range) ? 7L : 30L;
        ThreadLocalRandom r = ThreadLocalRandom.current();

        List<ConversionRateVO> list = Arrays.asList(
                buildChannel("直接访问", 500 * multiplier, r),
                buildChannel("社交媒体", 400 * multiplier, r),
                buildChannel("搜索引擎", 600 * multiplier, r),
                buildChannel("邮件营销", 200 * multiplier, r),
                buildChannel("外部链接", 300 * multiplier, r)
        );
        return ResultUtils.success(list);
    }

    private ConversionRateVO buildChannel(String name, long exposure, ThreadLocalRandom r) {
        long conversion = r.nextLong(exposure / 10, exposure / 2);
        BigDecimal rate = BigDecimal.valueOf(conversion * 100.0 / exposure)
                .setScale(1, RoundingMode.HALF_UP);
        return new ConversionRateVO(name, exposure, conversion, rate);
    }

    private BigDecimal randomChange() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return BigDecimal.valueOf(r.nextDouble(-15.0, 25.0))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private int resolveDays(String range) {
        switch (range) {
            case "today": return 1;
            case "30days": return 30;
            default: return 7;
        }
    }
}
