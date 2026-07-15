package com.limou.hrms.builder;

import com.limou.hrms.mapper.LeaveRequestMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.LeaveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 请假 Builder 单元测试 — 验证 PRD 6.3.4 所有路由规则
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeaveNodeBuilderTest {

    @Mock
    private LeaveRequestMapper leaveRequestMapper;
    @Mock
    private ApproverResolver approverResolver;

    @InjectMocks
    private LeaveNodeBuilder builder;

    private static final Long BIZ_ID = 1L;
    private static final Long APPLICANT_ID = 100L;
    private static final Long DIRECT_LEADER_ID = 200L;
    private static final Long DEPT_MANAGER_ID = 300L;
    private static final Long DEPT_ID = 10L;

    @BeforeEach
    void setUp() {
        when(approverResolver.resolveDirectLeader(APPLICANT_ID)).thenReturn(DIRECT_LEADER_ID);
        when(approverResolver.resolveDepartmentId(APPLICANT_ID)).thenReturn(DEPT_ID);
        when(approverResolver.resolveDeptManager(DEPT_ID)).thenReturn(DEPT_MANAGER_ID);
    }

    /** 年假 3天 → 仅直接上级 */
    @Test
    void annualLeave3days_shouldBeOneNode() {
        LeaveRequest req = buildRequest(1, "3");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(1, nodes.size());
        assertEquals("直接上级审批", nodes.get(0).getNodeName());
        assertEquals(DIRECT_LEADER_ID, nodes.get(0).getApproverId());
    }

    /** 年假 4天 → 直接上级 + 部门负责人 */
    @Test
    void annualLeave4days_shouldBeTwoNodes() {
        LeaveRequest req = buildRequest(1, "4");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(2, nodes.size());
        assertEquals("直接上级审批", nodes.get(0).getNodeName());
        assertEquals("部门负责人审批", nodes.get(1).getNodeName());
        assertEquals(DEPT_MANAGER_ID, nodes.get(1).getApproverId());
    }

    /** 调休 2天 → 仅直接上级（调休与年假同规则） */
    @Test
    void compTimeLeave2days_shouldBeOneNode() {
        LeaveRequest req = buildRequest(7, "2");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(1, nodes.size());
    }

    /** 病假 1天 → 仅直接上级 */
    @Test
    void sickLeave1day_shouldBeOneNode() {
        LeaveRequest req = buildRequest(2, "1");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(1, nodes.size());
    }

    /** 病假 2天 → 直接上级 + 部门负责人 */
    @Test
    void sickLeave2days_shouldBeTwoNodes() {
        LeaveRequest req = buildRequest(2, "2");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(2, nodes.size());
        assertEquals("部门负责人审批", nodes.get(1).getNodeName());
    }

    /** 事假 0.5天 → 仅直接上级 */
    @Test
    void personalLeaveHalfDay_shouldBeOneNode() {
        LeaveRequest req = buildRequest(3, "0.5");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(1, nodes.size());
    }

    /** 事假 2天 → 直接上级 + 部门负责人 */
    @Test
    void personalLeave2days_shouldBeTwoNodes() {
        LeaveRequest req = buildRequest(3, "2");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(2, nodes.size());
    }

    /** 婚假 → 直接上级 + HR备案 */
    @Test
    void marriageLeave_shouldBeDirectLeaderPlusHrRecord() {
        LeaveRequest req = buildRequest(4, "3");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);
        Long hrId = 400L;
        when(approverResolver.resolveHrApprover()).thenReturn(hrId);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(2, nodes.size());
        assertEquals("直接上级审批", nodes.get(0).getNodeName());
        assertEquals("HR备案", nodes.get(1).getNodeName());
        assertEquals(hrId, nodes.get(1).getApproverId());
    }

    /** 产假 → 直接上级 + HR备案 */
    @Test
    void maternityLeave_shouldBeDirectLeaderPlusHrRecord() {
        LeaveRequest req = buildRequest(5, "98");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);
        when(approverResolver.resolveHrApprover()).thenReturn(500L);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(2, nodes.size());
        assertEquals("HR备案", nodes.get(1).getNodeName());
    }

    /** 丧假 → 直接上级 + HR备案 */
    @Test
    void funeralLeave_shouldBeDirectLeaderPlusHrRecord() {
        LeaveRequest req = buildRequest(6, "3");
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(req);
        when(approverResolver.resolveHrApprover()).thenReturn(600L);

        List<ApprovalNode> nodes = builder.build(BIZ_ID, APPLICANT_ID);

        assertEquals(2, nodes.size());
        assertEquals("HR备案", nodes.get(1).getNodeName());
    }

    /** 申请不存在 → 抛异常 */
    @Test
    void applicationNotFound_shouldThrow() {
        when(leaveRequestMapper.selectById(BIZ_ID)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> builder.build(BIZ_ID, APPLICANT_ID));
    }

    // ==================== 辅助方法 ====================

    private LeaveRequest buildRequest(int leaveType, String leaveDays) {
        LeaveRequest req = new LeaveRequest();
        req.setId(BIZ_ID);
        req.setEmployeeId(APPLICANT_ID);
        req.setLeaveType(leaveType);
        req.setLeaveDays(new BigDecimal(leaveDays));
        return req;
    }
}
