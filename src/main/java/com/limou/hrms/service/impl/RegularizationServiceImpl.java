package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.regularization.RegularizationAddRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.vo.RegularizationVO;
import com.limou.hrms.service.ApprovalService;
import com.limou.hrms.service.RegularizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RegularizationServiceImpl extends ServiceImpl<HrRegularizationMapper, HrRegularization>
        implements RegularizationService {

    @Resource
    private ApprovalService approvalService;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeeDetailMapper employeeDetailMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private EmpMutationLogMapper empMutationLogMapper;
    @Resource
    private EmployeeChangeLogMapper employeeChangeLogMapper;
    @Resource
    private EmpSalaryProfileMapper empSalaryProfileMapper;
    @Resource
    private SalChangeLogMapper salChangeLogMapper;
    @Resource
    private HrOnboardingMapper hrOnboardingMapper;
    @Resource
    private ApprovalFlowMapper approvalFlowMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrRegularization addRegularization(RegularizationAddRequest request, Long hrEmployeeId, boolean submitNow) {
        validateRequest(request);
        Employee emp = employeeMapper.selectById(request.getEmployeeId());
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工不存在");
        ThrowUtils.throwIf(!Objects.equals(emp.getStatus(), EmployeeStatus.PROBATION.getCode()),
                ErrorCode.OPERATION_ERROR, "只有试用期员工可发起转正");

        Date probationEnd = calcProbationEndDate(emp);

        HrRegularization entity = new HrRegularization();
        entity.setEmployeeId(request.getEmployeeId());
        entity.setProbationComment(request.getEvaluation());
        entity.setProbationScore(request.getProbationScore());
        entity.setSalaryAdjustment(request.getSalaryAdjustment());
        entity.setAdjustRemark(request.getAdjustRemark());
        entity.setResult(request.getResult());
        entity.setExtendedMonths(request.getExtendedMonths());
        entity.setOperatorId(hrEmployeeId);
        entity.setBusinessNo(generateBusinessNo());
        entity.setApproverId(resolveApproverId(emp.getDepartmentId()));
        entity.setOriginHireDate(emp.getHireDate());
        entity.setProbationEndDate(probationEnd);
        entity.setConfirmDate(probationEnd);
        entity.setFlowId(resolveFlowId(request.getFlowId()));
        entity.setStatus("DRAFT");

        EmpSalaryProfile profile = empSalaryProfileMapper.selectOne(
                new LambdaQueryWrapper<EmpSalaryProfile>()
                        .eq(EmpSalaryProfile::getEmployeeId, request.getEmployeeId()));
        if (profile != null && profile.getBaseSalary() != null) {
            entity.setConfirmBaseSalary(profile.getBaseSalary());
        } else {
            entity.setConfirmBaseSalary(BigDecimal.ZERO);
        }

        save(entity);

        if (submitNow) {
            submitForApproval(entity.getId(), hrEmployeeId);
        }
        log.info("转正申请创建: id={}, employeeId={}, submitNow={}", entity.getId(), request.getEmployeeId(), submitNow);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long id, Long hrEmployeeId) {
        HrRegularization entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR, "转正申请不存在");
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "该申请已提交审批");

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        String employeeName = emp != null ? emp.getEmployeeName() : "";

        ApprovalRecord record = approvalService.startApproval(
                "REGULARIZATION", entity.getId(), hrEmployeeId, employeeName,
                emp != null ? emp.getDepartmentId() : null);
        entity.setRecordId(record.getId());
        entity.setFlowId(record.getFlowId());
        entity.setStatus("APPROVING");
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRegularization(Long id, RegularizationAddRequest request, Long hrEmployeeId) {
        HrRegularization entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "已提交审批的申请不可编辑");
        validateRequest(request);

        entity.setEmployeeId(request.getEmployeeId());
        entity.setProbationComment(request.getEvaluation());
        entity.setProbationScore(request.getProbationScore());
        entity.setSalaryAdjustment(request.getSalaryAdjustment());
        entity.setAdjustRemark(request.getAdjustRemark());
        entity.setResult(request.getResult());
        entity.setExtendedMonths(request.getExtendedMonths());
        entity.setRemark(request.getRemark());

        Employee emp = employeeMapper.selectById(request.getEmployeeId());
        if (emp != null) {
            entity.setOriginHireDate(emp.getHireDate());
            entity.setProbationEndDate(calcProbationEndDate(emp));
            entity.setApproverId(resolveApproverId(emp.getDepartmentId()));
        }
        updateById(entity);
    }

    @Override
    public void deleteRegularization(Long id, Long hrEmployeeId) {
        HrRegularization entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "已提交审批的申请不可删除");
        removeById(id);
    }

    @Override
    public Page<RegularizationVO> listRegularization(String keyword, List<String> statuses, int page, int size) {
        LambdaQueryWrapper<HrRegularization> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(HrRegularization::getCreateTime);
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(HrRegularization::getStatus, statuses);
        }
        Page<HrRegularization> entityPage = page(new Page<>(page, size), wrapper);
        Page<RegularizationVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream()
                .map(e -> convertToVO(e, keyword))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public RegularizationVO getRegularizationDetail(Long id) {
        HrRegularization entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        return convertToVO(entity, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalPassed(Long businessId) {
        HrRegularization entity = getById(businessId);
        if (entity == null) return;

        entity.setStatus("APPROVED");
        entity.setConfirmDate(new Date());
        updateById(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp == null) return;

        boolean isExtend = "EXTEND".equals(entity.getResult());

        if (isExtend && entity.getExtendedMonths() != null && entity.getExtendedMonths() > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(entity.getProbationEndDate());
            cal.add(Calendar.MONTH, entity.getExtendedMonths());
            entity.setProbationEndDate(cal.getTime());
            updateById(entity);
            log.info("转正审批: 延长试用期, id={}, extendedMonths={}, newEndDate={}",
                    entity.getId(), entity.getExtendedMonths(), entity.getProbationEndDate());
        } else {
            emp.setStatus(EmployeeStatus.REGULAR.getCode());
            employeeMapper.updateById(emp);
            insertChangeLog(emp.getId(), "status",
                    String.valueOf(EmployeeStatus.PROBATION.getCode()),
                    String.valueOf(EmployeeStatus.REGULAR.getCode()),
                    "FLOW_CHANGE", entity.getOperatorId());
        }

        EmpSalaryProfile profile = empSalaryProfileMapper.selectOne(
                new LambdaQueryWrapper<EmpSalaryProfile>()
                        .eq(EmpSalaryProfile::getEmployeeId, entity.getEmployeeId()));
        if (profile != null) {
            String oldValue = toJsonString(profile);

            if (!isExtend && profile.getProbationSalaryRatio() != null
                    && profile.getProbationSalaryRatio().compareTo(BigDecimal.ONE) < 0) {
                profile.setProbationSalaryRatio(BigDecimal.ONE);
            }

            if (entity.getSalaryAdjustment() != null
                    && entity.getSalaryAdjustment().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal oldBase = profile.getBaseSalary() != null ? profile.getBaseSalary() : BigDecimal.ZERO;
                profile.setBaseSalary(oldBase.add(entity.getSalaryAdjustment()));
            }

            empSalaryProfileMapper.updateById(profile);

            SalChangeLog salLog = new SalChangeLog();
            salLog.setEmployeeId(entity.getEmployeeId());
            salLog.setChangeType(4);
            salLog.setOldValue(oldValue);
            salLog.setNewValue(toJsonString(profile));
            salLog.setEffectiveDate(new Date());
            salLog.setOperatorId(entity.getOperatorId());
            salLog.setRemark(isExtend ? "延长试用期" : "转正调薪");
            salChangeLogMapper.insert(salLog);

            entity.setConfirmBaseSalary(profile.getBaseSalary());
            updateById(entity);
        }

        insertMutationLog(entity, emp, "APPROVED");
        log.info("转正审批通过: id={}, employeeId={}, result={}", entity.getId(), entity.getEmployeeId(), entity.getResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalRejected(Long businessId) {
        HrRegularization entity = getById(businessId);
        if (entity == null) return;
        entity.setStatus("REJECTED");
        updateById(entity);
        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        insertMutationLog(entity, emp, "REJECTED");
    }

    @Override
    public List<Employee> getProbationEndingEmployees() {
        List<Employee> probEmployees = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getStatus, EmployeeStatus.PROBATION.getCode()));
        List<Employee> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        Date sevenDaysLater = cal.getTime();

        for (Employee emp : probEmployees) {
            if (emp.getHireDate() == null) continue;
            Date endDate = calcProbationEndDate(emp);
            if (endDate != null && !endDate.after(sevenDaysLater)) {
                Long count = lambdaQuery().eq(HrRegularization::getEmployeeId, emp.getId())
                        .in(HrRegularization::getStatus, Arrays.asList("DRAFT", "APPROVING"))
                        .count();
                if (count == 0) {
                    result.add(emp);
                }
            }
        }
        return result;
    }

    // ========== 私有方法 ==========

    private RegularizationVO convertToVO(HrRegularization e, String keyword) {
        RegularizationVO vo = new RegularizationVO();
        BeanUtils.copyProperties(e, vo);
        vo.setProbationStartDate(e.getOriginHireDate());
        vo.setEvaluation(e.getProbationComment());

        Employee emp = employeeMapper.selectById(e.getEmployeeId());
        if (emp != null) {
            vo.setEmployeeName(emp.getEmployeeName());
            vo.setEmployeeNo(emp.getEmployeeNo());
            vo.setDeptName(getDeptName(emp.getDepartmentId()));
            vo.setPositionName(getPosName(emp.getPositionId()));
        }
        if (e.getApproverId() != null) {
            Employee approver = employeeMapper.selectById(e.getApproverId());
            vo.setApproverName(approver != null ? approver.getEmployeeName() : null);
        }
        if (e.getOperatorId() != null) {
            Employee operator = employeeMapper.selectById(e.getOperatorId());
            vo.setOperatorName(operator != null ? operator.getEmployeeName() : null);
        }
        if (e.getRecordId() != null) {
            ApprovalRecord record = approvalService.getById(e.getRecordId());
            if (record != null) {
                vo.setApprovalStatus(record.getStatus());
                vo.setApprovalProgress(record.getCurrentStep() + "/" + record.getTotalSteps());
            }
        }
        if (StringUtils.hasText(keyword) && vo.getEmployeeName() != null
                && !vo.getEmployeeName().contains(keyword)) {
            return null;
        }
        return vo;
    }

    private void validateRequest(RegularizationAddRequest req) {
        ThrowUtils.throwIf(req.getEmployeeId() == null, ErrorCode.PARAMS_ERROR, "员工不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getEvaluation()), ErrorCode.PARAMS_ERROR, "试用期表现评价不能为空");
    }

    private String generateBusinessNo() {
        String prefix = "ZZ" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        long maxSeq = 0;
        try {
            var wrapper = new LambdaQueryWrapper<HrRegularization>()
                    .likeRight(HrRegularization::getBusinessNo, prefix)
                    .orderByDesc(HrRegularization::getBusinessNo)
                    .last("LIMIT 1");
            HrRegularization last = getOne(wrapper);
            if (last != null && last.getBusinessNo() != null) {
                String seqStr = last.getBusinessNo().substring(prefix.length());
                maxSeq = Long.parseLong(seqStr);
            }
        } catch (Exception ex) { maxSeq = 0; }
        return prefix + String.format("%04d", maxSeq + 1);
    }

    private Date calcProbationEndDate(Employee emp) {
        if (emp.getHireDate() == null) return null;
        int months = getProbationMonths(emp.getId());
        Calendar cal = Calendar.getInstance();
        cal.setTime(emp.getHireDate());
        cal.add(Calendar.MONTH, months);
        return cal.getTime();
    }

    private int getProbationMonths(Long employeeId) {
        try {
            HrOnboarding onboarding = hrOnboardingMapper.selectOne(
                    new LambdaQueryWrapper<HrOnboarding>()
                            .eq(HrOnboarding::getEmployeeId, employeeId)
                            .orderByDesc(HrOnboarding::getCreateTime)
                            .last("LIMIT 1"));
            if (onboarding != null && onboarding.getProbationMonth() != null) {
                return onboarding.getProbationMonth();
            }
        } catch (Exception ignored) {}
        return 3;
    }

    private Long resolveApproverId(Long deptId) {
        if (deptId == null) return null;
        Department dept = departmentMapper.selectById(deptId);
        return dept != null ? dept.getManagerId() : null;
    }

    private Long resolveFlowId(Long requestFlowId) {
        if (requestFlowId != null) return requestFlowId;
        try {
            ApprovalFlow flow = approvalFlowMapper.selectOne(
                    new LambdaQueryWrapper<ApprovalFlow>()
                            .eq(ApprovalFlow::getBusinessType, "REGULARIZATION")
                            .eq(ApprovalFlow::getStatus, 1)
                            .last("LIMIT 1"));
            if (flow != null) return flow.getId();
        } catch (Exception ignored) {}
        return 2L;
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

    private void insertMutationLog(HrRegularization entity, Employee emp, String approvalStatus) {
        EmpMutationLog log = new EmpMutationLog();
        log.setBusinessType("REGULARIZATION");
        log.setBusinessId(entity.getId());
        log.setBusinessNo(entity.getBusinessNo());
        log.setEmployeeId(entity.getEmployeeId());
        log.setEmployeeName(emp != null ? emp.getEmployeeName() : "");
        log.setDeptId(emp != null ? emp.getDepartmentId() : null);
        log.setDeptName(getDeptName(emp != null ? emp.getDepartmentId() : null));
        log.setEffectDate(new Date());
        log.setApprovalStatus(approvalStatus);
        log.setOperatorId(entity.getOperatorId());
        log.setOperatorName(getEmployeeName(entity.getOperatorId()));
        log.setCreateTime(new Date());
        empMutationLogMapper.insert(log);
    }

    private void insertChangeLog(Long employeeId, String fieldName, String oldValue, String newValue,
                                  String changeType, Long operatorId) {
        EmployeeChangeLog clog = new EmployeeChangeLog();
        clog.setEmployeeId(employeeId);
        clog.setFieldName(fieldName);
        clog.setOldValue(oldValue);
        clog.setNewValue(newValue);
        clog.setChangeType(changeType);
        clog.setOperatorId(operatorId);
        clog.setCreateTime(new Date());
        employeeChangeLogMapper.insert(clog);
    }

    private String getEmployeeName(Long employeeId) {
        if (employeeId == null) return "";
        Employee e = employeeMapper.selectById(employeeId);
        return e != null ? e.getEmployeeName() : "";
    }

    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON序列化失败", e);
            return "{}";
        }
    }
}
