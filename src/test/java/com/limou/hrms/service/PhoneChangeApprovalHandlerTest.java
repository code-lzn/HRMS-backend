package com.limou.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.EmployeePersonalInfoMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.EmployeePersonalInfo;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.service.impl.PhoneChangeApprovalHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 手机号变更审批回调处理器单元测试 — 覆盖 ApprovalCallback 两个回调方法
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhoneChangeApprovalHandlerTest {

    @Mock private UserMapper userMapper;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeePersonalInfoMapper employeePersonalInfoMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PhoneChangeApprovalHandler handler;

    private static final Long USER_ID = 1L;
    private static final Long EMPLOYEE_ID = 100L;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 审批通过 ====================

    /** 审批通过：从 Redis 读取新手机号 → 更新 user + personal_info → 清除 Redis */
    @Test
    void onApproved_shouldChangePhoneAndSyncPersonalInfo() {
        String unbindKey = String.format(PhoneChangeApprovalHandler.PHONE_UNBIND_KEY, USER_ID);
        when(valueOperations.get(unbindKey)).thenReturn("13900001111");

        User user = new User();
        user.setId(USER_ID); user.setUserAccount("13800001234");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.updateById(any())).thenReturn(1);

        Employee employee = new Employee();
        employee.setId(EMPLOYEE_ID); employee.setUserId(USER_ID);
        when(employeeMapper.selectByUserId(USER_ID)).thenReturn(employee);

        EmployeePersonalInfo personalInfo = new EmployeePersonalInfo();
        personalInfo.setId(1L); personalInfo.setEmployeeId(EMPLOYEE_ID); personalInfo.setPhone("13800001234");
        when(employeePersonalInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(personalInfo);
        when(employeePersonalInfoMapper.updateById(any())).thenReturn(1);

        handler.onApproved(ApprovalBizType.PHONE_CHANGE, USER_ID);

        assertEquals("13900001111", user.getUserAccount());
        assertEquals("13900001111", personalInfo.getPhone());
        verify(userMapper).updateById(user);
        verify(employeePersonalInfoMapper).updateById(personalInfo);
        verify(stringRedisTemplate).delete(unbindKey); // 清除已消费数据
    }

    /** 审批通过但 Redis 无待处理数据 → 仅告警，不执行任何更新 */
    @Test
    void onApproved_noPendingData_shouldWarnAndReturn() {
        when(valueOperations.get(anyString())).thenReturn(null);
        assertDoesNotThrow(() -> handler.onApproved(ApprovalBizType.PHONE_CHANGE, USER_ID));
        verify(userMapper, never()).updateById(any());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    /** 非 PHONE_CHANGE 类型回调 → 直接忽略 */
    @Test
    void onApproved_differentBizType_shouldIgnore() {
        handler.onApproved(ApprovalBizType.LEAVE, USER_ID);
        verify(valueOperations, never()).get(anyString());
    }

    // ==================== 审批拒绝 ====================

    /** 审批拒绝：清除 Redis 中的待审批数据 */
    @Test
    void onRejected_shouldClearPendingData() {
        handler.onRejected(ApprovalBizType.PHONE_CHANGE, USER_ID);
        verify(stringRedisTemplate).delete(String.format(PhoneChangeApprovalHandler.PHONE_UNBIND_KEY, USER_ID));
    }

    /** 非 PHONE_CHANGE 类型回调 → 直接忽略 */
    @Test
    void onRejected_differentBizType_shouldIgnore() {
        handler.onRejected(ApprovalBizType.LEAVE, USER_ID);
        verify(stringRedisTemplate, never()).delete(anyString());
    }
}
