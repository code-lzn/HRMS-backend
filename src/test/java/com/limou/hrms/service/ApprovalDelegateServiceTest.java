package com.limou.hrms.service;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.ApprovalDelegateMapper;
import com.limou.hrms.model.entity.ApprovalDelegate;
import com.limou.hrms.service.impl.ApprovalDelegateServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 委托审批服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ApprovalDelegateServiceTest {

    @Mock
    private ApprovalDelegateMapper approvalDelegateMapper;

    @InjectMocks
    private ApprovalDelegateServiceImpl service;

    private static final Long DELEGATOR = 1L;
    private static final Long DELEGATE = 2L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 7, 15, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 7, 20, 18, 0);

    /** 正常创建委托 */
    @Test
    void createDelegate_shouldSucceed() {
        // 时间不重叠检查返回 0
        when(approvalDelegateMapper.selectCount(any())).thenReturn(0L);
        when(approvalDelegateMapper.insert(any())).thenReturn(1);

        ApprovalDelegate result = service.createDelegate(DELEGATOR, DELEGATE, START, END);

        assertNotNull(result);
        assertEquals(DELEGATOR, result.getDelegatorId());
        assertEquals(DELEGATE, result.getDelegateId());
        assertEquals(Integer.valueOf(1), result.getEnabled());
    }

    /** 不能委托给自己 */
    @Test
    void createDelegate_toSelf_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createDelegate(DELEGATOR, DELEGATOR, START, END));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    /** 结束时间必须晚于开始时间 */
    @Test
    void createDelegate_endBeforeStart_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createDelegate(DELEGATOR, DELEGATE, END, START));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    /** 同一时间段不能重复委托 */
    @Test
    void createDelegate_overlapping_shouldThrow() {
        when(approvalDelegateMapper.selectCount(any())).thenReturn(1L); // 存在重叠

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createDelegate(DELEGATOR, DELEGATE, START, END));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    /** 取消委托 — 仅委托人可取消 */
    @Test
    void cancelDelegate_notOwner_shouldThrow() {
        ApprovalDelegate existing = new ApprovalDelegate();
        existing.setId(100L);
        existing.setDelegatorId(DELEGATOR); // 委托人是A
        when(approvalDelegateMapper.selectById(100L)).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancelDelegate(100L, 999L)); // B来取消
        assertEquals(ErrorCode.FORBIDDEN_ERROR.getCode(), ex.getCode());
    }

    /** 取消不存在的委托 */
    @Test
    void cancelDelegate_notFound_shouldThrow() {
        when(approvalDelegateMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancelDelegate(999L, DELEGATOR));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), ex.getCode());
    }
}
