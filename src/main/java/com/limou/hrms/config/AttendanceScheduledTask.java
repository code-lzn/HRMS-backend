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
     * 每天 9:05 生成当日考勤记录（由 isWorkDay 判断是否真的需要生成）
     */
    @Scheduled(cron = "0 5 9 * * *")
    public void generateDailyAttendanceRecords() {
        Date now = new Date();
        if (!holidayConfigService.isWorkDay(now)) {
            return;
        }
        String today = DateUtil.formatDate(now);
        log.info("定时任务: 生成当日考勤记录 date={}", today);
        try {
            int count = attendanceService.generateDailyRecords(today);
            log.info("当日考勤记录生成完成, 新增 {} 条", count);
        } catch (Exception e) {
            log.error("生成当日考勤记录失败 date={}", today, e);
        }

        // 修正已生成记录中因考勤规则不同而被误标为迟到的记录
        try {
            int corrected = attendanceService.correctTodayLateStatus();
            if (corrected > 0) {
                log.info("当日考勤记录迟到修正完成, 修正 {} 条", corrected);
            }
        } catch (Exception e) {
            log.error("当日考勤记录迟到修正失败 date={}", today, e);
        }
    }

    /**
     * 每天 18:30 日终评估（由 isWorkDay 判断是否真的需要评估）
     */
    @Scheduled(cron = "0 30 18 * * *")
    public void evaluateEndOfDayAttendance() {
        Date now = new Date();
        if (!holidayConfigService.isWorkDay(now)) {
            return;
        }
        String today = DateUtil.formatDate(now);
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
