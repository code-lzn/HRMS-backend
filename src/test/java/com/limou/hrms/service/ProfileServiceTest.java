package com.limou.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.config.ProfileConfig;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.attendance.ClockRequest;
import com.limou.hrms.model.dto.profile.LeaveQueryDTO;
import com.limou.hrms.model.query.LeaveQuery;
import com.limou.hrms.model.dto.profile.PasswordChangeDTO;
import com.limou.hrms.model.dto.profile.PhoneChangeDTO;
import com.limou.hrms.model.dto.profile.PhoneUnbindDTO;
import com.limou.hrms.model.dto.employee.EmployeeUpdateRequest;
import com.limou.hrms.model.dto.profile.ProfileUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.model.vo.salary.PayslipVO;
import com.limou.hrms.service.impl.PhoneChangeApprovalHandler;
import com.limou.hrms.service.impl.ProfileServiceImpl;
import com.limou.hrms.service.salary.SalaryDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 个人中心聚合服务单元测试 — 覆盖 ProfileService 全部公开方法
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceTest {

    @Mock private DataScopeContext dataScopeContext;
    @Mock private ProfileConfig profileConfig;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeePersonalInfoMapper employeePersonalInfoMapper;
    @Mock private EmployeeWorkInfoMapper employeeWorkInfoMapper;
    @Mock private UserMapper userMapper;
    @Mock private LoginLogMapper loginLogMapper;
    @Mock private SalaryDetailMapper salaryDetailMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private PositionMapper positionMapper;
    @Mock private LeaveRequestMapper leaveRequestMapper;
    @Mock private EmployeeService employeeService;
    @Mock private AttendanceService attendanceService;
    @Mock private LeaveService leaveService;
    @Mock private SalaryDetailService salaryDetailService;
    @Mock private ApprovalFlowService approvalFlowService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private static final Long USER_ID = 1L;
    private static final Long EMPLOYEE_ID = 100L;

    private User loginUser;

    /** 每个用例前初始化登录用户及通用 Mock 行为 */
    @BeforeEach
    void setUp() {
        loginUser = new User();
        loginUser.setId(USER_ID);
        loginUser.setUserAccount("13800001234");
        loginUser.setUserPassword(DigestUtils.md5DigestAsHex(("limou" + "OldPass1").getBytes()));
        loginUser.setUserRole("user");

        when(dataScopeContext.getCurrentEmployeeId()).thenReturn(EMPLOYEE_ID);
        when(dataScopeContext.getCurrentUserId()).thenReturn(USER_ID);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(profileConfig.getEmployeeFields()).thenReturn(Arrays.asList("email", "address", "emergencyContact", "emergencyPhone"));
        when(profileConfig.getLockedFields()).thenReturn(Arrays.asList("name", "phone", "idCard", "departmentName", "positionName", "jobLevel"));
    }

    // ==================== 我的档案 ====================

    /** 正常查询：应聚合 Employee + PersonalInfo + WorkInfo + Dept + Position */
    @Test
    void getProfile_shouldReturnAssembledVO() { /* ... 省略 setup ... */
        Employee employee = new Employee();
        employee.setId(EMPLOYEE_ID); employee.setEmployeeNo("202401005");
        employee.setStatus(2); employee.setHireDate(LocalDate.of(2024, 1, 15));
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(employee);

        EmployeePersonalInfo personalInfo = new EmployeePersonalInfo();
        personalInfo.setName("张三"); personalInfo.setGender(1);
        personalInfo.setPhone("13800001234"); personalInfo.setEmail("zhangsan@test.com");
        personalInfo.setIdCard("330100199001011234"); personalInfo.setCurrentAddress("杭州市西湖区");
        personalInfo.setEmergencyContactName("张父"); personalInfo.setEmergencyContactPhone("13900001111");
        when(employeePersonalInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(personalInfo);

        EmployeeWorkInfo workInfo = new EmployeeWorkInfo();
        workInfo.setDepartmentId(1L); workInfo.setPositionId(2L); workInfo.setJobLevel("P5");
        when(employeeWorkInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(workInfo);

        Department dept = new Department(); dept.setName("技术部");
        when(departmentMapper.selectById(1L)).thenReturn(dept);
        Position position = new Position(); position.setName("Java开发工程师");
        when(positionMapper.selectById(2L)).thenReturn(position);

        ProfileVO vo = profileService.getProfile(loginUser);

        assertNotNull(vo);
        assertEquals(EMPLOYEE_ID, vo.getEmployeeId());
        assertEquals("202401005", vo.getEmployeeNo());
        assertEquals("张三", vo.getName());
        assertEquals("138****1234", vo.getPhone());        // 手机号脱敏
        assertEquals("3301**********1234", vo.getIdCard()); // 身份证脱敏
        assertEquals("139****1111", vo.getEmergencyPhone()); // 紧急联系人脱敏
        assertEquals("技术部", vo.getDepartmentName());
        assertEquals("Java开发工程师", vo.getPositionName());
        assertEquals("P5", vo.getJobLevel());
        assertEquals("正式", vo.getStatusDesc());
        assertNotNull(vo.getEditableFields());
        assertNotNull(vo.getLockedFields());
    }

    /** 员工不存在时应抛出 EMPLOYEE_NOT_FOUND */
    @Test
    void getProfile_employeeNotFound_shouldThrow() {
        when(employeeMapper.selectById(EMPLOYEE_ID)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> profileService.getProfile(loginUser));
        assertEquals(ErrorCode.EMPLOYEE_NOT_FOUND.getCode(), ex.getCode());
    }

    /** 编辑档案：应把 DTO 字段转为内部 DB 字段名后委托 EmployeeService */
    @Test
    void updateProfile_shouldDelegateToEmployeeService() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setEmail("new@test.com"); dto.setAddress("新地址");
        profileService.updateProfile(loginUser, dto);
        verify(employeeService).updateEmployee(eq(EMPLOYEE_ID), any(EmployeeUpdateRequest.class), eq(loginUser));
    }

    /** 所有字段均为 null 时不应触发更新 */
    @Test
    void updateProfile_emptyFields_shouldNoop() {
        profileService.updateProfile(loginUser, new ProfileUpdateDTO());
        verify(employeeService, never()).updateEmployee(anyLong(), any(EmployeeUpdateRequest.class), any());
    }

    // ==================== 我的考勤 ====================

    /** 考勤日历：正常委托 AttendanceService */
    @Test
    void getAttendanceCalendar_shouldDelegate() {
        AttendanceCalendarVO expected = new AttendanceCalendarVO();
        when(attendanceService.getCalendar(2024, 7, EMPLOYEE_ID)).thenReturn(expected);
        assertSame(expected, profileService.getAttendanceCalendar(loginUser, "2024-07"));
    }

    /** 月份格式错误（非 YYYY-MM）应抛参数异常 */
    @Test
    void getAttendanceCalendar_invalidFormat_shouldThrow() {
        assertThrows(BusinessException.class, () -> profileService.getAttendanceCalendar(loginUser, "2024/07"));
        assertThrows(BusinessException.class, () -> profileService.getAttendanceCalendar(loginUser, null));
    }

    /** 打卡：委托 AttendanceService.clock() */
    @Test
    void clock_shouldDelegate() {
        ClockResultVO expected = new ClockResultVO();
        when(attendanceService.clock(any(ClockRequest.class))).thenReturn(expected);
        assertSame(expected, profileService.clock(loginUser, 1));
        assertSame(expected, profileService.clock(loginUser, 2));
    }

    /** 打卡类型非法（非 1/2）应抛异常 */
    @Test
    void clock_invalidType_shouldThrow() {
        assertThrows(BusinessException.class, () -> profileService.clock(loginUser, 0));
        assertThrows(BusinessException.class, () -> profileService.clock(loginUser, null));
    }

    // ==================== 我的请假 ====================

    /** 请假列表：委托 LeaveService，并自动标记通知已读 */
//    @Test
//    void getMyLeaves_shouldDelegate() {
//        Page<LeaveRequestVO> expected = new Page<>(1, 20);
//        when(leaveService.queryRequests(any(LeaveQuery.class))).thenReturn(expected);
//        assertSame(expected, profileService.getMyLeaves(loginUser, new LeaveQueryDTO()));
//    }

    /** 取消请假：委托 LeaveService.cancel() */
    @Test
    void cancelLeave_shouldDelegate() {
        profileService.cancelLeave(loginUser, 10L);
        verify(leaveService).cancel(10L);
    }

    // ==================== 我的薪资 ====================

    /** 工资条列表：转换为前端 VO，标记 hasViewed */
    @Test
    void getMyPayslips_shouldReturnList() {
        PayslipVO p1 = new PayslipVO();
        p1.setId(1L); p1.setSalaryMonth("2024-07"); p1.setNetPay(new BigDecimal("12980.00")); p1.setPayslipViewed(0);
        when(salaryDetailService.getMyPayslips(EMPLOYEE_ID)).thenReturn(Arrays.asList(p1));
        when(salaryDetailService.isPayslipVerified(EMPLOYEE_ID, 1L)).thenReturn(false);

        List<PayslipListVO> result = profileService.getMyPayslips(loginUser);
        assertEquals(1, result.size());
        assertEquals("2024-07", result.get(0).getYearMonth());
        assertFalse(result.get(0).getHasViewed());
    }

    /** 发送工资条验证码：委托 SalaryDetailService */
    @Test
    void sendPayslipVerifyCode_shouldDelegate() {
        profileService.sendPayslipVerifyCode(loginUser, 1L);
        verify(salaryDetailService).sendPayslipVerifyCode(EMPLOYEE_ID, 1L);
    }

    /** 工资条详情 + 验证码：应先校验再返回 */
    @Test
    void getPayslipDetail_withVerifyCode_shouldVerifyFirst() {
        PayslipVO expected = new PayslipVO();
        when(salaryDetailService.getPayslipDetail(EMPLOYEE_ID, 1L)).thenReturn(expected);
        assertSame(expected, profileService.getPayslipDetail(loginUser, 1L, "123456"));
        verify(salaryDetailService).verifyPayslip(eq(EMPLOYEE_ID), eq(1L), any());
    }

    /** 工资条详情 + 无验证码（已通过验证）：直接返回 */
    @Test
    void getPayslipDetail_withoutVerifyCode_shouldSkipVerify() {
        PayslipVO expected = new PayslipVO();
        when(salaryDetailService.getPayslipDetail(EMPLOYEE_ID, 1L)).thenReturn(expected);
        assertSame(expected, profileService.getPayslipDetail(loginUser, 1L, null));
        verify(salaryDetailService, never()).verifyPayslip(anyLong(), anyLong(), any());
    }

    // ==================== 修改密码 ====================

    /** 正常修改：旧密码正确 + 新密码符合复杂度 → 更新成功，版本号递增 */
    @Test
    void changePassword_shouldSucceed() {
        when(userMapper.selectById(USER_ID)).thenReturn(loginUser);
        when(userMapper.updateById(any())).thenReturn(1);
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setOldPassword("OldPass1"); dto.setNewPassword("NewPass1"); dto.setConfirmPassword("NewPass1");
        assertDoesNotThrow(() -> profileService.changePassword(loginUser, dto));
        verify(userMapper).updateById(any(User.class));
    }

    /** 确认密码与新密码不一致应抛异常 */
    @Test
    void changePassword_confirmMismatch_shouldThrow() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setOldPassword("OldPass1"); dto.setNewPassword("NewPass1"); dto.setConfirmPassword("Different1");
        assertThrows(BusinessException.class, () -> profileService.changePassword(loginUser, dto));
    }

    /** 新密码纯数字（不含大小写字母）应抛弱密码异常 */
    @Test
    void changePassword_weakPassword_shouldThrow() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setOldPassword("OldPass1"); dto.setNewPassword("12345678"); dto.setConfirmPassword("12345678");
        assertThrows(BusinessException.class, () -> profileService.changePassword(loginUser, dto));
    }

    /** 新密码与旧密码相同应抛异常 */
    @Test
    void changePassword_sameAsOld_shouldThrow() {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setOldPassword("OldPass1"); dto.setNewPassword("OldPass1"); dto.setConfirmPassword("OldPass1");
        assertThrows(BusinessException.class, () -> profileService.changePassword(loginUser, dto));
    }

    /** 旧密码输入错误应抛 OLD_PASSWORD_ERROR */
    @Test
    void changePassword_wrongOldPassword_shouldThrow() {
        when(userMapper.selectById(USER_ID)).thenReturn(loginUser);
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setOldPassword("WrongPass1"); dto.setNewPassword("NewPass1"); dto.setConfirmPassword("NewPass1");
        BusinessException ex = assertThrows(BusinessException.class, () -> profileService.changePassword(loginUser, dto));
        assertEquals(ErrorCode.OLD_PASSWORD_ERROR.getCode(), ex.getCode());
    }

    // ==================== 手机验证码 ====================

    /** 发送验证码：写入 Redis 并设置频率限制 */
    @Test
    void sendPhoneVerifyCode_shouldStoreInRedis() {
        profileService.sendPhoneVerifyCode(loginUser, "13900001111");
        verify(valueOperations).set(contains("phone:verify:code"), anyString(), any());
        verify(valueOperations).set(contains("phone:verify:limit"), anyString(), any());
    }

    /** 手机号格式非法应抛异常 */
    @Test
    void sendPhoneVerifyCode_invalidPhone_shouldThrow() {
        assertThrows(BusinessException.class, () -> profileService.sendPhoneVerifyCode(loginUser, "12345"));
    }

    // ==================== 修改手机号 ====================

    /** 验证码正确 → 更新 user 登录账号 + 同步 personal_info */
    @Test
    void changePhone_shouldUpdatePhone() {
        when(valueOperations.get(contains("phone:verify:code:"))).thenReturn("123456");
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(userMapper.selectById(USER_ID)).thenReturn(loginUser);
        when(userMapper.updateById(any())).thenReturn(1);
        EmployeePersonalInfo personalInfo = new EmployeePersonalInfo();
        personalInfo.setEmployeeId(EMPLOYEE_ID);
        when(employeePersonalInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(personalInfo);
        when(employeePersonalInfoMapper.updateById(any())).thenReturn(1);
        PhoneChangeDTO dto = new PhoneChangeDTO();
        dto.setNewPhone("13900001111"); dto.setVerifyCode("123456");
        assertDoesNotThrow(() -> profileService.changePhone(loginUser, dto));
        assertEquals("13900001111", loginUser.getUserAccount());
    }

    /** 验证码错误应抛 VERIFY_CODE_ERROR */
    @Test
    void changePhone_wrongCode_shouldThrow() {
        when(valueOperations.get(contains("phone:verify:code:"))).thenReturn("654321");
        PhoneChangeDTO dto = new PhoneChangeDTO();
        dto.setNewPhone("13900001111"); dto.setVerifyCode("123456");
        assertThrows(BusinessException.class, () -> profileService.changePhone(loginUser, dto));
    }

    /** 新手机号已被其他用户占用应抛 PHONE_ALREADY_USED */
    @Test
    void changePhone_alreadyUsed_shouldThrow() {
        when(valueOperations.get(contains("phone:verify:code:"))).thenReturn("123456");
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);
        PhoneChangeDTO dto = new PhoneChangeDTO();
        dto.setNewPhone("13900001111"); dto.setVerifyCode("123456");
        BusinessException ex = assertThrows(BusinessException.class, () -> profileService.changePhone(loginUser, dto));
        assertEquals(ErrorCode.PHONE_ALREADY_USED.getCode(), ex.getCode());
    }

    // ==================== 手机解绑审批 ====================

    /** 验证码正确 → 存储待审批数据到 Redis → 创建审批实例 */
    @Test
    void submitPhoneUnbind_shouldCreateApproval() {
        when(valueOperations.get(contains("phone:verify:code:"))).thenReturn("123456");
        when(userMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        ApprovalInstance instance = new ApprovalInstance(); instance.setId(200L);
        when(approvalFlowService.createInstance(eq(ApprovalBizType.PHONE_CHANGE), eq(USER_ID), eq(EMPLOYEE_ID))).thenReturn(instance);
        PhoneUnbindDTO dto = new PhoneUnbindDTO();
        dto.setNewPhone("13900001111"); dto.setVerifyCode("123456");
        assertDoesNotThrow(() -> profileService.submitPhoneUnbind(loginUser, dto));
        verify(valueOperations).set(eq(String.format(PhoneChangeApprovalHandler.PHONE_UNBIND_KEY, USER_ID)), eq("13900001111"), any());
    }

    /** 验证码错误 → 不创建审批实例 */
    @Test
    void submitPhoneUnbind_wrongCode_shouldThrow() {
        when(valueOperations.get(contains("phone:verify:code:"))).thenReturn("654321");
        PhoneUnbindDTO dto = new PhoneUnbindDTO();
        dto.setNewPhone("13900001111"); dto.setVerifyCode("123456");
        assertThrows(BusinessException.class, () -> profileService.submitPhoneUnbind(loginUser, dto));
        verify(approvalFlowService, never()).createInstance(any(), anyLong(), anyLong());
    }

    // ==================== 登录日志 ====================

    /** 查询最近 20 条，转换为 VO 格式 */
    @Test
    void getLoginLogs_shouldReturnRecent20() {
        LoginLog log = new LoginLog();
        log.setId(1L); log.setUserId(USER_ID); log.setLoginTime(java.time.LocalDateTime.now());
        log.setIpAddress("192.168.1.1"); log.setDevice("Chrome"); log.setResult(1);
        when(loginLogMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(log));
        List<LoginLogVO> result = profileService.getLoginLogs(loginUser);
        assertEquals(1, result.size());
        assertEquals("成功", result.get(0).getResultDesc());
    }

    // ==================== 待办数量 ====================

    /** 工资条未读数 + 请假审批结果数 = 红点总数 */
    @Test
    void getPendingCount_shouldCountNewSalariesAndLeaves() {
        when(salaryDetailMapper.selectCount(any(QueryWrapper.class))).thenReturn(3L);
        when(leaveRequestMapper.selectCount(any(QueryWrapper.class))).thenReturn(2L);
        PendingCountVO vo = profileService.getPendingCount(loginUser);
        assertEquals(3, vo.getNewSalaryAvailable());
        assertEquals(2, vo.getLeaveApprovalResult());
        assertEquals(5, vo.getTotal());
    }

    // ==================== 薪资趋势 ====================

    /** 返回近 6 个月数组，当月无数据时补 0 */
    @Test
    void getSalaryTrend_shouldReturnLast6Months() {
        when(salaryDetailMapper.selectList(any(QueryWrapper.class))).thenReturn(new ArrayList<>());
        SalaryTrendVO vo = profileService.getSalaryTrend(loginUser);
        assertNotNull(vo);
        assertEquals(6, vo.getMonths().size());
        assertEquals(6, vo.getNetSalaries().size());
    }

    // ==================== 标记已读 ====================

    /** 查看请假列表后写入当前时间戳，后续 pending-count 不再重复计数 */
    @Test
    void markLeaveNotificationsRead_shouldSetRedis() {
        profileService.markLeaveNotificationsRead(loginUser);
        verify(valueOperations).set(contains("leave:notif:lastseen:"), anyString());
    }
}
