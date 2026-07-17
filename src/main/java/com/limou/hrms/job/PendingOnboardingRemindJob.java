package com.limou.hrms.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.OnboardingApplicationMapper;
import com.limou.hrms.model.entity.OnboardingApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * 待入职过期提醒定时任务 — 每天10:00扫描已批准但未入职的过期申请
 */
@Component
@Slf4j
public class PendingOnboardingRemindJob {

    @Resource
    private OnboardingApplicationMapper onboardingMapper;

    /**
     * 每天上午 10:00 执行
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void scanPendingOnboarding() {
        log.info("===== 待入职过期提醒扫描开始 =====");
        LocalDate today = LocalDate.now();
        // 状态=已批准待入职(3) 且 预计入职日期 < 今天
        List<OnboardingApplication> list = onboardingMapper.selectList(
                new QueryWrapper<OnboardingApplication>()
                        .eq("status", 3)   // 已批准待入职
                        .lt("expected_hire_date", today));

        for (OnboardingApplication app : list) {
            log.warn("【待入职过期提醒】入职申请ID={}, 候选人={}, 预计入职日期={}, 已过期{}天",
                    app.getId(), app.getName(), app.getExpectedHireDate(),
                    java.time.temporal.ChronoUnit.DAYS.between(app.getExpectedHireDate(), today));
            // TODO: MQ通知HR和部门负责人
        }
        log.info("===== 待入职过期提醒扫描结束，共 {} 条过期申请 =====", list.size());
    }
}
