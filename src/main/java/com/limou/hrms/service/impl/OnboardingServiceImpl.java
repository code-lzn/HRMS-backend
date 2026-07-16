package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.onboarding.OnboardingAddRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.vo.ApprovalPendingVO;
import com.limou.hrms.model.vo.MutationLogVO;
import com.limou.hrms.model.vo.OnboardingVO;
import com.limou.hrms.model.vo.UserVO;
import com.limou.hrms.service.ApprovalService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.OnboardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OnboardingServiceImpl extends ServiceImpl<HrOnboardingMapper, HrOnboarding>
        implements OnboardingService {

    @Resource
    private ApprovalService approvalService;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeeDetailMapper employeeDetailMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private EmpMutationLogMapper empMutationLogMapper;
    @Resource
    private EmployeeService employeeService;
    @Resource
    private ApprovalDetailMapper approvalDetailMapper;
    @Resource
    private ApprovalRecordMapper approvalRecordMapper;

    private static final String SALT = "hrms";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrOnboarding addOnboarding(OnboardingAddRequest request, Long hrEmployeeId, boolean submitNow) {
        validateRequest(request);
        HrOnboarding entity = new HrOnboarding();
        BeanUtils.copyProperties(request, entity);
        entity.setOperatorId(hrEmployeeId);
        entity.setBusinessNo(generateBusinessNo());
        entity.setApproverId(resolveApproverId(request.getDeptId()));
        save(entity);

        if (submitNow) {
            submitForApproval(entity.getId(), hrEmployeeId);
        }
        log.info("入职申请创建: id={}, name={}, submitNow={}", entity.getId(), request.getCandidateName(), submitNow);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long onboardingId, Long hrEmployeeId) {
        HrOnboarding entity = getById(onboardingId);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR, "入职申请不存在");
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "该申请已提交审批");

        ApprovalRecord record = approvalService.startApproval(
                "ONBOARDING", entity.getId(), hrEmployeeId, entity.getCandidateName(),
                entity.getDeptId());  // 按候选人入职部门解析部门负责人
        entity.setRecordId(record.getId());
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOnboarding(Long id, OnboardingAddRequest request, Long hrEmployeeId) {
        HrOnboarding entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "已提交审批的申请不可编辑");
        validateRequest(request);
        BeanUtils.copyProperties(request, entity, "id", "businessNo", "operatorId", "recordId", "employeeId", "approverId", "createTime");
        entity.setApproverId(resolveApproverId(request.getDeptId()));
        updateById(entity);
    }

    @Override
    public void deleteOnboarding(Long id, Long hrEmployeeId) {
        HrOnboarding entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "已提交审批的申请不可删除");
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOnboarding(Long id, Date actualEntryDate, Long hrEmployeeId) {
        HrOnboarding entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "请先提交审批");
        ThrowUtils.throwIf(entity.getEmployeeId() != null, ErrorCode.OPERATION_ERROR, "该申请已确认入职，不可重复操作");

        ApprovalRecord record = approvalService.getById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"APPROVED".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有审批通过的申请可确认到岗");

        ThrowUtils.throwIf(actualEntryDate == null, ErrorCode.PARAMS_ERROR, "入职日期不能为空");

        internalConfirmOnboarding(entity, actualEntryDate);
    }

    private void internalConfirmOnboarding(HrOnboarding entity, Date hireDate) {
        String employeeNo = generateEmployeeNo(entity.getDeptId());

        Employee emp = new Employee();
        emp.setEmployeeName(entity.getCandidateName());
        emp.setEmployeeNo(employeeNo);
        emp.setGender(0);
        emp.setPhone(entity.getPhone());
        emp.setEmail(entity.getEmail());
        emp.setDepartmentId(entity.getDeptId());
        emp.setPositionId(entity.getPositionId());
        emp.setEmploymentType(entity.getEmploymentType());
        emp.setStatus(EmployeeStatus.PROBATION.getCode());
        emp.setHireDate(hireDate);
        emp.setAccount(entity.getPhone());
        employeeMapper.insert(emp);

        EmployeeDetail detail = new EmployeeDetail();
        detail.setEmployeeId(emp.getId());
        detail.setAccount(entity.getPhone());
        detail.setIdCard(entity.getIdCard());
        detail.setBaseSalary(entity.getBaseSalary());
        detail.setBankAccount(entity.getBankAccount());
        detail.setBankName(entity.getBankName());
        employeeDetailMapper.insert(detail);

        Long roleId = determineRoleId(entity.getPositionId());

        User user = new User();
        user.setUserAccount(entity.getPhone());
        user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + "12345678").getBytes()));
        user.setUserName(entity.getCandidateName());
        user.setRoleId(roleId);
        userMapper.insert(user);

        emp.setUserId(user.getId());
        employeeMapper.updateById(emp);

        entity.setEmployeeId(emp.getId());
        updateById(entity);

        insertMutationLog(entity, emp.getId(), "APPROVED", hireDate);

        log.info("入职确认完成: name={}, employeeNo={}, roleId={}", entity.getCandidateName(), employeeNo, roleId);
    }

    private void insertMutationLog(HrOnboarding entity, Long employeeId, String approvalStatus, Date effectDate) {
        EmpMutationLog log = new EmpMutationLog();
        log.setBusinessType("ONBOARDING");
        log.setBusinessId(entity.getId());
        log.setBusinessNo(entity.getBusinessNo());
        log.setEmployeeId(employeeId);
        log.setEmployeeName(entity.getCandidateName());
        log.setDeptId(entity.getDeptId());
        Department dept = departmentMapper.selectById(entity.getDeptId());
        log.setDeptName(dept != null ? dept.getDeptName() : "");
        log.setEffectDate(effectDate);
        log.setApprovalStatus(approvalStatus);
        log.setOperatorId(entity.getOperatorId());
        if (entity.getOperatorId() != null) {
            Employee operator = employeeMapper.selectById(entity.getOperatorId());
            log.setOperatorName(operator != null ? operator.getEmployeeName() : "");
        } else {
            log.setOperatorName("");
        }
        log.setCreateTime(new Date());
        empMutationLogMapper.insert(log);
    }

    private Long determineRoleId(Long positionId) {
        if (positionId == null) {
            return 5L;
        }
        Position position = positionMapper.selectById(positionId);
        if (position != null && position.getSequence() != null && position.getSequence() == 1) {
            return 3L;
        }
        return 5L;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandonOnboarding(Long id, Long hrEmployeeId) {
        HrOnboarding entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "请先提交审批");

        ApprovalRecord record = approvalService.getById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"APPROVED".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有审批通过的申请可放弃");

        ApprovalRecord updateRecord = new ApprovalRecord();
        updateRecord.setId(record.getId());
        updateRecord.setStatus("CANCELLED");
        approvalService.updateById(updateRecord);
        log.info("入职申请已放弃: id={}, name={}", entity.getId(), entity.getCandidateName());
    }

    @Override
    public Page<OnboardingVO> listOnboarding(String keyword, List<String> statuses, int page, int size) {
        LambdaQueryWrapper<HrOnboarding> wrapper = new LambdaQueryWrapper<HrOnboarding>()
                .like(StringUtils.hasText(keyword), HrOnboarding::getCandidateName, keyword)
                .orderByDesc(HrOnboarding::getCreateTime);

        if (statuses != null && !statuses.isEmpty()) {
            if ("DRAFT".equals(statuses.get(0))) {
                wrapper.isNull(HrOnboarding::getRecordId);
            } else {
                List<Long> recordIds = approvalRecordMapper.selectList(
                        new LambdaQueryWrapper<ApprovalRecord>()
                                .eq(ApprovalRecord::getBusinessType, "ONBOARDING")
                                .in(ApprovalRecord::getStatus, statuses)
                ).stream().map(ApprovalRecord::getBusinessId).collect(Collectors.toList());

                if (!recordIds.isEmpty()) {
                    wrapper.in(HrOnboarding::getId, recordIds);
                }
            }
        }

        Page<HrOnboarding> entityPage = page(new Page<>(page, size), wrapper);
        Set<Long> deptIds = entityPage.getRecords().stream().map(HrOnboarding::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> posIds = entityPage.getRecords().stream().map(HrOnboarding::getPositionId).filter(Objects::nonNull).collect(Collectors.toSet());

        Page<OnboardingVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(e -> {
            OnboardingVO vo = new OnboardingVO();
            BeanUtils.copyProperties(e, vo);
            vo.setDeptName(getDeptName(e.getDeptId()));
            vo.setPositionName(getPosName(e.getPositionId()));
            vo.setApproverName(getEmployeeName(e.getApproverId()));
            if (e.getRecordId() != null) {
                ApprovalRecord record = approvalService.getById(e.getRecordId());
                if (record != null) {
                    vo.setApprovalStatus(record.getStatus());
                    vo.setApprovalProgress(record.getCurrentStep() + "/" + record.getTotalSteps());
                }
            }
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public OnboardingVO getOnboardingDetail(Long id) {
        HrOnboarding e = getById(id);
        ThrowUtils.throwIf(e == null, ErrorCode.NOT_FOUND_ERROR);
        OnboardingVO vo = new OnboardingVO();
        BeanUtils.copyProperties(e, vo);
        vo.setDeptName(getDeptName(e.getDeptId()));
        vo.setPositionName(getPosName(e.getPositionId()));
        vo.setApproverName(getEmployeeName(e.getApproverId()));
        if (e.getRecordId() != null) {
            ApprovalRecord record = approvalService.getById(e.getRecordId());
            if (record != null) {
                vo.setApprovalStatus(record.getStatus());
                vo.setApprovalProgress(record.getCurrentStep() + "/" + record.getTotalSteps());
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalPassed(Long businessId) {
        HrOnboarding entity = getById(businessId);
        if (entity != null) {
            log.info("入职审批通过: id={}, name={}", entity.getId(), entity.getCandidateName());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalRejected(Long businessId) {
        HrOnboarding entity = getById(businessId);
        if (entity != null) {
            log.info("入职审批拒绝: id={}, name={}", entity.getId(), entity.getCandidateName());
            if (entity.getEmployeeId() != null) {
                insertMutationLog(entity, entity.getEmployeeId(), "REJECTED", entity.getHireDate());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void employeeConfirm(Long id, Long employeeId) {
        HrOnboarding entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);

        ApprovalRecord record = approvalService.getById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"APPROVED".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有审批通过的申请可确认");

        ThrowUtils.throwIf(entity.getEmployeeId() != null, ErrorCode.OPERATION_ERROR, "该申请已确认入职");

        Date hireDate = entity.getHireDate() != null ? entity.getHireDate() : new Date();
        internalConfirmOnboarding(entity, hireDate);

        log.info("员工确认入职: onboardingId={}, candidateName={}", id, entity.getCandidateName());
    }

    @Override
    public java.util.List<MutationLogVO> getEmployeeMutationLogs(Long userId) {
        Employee emp = employeeService.getByUserId(userId);
        if (emp == null) {
            return new java.util.ArrayList<>();
        }

        List<EmpMutationLog> logs = empMutationLogMapper.selectList(
                new LambdaQueryWrapper<EmpMutationLog>()
                        .eq(EmpMutationLog::getEmployeeId, emp.getId())
                        .orderByDesc(EmpMutationLog::getCreateTime)
        );

        return logs.stream().map(log -> {
            MutationLogVO vo = new MutationLogVO();
            BeanUtils.copyProperties(log, vo);

            HrOnboarding onboarding = getById(log.getBusinessId());
            if (onboarding != null && onboarding.getRecordId() != null) {
                List<ApprovalDetail> details = approvalService.getApprovalDetails(onboarding.getRecordId());
                vo.setApprovalDetails(details);
            }

            return vo;
        }).collect(Collectors.toList());
    }

    private void validateRequest(OnboardingAddRequest req) {
        ThrowUtils.throwIf(!StringUtils.hasText(req.getCandidateName()), ErrorCode.PARAMS_ERROR, "姓名不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getPhone()), ErrorCode.PARAMS_ERROR, "手机号不能为空");
        ThrowUtils.throwIf(!req.getPhone().matches("^1[3-9]\\d{9}$"), ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getEmail()), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        ThrowUtils.throwIf(req.getDeptId() == null, ErrorCode.PARAMS_ERROR, "所属部门不能为空");
        ThrowUtils.throwIf(req.getPositionId() == null, ErrorCode.PARAMS_ERROR, "职位不能为空");
        ThrowUtils.throwIf(req.getHireDate() == null, ErrorCode.PARAMS_ERROR, "预定入职日期不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getEmploymentType()), ErrorCode.PARAMS_ERROR, "录用类型不能为空");
    }

    private String generateBusinessNo() {
        String prefix = "ON" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        long maxSeq = 0;
        try {
            var wrapper = new LambdaQueryWrapper<HrOnboarding>()
                    .likeRight(HrOnboarding::getBusinessNo, prefix)
                    .orderByDesc(HrOnboarding::getBusinessNo)
                    .last("LIMIT 1");
            HrOnboarding last = getOne(wrapper);
            if (last != null && last.getBusinessNo() != null) {
                String seqStr = last.getBusinessNo().substring(prefix.length());
                maxSeq = Long.parseLong(seqStr);
            }
        } catch (Exception e) { maxSeq = 0; }
        return prefix + String.format("%04d", maxSeq + 1);
    }

    private String generateEmployeeNo(Long departmentId) {
        String year = new SimpleDateFormat("yyyy").format(new Date());
        String deptCode = "00";
        if (departmentId != null) {
            Department dept = departmentMapper.selectById(departmentId);
            if (dept != null && StringUtils.hasText(dept.getDeptCode())) deptCode = dept.getDeptCode();
        }
        String prefix = year + deptCode;
        long maxSeq = 0;
        try {
            var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                    .likeRight("employeeNo", prefix).orderByDesc("employeeNo").last("LIMIT 1");
            Employee last = employeeMapper.selectOne(wrapper);
            if (last != null && last.getEmployeeNo() != null && last.getEmployeeNo().length() == 9)
                maxSeq = Long.parseLong(last.getEmployeeNo().substring(6));
        } catch (Exception e) { maxSeq = 0; }
        return prefix + String.format("%03d", maxSeq + 1);
    }

    private String generateRandomPwd() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) return null;
        Department d = departmentMapper.selectById(deptId);
        return d != null ? d.getDeptName() : null;
    }

    private String getPosName(Long posId) {
        if (posId == null) return null;
        Position p = positionMapper.selectById(posId);
        return p != null ? p.getName() : null;
    }

    /** 根据部门ID解析部门负责人ID */
    private Long resolveApproverId(Long deptId) {
        if (deptId == null) return null;
        Department dept = departmentMapper.selectById(deptId);
        return dept != null ? dept.getManagerId() : null;
    }

    /** 根据员工ID获取员工姓名 */
    private String getEmployeeName(Long employeeId) {
        if (employeeId == null) return null;
        Employee emp = employeeMapper.selectById(employeeId);
        return emp != null ? emp.getEmployeeName() : null;
    }

    @Override
    public List<ApprovalPendingVO> getDeptManagerOnboardingPendingList(Long employeeId) {
        List<Long> managedDeptIds = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getManagerId, employeeId)
        ).stream()
                .map(Department::getId)
                .collect(Collectors.toList());

        if (managedDeptIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<ApprovalRecord> deptRecords = approvalRecordMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .in(ApprovalRecord::getDepartmentId, managedDeptIds)
                        .eq(ApprovalRecord::getStatus, "APPROVING")
                        .eq(ApprovalRecord::getBusinessType, "ONBOARDING")
        );

        Set<Long> seenRecordIds = new HashSet<>();
        List<ApprovalPendingVO> result = new ArrayList<>();
        for (ApprovalRecord record : deptRecords) {
            if (!seenRecordIds.add(record.getId())) continue;

            ApprovalDetail detail = approvalDetailMapper.selectOne(
                    new LambdaQueryWrapper<ApprovalDetail>()
                            .eq(ApprovalDetail::getRecordId, record.getId())
                            .eq(ApprovalDetail::getStepOrder, record.getCurrentStep())
                            .eq(ApprovalDetail::getAction, "PENDING")
            );

            if (detail == null) continue;

            ApprovalPendingVO vo = new ApprovalPendingVO();
            vo.setRecordId(record.getId());
            vo.setDetailId(detail.getId());
            vo.setBusinessType(record.getBusinessType());
            vo.setBusinessTypeText("入职审批");
            vo.setBusinessId(record.getBusinessId());
            vo.setApplicantName(record.getApplicantName());
            vo.setApplyTime(record.getCreateTime());
            vo.setCurrentNodeName(detail.getNodeName());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deptManagerApprove(Long detailId, Long employeeId, String comment) {
        ApprovalDetail detail = validateDeptManagerApproval(detailId, employeeId);

        detail.setAction("APPROVE");
        detail.setComment(comment);
        detail.setOperateTime(new Date());
        approvalDetailMapper.updateById(detail);

        ApprovalRecord record = approvalRecordMapper.selectById(detail.getRecordId());
        ThrowUtils.throwIf(record == null, ErrorCode.APPROVAL_NOT_FOUND);

        advanceApproval(record.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deptManagerReject(Long detailId, Long employeeId, String comment) {
        ApprovalDetail detail = validateDeptManagerApproval(detailId, employeeId);

        detail.setAction("REJECT");
        detail.setComment(comment);
        detail.setOperateTime(new Date());
        approvalDetailMapper.updateById(detail);

        ApprovalRecord record = approvalRecordMapper.selectById(detail.getRecordId());
        ThrowUtils.throwIf(record == null, ErrorCode.APPROVAL_NOT_FOUND);

        record.setStatus("REJECTED");
        record.setFinishedAt(new Date());
        approvalRecordMapper.updateById(record);

        if ("ONBOARDING".equals(record.getBusinessType())) {
            onApprovalRejected(record.getBusinessId());
        }
    }

    private ApprovalDetail validateDeptManagerApproval(Long detailId, Long employeeId) {
        ApprovalDetail detail = approvalDetailMapper.selectById(detailId);
        ThrowUtils.throwIf(detail == null, ErrorCode.APPROVAL_NOT_FOUND);
        ThrowUtils.throwIf(!"PENDING".equals(detail.getAction()), ErrorCode.APPROVAL_NOT_PENDING);

        ApprovalRecord record = approvalRecordMapper.selectById(detail.getRecordId());
        ThrowUtils.throwIf(record == null, ErrorCode.APPROVAL_NOT_FOUND);

        boolean hasPermission = false;

        if (detail.getApproverId() != null && Objects.equals(detail.getApproverId(), employeeId)) {
            hasPermission = true;
        }

        if (!hasPermission && record.getDepartmentId() != null) {
            Department dept = departmentMapper.selectById(record.getDepartmentId());
            if (dept != null && Objects.equals(dept.getManagerId(), employeeId)) {
                hasPermission = true;
            }
        }

        ThrowUtils.throwIf(!hasPermission, ErrorCode.APPROVAL_NO_PERMISSION);

        if (!Objects.equals(detail.getApproverId(), employeeId)) {
            detail.setIsDelegated(1);
            detail.setDelegatedBy(detail.getApproverId());
            detail.setApproverId(employeeId);
        }

        return detail;
    }

    private void advanceApproval(Long recordId) {
        ApprovalRecord record = approvalRecordMapper.selectById(recordId);
        List<ApprovalDetail> details = approvalDetailMapper.selectList(
                new LambdaQueryWrapper<ApprovalDetail>()
                        .eq(ApprovalDetail::getRecordId, recordId)
                        .orderByAsc(ApprovalDetail::getStepOrder)
        );

        int currentStep = record.getCurrentStep();
        boolean allPassed = true;
        int maxStep = 0;

        for (ApprovalDetail detail : details) {
            if (detail.getStepOrder() <= currentStep) {
                if (!"APPROVE".equals(detail.getAction())) {
                    allPassed = false;
                    break;
                }
            }
            maxStep = Math.max(maxStep, detail.getStepOrder());
        }

        if (allPassed && currentStep < maxStep) {
            record.setCurrentStep(currentStep + 1);
            approvalRecordMapper.updateById(record);
        } else if (allPassed && currentStep >= maxStep) {
            record.setStatus("APPROVED");
            record.setFinishedAt(new Date());
            approvalRecordMapper.updateById(record);

            if ("ONBOARDING".equals(record.getBusinessType())) {
                onApprovalPassed(record.getBusinessId());
            }
        }
    }

    @Override
    public List<UserVO> getTransferableUsers() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .in(User::getRoleId, 1, 3)
                        .eq(User::getIsDelete, 0)
                        .orderByAsc(User::getId)
        );
        return users.stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
