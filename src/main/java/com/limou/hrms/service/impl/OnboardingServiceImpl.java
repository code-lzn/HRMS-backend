package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.OnboardingStatus;
import com.limou.hrms.model.enums.UserRoleEnum;
import org.springframework.util.DigestUtils;
import com.limou.hrms.model.query.OnboardingQuery;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.OnboardingService;
import com.limou.hrms.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 入职管理服务实现 — 含入职 CRUD + 审批回调 + 入职后处理
 */
@Service
@Slf4j
public class OnboardingServiceImpl
        extends ServiceImpl<OnboardingApplicationMapper, OnboardingApplication>
        implements OnboardingService, ApprovalCallback {

    @Resource
    private OnboardingApplicationMapper onboardingMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeePersonalInfoMapper personalInfoMapper;
    @Resource
    private EmployeeWorkInfoMapper workInfoMapper;
    @Resource
    private EmployeeNoSequenceMapper noSequenceMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private ApprovalInstanceMapper approvalInstanceMapper;
    @Resource
    private ApprovalNodeMapper approvalNodeMapper;
    @Resource
    private AesUtil aesUtil;
    @Resource
    private DataScopeContext dataScopeContext;
    @Resource
    private ApproverResolver approverResolver;
    @Resource
    private UserMapper userMapper;

    // ==================== 入职 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApplication(OnboardingCreateDTO dto) {
        OnboardingApplication app = new OnboardingApplication();
        app.setName(dto.getName());
        app.setGender(dto.getGender());
        app.setPhone(dto.getPhone());
        app.setEmail(dto.getEmail());
        app.setIdCard(dto.getIdCard());
        app.setExpectedHireDate(dto.getExpectedHireDate());
        app.setDepartmentId(dto.getDepartmentId());
        app.setPositionId(dto.getPositionId());
        app.setHireType(dto.getHireType());
        app.setDefaultProbationMonths(dto.getDefaultProbationMonths());
        app.setProbationRatio(dto.getProbationRatio());
        // 直接汇报人默认部门负责人
        if (dto.getDirectReportId() != null) {
            app.setDirectReportId(dto.getDirectReportId());
        } else {
            app.setDirectReportId(approverResolver.resolveDeptManager(dto.getDepartmentId()));
        }
        app.setApplicantId(dataScopeContext.getCurrentEmployeeId());
        app.setStatus(OnboardingStatus.DRAFT.getCode());
        onboardingMapper.insert(app);

        // 直接提交审批
        if (Boolean.TRUE.equals(dto.getSubmitDirectly())) {
            submitToApproval(app.getId());
        }

        log.info("入职申请创建成功: id={}, name={}", app.getId(), app.getName());
        return app.getId();
    }

    @Override
    public void updateDraft(Long id, OnboardingUpdateDTO dto) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_DRAFT_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.ONBOARDING_DRAFT_ONLY, "仅申请人可编辑草稿");
        }

        if (StringUtils.isNotBlank(dto.getName())) app.setName(dto.getName());
        if (dto.getGender() != null) app.setGender(dto.getGender());
        if (StringUtils.isNotBlank(dto.getPhone())) app.setPhone(dto.getPhone());
        if (StringUtils.isNotBlank(dto.getEmail())) app.setEmail(dto.getEmail());
        if (StringUtils.isNotBlank(dto.getIdCard())) app.setIdCard(dto.getIdCard());
        if (dto.getExpectedHireDate() != null) app.setExpectedHireDate(dto.getExpectedHireDate());
        if (dto.getDepartmentId() != null) app.setDepartmentId(dto.getDepartmentId());
        if (dto.getPositionId() != null) app.setPositionId(dto.getPositionId());
        if (dto.getHireType() != null) app.setHireType(dto.getHireType());
        if (dto.getDefaultProbationMonths() != null) app.setDefaultProbationMonths(dto.getDefaultProbationMonths());
        if (dto.getProbationRatio() != null) app.setProbationRatio(dto.getProbationRatio());
        if (dto.getDirectReportId() != null) app.setDirectReportId(dto.getDirectReportId());

        onboardingMapper.updateById(app);
        log.info("入职草稿更新成功: id={}", id);
    }

    @Override
    public void deleteDraft(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_DRAFT_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.ONBOARDING_DRAFT_ONLY, "仅申请人可删除草稿");
        }
        onboardingMapper.deleteById(id);
        log.info("入职草稿删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitToApproval(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_SUBMIT_DRAFT_ONLY);
        }
        validateFieldsComplete(app);

        // 创建审批实例（由 OnboardingNodeBuilder 构建节点链）
        ApprovalInstance instance = approvalFlowService.createInstance(
                ApprovalBizType.ONBOARDING, app.getId(), app.getApplicantId());

        app.setStatus(OnboardingStatus.PENDING.getCode());
        app.setApprovalInstanceId(instance.getId());
        onboardingMapper.updateById(app);

        log.info("入职申请已提交审批: id={}, instanceId={}", id, instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_CANCEL_FIRST_NODE_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.ONBOARDING_CANCEL_FIRST_NODE_ONLY, "仅申请人可撤回");
        }
        // 审批中心会校验是否第一节点
        approvalFlowService.cancel(app.getApprovalInstanceId());

        app.setStatus(OnboardingStatus.DRAFT.getCode());
        app.setApprovalInstanceId(null);
        onboardingMapper.updateById(app);

        log.info("入职申请已撤回: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmJoin(Long id, LocalDate actualHireDate) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.APPROVED.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_CONFIRM_APPROVED_ONLY);
        }
        if (app.getEmployeeId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "员工档案未创建，无法确认入职");
        }
        // 更新员工入职日期
        Employee employee = employeeMapper.selectById(app.getEmployeeId());
        if (employee != null) {
            employee.setHireDate(actualHireDate);
            employee.setStatus(EmployeeStatus.PROBATION.getValue());
            employeeMapper.updateById(employee);
        }

        app.setStatus(OnboardingStatus.JOINED.getCode());
        onboardingMapper.updateById(app);

        log.info("员工已确认入职: id={}, employeeId={}", id, app.getEmployeeId());
    }

    @Override
    public void abandon(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.APPROVED.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_CONFIRM_APPROVED_ONLY);
        }
        app.setStatus(OnboardingStatus.ABANDONED.getCode());
        onboardingMapper.updateById(app);
        log.info("入职申请已放弃: id={}", id);
    }

    @Override
    public Page<OnboardingListVO> list(OnboardingQuery query) {
        DataScopeEnum scope = dataScopeContext.getApprovalScope();
        switch (scope) {
            case ALL:
                return queryAllList(query);
            case DEPT:
                Long deptId = dataScopeContext.getCurrentDepartmentId();
                if (deptId == null) {
                    return new Page<>(query.getCurrent(), query.getPageSize());
                }
                return queryDeptList(deptId, query);
            case SELF:
                Long employeeId = dataScopeContext.getCurrentEmployeeId();
                if (employeeId == null) {
                    return new Page<>(query.getCurrent(), query.getPageSize());
                }
                return queryPersonalList(employeeId, query);
            default:
                return new Page<>(query.getCurrent(), query.getPageSize());
        }
    }

    @Override
    public OnboardingDetailVO getDetail(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        OnboardingDetailVO vo = buildDetailVO(app);

        // 审批中/已通过/已拒绝时附带审批进度
        if (app.getStatus() == OnboardingStatus.PENDING.getCode()
                || app.getStatus() == OnboardingStatus.APPROVED.getCode()
                || app.getStatus() == OnboardingStatus.REJECTED.getCode()) {
            if (app.getApprovalInstanceId() != null) {
                ApprovalInstanceVO progress = approvalFlowService.getDetail(app.getApprovalInstanceId());
                vo.setApprovalProgress(progress);
            }
        }
        return vo;
    }

    // ==================== 审批回调 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.ONBOARDING) return;
        OnboardingApplication app = getAppOrThrow(bizId);

        // 1. 生成工号
        String employeeNo = generateEmployeeNo(app.getDepartmentId());

        // 2. 写入 employee 表
        Employee employee = new Employee();
        employee.setEmployeeNo(employeeNo);
        employee.setStatus(EmployeeStatus.PROBATION.getValue());
        employee.setHireDate(app.getExpectedHireDate());
        employee.setHireType(app.getHireType());
        employeeMapper.insert(employee);

        // 3. 写入 personal_info（身份证号 AES 加密）
        EmployeePersonalInfo personalInfo = new EmployeePersonalInfo();
        personalInfo.setEmployeeId(employee.getId());
        personalInfo.setName(app.getName());
        personalInfo.setGender(app.getGender());
        personalInfo.setPhone(app.getPhone());
        personalInfo.setEmail(app.getEmail());
        personalInfo.setIdCard(aesUtil.encrypt(app.getIdCard()));
        personalInfoMapper.insert(personalInfo);

        // 4. 写入 work_info
        EmployeeWorkInfo workInfo = new EmployeeWorkInfo();
        workInfo.setEmployeeId(employee.getId());
        workInfo.setDepartmentId(app.getDepartmentId());
        workInfo.setPositionId(app.getPositionId());
        workInfo.setDirectReportId(app.getDirectReportId());
        workInfoMapper.insert(workInfo);

        // 5. 创建系统账号（账号=手机号，随机密码）
        String initialPassword = generateRandomPassword();
        User user = new User();
        user.setUserAccount(app.getPhone());
        user.setUserPassword(DigestUtils.md5DigestAsHex(("limou" + initialPassword).getBytes()));
        user.setUserName(app.getName());
        user.setUserRole(UserRoleEnum.USER.getValue());
        userMapper.insert(user);

        // 关联用户ID到员工
        employee.setUserId(user.getId());
        employeeMapper.updateById(employee);

        // 6. 更新入职申请
        app.setEmployeeId(employee.getId());
        app.setStatus(OnboardingStatus.APPROVED.getCode());
        onboardingMapper.updateById(app);

        log.info("入职审批通过后处理完成: id={}, employeeId={}, employeeNo={}", bizId, employee.getId(), employeeNo);
    }

    @Override
    public void onRejected(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.ONBOARDING) return;
        OnboardingApplication app = getAppOrThrow(bizId);
        app.setStatus(OnboardingStatus.REJECTED.getCode());
        onboardingMapper.updateById(app);
        log.info("入职审批已拒绝: id={}", bizId);
    }

    // ==================== 私有方法 ====================

    private OnboardingApplication getAppOrThrow(Long id) {
        OnboardingApplication app = onboardingMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND);
        }
        return app;
    }

    /** 校验必填字段完整 */
    private void validateFieldsComplete(OnboardingApplication app) {
        if (StringUtils.isBlank(app.getName())
                || app.getGender() == null
                || StringUtils.isBlank(app.getPhone())
                || StringUtils.isBlank(app.getEmail())
                || StringUtils.isBlank(app.getIdCard())
                || app.getExpectedHireDate() == null
                || app.getDepartmentId() == null
                || app.getPositionId() == null
                || app.getHireType() == null
                || app.getDefaultProbationMonths() == null
                || app.getProbationRatio() == null) {
            throw new BusinessException(ErrorCode.ONBOARDING_FIELDS_INCOMPLETE);
        }
    }

    /** 生成工号（复用 EmployeeServiceImpl 同款逻辑） */
    private String generateEmployeeNo(Long departmentId) {
        Department dept = departmentMapper.selectById(departmentId);
        if (dept == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门不存在");
        }
        String deptCode = dept.getCode();
        String shortCode = deptCode.length() == 2 ? deptCode : deptCode.substring(deptCode.length() - 2);
        int year = LocalDate.now().getYear();

        String lockKey = (year + "_" + shortCode).intern();
        synchronized (lockKey) {
            QueryWrapper<EmployeeNoSequence> query = new QueryWrapper<>();
            query.eq("year", year).eq("dept_code", shortCode);
            EmployeeNoSequence seq = noSequenceMapper.selectOne(query);
            if (seq == null) {
                seq = new EmployeeNoSequence();
                seq.setYear(year);
                seq.setDeptCode(shortCode);
                seq.setCurrentSeq(1);
                noSequenceMapper.insert(seq);
            } else {
                seq.setCurrentSeq(seq.getCurrentSeq() + 1);
                noSequenceMapper.updateById(seq);
            }
            return String.format("%04d%02d%03d", year, Integer.parseInt(shortCode), seq.getCurrentSeq());
        }
    }

    // ==================== 预览工号 & 手机查重 ====================

    @Override
    public String previewEmployeeNo(Long departmentId) {
        Department dept = departmentMapper.selectById(departmentId);
        if (dept == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门不存在");
        String deptCode = dept.getCode();
        String shortCode = deptCode.length() == 2 ? deptCode : deptCode.substring(deptCode.length() - 2);
        int year = LocalDate.now().getYear();
        // 查询当前最大序号，预览不消耗
        QueryWrapper<EmployeeNoSequence> query = new QueryWrapper<>();
        query.eq("year", year).eq("dept_code", shortCode);
        EmployeeNoSequence seq = noSequenceMapper.selectOne(query);
        int nextSeq = (seq != null) ? seq.getCurrentSeq() + 1 : 1;
        return String.format("%04d%02d%03d", year, Integer.parseInt(shortCode), nextSeq);
    }

    @Override
    public boolean isPhoneAvailable(String phone, Long excludeId) {
        // 检查 employee 表
        Long empCount = employeeMapper.selectCount(
                new QueryWrapper<Employee>().eq("phone", phone).last("LIMIT 1"));
        if (empCount != null && empCount > 0) return false;
        // 检查 employee_personal_info 表
        Long piCount = personalInfoMapper.selectCount(
                new QueryWrapper<EmployeePersonalInfo>().eq("phone", phone).last("LIMIT 1"));
        if (piCount != null && piCount > 0) return false;
        // 检查入职申请表（排除自己）
        QueryWrapper<OnboardingApplication> qw = new QueryWrapper<>();
        qw.eq("phone", phone);
        if (excludeId != null) qw.ne("id", excludeId);
        qw.last("LIMIT 1");
        Long onboardCount = onboardingMapper.selectCount(qw);
        return onboardCount == null || onboardCount == 0;
    }

    // ==================== 角色路由查询 ====================

    private Page<OnboardingListVO> queryAllList(OnboardingQuery query) {
        QueryWrapper<OnboardingApplication> qw = buildQueryWrapper(query);
        qw.orderByDesc("create_time");
        Page<OnboardingApplication> page = onboardingMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private Page<OnboardingListVO> queryDeptList(Long deptId, OnboardingQuery query) {
        QueryWrapper<OnboardingApplication> qw = buildQueryWrapper(query);
        qw.eq("department_id", deptId);
        qw.orderByDesc("create_time");
        Page<OnboardingApplication> page = onboardingMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private Page<OnboardingListVO> queryPersonalList(Long employeeId, OnboardingQuery query) {
        QueryWrapper<OnboardingApplication> qw = buildQueryWrapper(query);
        qw.eq("applicant_id", employeeId);
        qw.orderByDesc("create_time");
        Page<OnboardingApplication> page = onboardingMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private QueryWrapper<OnboardingApplication> buildQueryWrapper(OnboardingQuery query) {
        QueryWrapper<OnboardingApplication> qw = new QueryWrapper<>();
        if (query.getStatus() != null) {
            qw.eq("status", query.getStatus());
        }
        if (query.getDepartmentId() != null) {
            qw.eq("department_id", query.getDepartmentId());
        }
        if (StringUtils.isNotBlank(query.getKeyword())) {
            qw.and(w -> w.like("name", query.getKeyword()).or().like("phone", query.getKeyword()));
        }
        if (query.getHireDateStart() != null) {
            qw.ge("expected_hire_date", query.getHireDateStart());
        }
        if (query.getHireDateEnd() != null) {
            qw.le("expected_hire_date", query.getHireDateEnd());
        }
        return qw;
    }

    private Page<OnboardingListVO> toListVOPage(Page<OnboardingApplication> page) {
        List<OnboardingListVO> records = page.getRecords().stream().map(app -> {
            OnboardingListVO vo = new OnboardingListVO();
            vo.setId(app.getId());
            vo.setName(app.getName());
            vo.setPhone(maskPhone(app.getPhone()));
            vo.setDepartmentName(getDeptName(app.getDepartmentId()));
            vo.setPositionName(getPositionName(app.getPositionId()));
            vo.setExpectedHireDate(app.getExpectedHireDate());
            vo.setStatus(app.getStatus());
            OnboardingStatus statusEnum = OnboardingStatus.fromCode(app.getStatus());
            vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
            vo.setApplicantName(approverResolver.getEmployeeName(app.getApplicantId()));
            vo.setCreateTime(app.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<OnboardingListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private OnboardingDetailVO buildDetailVO(OnboardingApplication app) {
        OnboardingDetailVO vo = new OnboardingDetailVO();
        vo.setId(app.getId());
        vo.setName(app.getName());
        vo.setGender(app.getGender());
        vo.setGenderDesc(app.getGender() == 1 ? "男" : app.getGender() == 2 ? "女" : "");
        vo.setPhone(app.getPhone());
        vo.setEmail(app.getEmail());
        // 身份证号：HR/admin 直接展示，其他角色脱敏
        String role = dataScopeContext.getCurrentRole();
        boolean isHrOrAdmin = "hr".equals(role) || "admin".equals(role);
        vo.setIdCard(isHrOrAdmin ? app.getIdCard() : maskIdCard(app.getIdCard()));
        vo.setExpectedHireDate(app.getExpectedHireDate());
        vo.setDepartmentId(app.getDepartmentId());
        vo.setDepartmentName(getDeptName(app.getDepartmentId()));
        vo.setPositionId(app.getPositionId());
        vo.setPositionName(getPositionName(app.getPositionId()));
        vo.setHireType(app.getHireType());
        vo.setHireTypeDesc(getHireTypeDesc(app.getHireType()));
        vo.setDefaultProbationMonths(app.getDefaultProbationMonths());
        vo.setProbationRatio(app.getProbationRatio());
        vo.setDirectReportId(app.getDirectReportId());
        vo.setDirectReportName(approverResolver.getEmployeeName(app.getDirectReportId()));
        vo.setStatus(app.getStatus());
        OnboardingStatus statusEnum = OnboardingStatus.fromCode(app.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        vo.setApprovalInstanceId(app.getApprovalInstanceId());
        vo.setEmployeeId(app.getEmployeeId());
        vo.setApplicantId(app.getApplicantId());
        vo.setApplicantName(approverResolver.getEmployeeName(app.getApplicantId()));
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) return null;
        Department dept = departmentMapper.selectById(deptId);
        return dept != null ? dept.getName() : null;
    }

    private String getPositionName(Long positionId) {
        if (positionId == null) return null;
        Position position = positionMapper.selectById(positionId);
        return position != null ? position.getName() : null;
    }

    private String getHireTypeDesc(Integer hireType) {
        if (hireType == null) return "";
        switch (hireType) {
            case 1: return "全职";
            case 2: return "兼职";
            case 3: return "实习";
            default: return "";
        }
    }

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
