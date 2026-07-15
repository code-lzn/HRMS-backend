package com.limou.hrms.service;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalStatus;
import com.limou.hrms.model.enums.NodeStatus;
import com.limou.hrms.service.impl.ApprovalFlowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 审批引擎核心流程单元测试 — 验证状态机、权限校验、异常边界
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalFlowServiceTest {

    @Mock private ApprovalInstanceMapper instanceMapper;
    @Mock private ApprovalNodeMapper nodeMapper;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private ApprovalDelegateService delegateService;
    @Mock private CacheManager cacheManager;

    @InjectMocks
    private ApprovalFlowServiceImpl service;

    private ApprovalNode pendingNode;
    private ApprovalInstance pendingInstance;
    private Employee employee;

    private static final Long NODE_ID = 1L;
    private static final Long INSTANCE_ID = 100L;
    private static final Long APPROVER_ID = 10L;
    private static final Long APPLICANT_ID = 20L;

    @BeforeEach
    void setUp() {
        // Mock cacheManager for createInstance
        when(cacheManager.getCache(any())).thenReturn(null);
        // 默认无委托路由
        when(delegateService.resolveApprover(anyLong())).thenAnswer(inv -> inv.getArgument(0));

        // 待审批节点
        pendingNode = new ApprovalNode();
        pendingNode.setId(NODE_ID);
        pendingNode.setInstanceId(INSTANCE_ID);
        pendingNode.setApproverId(APPROVER_ID);
        pendingNode.setStatus(NodeStatus.PENDING.getCode());
        pendingNode.setNodeOrder(1);

        // 审批中实例
        pendingInstance = new ApprovalInstance();
        pendingInstance.setId(INSTANCE_ID);
        pendingInstance.setStatus(ApprovalStatus.PENDING.getCode());
        pendingInstance.setApplicantId(APPLICANT_ID);
        pendingInstance.setBizType("ONBOARDING");
        pendingInstance.setCurrentNodeOrder(1);

        // 操作用户
        employee = new Employee();
        employee.setId(APPROVER_ID);
    }

    // ==================== approve ====================

    /** 正常审批通过 */
    @Test
    void approve_shouldSucceed() {
        when(nodeMapper.selectById(NODE_ID)).thenReturn(pendingNode);
        when(instanceMapper.selectById(INSTANCE_ID)).thenReturn(pendingInstance);
        // 总节点数 > 当前 order → 非最后节点
        when(nodeMapper.selectCount(any())).thenReturn(2L);
        when(nodeMapper.updateById(any())).thenReturn(1);
        when(instanceMapper.updateById(any())).thenReturn(1);

        assertDoesNotThrow(() -> service.approve(NODE_ID, APPROVER_ID, "同意"));
    }

    /** 节点不存在 → 40002 */
    @Test
    void approve_nodeNotFound_shouldThrow() {
        when(nodeMapper.selectById(NODE_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(NODE_ID, APPROVER_ID, "同意"));
        assertEquals(ErrorCode.APPROVAL_NODE_NOT_FOUND.getCode(), ex.getCode());
    }

    /** 非当前审批人操作 → 40003 */
    @Test
    void approve_notOwner_shouldThrow() {
        when(nodeMapper.selectById(NODE_ID)).thenReturn(pendingNode);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(NODE_ID, 999L, "越权"));
        assertEquals(ErrorCode.APPROVAL_NODE_NOT_OWNER.getCode(), ex.getCode());
    }

    /** 已处理的节点再次操作 → 40004 */
    @Test
    void approve_alreadyHandled_shouldThrow() {
        pendingNode.setStatus(NodeStatus.APPROVED.getCode());
        when(nodeMapper.selectById(NODE_ID)).thenReturn(pendingNode);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(NODE_ID, APPROVER_ID, "重复"));
        assertEquals(ErrorCode.APPROVAL_NODE_ALREADY_HANDLED.getCode(), ex.getCode());
    }

    /** 超时节点操作 → 40011 */
    @Test
    void approve_timeout_shouldThrow() {
        pendingNode.setStatus(NodeStatus.TIMEOUT.getCode());
        when(nodeMapper.selectById(NODE_ID)).thenReturn(pendingNode);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.approve(NODE_ID, APPROVER_ID, "超时了"));
        assertEquals(ErrorCode.APPROVAL_NODE_TIMEOUT.getCode(), ex.getCode());
    }

    /** 最后节点通过 → 实例完成 + 回调 */
    @Test
    void approve_lastNode_shouldCompleteInstance() {
        when(nodeMapper.selectById(NODE_ID)).thenReturn(pendingNode);
        when(instanceMapper.selectById(INSTANCE_ID)).thenReturn(pendingInstance);
        when(nodeMapper.selectCount(any())).thenReturn(1L); // 只有1个节点
        when(nodeMapper.updateById(any())).thenReturn(1);
        when(instanceMapper.updateById(any())).thenReturn(1);

        service.approve(NODE_ID, APPROVER_ID, "同意");

        assertEquals(ApprovalStatus.APPROVED.getCode(), pendingInstance.getStatus());
    }

    // ==================== reject ====================

    /** 拒绝必须填意见 */
    @Test
    void reject_blankComment_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reject(NODE_ID, APPROVER_ID, ""));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());

        assertThrows(BusinessException.class,
                () -> service.reject(NODE_ID, APPROVER_ID, null));
    }

    /** 正常拒绝 → 实例拒绝 + 节点拒绝 */
    @Test
    void reject_shouldUpdateBoth() {
        when(nodeMapper.selectById(NODE_ID)).thenReturn(pendingNode);
        when(instanceMapper.selectById(INSTANCE_ID)).thenReturn(pendingInstance);
        when(nodeMapper.updateById(any())).thenReturn(1);
        when(instanceMapper.updateById(any())).thenReturn(1);

        service.reject(NODE_ID, APPROVER_ID, "不合适");

        assertEquals(NodeStatus.REJECTED.getCode(), pendingNode.getStatus());
        assertEquals(ApprovalStatus.REJECTED.getCode(), pendingInstance.getStatus());
    }

    // ==================== transfer ====================

    /** 不能转给自己 */
    @Test
    void transfer_toSelf_shouldThrow() {
        when(nodeMapper.selectById(NODE_ID)).thenReturn(pendingNode);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.transfer(NODE_ID, APPROVER_ID, APPROVER_ID));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    /** 目标审批人不存在 */
    @Test
    void transfer_targetNotExist_shouldThrow() {
        when(nodeMapper.selectById(NODE_ID)).thenReturn(pendingNode);
        when(employeeMapper.selectById(30L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.transfer(NODE_ID, APPROVER_ID, 30L));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    /** 正常转交 → 记录原审批人 + 更换审批人 + 重置为待审批 */
    @Test
    void transfer_shouldSucceed() {
        when(nodeMapper.selectById(NODE_ID)).thenReturn(pendingNode);
        Employee target = new Employee();
        target.setId(30L);
        when(employeeMapper.selectById(30L)).thenReturn(target);
        when(nodeMapper.updateById(any())).thenReturn(1);

        service.transfer(NODE_ID, APPROVER_ID, 30L);

        assertEquals(APPROVER_ID, pendingNode.getOriginalApproverId()); // 原审批人记录
        assertEquals(30L, pendingNode.getApproverId());                  // 新审批人
        assertEquals(NodeStatus.PENDING.getCode(), pendingNode.getStatus()); // 重置待审批
    }

    // ==================== cancel ====================

    /** 非申请人不能撤回 */
    @Test
    void cancel_notApplicant_shouldThrow() {
        when(instanceMapper.selectById(INSTANCE_ID)).thenReturn(pendingInstance);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancel(INSTANCE_ID, 999L));
        assertEquals(ErrorCode.APPROVAL_CANCEL_ONLY_FIRST_NODE.getCode(), ex.getCode());
    }

    /** 已通过的实例不能撤回 */
    @Test
    void cancel_alreadyApproved_shouldThrow() {
        pendingInstance.setStatus(ApprovalStatus.APPROVED.getCode());
        when(instanceMapper.selectById(INSTANCE_ID)).thenReturn(pendingInstance);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancel(INSTANCE_ID, APPLICANT_ID));
        assertEquals(ErrorCode.APPROVAL_NODE_ALREADY_HANDLED.getCode(), ex.getCode());
    }

    /** 非第一节点不能撤回 */
    @Test
    void cancel_notFirstNode_shouldThrow() {
        pendingInstance.setCurrentNodeOrder(2);
        when(instanceMapper.selectById(INSTANCE_ID)).thenReturn(pendingInstance);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.cancel(INSTANCE_ID, APPLICANT_ID));
        assertEquals(ErrorCode.APPROVAL_CANCEL_ONLY_FIRST_NODE.getCode(), ex.getCode());
    }

    /** 正常撤回 */
    @Test
    void cancel_shouldSucceed() {
        when(instanceMapper.selectById(INSTANCE_ID)).thenReturn(pendingInstance);
        when(instanceMapper.updateById(any())).thenReturn(1);

        service.cancel(INSTANCE_ID, APPLICANT_ID);

        assertEquals(ApprovalStatus.CANCELLED.getCode(), pendingInstance.getStatus());
    }
}
