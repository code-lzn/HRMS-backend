package com.limou.hrms.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.OnboardingApplicationMapper;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.OnboardingApplication;
import com.limou.hrms.model.enums.EmployeeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * 转正提醒定时任务 — 每天09:00扫描试用期即将到期员工
 */
@Component
@Slf4j
public class ProbationRemindJob {

    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private OnboardingApplicationMapper onboardingMapper;

    /**
     * 每天上午 9:00 执行
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void scanProbationRemind() {
        log.info("===== 转正提醒扫描开始 =====");
        LocalDate today = LocalDate.now();
        int remindDays = 7;
        List<Employee> probationEmployees = employeeMapper.selectList(
                new QueryWrapper<Employee>().eq("status", EmployeeStatus.PROBATION.getValue()));
        int count = 0;

        for (Employee emp : probationEmployees) {
            OnboardingApplication oa = onboardingMapper.selectOne(
                    new QueryWrapper<OnboardingApplication>()
                            .eq("employee_id", emp.getId())
                            .eq("status", 4)); // 已入职
            Integer probationMonths = (oa != null && oa.getDefaultProbationMonths() != null)
                    ? oa.getDefaultProbationMonths() : 3;
            LocalDate probationEnd = emp.getHireDate().plusMonths(probationMonths);
            LocalDate remindDate = probationEnd.minusDays(remindDays);

            if (!today.isBefore(remindDate) && !today.isAfter(probationEnd)) {
                log.info("【转正提醒】员工 {} (工号{}) 试用期将于 {} 结束，距到期 {} 天",
                        emp.getEmployeeNo(), emp.getEmployeeNo(), probationEnd,
                        java.time.temporal.ChronoUnit.DAYS.between(today, probationEnd));
                count++;
                // TODO: 发送MQ通知给HR
            }
        }
        log.info("===== 转正提醒扫描结束，本次提醒 {} 人 =====", count);
    }
}
