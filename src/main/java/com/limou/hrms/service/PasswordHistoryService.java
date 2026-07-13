package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.PasswordHistory;

public interface PasswordHistoryService extends IService<PasswordHistory> {

    /**
     * 检查密码是否在最近3次历史中
     */
    boolean isRecentlyUsed(Long userId, String newPasswordHash);

    /**
     * 保存密码历史（保留最近3条）
     */
    void savePasswordHistory(Long userId, String passwordHash);
}
