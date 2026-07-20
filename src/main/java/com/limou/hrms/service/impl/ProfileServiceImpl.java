package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.config.ProfileConfig;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.attendance.ClockRequest;
import com.limou.hrms.model.dto.profile.LeaveQueryDTO;
import com.limou.hrms.model.dto.profile.PasswordChangeDTO;
import com.limou.hrms.model.dto.profile.PhoneChangeDTO;
import com.limou.hrms.model.dto.profile.PhoneUnbindDTO;
import com.limou.hrms.model.dto.employee.EmployeeUpdateRequest;
import com.limou.hrms.model.dto.profile.ProfileUpdateDTO;
import com.limou.hrms.model.dto.salary.PayslipVerifyRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.model.vo.salary.PayslipVO;
import com.limou.hrms.service.*;
import com.limou.hrms.service.salary.SalaryDetailService;
import com.limou.hrms.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 个人中心聚合服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final String SALT = "pwd";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    /** 手机验证码 Redis Key */
    private static final String PHONE_VERIFY_CODE_KEY = "phone:verify:code:%d";
    /** 手机验证码发送限制 Key */
    private static final String PHONE_VERIFY_LIMIT_KEY = "phone:verify:limit:%d";
    /** 手机解绑审批待处理数据 Key */
    private static final String PHONE_UNBIND_KEY = "phone:unbind:%d";

    private final DataScopeContext dataScopeContext;
    private final ProfileConfig profileConfig;

    private final EmployeeMapper employeeMapper;
    private final EmployeePersonalInfoMapper employeePersonalInfoMapper;
    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;
    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final SalaryDetailMapper salaryDetailMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final LeaveRequestMapper leaveRequestMapper;

    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final SalaryDetailService salaryDetailService;
    private final ApprovalFlowService approvalFlowService;
    private final StringRedisTemplate stringRedisTemplate;

    private final SecureRandom secureRandom = new SecureRandom();

    // ==================== 我的档案 ====================

    @Override
    public ProfileVO getProfile(User loginUser) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();

        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            log.error("员工ID为{}的档案档案不存在", employeeId);
            throw new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }

        EmployeePersonalInfo personalInfo = employeePersonalInfoMapper.selectOne(
                new QueryWrapper<EmployeePersonalInfo>().eq("employee_id", employeeId));
        EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", employeeId));

        ProfileVO vo = new ProfileVO();
        vo.setEmployeeId(employeeId);
        vo.setEmployeeNo(employee.getEmployeeNo());
        vo.setStatus(employee.getStatus());
        vo.setStatusDesc(getEmployeeStatusDesc(employee.getStatus()));
        vo.setHireDate(employee.getHireDate() != null ? employee.getHireDate().toString() : null);

        if (personalInfo != null) {
            vo.setName(personalInfo.getName());
            vo.setGender(personalInfo.getGender());
            vo.setGenderDesc(personalInfo.getGender() == 1 ? "男" : personalInfo.getGender() == 2 ? "女" : "");
            vo.setPhone(SensitiveDataUtil.maskPhone(personalInfo.getPhone()));
            vo.setEmail(personalInfo.getEmail());
            vo.setIdCard(SensitiveDataUtil.maskIdCard(personalInfo.getIdCard()));
            vo.setBirthday(personalInfo.getBirthday() != null ? personalInfo.getBirthday().toString() : null);
            vo.setAddress(personalInfo.getCurrentAddress());
            vo.setEmergencyContact(personalInfo.getEmergencyContactName());
            vo.setEmergencyPhone(SensitiveDataUtil.maskPhone(personalInfo.getEmergencyContactPhone()));
        }

        if (workInfo != null) {
            if (workInfo.getDepartmentId() != null) {
                Department dept = departmentMapper.selectById(workInfo.getDepartmentId());
                vo.setDepartmentName(dept != null ? dept.getName() : "");
            }
            if (workInfo.getPositionId() != null) {
                Position position = positionMapper.selectById(workInfo.getPositionId());
                vo.setPositionName(position != null ? position.getName() : "");
            }
            vo.setJobLevel(workInfo.getJobLevel());
        }

        vo.setEditableFields(profileConfig.getEmployeeFields());
        vo.setLockedFields(profileConfig.getLockedFields());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(User loginUser, ProfileUpdateDTO dto) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();

        EmployeeUpdateRequest req = new EmployeeUpdateRequest();
        boolean hasField = false;
        if (dto.getEmail() != null) {
            if (!EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
                log.error("员工ID为{}更新邮箱时，邮箱格式错误", employeeId);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
            }
            req.setEmail(dto.getEmail());
            hasField = true;
        }
        if (dto.getAddress() != null) {
            if (dto.getAddress().length() > 256) {
                log.error("员工ID为{}更新地址时，地址长度超过256个字符", employeeId);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "地址长度不能超过256个字符");
            }
            req.setCurrentAddress(dto.getAddress());
            hasField = true;
        }
        if (dto.getEmergencyContact() != null) {
            if (dto.getEmergencyContact().length() > 32) {
                log.error("员工ID为{}更新紧急联系人姓名时，姓名长度超过32个字符", employeeId);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "紧急联系人姓名长度不能超过32个字符");
            }
            req.setEmergencyContactName(dto.getEmergencyContact());
            hasField = true;
        }
        if (dto.getEmergencyPhone() != null) {
            if (!PHONE_PATTERN.matcher(dto.getEmergencyPhone()).matches()) {
                log.error("员工ID为{}更新紧急联系人电话时，电话格式错误", employeeId);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "紧急联系人电话格式不正确");
            }
            req.setEmergencyContactPhone(dto.getEmergencyPhone());
            hasField = true;
        }

        if (!hasField) {
            return;
        }

        employeeService.updateEmployee(employeeId, req, loginUser);
        log.info("员工 {} 更新了个人档案", employeeId);
    }

    // ==================== 我的考勤 ====================

    @Override
    public AttendanceCalendarVO getAttendanceCalendar(User loginUser, String yearMonth) {
        if (yearMonth == null || !yearMonth.matches("\\d{4}-\\d{2}")) {
            log.error("员工获取考勤日历时，月份格式错误，正确格式：YYYY-MM");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "月份格式错误，正确格式：YYYY-MM");
        }
        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        Long employeeId = dataScopeContext.getCurrentEmployeeId();

        return attendanceService.getCalendar(year, month, employeeId);
    }

    @Override
    public ClockResultVO clock(User loginUser, Integer clockType) {
        if (clockType == null || (clockType != 1 && clockType != 2)) {
            log.error("员工ID为打卡时，打卡类型错误：1=上班 2=下班");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "打卡类型错误：1=上班 2=下班");
        }
        ClockRequest clockRequest = new ClockRequest();
        clockRequest.setClockType(clockType);
        return attendanceService.clock(clockRequest);
    }

    // ==================== 我的请假 ====================

    @Override
    public Page<LeaveRequestVO> getMyLeaves(User loginUser, LeaveQueryDTO query) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        int page = query.getPage() != null ? query.getPage() : 1;
        int size = query.getSize() != null ? query.getSize() : 20;

        return leaveService.queryRequests(employeeId, null, query.getStatus(), null, null, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeave(User loginUser, Long leaveId) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        leaveService.cancelLeaveRequest(leaveId, employeeId);
    }

    // ==================== 我的薪资 ====================

    @Override
    public List<PayslipListVO> getMyPayslips(User loginUser) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        List<PayslipVO> payslips = salaryDetailService.getMyPayslips(employeeId);

        return payslips.stream().map(p -> {
            PayslipListVO vo = new PayslipListVO();
            vo.setId(p.getId());
            vo.setYearMonth(p.getSalaryMonth());
            vo.setNetSalary(p.getNetPay());
            vo.setHasViewed(p.getPayslipViewed() != null && p.getPayslipViewed() > 0
                    || salaryDetailService.isPayslipVerified(employeeId, p.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void sendPayslipVerifyCode(User loginUser, Long salaryId) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        salaryDetailService.sendPayslipVerifyCode(employeeId, salaryId);
    }

    @Override
    public PayslipVO getPayslipDetail(User loginUser, Long salaryId, String verifyCode) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();

        // 如果传了验证码，先校验
        if (verifyCode != null && !verifyCode.isBlank()) {
            PayslipVerifyRequest req = new PayslipVerifyRequest();
            req.setVerifyType(1); // 短信验证码
            req.setVerifyCode(verifyCode);
            salaryDetailService.verifyPayslip(employeeId, salaryId, req);
        }

        return salaryDetailService.getPayslipDetail(employeeId, salaryId);
    }

    // ==================== 账号安全 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(User loginUser, PasswordChangeDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            log.error("两次输入的新密码不一致，新密码：{}，确认密码：{}，", dto.getNewPassword(), dto.getConfirmPassword());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }

        if (!PASSWORD_PATTERN.matcher(dto.getNewPassword()).matches()) {
            log.error("新密码格式错误，必须包含字母、数字和特殊字符");
            throw new BusinessException(ErrorCode.NEW_PASSWORD_WEAK);
        }

        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            log.error("新密码不能与旧密码相同");
            throw new BusinessException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        User user = userMapper.selectById(loginUser.getId());
        String oldEncrypted = DigestUtils.md5DigestAsHex((SALT + dto.getOldPassword()).getBytes());
        if (!user.getUserPassword().equals(oldEncrypted)) {
            log.error("旧密码错误");
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR);
        }

        String newEncrypted = DigestUtils.md5DigestAsHex((SALT + dto.getNewPassword()).getBytes());
        user.setUserPassword(newEncrypted);
        userMapper.updateById(user);

        // 递增密码版本号，使其他设备的登录态失效
        String versionKey = String.format("pwd:version:%d", loginUser.getId());
        stringRedisTemplate.opsForValue().increment(versionKey);

        log.info("用户 {} 修改了密码，已递增密码版本号，其他设备将被强制下线", loginUser.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(User loginUser, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(ErrorCode.NEW_PASSWORD_WEAK);
        }
        User user = userMapper.selectById(loginUser.getId());
        String newEncrypted = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());
        user.setUserPassword(newEncrypted);
        user.setPwdReset(0); // 清除首次登录标记
        userMapper.updateById(user);
        log.info("用户 {} 首次登录重置密码成功", loginUser.getId());
    }

    /**
     * 发送手机验证码（用于修改手机号时验证新手机号）
     */
    public void sendPhoneVerifyCode(User loginUser, String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            log.error("手机号格式错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }

        Long userId = loginUser.getId();

        // 频率限制：60 秒内不可重复发送
        String limitKey = String.format(PHONE_VERIFY_LIMIT_KEY, userId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            log.error("用户 {} 手机号变更验证，60秒内已发送过验证码，不能重复发送", userId);
            throw new BusinessException(ErrorCode.VERIFY_CODE_TOO_FREQUENT);
        }

        // 生成 6 位验证码
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        // 存入 Redis（5 分钟有效）
        String codeKey = String.format(PHONE_VERIFY_CODE_KEY, userId);
        stringRedisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(5));
        stringRedisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(60));

        log.info("【验证码】用户 {} 手机号变更验证 → {}，验证码: {}（5分钟有效，查看日志获取）",
                userId, phone, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePhone(User loginUser, PhoneChangeDTO dto) {
        String newPhone = dto.getNewPhone();
        Long userId = loginUser.getId();

        if (!PHONE_PATTERN.matcher(newPhone).matches()) {
            log.error("手机号格式错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }

        // 校验验证码
        String codeKey = String.format(PHONE_VERIFY_CODE_KEY, userId);
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(dto.getVerifyCode())) {
            log.error("验证码错误或已过期");
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }

        // 校验新手机号是否已被其他用户占用
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(User::getUserAccount, newPhone);
        wrapper.lambda().ne(User::getId, userId);
        Long count = userMapper.selectCount(wrapper);
        if (count > 0) {
            log.error("手机号 {} 已被其他用户占用", newPhone);
            throw new BusinessException(ErrorCode.PHONE_ALREADY_USED);
        }

        // 清除验证码
        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.delete(String.format(PHONE_VERIFY_LIMIT_KEY, userId));

        // 更新 user 表的登录账号
        User user = userMapper.selectById(userId);
        user.setUserAccount(newPhone);
        userMapper.updateById(user);

        // 同步更新 employee_personal_info 表的手机号
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        EmployeePersonalInfo personalInfo = employeePersonalInfoMapper.selectOne(
                new QueryWrapper<EmployeePersonalInfo>().eq("employee_id", employeeId));
        if (personalInfo != null) {
            personalInfo.setPhone(newPhone);
            employeePersonalInfoMapper.updateById(personalInfo);
        }

        log.info("用户 {} 修改了手机号为 {}", loginUser.getId(), SensitiveDataUtil.maskPhone(newPhone));
    }

    /**
     * 手机号解绑申请（审批流）：验证新手机 → 提交审批 → HR 审批通过后生效
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPhoneUnbind(User loginUser, PhoneUnbindDTO dto) {
        String newPhone = dto.getNewPhone();
        Long userId = loginUser.getId();
        Long employeeId = dataScopeContext.getCurrentEmployeeId();

        if (!PHONE_PATTERN.matcher(newPhone).matches()) {
            log.error("手机号格式错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }

        // 校验验证码
        String codeKey = String.format(PHONE_VERIFY_CODE_KEY, userId);
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(dto.getVerifyCode())) {
            log.error("验证码错误或已过期");
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }

        // 校验新手机号未被占用
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(User::getUserAccount, newPhone);
        wrapper.lambda().ne(User::getId, userId);
        if (userMapper.selectCount(wrapper) > 0) {
            log.error("手机号 {} 已被其他用户占用", newPhone);
            throw new BusinessException(ErrorCode.PHONE_ALREADY_USED);
        }

        // 清除验证码
        stringRedisTemplate.delete(codeKey);
        stringRedisTemplate.delete(String.format(PHONE_VERIFY_LIMIT_KEY, userId));

        // 存储待审批数据到 Redis
        String unbindKey = String.format(PHONE_UNBIND_KEY, userId);
        stringRedisTemplate.opsForValue().set(unbindKey, newPhone, Duration.ofDays(7));

        // 创建审批实例
        ApprovalInstance instance = approvalFlowService.createInstance(
                ApprovalBizType.PHONE_CHANGE, userId, employeeId);
        log.info("用户 {} 提交手机号变更审批（{} → {}），审批实例 id={}",
                userId, SensitiveDataUtil.maskPhone(loginUser.getUserAccount()),
                SensitiveDataUtil.maskPhone(newPhone), instance.getId());
    }

    @Override
    public List<LoginLogVO> getLoginLogs(User loginUser) {
        QueryWrapper<LoginLog> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(LoginLog::getUserId, loginUser.getId())
                .orderByDesc(LoginLog::getLoginTime)
                .last("LIMIT 20");

        List<LoginLog> logs = loginLogMapper.selectList(wrapper);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return logs.stream().map(log -> LoginLogVO.builder()
                .id(log.getId())
                .loginTime(log.getLoginTime() != null ? log.getLoginTime().format(fmt) : null)
                .ipAddress(log.getIpAddress())
                .device(log.getDevice())
                .result(log.getResult())
                .resultDesc(log.getResult() != null && log.getResult() == 1 ? "成功" : "失败")
                .build()).collect(Collectors.toList());
    }

    // ==================== 聚合 ====================

    @Override
    public PendingCountVO getPendingCount(User loginUser) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();

        // ① 新工资条可查看数
        QueryWrapper<SalaryDetail> salaryWrapper = new QueryWrapper<>();
        salaryWrapper.lambda()
                .eq(SalaryDetail::getEmployeeId, employeeId)
                .eq(SalaryDetail::getPayslipViewed, 0)
                .inSql(SalaryDetail::getBatchId,
                        "SELECT id FROM salary_batch WHERE status IN (4, 5)");
        long newSalaryCount = salaryDetailMapper.selectCount(salaryWrapper);

        // ② 请假审批结果通知数（已通过/已拒绝，记录上次查看时间避免重复计数）
        String lastSeenKey = String.format("leave:notif:lastseen:%d", employeeId);
        String lastSeenStr = stringRedisTemplate.opsForValue().get(lastSeenKey);
        QueryWrapper<LeaveRequest> leaveWrapper = new QueryWrapper<>();
        leaveWrapper.lambda()
                .eq(LeaveRequest::getEmployeeId, employeeId)
                .in(LeaveRequest::getStatus, 3, 4); // 已通过、已拒绝
        if (lastSeenStr != null && !lastSeenStr.isEmpty()) {
            leaveWrapper.lambda().gt(LeaveRequest::getUpdateTime, lastSeenStr);
        }
        long leaveApprovalResult = leaveRequestMapper.selectCount(leaveWrapper);

        int total = (int) newSalaryCount + (int) leaveApprovalResult;

        return PendingCountVO.builder()
                .leaveApprovalResult((int) leaveApprovalResult)
                .newSalaryAvailable((int) newSalaryCount)
                .total(total)
                .build();
    }

    /**
     * 标记请假通知已读（用户查看请假列表后调用）
     */
    public void markLeaveNotificationsRead(User loginUser) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        String lastSeenKey = String.format("leave:notif:lastseen:%d", employeeId);
        stringRedisTemplate.opsForValue().set(lastSeenKey,
                java.time.LocalDateTime.now().toString());
    }

    @Override
    public SalaryTrendVO getSalaryTrend(User loginUser) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();

        List<String> months = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            months.add(now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        List<BigDecimal> netSalaries = new ArrayList<>();
        for (String month : months) {
            QueryWrapper<SalaryDetail> monthWrapper = new QueryWrapper<>();
            monthWrapper.lambda()
                    .eq(SalaryDetail::getEmployeeId, employeeId)
                    .inSql(SalaryDetail::getBatchId,
                            "SELECT id FROM salary_batch WHERE status IN (4, 5) AND salary_month = '" + month + "'");
            List<SalaryDetail> monthDetails = salaryDetailMapper.selectList(monthWrapper);
            if (!monthDetails.isEmpty()) {
                netSalaries.add(monthDetails.get(0).getNetPay());
            } else {
                netSalaries.add(BigDecimal.ZERO);
            }
        }

        return SalaryTrendVO.builder()
                .months(months)
                .netSalaries(netSalaries)
                .build();
    }

    // ==================== 工具 ====================

    private String getEmployeeStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 1: return "试用期";
            case 2: return "正式";
            case 3: return "待离职";
            case 4: return "已离职";
            default: return "未知";
        }
    }
}
