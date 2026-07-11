package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.RegularizationReminderMapper;
import com.limou.hrms.model.entity.RegularizationReminder;
import com.limou.hrms.service.RegularizationReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 转正提醒服务实现
 */
@Service
@Slf4j
public class RegularizationReminderServiceImpl
        extends ServiceImpl<RegularizationReminderMapper, RegularizationReminder>
        implements RegularizationReminderService {

    @Override
    public void scanAndRemind() {
        log.info("===== 开始扫描转正提醒 =====");

        // todo: 扫描试用期即将到期的员工
        // SELECT * FROM employee e
        // WHERE e.status = 1 (试用期)
        // AND DATE_ADD(e.hire_date, INTERVAL e.probation_months MONTH) - INTERVAL 7 DAY <= CURDATE()
        // AND NOT EXISTS (
        //   SELECT 1 FROM regularization_reminder r
        //   WHERE r.employee_id = e.id AND r.remind_date = CURDATE()
        // )
        //
        // 对每个符合条件的员工，通过唯一索引(employee_id, remind_date)保证幂等：
        // INSERT INTO regularization_reminder (employee_id, remind_date) VALUES (?, CURDATE())

        // 示例：插入提醒记录（唯一索引保证幂等）
        LocalDate today = LocalDate.now();
        log.info("转正提醒扫描完成, date={}", today);
        // todo: 发送通知给HR（站内信/邮件）
    }
}
