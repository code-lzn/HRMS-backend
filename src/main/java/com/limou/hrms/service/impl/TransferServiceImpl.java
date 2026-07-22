package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.transfer.TransferAddRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.vo.TransferVO;
import com.limou.hrms.service.ApprovalService;
import com.limou.hrms.service.TransferService;
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
public class TransferServiceImpl extends ServiceImpl<HrTransferMapper, HrTransfer>
        implements TransferService {

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
    private ApprovalFlowMapper approvalFlowMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrTransfer addTransfer(TransferAddRequest request, Long hrEmployeeId, boolean submitNow) {
        validateRequest(request);
        Employee emp = employeeMapper.selectById(request.getEmployeeId());
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工不存在");
        ThrowUtils.throwIf(!Objects.equals(emp.getStatus(), EmployeeStatus.PROBATION.getCode())
                        && !Objects.equals(emp.getStatus(), EmployeeStatus.REGULAR.getCode()),
                ErrorCode.OPERATION_ERROR, "只有试用期或正式员工可发起调岗");

        ThrowUtils.throwIf(request.getToDeptId().equals(emp.getDepartmentId()),
                ErrorCode.OPERATION_ERROR, "新部门与当前部门相同");

        HrTransfer entity = new HrTransfer();
        entity.setEmployeeId(request.getEmployeeId());
        entity.setTargetDeptId(request.getToDeptId());
        entity.setTargetPositionId(request.getToPositionId());
        entity.setToRankCode(request.getToRankCode());
        entity.setToReporterId(request.getToReporterId());
        entity.setNewBaseSalary(request.getSalaryAdjustment());
        entity.setTransferReason(request.getReason());
        entity.setTransferDate(request.getEffectiveDate() != null ? request.getEffectiveDate() : new Date());
        entity.setFlowId(resolveFlowId(request.getFlowId()));
        entity.setRemark(request.getRemark());
        entity.setWorkLocation(request.getWorkLocation());
        entity.setEmploymentType(request.getEmploymentType());
        entity.setOperatorId(hrEmployeeId);
        entity.setBusinessNo(generateBusinessNo());
        entity.setSourceDeptId(emp.getDepartmentId() != null ? emp.getDepartmentId() : 0);
        entity.setSourcePositionId(emp.getPositionId() != null ? emp.getPositionId() : 0);
        entity.setStatus("DRAFT");
        save(entity);

        if (submitNow) {
            submitForApproval(entity.getId(), hrEmployeeId);
        }
        log.info("调岗申请创建: id={}, employeeId={}", entity.getId(), request.getEmployeeId());
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long id, Long hrEmployeeId) {
        HrTransfer entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR, "调岗申请不存在");
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "该申请已提交审批");

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        String employeeName = emp != null ? emp.getEmployeeName() : "";

        Long fromDeptManagerId = resolveApproverId(entity.getSourceDeptId());
        Long toDeptManagerId = resolveApproverId(entity.getTargetDeptId());

        Map<Integer, Long> overrides = new HashMap<>();
        if (fromDeptManagerId != null) overrides.put(1, fromDeptManagerId);
        if (toDeptManagerId != null) overrides.put(2, toDeptManagerId);
        // 节点3 (HR负责人) 不传覆盖，由 resolveApprover 动态查找HR角色用户

        ApprovalRecord record = approvalService.startApproval(
                "TRANSFER", entity.getId(), hrEmployeeId, employeeName,
                entity.getTargetDeptId(), overrides);
        entity.setRecordId(record.getId());
        entity.setStatus("APPROVING");
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTransfer(Long id, TransferAddRequest request, Long hrEmployeeId) {
        HrTransfer entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "已提交审批的申请不可编辑");
        validateRequest(request);

        entity.setEmployeeId(request.getEmployeeId());
        entity.setTargetDeptId(request.getToDeptId());
        entity.setTargetPositionId(request.getToPositionId());
        entity.setToRankCode(request.getToRankCode());
        entity.setToReporterId(request.getToReporterId());
        entity.setNewBaseSalary(request.getSalaryAdjustment());
        entity.setTransferReason(request.getReason());
        entity.setTransferDate(request.getEffectiveDate());
        entity.setFlowId(request.getFlowId());
        entity.setRemark(request.getRemark());
        entity.setWorkLocation(request.getWorkLocation());
        entity.setEmploymentType(request.getEmploymentType());

        Employee emp = employeeMapper.selectById(request.getEmployeeId());
        if (emp != null) {
            entity.setSourceDeptId(emp.getDepartmentId());
            entity.setSourcePositionId(emp.getPositionId());
        }
        updateById(entity);
    }

    @Override
    public void deleteTransfer(Long id, Long hrEmployeeId) {
        HrTransfer entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "已提交审批的申请不可删除");
        removeById(id);
    }

    @Override
    public Page<TransferVO> listTransfer(String keyword, List<String> statuses, int page, int size) {
        LambdaQueryWrapper<HrTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(HrTransfer::getCreateTime);
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(HrTransfer::getStatus, statuses);
        }
        Page<HrTransfer> entityPage = page(new Page<>(page, size), wrapper);
        List<HrTransfer> records = entityPage.getRecords();
        Map<Long, Employee> empMap = batchLoadEmployees(records);
        records = records.stream()
                .filter(r -> {
                    Employee emp = empMap.get(r.getEmployeeId());
                    return emp != null && (emp.getIsDeleted() == null || emp.getIsDeleted() == 0);
                })
                .collect(Collectors.toList());
        Page<TransferVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(records.stream()
                .map(e -> convertToVO(e, keyword, empMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public TransferVO getTransferDetail(Long id) {
        HrTransfer entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        Map<Long, Employee> empMap = batchLoadEmployees(Collections.singletonList(entity));
        return convertToVO(entity, null, empMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalPassed(Long businessId) {
        HrTransfer entity = getById(businessId);
        if (entity == null) return;

        entity.setStatus("APPROVED");
        if (entity.getTransferDate() == null) entity.setTransferDate(new Date());
        updateById(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp == null) return;

        List<EmployeeChangeLog> changeLogs = new ArrayList<>();
        Date now = new Date();

        if (entity.getTargetDeptId() != null && !entity.getTargetDeptId().equals(emp.getDepartmentId())) {
            String oldDeptName = getDeptName(emp.getDepartmentId());
            String newDeptName = getDeptName(entity.getTargetDeptId());
            changeLogs.add(buildChangeLog(emp.getId(), "departmentId",
                    oldDeptName, newDeptName, entity.getOperatorId()));
            emp.setDepartmentId(entity.getTargetDeptId());
        }

        if (entity.getTargetPositionId() != null && !entity.getTargetPositionId().equals(emp.getPositionId())) {
            String oldPos = getPosName(emp.getPositionId());
            String newPos = getPosName(entity.getTargetPositionId());
            changeLogs.add(buildChangeLog(emp.getId(), "positionId",
                    oldPos, newPos, entity.getOperatorId()));
            emp.setPositionId(entity.getTargetPositionId());
        }

        employeeMapper.updateById(emp);

        if (entity.getToRankCode() != null || entity.getToReporterId() != null) {
            EmployeeDetail detail = employeeDetailMapper.selectOne(
                    new LambdaQueryWrapper<EmployeeDetail>()
                            .eq(EmployeeDetail::getEmployeeId, emp.getId()));
            if (detail != null) {
                if (entity.getToRankCode() != null && !entity.getToRankCode().equals(detail.getJobLevel())) {
                    changeLogs.add(buildChangeLog(emp.getId(), "jobLevel",
                            detail.getJobLevel(), entity.getToRankCode(), entity.getOperatorId()));
                    detail.setJobLevel(entity.getToRankCode());
                }
                if (entity.getToReporterId() != null && !entity.getToReporterId().equals(detail.getDirectReportId())) {
                    changeLogs.add(buildChangeLog(emp.getId(), "directReportId",
                            getEmployeeName(detail.getDirectReportId()),
                            getEmployeeName(entity.getToReporterId()), entity.getOperatorId()));
                    detail.setDirectReportId(entity.getToReporterId());
                }
                employeeDetailMapper.updateById(detail);
            }
        }

        for (EmployeeChangeLog clog : changeLogs) {
            employeeChangeLogMapper.insert(clog);
        }

        if (entity.getNewBaseSalary() != null
                && entity.getNewBaseSalary().compareTo(BigDecimal.ZERO) > 0) {
            EmpSalaryProfile profile = empSalaryProfileMapper.selectOne(
                    new LambdaQueryWrapper<EmpSalaryProfile>()
                            .eq(EmpSalaryProfile::getEmployeeId, emp.getId()));
            if (profile != null) {
                String oldValue = toJsonString(profile);
                profile.setBaseSalary(entity.getNewBaseSalary());
                empSalaryProfileMapper.updateById(profile);

                SalChangeLog salLog = new SalChangeLog();
                salLog.setEmployeeId(emp.getId());
                salLog.setChangeType(5);
                salLog.setOldValue(oldValue);
                salLog.setNewValue(toJsonString(profile));
                salLog.setEffectiveDate(entity.getTransferDate() != null ? entity.getTransferDate() : now);
                salLog.setOperatorId(entity.getOperatorId());
                salLog.setRemark("调岗调薪");
                salChangeLogMapper.insert(salLog);
            }
        }

        insertMutationLog(entity, emp, "APPROVED");
        log.info("调岗审批通过: id={}, employeeId={}", entity.getId(), entity.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalRejected(Long businessId) {
        HrTransfer entity = getById(businessId);
        if (entity == null) return;
        entity.setStatus("REJECTED");
        updateById(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        insertMutationLog(entity, emp, "REJECTED");
        log.info("调岗审批拒绝: id={}, employeeId={}", entity.getId(), entity.getEmployeeId());
    }

    // ========== 私有方法 ==========

    private TransferVO convertToVO(HrTransfer e, String keyword, Map<Long, Employee> empMap) {
        TransferVO vo = new TransferVO();
        BeanUtils.copyProperties(e, vo);
        vo.setFromDeptId(e.getSourceDeptId());
        vo.setToDeptId(e.getTargetDeptId());
        vo.setToPositionId(e.getTargetPositionId());
        vo.setEffectiveDate(e.getTransferDate());
        vo.setReason(e.getTransferReason());
        vo.setSalaryAdjustment(e.getNewBaseSalary());

        Employee emp = empMap.get(e.getEmployeeId());
        if (emp != null) {
            vo.setEmployeeName(emp.getEmployeeName());
            vo.setEmployeeNo(emp.getEmployeeNo());
        }
        vo.setFromDeptName(getDeptName(e.getSourceDeptId()));
        vo.setToDeptName(getDeptName(e.getTargetDeptId()));
        vo.setToPositionName(getPosName(e.getTargetPositionId()));
        Employee toReporter = empMap.get(e.getToReporterId());
        vo.setToReporterName(toReporter != null ? toReporter.getEmployeeName() : null);

        if (e.getApproverId() != null) {
            Employee approver = empMap.get(e.getApproverId());
            vo.setApproverName(approver != null ? approver.getEmployeeName() : null);
        }
        if (e.getOperatorId() != null) {
            Employee operator = empMap.get(e.getOperatorId());
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

    private Map<Long, Employee> batchLoadEmployees(List<HrTransfer> records) {
        List<Long> ids = records.stream().map(r -> {
            List<Long> list = new ArrayList<>();
            if (r.getEmployeeId() != null) list.add(r.getEmployeeId());
            if (r.getApproverId() != null) list.add(r.getApproverId());
            if (r.getToReporterId() != null) list.add(r.getToReporterId());
            if (r.getOperatorId() != null) list.add(r.getOperatorId());
            return list;
        }).flatMap(List::stream).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyMap();
        List<Employee> emps = employeeMapper.selectBatchIdsAll(ids);
        return emps.stream().collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
    }

    private void validateRequest(TransferAddRequest req) {
        ThrowUtils.throwIf(req.getEmployeeId() == null, ErrorCode.PARAMS_ERROR, "员工不能为空");
        ThrowUtils.throwIf(req.getToDeptId() == null, ErrorCode.PARAMS_ERROR, "新部门不能为空");
        ThrowUtils.throwIf(req.getToPositionId() == null, ErrorCode.PARAMS_ERROR, "新职位不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getEmploymentType()), ErrorCode.PARAMS_ERROR, "入职类型不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getReason()), ErrorCode.PARAMS_ERROR, "调岗原因不能为空");
    }

    private String generateBusinessNo() {
        String prefix = "DG" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        long maxSeq = 0;
        try {
            var wrapper = new LambdaQueryWrapper<HrTransfer>()
                    .likeRight(HrTransfer::getBusinessNo, prefix)
                    .orderByDesc(HrTransfer::getBusinessNo)
                    .last("LIMIT 1");
            HrTransfer last = getOne(wrapper);
            if (last != null && last.getBusinessNo() != null) {
                String seqStr = last.getBusinessNo().substring(prefix.length());
                maxSeq = Long.parseLong(seqStr);
            }
        } catch (Exception ex) { maxSeq = 0; }
        return prefix + String.format("%04d", maxSeq + 1);
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
                            .eq(ApprovalFlow::getBusinessType, "TRANSFER")
                            .eq(ApprovalFlow::getStatus, 1)
                            .last("LIMIT 1"));
            if (flow != null) return flow.getId();
        } catch (Exception ignored) {}
        return 3L;
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

    private String getEmployeeName(Long employeeId) {
        if (employeeId == null) return null;
        Employee e = employeeMapper.selectById(employeeId);
        return e != null ? e.getEmployeeName() : null;
    }

    private void insertMutationLog(HrTransfer entity, Employee emp, String approvalStatus) {
        EmpMutationLog log = new EmpMutationLog();
        log.setBusinessType("TRANSFER");
        log.setBusinessId(entity.getId());
        log.setBusinessNo(entity.getBusinessNo());
        log.setEmployeeId(entity.getEmployeeId());
        log.setEmployeeName(emp != null ? emp.getEmployeeName() : "");
        log.setDeptId(entity.getTargetDeptId());
        log.setDeptName(getDeptName(entity.getTargetDeptId()));
        log.setEffectDate(entity.getTransferDate() != null ? entity.getTransferDate() : new Date());
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

    private EmployeeChangeLog buildChangeLog(Long employeeId, String fieldName,
                                              String oldValue, String newValue, Long operatorId) {
        EmployeeChangeLog clog = new EmployeeChangeLog();
        clog.setEmployeeId(employeeId);
        clog.setFieldName(fieldName);
        clog.setOldValue(oldValue);
        clog.setNewValue(newValue);
        clog.setChangeType("FLOW_CHANGE");
        clog.setOperatorId(operatorId);
        clog.setCreateTime(new Date());
        return clog;
    }

    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON序列化失败", e);
            return "{}";
        }
    }

    @Override
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("draft", lambdaQuery().eq(HrTransfer::getStatus, "DRAFT").count());
        stats.put("approving", lambdaQuery().eq(HrTransfer::getStatus, "APPROVING").count());
        stats.put("approved", lambdaQuery().eq(HrTransfer::getStatus, "APPROVED").count());
        stats.put("effective", lambdaQuery().eq(HrTransfer::getStatus, "EFFECTIVE").count());
        return stats;
    }
}
