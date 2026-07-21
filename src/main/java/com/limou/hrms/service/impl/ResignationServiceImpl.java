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
import com.limou.hrms.model.dto.resignation.ResignationCreateDTO;
import com.limou.hrms.model.dto.resignation.ResignationUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.ResignationStatus;
import com.limou.hrms.model.query.ResignationQuery;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.ResignationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 离职管理服务实现 — 含离职 CRUD + 审批回调
 */
@Service
@Slf4j
public class ResignationServiceImpl
        extends ServiceImpl<ResignationApplicationMapper, ResignationApplication>
        implements ResignationService, ApprovalCallback {

    @Resource
    private ResignationApplicationMapper resignationMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeeWorkInfoMapper workInfoMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private DataScopeContext dataScopeContext;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApplication(ResignationCreateDTO dto) {
        Employee employee = employeeMapper.selectById(dto.getEmployeeId());
        if (employee == null) {
            log.warn("离职申请创建失败: id为{}的员工不存在", dto.getEmployeeId());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "员工不存在");
        }
        Integer empStatus = employee.getStatus();
        if (empStatus == null
                || (empStatus != EmployeeStatus.PROBATION.getValue()
                    && empStatus != EmployeeStatus.REGULAR.getValue())) {
            log.warn("离职申请创建失败: 员工id为{}, 状态为{}, 仅试用期或正式员工可离职", dto.getEmployeeId(), EmployeeStatus.getByValue(empStatus).getDesc());
            throw new BusinessException(ErrorCode.RESIGNATION_EMPLOYEE_NOT_ACTIVE);
        }
        if (dto.getResignationDate().isBefore(LocalDate.now())) {
            log.warn("离职申请创建失败: 员工id为{}, 离职日期为{}, 不能早于当前日期{}", dto.getEmployeeId(), dto.getResignationDate(), LocalDate.now());
            throw new BusinessException(ErrorCode.RESIGNATION_DATE_BEFORE_TODAY);
        }

        ResignationApplication app = new ResignationApplication();
        app.setEmployeeId(dto.getEmployeeId());
        app.setResignationDate(dto.getResignationDate());
        app.setResignationType(dto.getResignationType());
        app.setReason(dto.getReason());
        app.setHandoverToId(dto.getHandoverToId());
        app.setRemark(dto.getRemark());
        app.setApplicantId(dataScopeContext.getCurrentEmployeeId());
        app.setStatus(ResignationStatus.DRAFT.getCode());
        resignationMapper.insert(app);

        if (Boolean.TRUE.equals(dto.getSubmitDirectly())) submitToApproval(app.getId());

        log.info("离职申请创建成功: 表单id={}, employeeId={}", app.getId(), dto.getEmployeeId());
        return app.getId();
    }

    @Override
    public void updateDraft(Long id, ResignationUpdateDTO dto) {
        ResignationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ResignationStatus.DRAFT.getCode()) {
            log.warn("离职草稿更新失败: 表单id={}, 当前状态为{}, 仅草稿可编辑", id, ResignationStatus.fromCode(app.getStatus()).getDesc());
            throw new BusinessException(ErrorCode.RESIGNATION_DRAFT_ONLY);
        }
        if (!app.getApplicantId().equals(dataScopeContext.getCurrentEmployeeId())) {
            log.warn("离职草稿更新失败: 表单id={}, 仅申请人可编辑", id);
            throw new BusinessException(ErrorCode.RESIGNATION_DRAFT_ONLY, "仅申请人可编辑草稿");
        }
        if (dto.getResignationDate() != null) app.setResignationDate(dto.getResignationDate());
        if (dto.getResignationType() != null) app.setResignationType(dto.getResignationType());
        if (dto.getReason() != null) app.setReason(dto.getReason());
        if (dto.getHandoverToId() != null) app.setHandoverToId(dto.getHandoverToId());
        if (dto.getRemark() != null) app.setRemark(dto.getRemark());
        resignationMapper.updateById(app);
        log.info("离职草稿更新成功: 表单id={}", id);
    }

    @Override
    public void deleteDraft(Long id) {
        ResignationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ResignationStatus.DRAFT.getCode()) {
            log.warn("离职草稿删除失败: 表单id={}, 当前状态为{}, 仅草稿可删除", id, ResignationStatus.fromCode(app.getStatus()).getDesc());
            throw new BusinessException(ErrorCode.RESIGNATION_DRAFT_ONLY);
        }
        if (!app.getApplicantId().equals(dataScopeContext.getCurrentEmployeeId())) {
            log.warn("离职草稿删除失败: 表单id={}, 仅申请人可删除", id);
            throw new BusinessException(ErrorCode.RESIGNATION_DRAFT_ONLY, "仅申请人可删除草稿");
        }
        resignationMapper.deleteById(id);
        log.info("离职草稿删除成功: 表单id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitToApproval(Long id) {
        ResignationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ResignationStatus.DRAFT.getCode()) {
            log.warn("离职申请提交审批失败: 表单id={}, 当前状态为{}, 仅草稿可提交", id, ResignationStatus.fromCode(app.getStatus()).getDesc());
            throw new BusinessException(ErrorCode.RESIGNATION_SUBMIT_DRAFT_ONLY);
        }
        if (StringUtils.isBlank(app.getReason()) || app.getResignationDate() == null || app.getHandoverToId() == null) {
            log.warn("离职申请提交审批失败: 表单id={}, 必填字段不完整", id);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必填字段不完整");
        }

        ApprovalInstance instance = approvalFlowService.createInstance(
                ApprovalBizType.RESIGNATION, app.getId(), app.getApplicantId());
        app.setStatus(ResignationStatus.PENDING.getCode());
        app.setApprovalInstanceId(instance.getId());
        resignationMapper.updateById(app);
        log.info("离职申请已提交审批: 表单id={}, instanceId={}", id, instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        ResignationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ResignationStatus.PENDING.getCode()) {
            log.warn("离职申请撤回失败: 表单id={}, 当前状态为{}, 仅审批中可撤回", id, ResignationStatus.fromCode(app.getStatus()).getDesc());
            throw new BusinessException(ErrorCode.RESIGNATION_CANCEL_FIRST_NODE_ONLY);
        }
        if (!app.getApplicantId().equals(dataScopeContext.getCurrentEmployeeId())) {
            log.warn("离职申请撤回失败: 表单id={}, 仅申请人可撤回", id);
            throw new BusinessException(ErrorCode.RESIGNATION_CANCEL_FIRST_NODE_ONLY, "仅申请人可撤回");
        }
        approvalFlowService.cancel(app.getApprovalInstanceId());
        app.setStatus(ResignationStatus.DRAFT.getCode());
        app.setApprovalInstanceId(null);
        resignationMapper.updateById(app);
        log.info("离职申请已撤回: 表单id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmResignation(Long id) {
        ResignationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ResignationStatus.APPROVED.getCode()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅待离职状态可确认");
        }
        app.setStatus(ResignationStatus.RESIGNED.getCode());
        resignationMapper.updateById(app);
        // 更新员工状态为已离职
        Employee employee = employeeMapper.selectById(app.getEmployeeId());
        if (employee != null) {
            employee.setStatus(EmployeeStatus.RESIGNED.getValue());
            employeeMapper.updateById(employee);
        }
        log.info("离职已确认: 表单id={}, employeeId={}", id, app.getEmployeeId());
    }

    @Override
    public Page<ResignationListVO> list(ResignationQuery query) {
        switch (dataScopeContext.getApprovalScope()) {
            case ALL: return queryAllList(query);
            case DEPT: {
                Long deptId = dataScopeContext.getCurrentDepartmentId();
                if (deptId == null) return new Page<>(query.getCurrent(), query.getPageSize());
                return queryDeptList(deptId, query);
            }
            case SELF: {
                Long employeeId = dataScopeContext.getCurrentEmployeeId();
                if (employeeId == null) return new Page<>(query.getCurrent(), query.getPageSize());
                return queryPersonalList(employeeId, query);
            }
            default: return new Page<>(query.getCurrent(), query.getPageSize());
        }
    }

    @Override
    public ResignationDetailVO getDetail(Long id) {
        ResignationApplication app = getAppOrThrow(id);
        ResignationDetailVO vo = buildDetailVO(app);
        if (app.getApprovalInstanceId() != null)
            vo.setApprovalProgress(approvalFlowService.getDetail(app.getApprovalInstanceId()));
        return vo;
    }

    // ==================== 审批回调 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.RESIGNATION) return;
        ResignationApplication app = getAppOrThrow(bizId);
        app.setStatus(ResignationStatus.APPROVED.getCode());
        resignationMapper.updateById(app);

        // 员工状态 → 待离职
        Employee employee = employeeMapper.selectById(app.getEmployeeId());
        if (employee != null) {
            employee.setStatus(EmployeeStatus.PENDING_RESIGNATION.getValue());
            employeeMapper.updateById(employee);
        }
        log.info("离职审批通过，员工待离职: id={}, employeeId={}", bizId, app.getEmployeeId());
    }

    @Override
    public void onRejected(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.RESIGNATION) return;
        ResignationApplication app = getAppOrThrow(bizId);
        app.setStatus(ResignationStatus.REJECTED.getCode());
        resignationMapper.updateById(app);
        log.info("离职审批已拒绝: id={}, employeeId={}", bizId, app.getEmployeeId());
    }

    // ==================== 私有方法 ====================

    private ResignationApplication getAppOrThrow(Long id) {
        ResignationApplication app = resignationMapper.selectById(id);
        if (app == null) throw new BusinessException(ErrorCode.RESIGNATION_NOT_FOUND);
        return app;
    }

    // ==================== 角色路由 ====================

    private Page<ResignationListVO> queryAllList(ResignationQuery query) {
        QueryWrapper<ResignationApplication> qw = buildQw(query);
        qw.orderByDesc("create_time");
        return toListVOPage(resignationMapper.selectPage(new Page<>(query.getCurrent(), query.getPageSize()), qw));
    }

    private Page<ResignationListVO> queryDeptList(Long deptId, ResignationQuery query) {
        QueryWrapper<ResignationApplication> qw = buildQw(query);
        qw.inSql("employee_id", "SELECT w.employee_id FROM employee_work_info w WHERE w.department_id = " + deptId);
        qw.orderByDesc("create_time");
        return toListVOPage(resignationMapper.selectPage(new Page<>(query.getCurrent(), query.getPageSize()), qw));
    }

    private Page<ResignationListVO> queryPersonalList(Long employeeId, ResignationQuery query) {
        QueryWrapper<ResignationApplication> qw = buildQw(query);
        qw.eq("applicant_id", employeeId);
        qw.orderByDesc("create_time");
        return toListVOPage(resignationMapper.selectPage(new Page<>(query.getCurrent(), query.getPageSize()), qw));
    }

    private QueryWrapper<ResignationApplication> buildQw(ResignationQuery query) {
        QueryWrapper<ResignationApplication> qw = new QueryWrapper<>();
        if (query.getStatus() != null) qw.eq("status", query.getStatus());
        if (query.getEmployeeId() != null) qw.eq("employee_id", query.getEmployeeId());
        if (StringUtils.isNotBlank(query.getKeyword())) {
            qw.and(w -> w
                .inSql("employee_id", "SELECT e.id FROM employee e " +
                    "INNER JOIN employee_personal_info pi ON pi.employee_id = e.id " +
                    "WHERE pi.name LIKE '%" + query.getKeyword() + "%' " +
                    "OR e.employee_no LIKE '%" + query.getKeyword() + "%'"));
        }
        return qw;
    }

    private Page<ResignationListVO> toListVOPage(Page<ResignationApplication> page) {
        List<ResignationApplication> apps = page.getRecords();
        Set<Long> empIds = new HashSet<>();
        for (ResignationApplication a : apps) {
            if (a.getEmployeeId() != null) empIds.add(a.getEmployeeId());
            if (a.getHandoverToId() != null) empIds.add(a.getHandoverToId());
            if (a.getApplicantId() != null) empIds.add(a.getApplicantId());
        }
        Map<Long, EmployeeWorkInfo> wiMap = empIds.isEmpty() ? Collections.emptyMap() :
                workInfoMapper.selectList(new QueryWrapper<EmployeeWorkInfo>().in("employee_id", empIds))
                        .stream().collect(Collectors.toMap(EmployeeWorkInfo::getEmployeeId, w -> w, (a, b) -> a));
        Set<Long> deptIds = wiMap.values().stream().map(EmployeeWorkInfo::getDepartmentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> posIds = wiMap.values().stream().map(EmployeeWorkInfo::getPositionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptMap = deptIds.isEmpty() ? Collections.emptyMap() :
                departmentMapper.selectBatchIds(deptIds).stream().collect(Collectors.toMap(Department::getId, Department::getName));
        Map<Long, String> posMap = posIds.isEmpty() ? Collections.emptyMap() :
                positionMapper.selectBatchIds(posIds).stream().collect(Collectors.toMap(Position::getId, Position::getName));
        Map<Long, String> empNoMap = new HashMap<>(), empNameMap = new HashMap<>();
        for (Long eid : empIds) { empNoMap.put(eid, getEmployeeNo(eid)); empNameMap.put(eid, approverResolver.getEmployeeName(eid)); }

        List<ResignationListVO> records = apps.stream().map(app -> {
            EmployeeWorkInfo wi = wiMap.get(app.getEmployeeId());
            ResignationListVO vo = new ResignationListVO();
            vo.setId(app.getId());
            vo.setEmployeeId(app.getEmployeeId());
            vo.setEmployeeName(empNameMap.getOrDefault(app.getEmployeeId(), ""));
            vo.setEmployeeNo(empNoMap.getOrDefault(app.getEmployeeId(), ""));
            vo.setDepartmentName(wi != null ? deptMap.getOrDefault(wi.getDepartmentId(), "") : "");
            vo.setPositionName(wi != null ? posMap.getOrDefault(wi.getPositionId(), "") : "");
            vo.setHandoverToName(empNameMap.getOrDefault(app.getHandoverToId(), ""));
            vo.setResignationDate(app.getResignationDate());
            vo.setResignationType(app.getResignationType());
            vo.setResignationTypeDesc(getResignationTypeDesc(app.getResignationType()));
            vo.setStatus(app.getStatus());
            vo.setStatusDesc(getStatusDesc(app.getStatus()));
            vo.setApplicantName(empNameMap.getOrDefault(app.getApplicantId(), ""));
            vo.setCreateTime(app.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        Page<ResignationListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private ResignationDetailVO buildDetailVO(ResignationApplication app) {
        ResignationDetailVO vo = new ResignationDetailVO();
        vo.setId(app.getId());
        vo.setEmployeeId(app.getEmployeeId());
        vo.setEmployeeName(approverResolver.getEmployeeName(app.getEmployeeId()));
        vo.setEmployeeNo(getEmployeeNo(app.getEmployeeId()));
        vo.setDepartmentName(getDeptNameByEmployeeId(app.getEmployeeId()));
        vo.setPositionName(getPositionNameByEmployeeId(app.getEmployeeId()));
        vo.setResignationDate(app.getResignationDate());
        vo.setResignationType(app.getResignationType());
        vo.setResignationTypeDesc(getResignationTypeDesc(app.getResignationType()));
        vo.setReason(app.getReason());
        vo.setHandoverToId(app.getHandoverToId());
        vo.setHandoverToName(approverResolver.getEmployeeName(app.getHandoverToId()));
        vo.setStatus(app.getStatus());
        vo.setStatusDesc(getStatusDesc(app.getStatus()));
        vo.setApprovalInstanceId(app.getApprovalInstanceId());
        vo.setActualResignationDate(app.getActualResignationDate());
        vo.setApplicantId(app.getApplicantId());
        vo.setApplicantName(approverResolver.getEmployeeName(app.getApplicantId()));
        vo.setRemark(app.getRemark());
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());
        return vo;
    }

    private String getStatusDesc(Integer s) {
        ResignationStatus rs = ResignationStatus.fromCode(s);
        return rs != null ? rs.getDesc() : "";
    }

    private String getResignationTypeDesc(Integer t) {
        if (t == null) return "";
        switch (t) { case 1: return "辞职"; case 2: return "辞退"; case 3: return "合同到期不续签"; case 4: return "其他"; default: return ""; }
    }

    private String getEmployeeNo(Long eid) {
        if (eid == null) return null;
        Employee e = employeeMapper.selectById(eid);
        return e != null ? e.getEmployeeNo() : null;
    }

    private String getDeptNameByEmployeeId(Long eid) {
        if (eid == null) return null;
        EmployeeWorkInfo wi = workInfoMapper.selectOne(new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", eid));
        if (wi == null || wi.getDepartmentId() == null) return null;
        Department d = departmentMapper.selectById(wi.getDepartmentId());
        return d != null ? d.getName() : null;
    }

    private String getPositionNameByEmployeeId(Long eid) {
        if (eid == null) return null;
        EmployeeWorkInfo wi = workInfoMapper.selectOne(new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", eid));
        if (wi == null || wi.getPositionId() == null) return null;
        Position p = positionMapper.selectById(wi.getPositionId());
        return p != null ? p.getName() : null;
    }
}
