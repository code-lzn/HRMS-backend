package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.RegularizationReminder;

/**
 * 转正提醒服务
 */
public interface RegularizationReminderService extends IService<RegularizationReminder> {

    /**
     * 扫描即将到期的试用期员工并发送转正提醒
     * 每日08:00定时执行，通过唯一索引保证幂等性
     */
    void scanAndRemind();
}
