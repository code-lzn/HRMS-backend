package com.limou.hrms.task;

import com.limou.hrms.service.RegularizationReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 转正提醒定时任务
 * 每日 08:00 扫描试用期即将到期的员工，发送转正提醒给HR
 */
@Component
@Slf4j
public class RegularizationReminderTask {

    @Resource
    private RegularizationReminderService regularizationReminderService;

    /**
     * 每日 08:00 执行
     * 扫描试用期到期前7天的员工，通过唯一索引保证幂等
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scanAndRemind() {
        log.info("===== 定时任务：转正提醒扫描 开始 =====");
        try {
            regularizationReminderService.scanAndRemind();
        } catch (Exception e) {
            log.error("转正提醒扫描执行异常", e);
        }
        log.info("===== 定时任务：转正提醒扫描 结束 =====");
    }
}
