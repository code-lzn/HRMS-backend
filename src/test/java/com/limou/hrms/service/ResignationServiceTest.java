package com.limou.hrms.service;

import com.limou.hrms.builder.ApproverResolver;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.resignation.ResignationCreateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.vo.ResignationDetailVO;
import com.limou.hrms.service.impl.ResignationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResignationServiceTest {

    @Mock private ResignationApplicationMapper resignationMapper;
    @Mock private ApprovalFlowService approvalFlowService;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeeWorkInfoMapper workInfoMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private PositionMapper positionMapper;
    @Mock private DataScopeContext dataScopeContext;
    @Mock private ApproverResolver approverResolver;

    @InjectMocks private ResignationServiceImpl service;

    private static final Long APP_ID = 1L, APPLICANT_ID = 10L, EMPLOYEE_ID = 100L;

    @BeforeEach
    void setUp() {
        when(dataScopeContext.getCurrentEmployeeId()).thenReturn(APPLICANT_ID);
        when(dataScopeContext.getCurrentRole()).thenReturn("hr");
        when(dataScopeContext.getApprovalScope()).thenReturn(DataScopeEnum.SELF);
        when(approverResolver.getEmployeeName(anyLong())).thenReturn("测试姓名");
        doAnswer(inv -> { ResignationApplication a = inv.getArgument(0); a.setId(APP_ID); return 1; })
                .when(resignationMapper).insert(any());
    }

    private ResignationCreateDTO buildDTO() {
        ResignationCreateDTO dto = new ResignationCreateDTO();
        dto.setEmployeeId(EMPLOYEE_ID);
        dto.setResignationDate(LocalDate.now().plusDays(30));
        dto.setResignationType(1);
        dto.setReason("个人原因");
        dto.setHandoverToId(50L);
        return dto;
    }

    private Employee mockEmployee() {
        Employee e = new Employee(); e.setId(EMPLOYEE_ID);
        e.setStatus(EmployeeStatus.REGULAR.getValue()); e.setEmployeeNo("202601005"); return e;
    }

    /** 保存为草稿 */
    @Test void createApplication_shouldSucceed() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(mockEmployee());
        assertNotNull(service.createApplication(buildDTO()));
    }

    /** 非在职员工发起离职应抛异常 */
    @Test void createApplication_notActive_shouldThrow() {
        Employee e = mockEmployee(); e.setStatus(EmployeeStatus.RESIGNED.getValue());
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(e);
        assertThrows(BusinessException.class, () -> service.createApplication(buildDTO()));
    }

    /** 离职日期早于今天应抛异常 */
    @Test void createApplication_pastDate_shouldThrow() {
        ResignationCreateDTO dto = buildDTO();
        dto.setResignationDate(LocalDate.now().minusDays(1));
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(mockEmployee());
        assertThrows(BusinessException.class, () -> service.createApplication(dto));
    }

    /** 审批通过回调：员工→待离职，申请→待离职 */
    @Test void onApproved_shouldSetPendingResignation() {
        ResignationApplication app = new ResignationApplication();
        app.setId(APP_ID); app.setEmployeeId(EMPLOYEE_ID);
        when(resignationMapper.selectById(APP_ID)).thenReturn(app);
        Employee e = mockEmployee();
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(e);
        when(employeeMapper.updateById(any())).thenReturn(1);
        when(resignationMapper.updateById(any())).thenReturn(1);

        service.onApproved(ApprovalBizType.RESIGNATION, APP_ID);

        assertEquals(3, app.getStatus());
        assertEquals(EmployeeStatus.PENDING_RESIGNATION.getValue(), e.getStatus());
    }

    /** 审批拒绝回调：状态→已拒绝 */
    @Test void onRejected_shouldUpdateStatus() {
        ResignationApplication app = new ResignationApplication();
        app.setId(APP_ID);
        when(resignationMapper.selectById(APP_ID)).thenReturn(app);
        when(resignationMapper.updateById(any())).thenReturn(1);

        service.onRejected(ApprovalBizType.RESIGNATION, APP_ID);
        assertEquals(5, app.getStatus());
    }

    /** 获取详情正常返回 */
    @Test void getDetail_shouldReturnVO() {
        ResignationApplication app = new ResignationApplication();
        app.setId(APP_ID); app.setEmployeeId(EMPLOYEE_ID); app.setReason("个人原因");
        when(resignationMapper.selectById(APP_ID)).thenReturn(app);
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(mockEmployee());

        ResignationDetailVO vo = service.getDetail(APP_ID);
        assertNotNull(vo);
    }
}
