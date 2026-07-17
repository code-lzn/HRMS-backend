package com.limou.hrms.service;

import com.limou.hrms.builder.ApproverResolver;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.onboarding.OnboardingCreateDTO;
import com.limou.hrms.model.dto.onboarding.OnboardingUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.OnboardingStatus;
import com.limou.hrms.model.vo.OnboardingDetailVO;
import com.limou.hrms.service.impl.OnboardingServiceImpl;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.util.AesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 入职管理服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnboardingServiceTest {

    @Mock private OnboardingApplicationMapper onboardingMapper;
    @Mock private ApprovalFlowService approvalFlowService;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeePersonalInfoMapper personalInfoMapper;
    @Mock private EmployeeWorkInfoMapper workInfoMapper;
    @Mock private EmployeeNoSequenceMapper noSequenceMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private ApprovalInstanceMapper approvalInstanceMapper;
    @Mock private ApprovalNodeMapper approvalNodeMapper;
    @Mock private PositionMapper positionMapper;
    @Mock private AesUtil aesUtil;
    @Mock private DataScopeContext dataScopeContext;
    @Mock private ApproverResolver approverResolver;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private OnboardingServiceImpl service;

    private static final Long APP_ID = 1L;
    private static final Long APPLICANT_ID = 10L;

    @BeforeEach
    void setUp() {
        when(dataScopeContext.getCurrentEmployeeId()).thenReturn(APPLICANT_ID);
        when(dataScopeContext.getCurrentRole()).thenReturn("hr");
        when(dataScopeContext.getApprovalScope()).thenReturn(DataScopeEnum.SELF);
        when(aesUtil.encrypt(any())).thenReturn("encrypted");
        when(approverResolver.resolveDeptManager(anyLong())).thenReturn(100L);
        when(approverResolver.getEmployeeName(anyLong())).thenReturn("测试姓名");
        when(userMapper.insert(any())).thenReturn(1);

        // MyBatis-Plus insert 会自动设置 ID，Mockito 不会，用 doAnswer 模拟
        doAnswer(inv -> { OnboardingApplication a = inv.getArgument(0); a.setId(APP_ID); return 1; })
                .when(onboardingMapper).insert(any(OnboardingApplication.class));
        doAnswer(inv -> { Employee e = inv.getArgument(0); e.setId(100L); return 1; })
                .when(employeeMapper).insert(any(Employee.class));
    }

    private OnboardingCreateDTO buildCreateDTO() {
        OnboardingCreateDTO dto = new OnboardingCreateDTO();
        dto.setName("张三");
        dto.setGender(1);
        dto.setPhone("13800001234");
        dto.setEmail("zhangsan@test.com");
        dto.setIdCard("330100199001011234");
        dto.setExpectedHireDate(LocalDate.of(2026, 8, 1));
        dto.setDepartmentId(1L);
        dto.setPositionId(1L);
        dto.setHireType(1);
        dto.setDefaultProbationMonths(3);
        dto.setProbationRatio(new BigDecimal("0.80"));
        dto.setSubmitDirectly(false);
        return dto;
    }

    private OnboardingApplication mockApp() {
        OnboardingApplication app = new OnboardingApplication();
        app.setId(APP_ID);
        app.setName("张三");
        app.setGender(1);
        app.setPhone("13800001234");
        app.setEmail("zhangsan@test.com");
        app.setIdCard("330100199001011234");
        app.setExpectedHireDate(LocalDate.of(2026, 8, 1));
        app.setDepartmentId(1L);
        app.setPositionId(1L);
        app.setHireType(1);
        app.setDefaultProbationMonths(3);
        app.setProbationRatio(new BigDecimal("0.80"));
        app.setStatus(OnboardingStatus.DRAFT.getCode());
        app.setApplicantId(APPLICANT_ID);
        return app;
    }

    // ==================== 创建申请 ====================

    /** 保存为草稿 */
    @Test
    void createApplication_draft_shouldSucceed() {
        Long id = service.createApplication(buildCreateDTO());

        assertNotNull(id);
    }

    /** 直接提交审批：创建入职申请时应创建审批实例 */
    @Test
    void createApplication_submitDirectly_shouldCallSubmit() {
        OnboardingCreateDTO dto = buildCreateDTO();
        dto.setSubmitDirectly(true);
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(anyLong())).thenReturn(app);
        ApprovalInstance mockInstance = new ApprovalInstance();
        mockInstance.setId(200L);
        when(approvalFlowService.createInstance(any(), anyLong(), anyLong()))
                .thenReturn(mockInstance);

        Long id = service.createApplication(dto);

        assertNotNull(id);
        verify(approvalFlowService, times(1)).createInstance(any(), anyLong(), anyLong());
    }

    // ==================== 更新草稿 ====================

    /** 草稿状态可编辑 */
    @Test
    void updateDraft_shouldSucceed() {
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        OnboardingUpdateDTO dto = new OnboardingUpdateDTO();
        dto.setName("李四");

        assertDoesNotThrow(() -> service.updateDraft(APP_ID, dto));
        assertEquals("李四", app.getName());
    }

    /** 非草稿状态编辑应抛异常 */
    @Test
    void updateDraft_nonDraft_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateDraft(APP_ID, new OnboardingUpdateDTO()));
        assertEquals(ErrorCode.ONBOARDING_DRAFT_ONLY.getCode(), ex.getCode());
    }

    // ==================== 删除草稿 ====================

    /** 草稿状态可删除 */
    @Test
    void deleteDraft_shouldSucceed() {
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.deleteById(APP_ID)).thenReturn(1);

        assertDoesNotThrow(() -> service.deleteDraft(APP_ID));
    }

    /** 非草稿状态删除应抛异常 */
    @Test
    void deleteDraft_nonDraft_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        assertThrows(BusinessException.class, () -> service.deleteDraft(APP_ID));
    }

    // ==================== 提交审批 ====================

    /** 提交审批：状态→审批中，关联审批实例 */
    @Test
    void submitToApproval_shouldSucceed() {
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(100L);
        when(approvalFlowService.createInstance(any(), anyLong(), anyLong())).thenReturn(instance);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.submitToApproval(APP_ID);

        assertEquals(OnboardingStatus.PENDING.getCode(), app.getStatus());
        assertEquals(100L, app.getApprovalInstanceId());
    }

    /** 非草稿状态提交应抛异常 */
    @Test
    void submitToApproval_nonDraft_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        assertThrows(BusinessException.class, () -> service.submitToApproval(APP_ID));
    }

    /** 必填字段不完整时提交应抛异常 */
    @Test
    void submitToApproval_incompleteFields_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setName(null); // 缺少姓名
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitToApproval(APP_ID));
        assertEquals(ErrorCode.ONBOARDING_FIELDS_INCOMPLETE.getCode(), ex.getCode());
    }

    // ==================== 撤回 ====================

    /** 撤回：状态回退草稿，清空审批实例ID */
    @Test
    void cancel_shouldSucceed() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        app.setApprovalInstanceId(100L);
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.cancel(APP_ID);

        assertEquals(OnboardingStatus.DRAFT.getCode(), app.getStatus());
        assertNull(app.getApprovalInstanceId());
        verify(approvalFlowService, times(1)).cancel(100L);
    }

    /** 非申请人撤回应抛异常 */
    @Test
    void cancel_notApplicant_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        app.setApplicantId(999L); // 不是当前用户
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        assertThrows(BusinessException.class, () -> service.cancel(APP_ID));
    }

    // ==================== 确认入职 ====================

    /** HR确认入职：状态→已入职，更新员工入职日期 */
    @Test
    void confirmJoin_shouldSucceed() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.APPROVED.getCode());
        app.setEmployeeId(100L);
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        Employee employee = new Employee();
        employee.setId(100L);
        when(employeeMapper.selectById(100L)).thenReturn(employee);
        when(employeeMapper.updateById(any())).thenReturn(1);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.confirmJoin(APP_ID, LocalDate.of(2026, 8, 1));

        assertEquals(OnboardingStatus.JOINED.getCode(), app.getStatus());
    }

    /** 非"已批准待入职"状态确认应抛异常 */
    @Test
    void confirmJoin_notApproved_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.DRAFT.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        assertThrows(BusinessException.class, () ->
                service.confirmJoin(APP_ID, LocalDate.now()));
    }

    // ==================== 放弃入职 ====================

    /** HR标记放弃：状态→已放弃 */
    @Test
    void abandon_shouldSucceed() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.APPROVED.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.abandon(APP_ID);

        assertEquals(OnboardingStatus.ABANDONED.getCode(), app.getStatus());
    }

    // ==================== 审批回调 ====================

    /** 审批通过回调：生成工号+写入employee/personal_info/work_info三表+创建系统账号 */
    @Test
    void onApproved_shouldWriteEmployeeAndThreeTables() {
        OnboardingApplication app = mockApp();
        app.setDepartmentId(1L);
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        Department dept = new Department();
        dept.setCode("01");
        when(departmentMapper.selectById(1L)).thenReturn(dept);
        EmployeeNoSequence seq = new EmployeeNoSequence();
        seq.setCurrentSeq(5);
        when(noSequenceMapper.selectOne(any())).thenReturn(seq);
        when(noSequenceMapper.updateById(any())).thenReturn(1);
        when(personalInfoMapper.insert(any())).thenReturn(1);
        when(workInfoMapper.insert(any())).thenReturn(1);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.onApproved(ApprovalBizType.ONBOARDING, APP_ID);

        assertEquals(OnboardingStatus.APPROVED.getCode(), app.getStatus());
        assertNotNull(app.getEmployeeId());
        verify(employeeMapper, times(1)).insert(any());
        verify(personalInfoMapper, times(1)).insert(any());
        verify(workInfoMapper, times(1)).insert(any());
    }

    /** 审批拒绝回调：状态→已拒绝 */
    @Test
    void onRejected_shouldUpdateStatus() {
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.onRejected(ApprovalBizType.ONBOARDING, APP_ID);

        assertEquals(OnboardingStatus.REJECTED.getCode(), app.getStatus());
    }

    /** 获取详情正常返回 */
    @Test
    void getDetail_shouldReturnVO() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.DRAFT.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        OnboardingDetailVO vo = service.getDetail(APP_ID);

        assertNotNull(vo);
        assertEquals(app.getName(), vo.getName());
    }

    /** 不存在的入职申请查询应抛异常 */
    @Test
    void getDetail_notFound_shouldThrow() {
        when(onboardingMapper.selectById(APP_ID)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getDetail(APP_ID));
    }

    // ==================== 预览工号 ====================

    /** 预览工号：查询序列返回下一个工号 */
    @Test
    void previewEmployeeNo_shouldReturnFormattedNo() {
        Department dept = new Department();
        dept.setCode("01");
        when(departmentMapper.selectById(1L)).thenReturn(dept);
        EmployeeNoSequence seq = new EmployeeNoSequence();
        seq.setCurrentSeq(5);
        when(noSequenceMapper.selectOne(any())).thenReturn(seq);

        String no = service.previewEmployeeNo(1L);

        assertNotNull(no);
        assertTrue(no.length() == 9);
    }

    // ==================== 手机号查重 ====================

    /** 手机号未被占用 */
    @Test
    void isPhoneAvailable_shouldReturnTrue() {
        when(employeeMapper.selectCount(any())).thenReturn(0L);
        when(personalInfoMapper.selectCount(any())).thenReturn(0L);
        when(onboardingMapper.selectCount(any())).thenReturn(0L);

        assertTrue(service.isPhoneAvailable("13800009999", null));
    }

    /** 手机号已被占用 */
    @Test
    void isPhoneAvailable_shouldReturnFalse() {
        when(employeeMapper.selectCount(any())).thenReturn(1L);
        when(personalInfoMapper.selectCount(any())).thenReturn(0L);

        assertFalse(service.isPhoneAvailable("13800001234", null));
    }
}
