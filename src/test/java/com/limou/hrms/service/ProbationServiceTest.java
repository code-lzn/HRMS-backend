package com.limou.hrms.service;

import com.limou.hrms.builder.ApproverResolver;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.vo.PendingEmployeeVO;
import com.limou.hrms.model.dto.probation.ProbationCreateDTO;
import com.limou.hrms.model.dto.probation.ProbationHandleResultDTO;
import com.limou.hrms.model.dto.probation.ProbationUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.ProbationResult;
import com.limou.hrms.model.vo.ProbationDetailVO;
import com.limou.hrms.service.impl.ProbationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 转正管理服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProbationServiceTest {

    @Mock private ProbationApplicationMapper probationMapper;
    @Mock private OnboardingApplicationMapper onboardingMapper;
    @Mock private ResignationApplicationMapper resignationMapper;
    @Mock private ApprovalFlowService approvalFlowService;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeeWorkInfoMapper workInfoMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private PositionMapper positionMapper;
    @Mock private DataScopeContext dataScopeContext;
    @Mock private ApproverResolver approverResolver;

    @InjectMocks
    private ProbationServiceImpl service;

    private static final Long APP_ID = 1L;
    private static final Long EMPLOYEE_ID = 100L;
    private static final Long APPLICANT_ID = 10L;

    @BeforeEach
    void setUp() {
        when(dataScopeContext.getCurrentEmployeeId()).thenReturn(APPLICANT_ID);
        when(dataScopeContext.getCurrentRole()).thenReturn("hr");
        when(dataScopeContext.getApprovalScope()).thenReturn(DataScopeEnum.SELF);
        when(approverResolver.getEmployeeName(anyLong())).thenReturn("测试姓名");

        doAnswer(inv -> { ProbationApplication a = inv.getArgument(0); a.setId(APP_ID); return 1; })
                .when(probationMapper).insert(any(ProbationApplication.class));
    }

    private ProbationCreateDTO buildCreateDTO() {
        ProbationCreateDTO dto = new ProbationCreateDTO();
        dto.setEmployeeId(EMPLOYEE_ID);
        dto.setPerformanceReview("表现优异");
        dto.setSubmitDirectly(false);
        return dto;
    }

    private Employee mockEmployee() {
        Employee e = new Employee();
        e.setId(EMPLOYEE_ID);
        e.setStatus(EmployeeStatus.PROBATION.getValue());
        e.setEmployeeNo("202601005");
        return e;
    }

    private ProbationApplication mockApp() {
        ProbationApplication app = new ProbationApplication();
        app.setId(APP_ID);
        app.setEmployeeId(EMPLOYEE_ID);
        app.setPerformanceReview("表现优异");
        app.setStatus(1); // 草稿
        app.setApplicantId(APPLICANT_ID);
        return app;
    }

    // ==================== 创建申请 ====================

    /** 保存为草稿，自动带入试用期信息 */
    @Test
    void createApplication_draft_shouldSucceed() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(mockEmployee());

        Long id = service.createApplication(buildCreateDTO());

        assertNotNull(id);
    }

    /** 直接提交审批：创建转正申请时应创建审批实例 */
    @Test
    void createApplication_submitDirectly_shouldCallSubmit() {
        ProbationCreateDTO dto = buildCreateDTO();
        dto.setSubmitDirectly(true);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(mockEmployee());
        ProbationApplication app = mockApp();
        when(probationMapper.selectById(anyLong())).thenReturn(app);
        ApprovalInstance mockInstance = new ApprovalInstance();
        mockInstance.setId(200L);
        when(approvalFlowService.createInstance(any(), anyLong(), anyLong()))
                .thenReturn(mockInstance);
        when(probationMapper.updateById(any())).thenReturn(1);

        service.createApplication(dto);

        verify(approvalFlowService, times(1)).createInstance(any(), anyLong(), anyLong());
    }

    /** 非试用期员工发起转正应抛异常 */
    @Test
    void createApplication_notProbation_shouldThrow() {
        Employee e = mockEmployee();
        e.setStatus(EmployeeStatus.REGULAR.getValue());
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(e);

        assertThrows(BusinessException.class, () -> service.createApplication(buildCreateDTO()));
    }

    // ==================== 提交审批 ====================

    /** 提交审批：状态→审批中，关联审批实例 */
    @Test
    void submitToApproval_shouldSucceed() {
        ProbationApplication app = mockApp();
        when(probationMapper.selectById(APP_ID)).thenReturn(app);
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(200L);
        when(approvalFlowService.createInstance(any(), anyLong(), anyLong())).thenReturn(instance);
        when(probationMapper.updateById(any())).thenReturn(1);

        service.submitToApproval(APP_ID);

        assertEquals(2, app.getStatus()); // 审批中
        assertEquals(200L, app.getApprovalInstanceId());
    }

    /** 非草稿状态提交应抛异常 */
    @Test
    void submitToApproval_nonDraft_shouldThrow() {
        ProbationApplication app = mockApp();
        app.setStatus(2); // 已审批中
        when(probationMapper.selectById(APP_ID)).thenReturn(app);

        assertThrows(BusinessException.class, () -> service.submitToApproval(APP_ID));
    }

    // ==================== 撤回 ====================

    /** 撤回：状态回退草稿，清空审批实例ID */
    @Test
    void cancel_shouldSucceed() {
        ProbationApplication app = mockApp();
        app.setStatus(2);
        app.setApprovalInstanceId(200L);
        when(probationMapper.selectById(APP_ID)).thenReturn(app);
        when(probationMapper.updateById(any())).thenReturn(1);

        service.cancel(APP_ID);

        assertEquals(1, app.getStatus()); // 回退草稿
        assertNull(app.getApprovalInstanceId());
        verify(approvalFlowService, times(1)).cancel(200L);
    }

    // ==================== 处理结果 ====================

    /** 处理结果-通过：员工→正式，申请→已完成 */
    @Test
    void handleResult_pass_shouldUpdateEmployee() {
        ProbationApplication app = mockApp();
        app.setStatus(4); // 已拒绝
        when(probationMapper.selectById(APP_ID)).thenReturn(app);
        Employee e = mockEmployee();
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(e);
        when(employeeMapper.updateById(any())).thenReturn(1);
        when(probationMapper.updateById(any())).thenReturn(1);

        ProbationHandleResultDTO dto = new ProbationHandleResultDTO();
        dto.setResult(ProbationResult.PASS.getCode());
        service.handleResult(APP_ID, dto);

        assertEquals(ProbationResult.PASS.getCode(), app.getResult());
        assertEquals(EmployeeStatus.REGULAR.getValue(), e.getStatus());
    }

    /** 处理结果-延长试用：记录延长后的试用期结束日期 */
    @Test
    void handleResult_extend_shouldSetExtendedDate() {
        ProbationApplication app = mockApp();
        app.setStatus(4);
        when(probationMapper.selectById(APP_ID)).thenReturn(app);
        when(probationMapper.updateById(any())).thenReturn(1);

        ProbationHandleResultDTO dto = new ProbationHandleResultDTO();
        dto.setResult(ProbationResult.EXTEND.getCode());
        dto.setExtendedEndDate(java.time.LocalDate.of(2026, 10, 1));
        service.handleResult(APP_ID, dto);

        assertEquals(ProbationResult.EXTEND.getCode(), app.getResult());
        assertNotNull(app.getExtendedEndDate());
    }

    /** 处理结果-延长缺少日期应抛异常 */
    @Test
    void handleResult_extendWithoutDate_shouldThrow() {
        ProbationApplication app = mockApp();
        app.setStatus(4);
        when(probationMapper.selectById(APP_ID)).thenReturn(app);

        ProbationHandleResultDTO dto = new ProbationHandleResultDTO();
        dto.setResult(ProbationResult.EXTEND.getCode());

        assertThrows(BusinessException.class, () -> service.handleResult(APP_ID, dto));
    }

    /** 非已拒绝状态调用处理结果应抛异常 */
    @Test
    void handleResult_notRejected_shouldThrow() {
        ProbationApplication app = mockApp();
        app.setStatus(2); // 审批中
        when(probationMapper.selectById(APP_ID)).thenReturn(app);

        ProbationHandleResultDTO dto = new ProbationHandleResultDTO();
        dto.setResult(ProbationResult.PASS.getCode());

        assertThrows(BusinessException.class, () -> service.handleResult(APP_ID, dto));
    }

    // ==================== 审批回调 ====================

    /** 审批通过回调：员工→正式，记录PASS结果 */
    @Test
    void onApproved_shouldMakeEmployeeRegular() {
        ProbationApplication app = mockApp();
        when(probationMapper.selectById(APP_ID)).thenReturn(app);
        Employee e = mockEmployee();
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(e);
        when(employeeMapper.updateById(any())).thenReturn(1);
        when(probationMapper.updateById(any())).thenReturn(1);

        service.onApproved(ApprovalBizType.PROBATION, APP_ID);

        assertEquals(EmployeeStatus.REGULAR.getValue(), e.getStatus());
        assertEquals(ProbationResult.PASS.getCode(), app.getResult());
        assertEquals(3, app.getStatus()); // 已完成
    }

    /** 审批拒绝回调：状态→已拒绝 */
    @Test
    void onRejected_shouldUpdateStatus() {
        ProbationApplication app = mockApp();
        when(probationMapper.selectById(APP_ID)).thenReturn(app);
        when(probationMapper.updateById(any())).thenReturn(1);

        service.onRejected(ApprovalBizType.PROBATION, APP_ID);

        assertEquals(4, app.getStatus()); // 已拒绝
    }

    // ==================== 查询 ====================

    /** 获取详情正常返回 */
    @Test
    void getDetail_shouldReturnVO() {
        ProbationApplication app = mockApp();
        when(probationMapper.selectById(APP_ID)).thenReturn(app);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(mockEmployee());

        ProbationDetailVO vo = service.getDetail(APP_ID);

        assertNotNull(vo);
        assertEquals(app.getPerformanceReview(), vo.getPerformanceReview());
    }

    /** 不存在的申请查询应抛异常 */
    @Test
    void getDetail_notFound_shouldThrow() {
        when(probationMapper.selectById(APP_ID)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getDetail(APP_ID));
    }

    /** 草稿状态可编辑 */
    @Test
    void updateDraft_shouldSucceed() {
        ProbationApplication app = mockApp();
        when(probationMapper.selectById(APP_ID)).thenReturn(app);
        when(probationMapper.updateById(any())).thenReturn(1);

        ProbationUpdateDTO dto = new ProbationUpdateDTO();
        dto.setPerformanceReview("更新评价");

        assertDoesNotThrow(() -> service.updateDraft(APP_ID, dto));
        assertEquals("更新评价", app.getPerformanceReview());
    }
}
