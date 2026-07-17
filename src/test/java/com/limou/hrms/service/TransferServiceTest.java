package com.limou.hrms.service;

import com.limou.hrms.builder.ApproverResolver;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.transfer.TransferCreateDTO;
import com.limou.hrms.model.dto.transfer.TransferUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.common.PageRequest;
import com.limou.hrms.model.vo.TransferDetailVO;
import com.limou.hrms.model.vo.TransferHistoryVO;
import com.limou.hrms.service.impl.TransferServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 调岗管理服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferServiceTest {

    @Mock private TransferApplicationMapper transferMapper;
    @Mock private TransferHistoryMapper transferHistoryMapper;
    @Mock private ApprovalFlowService approvalFlowService;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeeWorkInfoMapper workInfoMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private PositionMapper positionMapper;
    @Mock private DataScopeContext dataScopeContext;
    @Mock private ApproverResolver approverResolver;

    @InjectMocks
    private TransferServiceImpl service;

    private static final Long APP_ID = 1L;
    private static final Long APPLICANT_ID = 10L;
    private static final Long EMPLOYEE_ID = 100L;
    private static final Long OLD_DEPT_ID = 1L;
    private static final Long NEW_DEPT_ID = 2L;

    @BeforeEach
    void setUp() {
        when(dataScopeContext.getCurrentEmployeeId()).thenReturn(APPLICANT_ID);
        when(dataScopeContext.getCurrentRole()).thenReturn("hr");
        when(dataScopeContext.getApprovalScope()).thenReturn(DataScopeEnum.SELF);
        when(approverResolver.getEmployeeName(anyLong())).thenReturn("测试姓名");
        doAnswer(inv -> { TransferApplication a = inv.getArgument(0); a.setId(APP_ID); return 1; })
                .when(transferMapper).insert(any(TransferApplication.class));
    }

    private Employee mockEmployee() {
        Employee e = new Employee();
        e.setId(EMPLOYEE_ID);
        e.setStatus(EmployeeStatus.REGULAR.getValue());
        e.setEmployeeNo("202601005");
        return e;
    }

    private EmployeeWorkInfo mockWorkInfo() {
        EmployeeWorkInfo wi = new EmployeeWorkInfo();
        wi.setEmployeeId(EMPLOYEE_ID);
        wi.setDepartmentId(OLD_DEPT_ID);
        wi.setPositionId(10L);
        wi.setJobLevel("P5");
        wi.setDirectReportId(50L);
        return wi;
    }

    private TransferCreateDTO buildCreateDTO() {
        TransferCreateDTO dto = new TransferCreateDTO();
        dto.setEmployeeId(EMPLOYEE_ID);
        dto.setToDepartmentId(NEW_DEPT_ID);
        dto.setReason("业务需要");
        dto.setSubmitDirectly(false);
        return dto;
    }

    private TransferApplication mockApp() {
        TransferApplication app = new TransferApplication();
        app.setId(APP_ID);
        app.setEmployeeId(EMPLOYEE_ID);
        app.setFromDepartmentId(OLD_DEPT_ID);
        app.setToDepartmentId(NEW_DEPT_ID);
        app.setFromPositionId(10L);
        app.setReason("业务需要");
        app.setStatus(1);
        app.setApplicantId(APPLICANT_ID);
        return app;
    }

    // ==================== 创建 ====================

    /** 保存为草稿，自动带入原岗位信息 */
    @Test
    void createApplication_shouldSucceed() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(mockEmployee());
        when(workInfoMapper.selectOne(any())).thenReturn(mockWorkInfo());

        Long id = service.createApplication(buildCreateDTO());

        assertNotNull(id);
    }

    /** 调岗前后部门相同应抛异常 */
    @Test
    void createApplication_sameDept_shouldThrow() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(mockEmployee());
        EmployeeWorkInfo wi = mockWorkInfo();
        wi.setDepartmentId(NEW_DEPT_ID); // 同部门
        when(workInfoMapper.selectOne(any())).thenReturn(wi);

        assertThrows(BusinessException.class, () -> service.createApplication(buildCreateDTO()));
    }

    /** 非在职员工发起调岗应抛异常 */
    @Test
    void createApplication_notActive_shouldThrow() {
        Employee e = mockEmployee();
        e.setStatus(EmployeeStatus.RESIGNED.getValue());
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(e);
        when(workInfoMapper.selectOne(any())).thenReturn(mockWorkInfo());

        assertThrows(BusinessException.class, () -> service.createApplication(buildCreateDTO()));
    }

    // ==================== 提交 ====================

    /** 提交审批：状态→审批中 */
    @Test
    void submitToApproval_shouldSucceed() {
        TransferApplication app = mockApp();
        when(transferMapper.selectById(APP_ID)).thenReturn(app);
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(300L);
        when(approvalFlowService.createInstance(any(), anyLong(), anyLong())).thenReturn(instance);
        when(transferMapper.updateById(any())).thenReturn(1);

        service.submitToApproval(APP_ID);

        assertEquals(2, app.getStatus());
        assertEquals(300L, app.getApprovalInstanceId());
    }

    // ==================== 回调 ====================

    /** 审批通过回调：更新员工work_info + 写入调岗历史 */
    @Test
    void onApproved_shouldUpdateWorkInfoAndHistory() {
        TransferApplication app = mockApp();
        app.setToPositionId(20L);
        app.setToJobLevel("P6");
        when(transferMapper.selectById(APP_ID)).thenReturn(app);
        EmployeeWorkInfo wi = mockWorkInfo();
        when(workInfoMapper.selectOne(any())).thenReturn(wi);
        when(workInfoMapper.updateById(any())).thenReturn(1);
        when(transferHistoryMapper.insert(any())).thenReturn(1);
        when(transferMapper.updateById(any())).thenReturn(1);

        service.onApproved(ApprovalBizType.TRANSFER, APP_ID);

        assertEquals(NEW_DEPT_ID, wi.getDepartmentId());
        assertEquals(3, app.getStatus()); // 已生效
        verify(transferHistoryMapper, times(1)).insert(any());
    }

    /** 审批拒绝回调：状态→已拒绝 */
    @Test
    void onRejected_shouldUpdateStatus() {
        TransferApplication app = mockApp();
        when(transferMapper.selectById(APP_ID)).thenReturn(app);
        when(transferMapper.updateById(any())).thenReturn(1);

        service.onRejected(ApprovalBizType.TRANSFER, APP_ID);

        assertEquals(4, app.getStatus());
    }

    // ==================== 详情 ====================

    /** 获取详情正常返回 */
    @Test
    void getDetail_shouldReturnVO() {
        TransferApplication app = mockApp();
        when(transferMapper.selectById(APP_ID)).thenReturn(app);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(mockEmployee());

        TransferDetailVO vo = service.getDetail(APP_ID);

        assertNotNull(vo);
        assertEquals(app.getReason(), vo.getReason());
    }

    // ==================== 撤回 ====================

    /** 撤回：状态回退草稿 */
    @Test
    void cancel_shouldSucceed() {
        TransferApplication app = mockApp();
        app.setStatus(2);
        app.setApprovalInstanceId(300L);
        when(transferMapper.selectById(APP_ID)).thenReturn(app);
        when(transferMapper.updateById(any())).thenReturn(1);

        service.cancel(APP_ID);

        assertEquals(1, app.getStatus());
        verify(approvalFlowService, times(1)).cancel(300L);
    }

    // ==================== 更新草稿 ====================

    /** 草稿状态可编辑 */
    @Test
    void updateDraft_shouldSucceed() {
        TransferApplication app = mockApp();
        when(transferMapper.selectById(APP_ID)).thenReturn(app);
        when(transferMapper.updateById(any())).thenReturn(1);

        TransferUpdateDTO dto = new TransferUpdateDTO();
        dto.setReason("新原因");

        assertDoesNotThrow(() -> service.updateDraft(APP_ID, dto));
        assertEquals("新原因", app.getReason());
    }

    // ==================== 删除草稿 ====================

    /** 草稿状态可删除 */
    @Test
    void deleteDraft_shouldSucceed() {
        TransferApplication app = mockApp();
        when(transferMapper.selectById(APP_ID)).thenReturn(app);
        when(transferMapper.deleteById(APP_ID)).thenReturn(1);

        assertDoesNotThrow(() -> service.deleteDraft(APP_ID));
    }

    // ==================== 调岗历史 ====================

    /** 查询员工调岗历史 */
    @Test
    void getHistory_shouldReturnPage() {
        when(transferHistoryMapper.selectPage(any(), any())).thenReturn(
                new Page<TransferHistory>(1, 20));

        com.limou.hrms.common.PageRequest page = new com.limou.hrms.common.PageRequest();
        page.setCurrent(1); page.setPageSize(20);
        Page<TransferHistoryVO> result = service.getHistory(EMPLOYEE_ID, page);

        assertNotNull(result);
        assertTrue(result.getRecords().isEmpty());
    }
}
