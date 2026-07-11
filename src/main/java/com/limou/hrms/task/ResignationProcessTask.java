package com.limou.hrms.task;

import com.limou.hrms.service.ResignationApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 离职生效处理定时任务
 * 每日 00:05 扫描待离职到期员工，执行离职生效处理
 */
@Component
@Slf4j
public class ResignationProcessTask {

    @Resource
    private ResignationApplicationService resignationApplicationService;

    /**
     * 每日 00:05 执行
     * 扫描 status=2(已通过待离职) 且 leave_date<=今天 的员工
     * 执行：状态变更、账号禁用、工号释放、考勤移除、字段脱敏
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void processResignations() {
        log.info("===== 定时任务：离职生效处理 开始 =====");
        try {
            resignationApplicationService.processPendingResignations();
        } catch (Exception e) {
            log.error("离职生效处理执行异常", e);
        }
        log.info("===== 定时任务：离职生效处理 结束 =====");
    }
}
