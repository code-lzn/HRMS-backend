package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.limou.hrms.service.EmpSalaryProfileService;
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
    private EmpSalaryProfileService salaryProfileService;
    @Resource
    private ApprovalDetailMapper approvalDetailMapper;
    @Resource
    private ApprovalRecordMapper approvalRecordMapper;

    private static final String SALT = "hrms";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrOnboarding addOnboarding(OnboardingAddRequest request, Long hrEmployeeId, boolean submitNow) {
        validateRequest(request);
        checkDuplicateIdCard(request.getIdCard());
        checkDuplicatePhone(request.getPhone());
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

        // 清理可能孤立的审批记录（例如撤回后 recordId 已清空但旧记录残留）
        if (entity.getRecordId() != null) {
            ApprovalRecord existingRecord = approvalRecordMapper.selectById(entity.getRecordId());
            if (existingRecord == null) {
                // recordId 指向不存在的记录，清空它
                update(new LambdaUpdateWrapper<HrOnboarding>()
                        .set(HrOnboarding::getRecordId, null)
                        .eq(HrOnboarding::getId, onboardingId));
            } else {
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "该申请已提交审批");
            }
        }

        // 兜底：通过 uk_business 查找并删除任何残留的审批记录
        ApprovalRecord orphan = approvalRecordMapper.selectOne(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getBusinessType, "ONBOARDING")
                        .eq(ApprovalRecord::getBusinessId, onboardingId));
        if (orphan != null) {
            approvalDetailMapper.delete(new LambdaQueryWrapper<ApprovalDetail>()
                    .eq(ApprovalDetail::getRecordId, orphan.getId()));
            approvalRecordMapper.deleteById(orphan.getId());
        }

        Map<Integer, Long> overrides = new HashMap<>();
        if (entity.getApproverId() != null) {
            overrides.put(1, entity.getApproverId());
        }
        ApprovalRecord record = approvalService.startApproval(
                "ONBOARDING", entity.getId(), hrEmployeeId, entity.getCandidateName(),
                entity.getDeptId(), overrides);
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
    @Transactional(rollbackFor = Exception.class)
    public void deleteOnboarding(Long id, Long hrEmployeeId) {
        HrOnboarding entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);

        if (entity.getRecordId() != null) {
            ApprovalRecord record = approvalRecordMapper.selectById(entity.getRecordId());
            if (record != null) {
                ThrowUtils.throwIf(!"APPROVING".equals(record.getStatus()),
                        ErrorCode.OPERATION_ERROR, "仅审批中的申请可删除");
                approvalDetailMapper.delete(new LambdaQueryWrapper<ApprovalDetail>()
                        .eq(ApprovalDetail::getRecordId, record.getId()));
                approvalRecordMapper.deleteById(record.getId());
            }
        }
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
        emp.setGender("MALE".equals(entity.getGender()) ? 1 : 0);
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
        detail.setBirthday(entity.getBirthday());
        detail.setRegisteredAddress(entity.getRegisteredAddress());
        detail.setCurrentAddress(entity.getCurrentAddress());
        detail.setWorkLocation(entity.getWorkLocation());
        detail.setBaseSalary(entity.getBaseSalary());
        detail.setBankAccount(entity.getBankAccount());
        detail.setBankName(entity.getBankName());
        detail.setDirectReportId(entity.getDirectReportId());
        detail.setContractType(entity.getContractType());
        employeeDetailMapper.insert(detail);

        Long roleId = determineRoleId(entity.getPositionId());

        User user = new User();
        user.setUserAccount(entity.getPhone());
        user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + "12345678").getBytes()));
        user.setUserName(entity.getCandidateName());
        user.setRoleId(roleId);
        user.setEmployeeId(emp.getId());
        user.setUserAvatar("https://gd-hbimg.huaban.com/08aaeb96f1f7360a2016ab5da1d6dd2d8f9933b62f9137-uqfbvd_fw658webp");
        userMapper.insert(user);

        emp.setUserId(user.getId());
        employeeMapper.updateById(emp);

        entity.setEmployeeId(emp.getId());
        updateById(entity);

        // 更新审批记录状态为已入职
        ApprovalRecord record = approvalRecordMapper.selectById(entity.getRecordId());
        if (record != null) {
            record.setStatus("ONBOARDED");
            record.setFinishedAt(new Date());
            approvalRecordMapper.updateById(record);
        }

        insertMutationLog(entity, emp.getId(), "APPROVED", hireDate);

        // 创建薪资档案
        EmpSalaryProfile salaryProfile = new EmpSalaryProfile();
        salaryProfile.setEmployeeId(emp.getId());
        salaryProfile.setBaseSalary(entity.getBaseSalary());
        salaryProfile.setSocialInsuranceBase(entity.getSocialInsuranceBase());
        salaryProfile.setHousingFundBase(entity.getHousingFundBase());
        salaryProfile.setBankAccount(entity.getBankAccount());
        salaryProfile.setBankName(entity.getBankName());
        salaryProfile.setEffectiveDate(hireDate);
        salaryProfile.setAllowanceBase(java.math.BigDecimal.ZERO);
        salaryProfile.setPerformanceBase(java.math.BigDecimal.ZERO);
        salaryProfileService.save(salaryProfile);

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
    @Transactional(rollbackFor = Exception.class)
    public void revokeOnboarding(Long id, Long hrEmployeeId) {
        HrOnboarding entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "该申请未提交审批");

        ApprovalRecord record = approvalService.getById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"APPROVING".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有审批中的申请可撤回");
        ThrowUtils.throwIf(record.getCurrentStep() == null || record.getCurrentStep() > 1,
                ErrorCode.OPERATION_ERROR, "仅第一级审批前可撤回");

        // 删除审批明细
        approvalDetailMapper.delete(new LambdaQueryWrapper<ApprovalDetail>()
                .eq(ApprovalDetail::getRecordId, record.getId()));
        // 删除审批记录（否则唯一键 uk_business 冲突，无法重新提交）
        approvalRecordMapper.deleteById(record.getId());

        update(new LambdaUpdateWrapper<HrOnboarding>()
                .set(HrOnboarding::getRecordId, null)
                .set(HrOnboarding::getFlowId, null)
                .eq(HrOnboarding::getId, id));
        log.info("入职申请已撤回: id={}, name={}", entity.getId(), entity.getCandidateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHireDate(Long id, Date newHireDate, Long hrEmployeeId) {
        ThrowUtils.throwIf(newHireDate == null, ErrorCode.PARAMS_ERROR, "入职日期不能为空");
        HrOnboarding entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "该申请未提交审批");

        ApprovalRecord record = approvalService.getById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"APPROVED".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有审批通过的申请可修改入职日期");

        entity.setHireDate(newHireDate);
        updateById(entity);
        log.info("入职日期已修改: id={}, name={}, hireDate={}", entity.getId(), entity.getCandidateName(), newHireDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitOnboarding(Long id, Long hrEmployeeId) {
        HrOnboarding entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "该申请未提交审批");

        ApprovalRecord record = approvalService.getById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"REJECTED".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有已拒绝的申请可重新发起");

        // 删除旧审批明细和记录（否则唯一键 uk_business 冲突）
        approvalDetailMapper.delete(new LambdaQueryWrapper<ApprovalDetail>()
                .eq(ApprovalDetail::getRecordId, record.getId()));
        approvalRecordMapper.deleteById(record.getId());

        update(new LambdaUpdateWrapper<HrOnboarding>()
                .set(HrOnboarding::getRecordId, null)
                .set(HrOnboarding::getFlowId, null)
                .set(HrOnboarding::getEmployeeId, null)
                .eq(HrOnboarding::getId, id));

        submitForApproval(entity.getId(), hrEmployeeId);
        log.info("入职申请重新发起: id={}, name={}", entity.getId(), entity.getCandidateName());
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
                } else {
                    // 没有匹配的审批记录，直接返回空页
                    Page<OnboardingVO> empty = new Page<>(page, size, 0);
                    empty.setRecords(Collections.emptyList());
                    return empty;
                }
            }
        }

        Page<HrOnboarding> entityPage = page(new Page<>(page, size), wrapper);
        List<HrOnboarding> entities = entityPage.getRecords();
        if (entities.isEmpty()) {
            Page<OnboardingVO> empty = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        // 批量加载所有关联数据
        Set<Long> deptIds = entities.stream().map(HrOnboarding::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> posIds = entities.stream().map(HrOnboarding::getPositionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> empIds = new HashSet<>();
        entities.forEach(e -> {
            if (e.getApproverId() != null) empIds.add(e.getApproverId());
            if (e.getDirectReportId() != null) empIds.add(e.getDirectReportId());
        });
        Set<Long> recordIds = entities.stream().map(HrOnboarding::getRecordId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> deptNameMap = batchLoadDeptNames(deptIds);
        Map<Long, String> posNameMap = batchLoadPosNames(posIds);
        Map<Long, String> empNameMap = batchLoadEmpNames(empIds);
        Map<Long, ApprovalRecord> recordMap = batchLoadApprovalRecords(recordIds);

        // 批量获取拒绝原因
        Map<Long, String> rejectionMap = new HashMap<>();
        List<ApprovalDetail> rejectedDetails = approvalDetailMapper.selectList(
                new LambdaQueryWrapper<ApprovalDetail>()
                        .in(ApprovalDetail::getRecordId, recordIds)
                        .eq(ApprovalDetail::getAction, "REJECT")
        );
        for (ApprovalDetail d : rejectedDetails) {
            if (d.getComment() != null) {
                rejectionMap.put(d.getRecordId(), d.getComment());
            }
        }

        Page<OnboardingVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entities.stream().map(e -> {
            OnboardingVO vo = new OnboardingVO();
            BeanUtils.copyProperties(e, vo);
            vo.setDeptName(deptNameMap.get(e.getDeptId()));
            vo.setPositionName(posNameMap.get(e.getPositionId()));
            vo.setApproverName(empNameMap.get(e.getApproverId()));
            vo.setDirectReportId(e.getDirectReportId());
            vo.setDirectReportName(empNameMap.get(e.getDirectReportId()));
            if (e.getRecordId() != null) {
                ApprovalRecord record = recordMap.get(e.getRecordId());
                if (record != null) {
                    vo.setApprovalStatus(record.getStatus());
                    vo.setApprovalProgress(record.getCurrentStep() + "/" + record.getTotalSteps());
                    if ("REJECTED".equals(record.getStatus())) {
                        vo.setRejectionReason(rejectionMap.get(e.getRecordId()));
                    }
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

        Set<Long> deptIds = e.getDeptId() != null ? Collections.singleton(e.getDeptId()) : Collections.emptySet();
        Set<Long> posIds = e.getPositionId() != null ? Collections.singleton(e.getPositionId()) : Collections.emptySet();
        Set<Long> empIds = new HashSet<>();
        if (e.getApproverId() != null) empIds.add(e.getApproverId());
        if (e.getDirectReportId() != null) empIds.add(e.getDirectReportId());

        Map<Long, String> deptNameMap = batchLoadDeptNames(deptIds);
        Map<Long, String> posNameMap = batchLoadPosNames(posIds);
        Map<Long, String> empNameMap = batchLoadEmpNames(empIds);

        OnboardingVO vo = new OnboardingVO();
        BeanUtils.copyProperties(e, vo);
        vo.setDeptName(deptNameMap.get(e.getDeptId()));
        vo.setPositionName(posNameMap.get(e.getPositionId()));
        vo.setApproverName(empNameMap.get(e.getApproverId()));
        vo.setDirectReportId(e.getDirectReportId());
        vo.setDirectReportName(empNameMap.get(e.getDirectReportId()));
        if (e.getRecordId() != null) {
            ApprovalRecord record = approvalService.getById(e.getRecordId());
            if (record != null) {
                vo.setApprovalStatus(record.getStatus());
                vo.setApprovalProgress(record.getCurrentStep() + "/" + record.getTotalSteps());
                if ("REJECTED".equals(record.getStatus())) {
                    ApprovalDetail rejectedDetail = approvalDetailMapper.selectOne(
                            new LambdaQueryWrapper<ApprovalDetail>()
                                    .eq(ApprovalDetail::getRecordId, e.getRecordId())
                                    .eq(ApprovalDetail::getAction, "REJECT")
                                    .orderByDesc(ApprovalDetail::getOperateTime)
                                    .last("LIMIT 1")
                    );
                    if (rejectedDetail != null) {
                        vo.setRejectionReason(rejectedDetail.getComment());
                    }
                }
            }
        }
        return vo;
    }

    // ===== 批量加载辅助方法 =====

    private Map<Long, String> batchLoadDeptNames(Set<Long> deptIds) {
        if (deptIds.isEmpty()) return Collections.emptyMap();
        return departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, d -> d.getDeptName() != null ? d.getDeptName() : ""));
    }

    private Map<Long, String> batchLoadPosNames(Set<Long> posIds) {
        if (posIds.isEmpty()) return Collections.emptyMap();
        return positionMapper.selectBatchIds(posIds).stream()
                .collect(Collectors.toMap(Position::getId, p -> p.getName() != null ? p.getName() : ""));
    }

    private Map<Long, String> batchLoadEmpNames(Set<Long> empIds) {
        if (empIds.isEmpty()) return Collections.emptyMap();
        return employeeMapper.selectBatchIds(empIds).stream()
                .collect(Collectors.toMap(Employee::getId, emp -> emp.getEmployeeName() != null ? emp.getEmployeeName() : ""));
    }

    private Map<Long, ApprovalRecord> batchLoadApprovalRecords(Set<Long> recordIds) {
        if (recordIds.isEmpty()) return Collections.emptyMap();
        return approvalService.listByIds(recordIds).stream()
                .collect(Collectors.toMap(ApprovalRecord::getId, r -> r));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalPassed(Long businessId) {
        HrOnboarding entity = getById(businessId);
        if (entity == null) {
            return;
        }
        log.info("入职审批通过，等待HR确认入职: id={}, name={}", entity.getId(), entity.getCandidateName());
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
        ThrowUtils.throwIf(req.getDirectReportId() == null, ErrorCode.PARAMS_ERROR, "直接汇报人不能为空");
        ThrowUtils.throwIf(req.getBaseSalary() == null, ErrorCode.PARAMS_ERROR, "基本工资不能为空");
    }

    private void checkDuplicateIdCard(String idCard) {
        if (!StringUtils.hasText(idCard)) {
            return;
        }
        long count = lambdaQuery()
                .eq(HrOnboarding::getIdCard, idCard)
                .count();
        ThrowUtils.throwIf(count > 0, ErrorCode.OPERATION_ERROR, "该身份证号已存在入职申请，不可重复提交");
    }

    private void checkDuplicatePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        long onboardingCount = lambdaQuery()
                .eq(HrOnboarding::getPhone, phone)
                .count();
        ThrowUtils.throwIf(onboardingCount > 0, ErrorCode.OPERATION_ERROR, "该手机号已存在入职申请，不可重复提交");

        long employeeCount = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getPhone, phone)
        );
        ThrowUtils.throwIf(employeeCount > 0, ErrorCode.OPERATION_ERROR, "该手机号已被员工使用，不可重复提交");
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
            if (dept != null && dept.getDeptCode() != null) deptCode = dept.getDeptCode().trim();
        }
        String prefix = year + deptCode;
        long maxSeq = 0;
        try {
            String maxNo = employeeMapper.selectMaxEmployeeNoByPrefix(prefix);
            if (maxNo != null && maxNo.length() == 9)
                maxSeq = Long.parseLong(maxNo.substring(6));
        } catch (Exception e) { maxSeq = 1; }
        return prefix + String.format("%03d", maxSeq + 1);
    }

    private String generateRandomPwd() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    /** 根据部门ID解析部门负责人ID */
    private Long resolveApproverId(Long deptId) {
        if (deptId == null) return null;
        Department dept = departmentMapper.selectById(deptId);
        return dept != null ? dept.getManagerId() : null;
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

    @Override
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();

        // 审批中/已批准/已入职/已拒绝：从 approval_record 按状态统计
        List<ApprovalRecord> records = approvalRecordMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getBusinessType, "ONBOARDING"));
        Map<String, Long> statusCounts = records.stream()
                .collect(Collectors.groupingBy(ApprovalRecord::getStatus, Collectors.counting()));

        Set<Long> validRecordIds = records.stream()
                .map(ApprovalRecord::getId).collect(Collectors.toSet());

        // 草稿：recordId 为空，或指向已不存在的审批记录（孤儿 recordId）
        long draft = lambdaQuery()
                .and(w -> w.isNull(HrOnboarding::getRecordId)
                        .or().notIn(!validRecordIds.isEmpty(), HrOnboarding::getRecordId, validRecordIds))
                .count();
        stats.put("draft", draft);

        stats.put("approving", statusCounts.getOrDefault("APPROVING", 0L));
        stats.put("approved", statusCounts.getOrDefault("APPROVED", 0L));
        stats.put("onboarded", statusCounts.getOrDefault("ONBOARDED", 0L));

        return stats;
    }
}
