package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.EmployeePersonalInfoMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.EmployeePersonalInfo;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 手机号变更审批回调处理器
 * <p>
 * 负责接收审批结果，执行或清除待处理的手机号变更。
 * 审批发起逻辑见 {@link ProfileServiceImpl#submitPhoneUnbind}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhoneChangeApprovalHandler implements ApprovalCallback {

    /** 手机解绑审批待处理数据 Key：phone:unbind:{userId} */
    public static final String PHONE_UNBIND_KEY = "phone:unbind:%d";

    private final UserMapper userMapper;
    private final EmployeeMapper employeeMapper;
    private final EmployeePersonalInfoMapper employeePersonalInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.PHONE_CHANGE) {
            return;
        }
        Long userId = bizId;
        String unbindKey = String.format(PHONE_UNBIND_KEY, userId);
        String newPhone = stringRedisTemplate.opsForValue().get(unbindKey);
        if (newPhone == null) {
            log.warn("手机号变更审批通过，但 Redis 无待处理数据，userId={}", userId);
            return;
        }

        // 更新 user 登录账号
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setUserAccount(newPhone);
            userMapper.updateById(user);
        }

        // 同步 personal_info
        Employee employee = employeeMapper.selectByUserId(userId);
        if (employee != null) {
            EmployeePersonalInfo personalInfo = employeePersonalInfoMapper.selectOne(
                    new QueryWrapper<EmployeePersonalInfo>().eq("employee_id", employee.getId()));
            if (personalInfo != null) {
                personalInfo.setPhone(newPhone);
                employeePersonalInfoMapper.updateById(personalInfo);
            }
        }

        stringRedisTemplate.delete(unbindKey);
        log.info("手机号变更审批通过，用户 {} → {}", userId, SensitiveDataUtil.maskPhone(newPhone));
    }

    @Override
    public void onRejected(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.PHONE_CHANGE) {
            return;
        }
        stringRedisTemplate.delete(String.format(PHONE_UNBIND_KEY, bizId));
        log.info("手机号变更审批被拒绝，用户 {} 待处理数据已清除", bizId);
    }
}
