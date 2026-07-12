package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.PasswordHistoryMapper;
import com.limou.hrms.model.entity.PasswordHistory;
import com.limou.hrms.service.PasswordHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PasswordHistoryServiceImpl extends ServiceImpl<PasswordHistoryMapper, PasswordHistory>
        implements PasswordHistoryService {

    private static final int MAX_HISTORY = 3;

    @Override
    public boolean isRecentlyUsed(Long userId, String newPasswordHash) {
        List<PasswordHistory> histories = this.lambdaQuery()
                .eq(PasswordHistory::getUserId, userId)
                .orderByDesc(PasswordHistory::getCreateTime)
                .last("LIMIT " + MAX_HISTORY)
                .list();

        return histories.stream()
                .anyMatch(h -> Objects.equals(h.getPasswordHash(), newPasswordHash));
    }

    @Override
    public void savePasswordHistory(Long userId, String passwordHash) {
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        this.save(history);

        // 保留最近3条，删除多余的旧记录
        List<PasswordHistory> all = this.lambdaQuery()
                .eq(PasswordHistory::getUserId, userId)
                .orderByDesc(PasswordHistory::getCreateTime)
                .list();

        if (all.size() > MAX_HISTORY) {
            List<Long> toDelete = all.stream()
                    .skip(MAX_HISTORY)
                    .map(PasswordHistory::getId)
                    .collect(Collectors.toList());
            this.removeByIds(toDelete);
        }
    }
}
