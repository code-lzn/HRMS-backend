package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.resignation.ResignationAddRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.vo.ResignationVO;
import com.limou.hrms.service.ApprovalService;
import com.limou.hrms.service.ResignationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResignationServiceImpl extends ServiceImpl<HrResignationMapper, HrResignation>
        implements ResignationService {

    @Resource
    private ApprovalService approvalService;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private EmpMutationLogMapper empMutationLogMapper;
    @Resource
    private EmployeeChangeLogMapper employeeChangeLogMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ApprovalFlowMapper approvalFlowMapper;
    @Resource
    private ApprovalDetailMapper approvalDetailMapper;
    @Resource
    private ApprovalRecordMapper approvalRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrResignation addResignation(ResignationAddRequest request, Long hrEmployeeId, boolean submitNow) {
        validateRequest(request);
        Employee emp = employeeMapper.selectById(request.getEmployeeId());
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工不存在");
        ThrowUtils.throwIf(!Objects.equals(emp.getStatus(), EmployeeStatus.PROBATION.getCode())
                        && !Objects.equals(emp.getStatus(), EmployeeStatus.REGULAR.getCode()),
                ErrorCode.OPERATION_ERROR, "只有试用期或正式员工可发起离职");

        ThrowUtils.throwIf(request.getResignDate() == null
                        || request.getResignDate().before(new Date()),
                ErrorCode.PARAMS_ERROR, "离职日期必须 >= 今天");

        HrResignation entity = new HrResignation();
        entity.setEmployeeId(request.getEmployeeId());
        entity.setLastWorkDate(request.getResignDate());
        entity.setResignReasonType(request.getResignReasonType());
        entity.setResignType(convertResignType(request.getResignType()));
        entity.setResignReason(request.getDetailReason());
        entity.setHandoverPersonId(request.getHandoverPersonId());
        entity.setFlowId(resolveFlowId(request.getFlowId()));
        entity.setRemark(request.getRemark());
        entity.setOperatorId(hrEmployeeId);
        entity.setBusinessNo(generateBusinessNo());
        entity.setApproverId(resolveApproverId(emp.getDepartmentId()));
        entity.setApplyDate(new Date());
        entity.setStatus("DRAFT");
        save(entity);

        if (submitNow) {
            submitForApproval(entity.getId(), hrEmployeeId);
        }
        log.info("离职申请创建: id={}, employeeId={}", entity.getId(), request.getEmployeeId());
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long id, Long hrEmployeeId) {
        HrResignation entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR, "离职申请不存在");
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "该申请已提交审批");

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        String employeeName = emp != null ? emp.getEmployeeName() : "";

        Long deptId = emp != null ? emp.getDepartmentId() : null;
        Long deptManagerId = null;
        if (deptId != null) {
            Department dept = departmentMapper.selectById(deptId);
            deptManagerId = dept != null ? dept.getManagerId() : null;
        }
        Map<Integer, Long> overrides = new HashMap<>();
        if (deptManagerId != null) overrides.put(1, deptManagerId); // 节点1: 部门负责人
        // 节点2 (HR负责人) 不传覆盖，由 resolveApprover 动态查找HR角色用户

        ApprovalRecord record = approvalService.startApproval(
                "RESIGNATION", entity.getId(), hrEmployeeId, employeeName,
                deptId, overrides);
        entity.setRecordId(record.getId());
        entity.setStatus("APPROVING");
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResignation(Long id, ResignationAddRequest request, Long hrEmployeeId) {
        HrResignation entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "已提交审批的申请不可编辑");
        validateRequest(request);

        entity.setEmployeeId(request.getEmployeeId());
        entity.setLastWorkDate(request.getResignDate());
        entity.setResignReasonType(request.getResignReasonType());
        entity.setResignType(convertResignType(request.getResignType()));
        entity.setResignReason(request.getDetailReason());
        entity.setHandoverPersonId(request.getHandoverPersonId());
        entity.setFlowId(request.getFlowId());
        entity.setRemark(request.getRemark());
        updateById(entity);
    }

    @Override
    public void deleteResignation(Long id, Long hrEmployeeId) {
        HrResignation entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() != null, ErrorCode.OPERATION_ERROR, "已提交审批的申请不可删除");
        removeById(id);
    }

    @Override
    public Page<ResignationVO> listResignation(String keyword, List<String> statuses, int page, int size) {
        LambdaQueryWrapper<HrResignation> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(HrResignation::getCreateTime);
        if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(HrResignation::getStatus, statuses);
        }
        Page<HrResignation> entityPage = page(new Page<>(page, size), wrapper);
        List<HrResignation> records = entityPage.getRecords();
        Map<Long, Employee> empMap = batchLoadEmployees(records);

        Set<Long> recordIds = records.stream().map(HrResignation::getRecordId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> rejectionMap = new HashMap<>();
        if (!recordIds.isEmpty()) {
            List<ApprovalDetail> rejectedDetails = approvalDetailMapper.selectList(
                    new LambdaQueryWrapper<ApprovalDetail>()
                            .in(ApprovalDetail::getRecordId, recordIds)
                            .eq(ApprovalDetail::getAction, "REJECT"));
            for (ApprovalDetail d : rejectedDetails) {
                if (d.getComment() != null) rejectionMap.put(d.getRecordId(), d.getComment());
            }
        }

        records = records.stream()
                .filter(r -> {
                    Employee emp = empMap.get(r.getEmployeeId());
                    return emp != null && (emp.getIsDeleted() == null || emp.getIsDeleted() == 0);
                })
                .collect(Collectors.toList());
        Page<ResignationVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(records.stream()
                .map(e -> convertToVO(e, keyword, empMap, rejectionMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public ResignationVO getResignationDetail(Long id) {
        HrResignation entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        Map<Long, Employee> empMap = batchLoadEmployees(Collections.singletonList(entity));
        return convertToVO(entity, null, empMap, Collections.emptyMap());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalPassed(Long businessId) {
        HrResignation entity = getById(businessId);
        if (entity == null) return;

        entity.setStatus("PENDING_RESIGN");
        updateById(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null) {
            String oldStatus = String.valueOf(emp.getStatus());
            emp.setStatus(EmployeeStatus.PENDING_LEAVE.getCode());
            employeeMapper.updateById(emp);

            EmployeeChangeLog clog = new EmployeeChangeLog();
            clog.setEmployeeId(emp.getId());
            clog.setFieldName("status");
            clog.setOldValue(oldStatus);
            clog.setNewValue(String.valueOf(EmployeeStatus.PENDING_LEAVE.getCode()));
            clog.setChangeType("FLOW_CHANGE");
            clog.setOperatorId(entity.getOperatorId());
            clog.setCreateTime(new Date());
            employeeChangeLogMapper.insert(clog);
        }

        insertMutationLog(entity, emp, "APPROVED");
        log.info("离职审批通过: id={}, employeeId={}, lastWorkDate={}",
                entity.getId(), entity.getEmployeeId(), entity.getLastWorkDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalRejected(Long businessId) {
        HrResignation entity = getById(businessId);
        if (entity == null) return;
        entity.setStatus("REJECTED");
        updateById(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        insertMutationLog(entity, emp, "REJECTED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeResignation(Long id, Long hrEmployeeId) {
        HrResignation entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "该申请未提交审批");

        ApprovalRecord record = approvalService.getById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"APPROVING".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有审批中的申请可撤回");
        ThrowUtils.throwIf(record.getCurrentStep() == null || record.getCurrentStep() > 1,
                ErrorCode.OPERATION_ERROR, "仅第一级审批前可撤回");

        // 先清空实体的审批关联，避免 FK 约束冲突
        update(new LambdaUpdateWrapper<HrResignation>()
                .set(HrResignation::getRecordId, null)
                .set(HrResignation::getFlowId, null)
                .set(HrResignation::getStatus, "DRAFT")
                .eq(HrResignation::getId, id));

        // 再删除审批明细和记录
        approvalDetailMapper.delete(new LambdaQueryWrapper<ApprovalDetail>()
                .eq(ApprovalDetail::getRecordId, record.getId()));
        approvalRecordMapper.deleteById(record.getId());
        log.info("离职申请已撤回: id={}, employeeId={}", entity.getId(), entity.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandonResignation(Long id, Long hrEmployeeId) {
        HrResignation entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "请先提交审批");

        ApprovalRecord record = approvalRecordMapper.selectById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"APPROVED".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有审批通过的申请可放弃");

        record.setStatus("CANCELLED");
        record.setFinishedAt(new Date());
        approvalRecordMapper.updateById(record);

        entity.setStatus("CANCELLED");
        updateById(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null && Objects.equals(emp.getStatus(), EmployeeStatus.PENDING_LEAVE.getCode())) {
            emp.setStatus(EmployeeStatus.REGULAR.getCode());
            employeeMapper.updateById(emp);
        }
        log.info("离职申请已放弃: id={}, employeeId={}", entity.getId(), entity.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResignDate(Long id, Date newDate, Long hrEmployeeId) {
        ThrowUtils.throwIf(newDate == null, ErrorCode.PARAMS_ERROR, "离职日期不能为空");
        HrResignation entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "该申请未提交审批");

        ApprovalRecord record = approvalRecordMapper.selectById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"APPROVED".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有审批通过的申请可修改离职日期");

        entity.setLastWorkDate(newDate);
        updateById(entity);
        log.info("离职日期已修改: id={}, employeeId={}, newDate={}", entity.getId(), entity.getEmployeeId(), newDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmResignation(Long id, Long hrEmployeeId) {
        HrResignation entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "请先提交审批");

        ApprovalRecord record = approvalRecordMapper.selectById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"APPROVED".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有审批通过的申请可确认离职");

        entity.setStatus("RESIGNED");
        updateById(entity);

        Employee emp = employeeMapper.selectById(entity.getEmployeeId());
        if (emp != null) {
            emp.setStatus(EmployeeStatus.RESIGNED.getCode());
            employeeMapper.updateById(emp);

            if (emp.getUserId() != null) {
                User user = userMapper.selectById(emp.getUserId());
                if (user != null) {
                    user.setIsDelete(1);
                    userMapper.updateById(user);
                }
            }

            EmployeeChangeLog clog = new EmployeeChangeLog();
            clog.setEmployeeId(emp.getId());
            clog.setFieldName("status");
            clog.setOldValue(String.valueOf(EmployeeStatus.PENDING_LEAVE.getCode()));
            clog.setNewValue(String.valueOf(EmployeeStatus.RESIGNED.getCode()));
            clog.setChangeType("FLOW_CHANGE");
            clog.setOperatorId(hrEmployeeId);
            clog.setCreateTime(new Date());
            employeeChangeLogMapper.insert(clog);
        }

        insertMutationLog(entity, emp, "RESIGNED");
        log.info("离职已确认: id={}, employeeId={}", entity.getId(), entity.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitResignation(Long id, Long hrEmployeeId) {
        HrResignation entity = getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(entity.getRecordId() == null, ErrorCode.OPERATION_ERROR, "该申请未提交审批");

        ApprovalRecord record = approvalRecordMapper.selectById(entity.getRecordId());
        ThrowUtils.throwIf(record == null || !"REJECTED".equals(record.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有已拒绝的申请可重新发起");

        approvalDetailMapper.delete(new LambdaQueryWrapper<ApprovalDetail>()
                .eq(ApprovalDetail::getRecordId, record.getId()));
        approvalRecordMapper.deleteById(record.getId());

        update(new LambdaUpdateWrapper<HrResignation>()
                .set(HrResignation::getRecordId, null)
                .set(HrResignation::getFlowId, null)
                .eq(HrResignation::getId, id));

        submitForApproval(entity.getId(), hrEmployeeId);
        log.info("离职申请重新发起: id={}, employeeId={}", entity.getId(), entity.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processDailyResignations() {
        Date today = new Date();
        List<HrResignation> pendingList = lambdaQuery()
                .eq(HrResignation::getStatus, "PENDING_RESIGN")
                .le(HrResignation::getLastWorkDate, today)
                .list();

        for (HrResignation entity : pendingList) {
            entity.setStatus("RESIGNED");
            updateById(entity);

            Employee emp = employeeMapper.selectById(entity.getEmployeeId());
            if (emp != null) {
                emp.setStatus(EmployeeStatus.RESIGNED.getCode());
                employeeMapper.updateById(emp);

                if (emp.getUserId() != null) {
                    User user = userMapper.selectById(emp.getUserId());
                    if (user != null) {
                        user.setIsDelete(1);
                        userMapper.updateById(user);
                    }
                }

                EmployeeChangeLog clog = new EmployeeChangeLog();
                clog.setEmployeeId(emp.getId());
                clog.setFieldName("status");
                clog.setOldValue(String.valueOf(EmployeeStatus.PENDING_LEAVE.getCode()));
                clog.setNewValue(String.valueOf(EmployeeStatus.RESIGNED.getCode()));
                clog.setChangeType("SYSTEM");
                clog.setCreateTime(new Date());
                employeeChangeLogMapper.insert(clog);
            }

            log.info("离职日期到达，员工状态已变更为已离职: resignId={}, employeeId={}",
                    entity.getId(), entity.getEmployeeId());
        }
    }

    // ========== 私有方法 ==========

    private ResignationVO convertToVO(HrResignation e, String keyword, Map<Long, Employee> empMap,
                                        Map<Long, String> rejectionMap) {
        ResignationVO vo = new ResignationVO();
        vo.setId(e.getId());
        vo.setBusinessNo(e.getBusinessNo());
        vo.setFlowId(e.getFlowId());
        vo.setRecordId(e.getRecordId());
        vo.setEmployeeId(e.getEmployeeId());
        vo.setResignDate(e.getLastWorkDate());
        vo.setResignReasonType(e.getResignReasonType());
        vo.setResignType(convertResignTypeBack(e.getResignType()));
        vo.setDetailReason(e.getResignReason());
        vo.setHandoverPersonId(e.getHandoverPersonId());
        vo.setStatus(e.getStatus());
        vo.setOperatorId(e.getOperatorId());
        vo.setRemark(e.getRemark());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());

        Employee emp = empMap.get(e.getEmployeeId());
        if (emp != null) {
            vo.setEmployeeName(emp.getEmployeeName());
            vo.setEmployeeNo(emp.getEmployeeNo());
            vo.setDeptName(getDeptName(emp.getDepartmentId()));
            vo.setPositionName(getPosName(emp.getPositionId()));
        }

        if (e.getApproverId() != null) {
            Employee approver = empMap.get(e.getApproverId());
            vo.setApproverName(approver != null ? approver.getEmployeeName() : null);
        }
        if (e.getHandoverPersonId() != null) {
            Employee handover = empMap.get(e.getHandoverPersonId());
            vo.setHandoverPersonName(handover != null ? handover.getEmployeeName() : null);
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
                if ("REJECTED".equals(record.getStatus()) && rejectionMap != null) {
                    vo.setRejectionReason(rejectionMap.get(e.getRecordId()));
                }
            }
        }
        if (StringUtils.hasText(keyword) && vo.getEmployeeName() != null
                && !vo.getEmployeeName().contains(keyword)) {
            return null;
        }
        return vo;
    }

    private Map<Long, Employee> batchLoadEmployees(List<HrResignation> records) {
        List<Long> ids = records.stream().map(r -> {
            List<Long> list = new ArrayList<>();
            if (r.getEmployeeId() != null) list.add(r.getEmployeeId());
            if (r.getApproverId() != null) list.add(r.getApproverId());
            if (r.getHandoverPersonId() != null) list.add(r.getHandoverPersonId());
            if (r.getOperatorId() != null) list.add(r.getOperatorId());
            return list;
        }).flatMap(List::stream).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyMap();
        List<Employee> emps = employeeMapper.selectBatchIdsAll(ids);
        return emps.stream().collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
    }

    private void validateRequest(ResignationAddRequest req) {
        ThrowUtils.throwIf(req.getEmployeeId() == null, ErrorCode.PARAMS_ERROR, "员工不能为空");
        ThrowUtils.throwIf(req.getResignDate() == null, ErrorCode.PARAMS_ERROR, "离职日期不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getResignReasonType()),
                ErrorCode.PARAMS_ERROR, "离职原因大类不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getResignType()),
                ErrorCode.PARAMS_ERROR, "离职类型不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(req.getDetailReason()),
                ErrorCode.PARAMS_ERROR, "详细说明不能为空");
        ThrowUtils.throwIf(req.getHandoverPersonId() == null,
                ErrorCode.PARAMS_ERROR, "工作交接人不能为空");
    }

    private String generateBusinessNo() {
        String prefix = "LZ" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        long maxSeq = 0;
        try {
            var wrapper = new LambdaQueryWrapper<HrResignation>()
                    .likeRight(HrResignation::getBusinessNo, prefix)
                    .orderByDesc(HrResignation::getBusinessNo)
                    .last("LIMIT 1");
            HrResignation last = getOne(wrapper);
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
                            .eq(ApprovalFlow::getBusinessType, "RESIGNATION")
                            .eq(ApprovalFlow::getStatus, 1)
                            .last("LIMIT 1"));
            if (flow != null) return flow.getId();
        } catch (Exception ignored) {}
        return 4L;
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

    private void insertMutationLog(HrResignation entity, Employee emp, String approvalStatus) {
        EmpMutationLog log = new EmpMutationLog();
        log.setBusinessType("RESIGNATION");
        log.setBusinessId(entity.getId());
        log.setBusinessNo(entity.getBusinessNo());
        log.setEmployeeId(entity.getEmployeeId());
        log.setEmployeeName(emp != null ? emp.getEmployeeName() : "");
        log.setDeptId(emp != null ? emp.getDepartmentId() : null);
        log.setDeptName(getDeptName(emp != null ? emp.getDepartmentId() : null));
        log.setEffectDate(entity.getLastWorkDate());
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

    private Integer convertResignType(String type) {
        if (type == null) return null;
        switch (type) {
            case "RESIGN": return 1;
            case "DISMISS": return 2;
            case "CONTRACT_EXPIRE": return 3;
            case "OTHER": return 4;
            default: return Integer.parseInt(type);
        }
    }

    private String convertResignTypeBack(Integer type) {
        if (type == null) return null;
        switch (type) {
            case 1: return "RESIGN";
            case 2: return "DISMISS";
            case 3: return "CONTRACT_EXPIRE";
            case 4: return "OTHER";
            default: return String.valueOf(type);
        }
    }

    @Override
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("draft", lambdaQuery().eq(HrResignation::getStatus, "DRAFT").count());
        stats.put("approving", lambdaQuery().eq(HrResignation::getStatus, "APPROVING").count());
        stats.put("pending", lambdaQuery().eq(HrResignation::getStatus, "PENDING_RESIGN").count());
        stats.put("resigned", lambdaQuery().eq(HrResignation::getStatus, "RESIGNED").count());
        return stats;
    }
}
