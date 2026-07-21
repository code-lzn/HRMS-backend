package com.limou.hrms.config;

import cn.hutool.core.date.DateUtil;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.HolidayConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 考勤模块定时任务
 */
@Slf4j
@Configuration
public class AttendanceScheduledTask {

    @Resource
    private AttendanceService attendanceService;

    @Resource
    private HolidayConfigService holidayConfigService;

    /**
     * 每天 19:00 日终评估：先生成所有员工当天记录，再评估打卡状态
     */
    @Scheduled(cron = "0 0 19 * * *")
    public void evaluateEndOfDayAttendance() {
        Date now = new Date();
        if (!holidayConfigService.isWorkDay(now)) {
            return;
        }
        String today = DateUtil.formatDate(now);
        log.info("定时任务: 生成当日考勤记录 date={}", today);
        try {
            int generated = attendanceService.generateDailyRecords(today);
            log.info("当日考勤记录生成完成, 新增 {} 条", generated);
        } catch (Exception e) {
            log.error("生成当日考勤记录失败 date={}", today, e);
        }

        log.info("定时任务: 日终考勤评估 date={}", today);
        try {
            int count = attendanceService.evaluateEndOfDay(today);
            log.info("日终考勤评估完成, 更新 {} 条", count);
        } catch (Exception e) {
            log.error("日终考勤评估失败 date={}", today, e);
        }
    }

    /**
     * 每 30 分钟同步考勤异常审批结果（拒绝的恢复为正常）
     */
    @Scheduled(cron = "0 */30 9-18 * * *")
    public void syncAnomalyApprovals() {
        try {
            int count = attendanceService.syncAnomalyApprovals();
            if (count > 0) {
                log.info("考勤异常审批同步完成, 恢复 {} 条", count);
            }
        } catch (Exception e) {
            log.error("考勤异常审批同步失败", e);
        }
    }
}
