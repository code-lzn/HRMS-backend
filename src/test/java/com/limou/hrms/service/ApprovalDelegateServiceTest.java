package com.limou.hrms.service;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.ApprovalDelegateMapper;
import com.limou.hrms.model.dto.approval.DelegateSettingDTO;
import com.limou.hrms.model.entity.ApprovalDelegate;
import com.limou.hrms.service.impl.ApprovalDelegateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 委托审批服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalDelegateServiceTest {

    @Mock
    private ApprovalDelegateMapper approvalDelegateMapper;
    @Mock
    private DataScopeContext dataScopeContext;
    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private ApprovalDelegateServiceImpl service;

    private static final Long DELEGATOR = 1L;
    private static final Long DELEGATE = 2L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 7, 15, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 7, 20, 18, 0);

    @BeforeEach
    void setUp() {
        service.setCurrentUserForTest(DELEGATOR);
        org.springframework.cache.Cache mockCache = mock(org.springframework.cache.Cache.class);
        when(cacheManager.getCache(any())).thenReturn(mockCache);
    }

    private DelegateSettingDTO buildDto(Long delegateId, LocalDateTime start, LocalDateTime end) {
        DelegateSettingDTO dto = new DelegateSettingDTO();
        dto.setDelegateId(delegateId);
        dto.setStartTime(start);
        dto.setEndTime(end);
        return dto;
    }

    @Test
    void createDelegate_shouldSucceed() {
        when(approvalDelegateMapper.selectCount(any())).thenReturn(0L);
        when(approvalDelegateMapper.insert(any())).thenReturn(1);

        ApprovalDelegate result = service.createDelegate(buildDto(DELEGATE, START, END));

        assertNotNull(result);
        assertEquals(DELEGATOR, result.getDelegatorId());
        assertEquals(DELEGATE, result.getDelegateId());
        assertEquals(Integer.valueOf(1), result.getEnabled());
    }

    @Test
    void createDelegate_toSelf_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createDelegate(buildDto(DELEGATOR, START, END)));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createDelegate_endBeforeStart_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createDelegate(buildDto(DELEGATE, END, START)));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void createDelegate_overlapping_shouldThrow() {
        when(approvalDelegateMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createDelegate(buildDto(DELEGATE, START, END)));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void cancelDelegate_notOwner_shouldThrow() {
        ApprovalDelegate existing = new ApprovalDelegate();
        existing.setId(100L);
        existing.setDelegatorId(999L); // 委托人是别人
        when(approvalDelegateMapper.selectById(100L)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancelDelegate(100L));
        assertEquals(ErrorCode.FORBIDDEN_ERROR.getCode(), ex.getCode());
    }

    @Test
    void cancelDelegate_notFound_shouldThrow() {
        when(approvalDelegateMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancelDelegate(999L));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), ex.getCode());
    }
}
