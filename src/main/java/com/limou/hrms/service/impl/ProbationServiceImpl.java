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
import com.limou.hrms.model.dto.probation.ProbationCreateDTO;
import com.limou.hrms.model.dto.probation.ProbationHandleResultDTO;
import com.limou.hrms.model.dto.probation.ProbationUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.ProbationResult;
import com.limou.hrms.model.enums.ProbationStatus;
import com.limou.hrms.model.query.ProbationQuery;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.model.entity.OnboardingApplication;
import com.limou.hrms.model.entity.ResignationApplication;
import com.limou.hrms.model.enums.ResignationStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.ProbationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * 转正管理服务实现 — 含转正 CRUD + 审批回调 + 结果处理
 */
@Service
@Slf4j
public class ProbationServiceImpl
        extends ServiceImpl<ProbationApplicationMapper, ProbationApplication>
        implements ProbationService, ApprovalCallback {

    @Resource
    private ProbationApplicationMapper probationMapper;
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
    @Resource
    private OnboardingApplicationMapper onboardingMapper;
    @Resource
    private ResignationApplicationMapper resignationMapper;
    @Resource
    private EmployeeSalaryMapper employeeSalaryMapper;

    // ==================== 转正 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApplication(ProbationCreateDTO dto) {
        // 校验员工在职状态为试用期
        Employee employee = employeeMapper.selectById(dto.getEmployeeId());
        if (employee == null) {
            log.warn("员工ID为{}不存在", dto.getEmployeeId());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "员工不存在");
        }
        if (employee.getStatus() == null || employee.getStatus() != EmployeeStatus.PROBATION.getValue()) {
            log.warn("员工ID为{}在职状态不是试用期", dto.getEmployeeId());
            throw new BusinessException(ErrorCode.PROBATION_EMPLOYEE_NOT_PROBATION);
        }

        ProbationApplication app = new ProbationApplication();
        app.setEmployeeId(dto.getEmployeeId());
        app.setPerformanceReview(dto.getPerformanceReview());
        app.setSalaryAdjustment(dto.getSalaryAdjustment());
        app.setRemark(dto.getRemark());
        // 从员工档案自动带入试用期信息
        app.setProbationStartDate(employee.getHireDate());
        // 试用期结束日期 = 入职日期 + 试用期月数
        EmployeeWorkInfo workInfo = workInfoMapper.selectOne(
                new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", dto.getEmployeeId()));
        int probationMonths = 3; // 默认3个月
        if (workInfo != null && workInfo.getPositionId() != null) {
            Position position = positionMapper.selectById(workInfo.getPositionId());
            if (position != null && position.getDefaultProbationMonths() != null) {
                probationMonths = position.getDefaultProbationMonths();
            }
        }
        app.setProbationEndDate(employee.getHireDate().plusMonths(probationMonths));
        app.setApplicantId(dataScopeContext.getCurrentEmployeeId());
        app.setStatus(ProbationStatus.DRAFT.getCode());// 草稿
        probationMapper.insert(app);

        // 直接提交审批
        if (Boolean.TRUE.equals(dto.getSubmitDirectly())) {
            submitToApproval(app.getId());
        }

        log.info("转正申请创建成功: 表单id={}, employeeId={}", app.getId(), dto.getEmployeeId());
        return app.getId();
    }

    @Override
    public void updateDraft(Long id, ProbationUpdateDTO dto) {
        ProbationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ProbationStatus.DRAFT.getCode()) { // 仅草稿
            throw new BusinessException(ErrorCode.PROBATION_DRAFT_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.PROBATION_DRAFT_ONLY, "仅申请人可编辑草稿");
        }

        if (StringUtils.isNotBlank(dto.getPerformanceReview())) app.setPerformanceReview(dto.getPerformanceReview());
        if (dto.getSalaryAdjustment() != null) app.setSalaryAdjustment(dto.getSalaryAdjustment());
        if (dto.getRemark() != null) app.setRemark(dto.getRemark());

        probationMapper.updateById(app);
        log.info("转正草稿更新成功: id={}", id);
    }

    @Override
    public void deleteDraft(Long id) {
        ProbationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ProbationStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.PROBATION_DRAFT_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.PROBATION_DRAFT_ONLY, "仅申请人可删除草稿");
        }
        probationMapper.deleteById(id);
        log.info("转正草稿删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitToApproval(Long id) {
        ProbationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ProbationStatus.DRAFT.getCode()) {
            log.warn("转正申请状态为{}，仅草稿状态可提交审批", app.getStatus());
            throw new BusinessException(ErrorCode.PROBATION_SUBMIT_DRAFT_ONLY);
        }
        // 校验必填字段
        if (StringUtils.isBlank(app.getPerformanceReview())) {
            log.warn("转正申请id为{}，试用期表现评价不能为空", id);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "试用期表现评价不能为空");
        }

        ApprovalInstance instance = approvalFlowService.createInstance(
                ApprovalBizType.PROBATION, app.getId(), app.getApplicantId());

        app.setStatus(ProbationStatus.PENDING.getCode());
        app.setApprovalInstanceId(instance.getId());
        probationMapper.updateById(app);

        log.info("转正申请已提交审批: id={}, instanceId={}", id, instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        ProbationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ProbationStatus.PENDING.getCode()) {
            log.warn("转正申请状态为{}，仅审批中状态可撤回", app.getStatus());
            throw new BusinessException(ErrorCode.PROBATION_CANCEL_FIRST_NODE_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            log.warn("转正申请id为{},仅申请人可以撤回，当前用户ID为{},申请人ID为{}", id, currentEmployeeId, app.getApplicantId());
            throw new BusinessException(ErrorCode.PROBATION_CANCEL_FIRST_NODE_ONLY, "仅申请人可撤回");
        }

        approvalFlowService.cancel(app.getApprovalInstanceId());

        app.setStatus(ProbationStatus.DRAFT.getCode());// 回退草稿
        app.setApprovalInstanceId(null);
        probationMapper.updateById(app);

        log.info("转正申请已撤回: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleResult(Long id, ProbationHandleResultDTO dto) {
        ProbationApplication app = getAppOrThrow(id);
        if (app.getStatus() != ProbationStatus.REJECTED.getCode()) { // 仅已拒绝状态
            log.warn("转正申请id为{},状态为{}，仅已拒绝状态可处理", id, app.getStatus());
            throw new BusinessException(ErrorCode.PROBATION_HANDLE_REJECTED_ONLY);
        }

        ProbationResult result = ProbationResult.fromCode(dto.getResult());
        if (result == null) {
            log.warn("转正申请id为{},处理结果为{}，无效的处理结果", id, dto.getResult());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的处理结果");
        }

        switch (result) {
            case PASS:
                Employee emp = employeeMapper.selectById(app.getEmployeeId());
                if (emp != null) {
                    emp.setStatus(EmployeeStatus.REGULAR.getValue());
                    employeeMapper.updateById(emp);
                }
                // 薪资调整
                applySalaryAdjustment(app);
                app.setResult(ProbationResult.PASS.getCode());
                app.setStatus(ProbationStatus.COMPLETED.getCode()); // 已完成
                break;
            case EXTEND:
                if (dto.getExtendedEndDate() == null) {
                    log.warn("转正申请id为{},处理结果为{}，延长试用期结束日期不能为空", id, dto.getResult());
                    throw new BusinessException(ErrorCode.PROBATION_EXTEND_DATE_REQUIRED);
                }
                app.setExtendedEndDate(dto.getExtendedEndDate());
                app.setResult(ProbationResult.EXTEND.getCode());
                app.setStatus(ProbationStatus.COMPLETED.getCode()); // 已完成（延长试用也是终态）
                break;
            case FAIL:
                app.setResult(ProbationResult.FAIL.getCode());
                app.setStatus(ProbationStatus.COMPLETED.getCode()); // 已完成
                // 自动创建离职草稿
                ResignationApplication resignation = new ResignationApplication();
                resignation.setEmployeeId(app.getEmployeeId());
                resignation.setResignationType(2); // 辞退
                resignation.setReason("转正不通过，予以辞退");
                resignation.setStatus(ResignationStatus.DRAFT.getCode());
                resignation.setApplicantId(dataScopeContext.getCurrentEmployeeId());
                resignationMapper.insert(resignation);
                log.info("转正不通过，已自动创建离职草稿: employeeId={}, resignationId={}",
                        app.getEmployeeId(), resignation.getId());
                break;
        }

        probationMapper.updateById(app);
        log.info("转正结果已处理: 表单id={}, result={}", id, result.getDesc());
    }

    @Override
    public Page<ProbationListVO> list(ProbationQuery query) {
        DataScopeEnum scope = dataScopeContext.getApprovalScope();
        switch (scope) {
            case ALL:
                return queryAllList(query);
            case DEPT:
                Long deptId = dataScopeContext.getCurrentDepartmentId();
                if (deptId == null) return new Page<>(query.getCurrent(), query.getPageSize());
                return queryDeptList(deptId, query);
            case SELF:
                Long employeeId = dataScopeContext.getCurrentEmployeeId();
                if (employeeId == null) return new Page<>(query.getCurrent(), query.getPageSize());
                return queryPersonalList(employeeId, query);
            default:
                return new Page<>(query.getCurrent(), query.getPageSize());
        }
    }

    @Override
    public ProbationDetailVO getDetail(Long id) {
        ProbationApplication app = getAppOrThrow(id);
        ProbationDetailVO vo = buildDetailVO(app);

        // 审批中/已通过/已拒绝时附带审批进度
        if (app.getApprovalInstanceId() != null) {
            ApprovalInstanceVO progress = approvalFlowService.getDetail(app.getApprovalInstanceId());
            vo.setApprovalProgress(progress);
        }
        return vo;
    }

    @Override
    public List<PendingEmployeeVO> getPendingEmployees(Integer days) {
        if (days == null || days <= 0) days = 7;
        LocalDate now = LocalDate.now();
        List<PendingEmployeeVO> result = new ArrayList<>();

        // 查询所有试用期员工
        List<Employee> probationEmployees = employeeMapper.selectList(
                new QueryWrapper<Employee>().eq("status", EmployeeStatus.PROBATION.getValue()));
        for (Employee emp : probationEmployees) {
            // 查入职申请获取试用期月数
            OnboardingApplication oa = onboardingMapper.selectOne(
                    new QueryWrapper<OnboardingApplication>()
                            .eq("employee_id", emp.getId())
                            .eq("status", 4)); // 已入职
            Integer probationMonths = (oa != null && oa.getDefaultProbationMonths() != null)
                    ? oa.getDefaultProbationMonths() : 3;
            LocalDate probationEnd = emp.getHireDate().plusMonths(probationMonths);
            long daysRemaining = ChronoUnit.DAYS.between(now, probationEnd);

            // 试用期结束 - days天 ≤ 今天
            if (daysRemaining <= days && daysRemaining >= -90) { // 已过90天的不显示
                PendingEmployeeVO vo = new PendingEmployeeVO();
                vo.setEmployeeId(emp.getId());
                vo.setEmployeeName(approverResolver.getEmployeeName(emp.getId()));
                vo.setEmployeeNo(emp.getEmployeeNo());
                vo.setDepartmentName(getDeptNameByEmployeeId(emp.getId()));
                vo.setPositionName(getPositionNameByEmployeeId(emp.getId()));
                vo.setHireDate(emp.getHireDate());
                vo.setProbationEndDate(probationEnd);
                vo.setDaysRemaining(daysRemaining);
                // 检查是否已有审批中/已完成的转正申请
                Long pendingCount = probationMapper.selectCount(
                        new QueryWrapper<ProbationApplication>()
                                .eq("employee_id", emp.getId())
                                .in("status", ProbationStatus.PENDING.getCode(), ProbationStatus.COMPLETED.getCode())); // 审批中或已完成
                vo.setHasPendingApplication(pendingCount > 0);
                result.add(vo);
            }
        }
        result.sort((a, b) -> Long.compare(a.getDaysRemaining(), b.getDaysRemaining()));
        return result;
    }

    // ==================== 审批回调 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.PROBATION) return;
        ProbationApplication app = getAppOrThrow(bizId);

        // 员工状态 → 正式
        Employee employee = employeeMapper.selectById(app.getEmployeeId());
        if (employee != null) {
            employee.setStatus(EmployeeStatus.REGULAR.getValue());
            employeeMapper.updateById(employee);
        }

        // 薪资调整（有填则生效）
        applySalaryAdjustment(app);

        app.setResult(ProbationResult.PASS.getCode());
        app.setStatus(ProbationStatus.COMPLETED.getCode()); // 已完成
        probationMapper.updateById(app);

        log.info("转正审批通过: 表单id={}, employeeId={}", bizId, app.getEmployeeId());
    }

    @Override
    public void onRejected(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.PROBATION) return;
        ProbationApplication app = getAppOrThrow(bizId);
        app.setStatus(ProbationStatus.REJECTED.getCode()); // 已拒绝
        probationMapper.updateById(app);
        log.info("转正审批已拒绝: 表单id={}", bizId);
    }

    // ==================== 私有方法 ====================

    /**
     * 转正薪资调整：从 probation_application.salary_adjustment 加到 employee_salary.base_salary
     */
    private void applySalaryAdjustment(ProbationApplication app) {
        if (app.getSalaryAdjustment() == null
                || app.getSalaryAdjustment().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }
        EmployeeSalary salary = employeeSalaryMapper.selectOne(
                new QueryWrapper<EmployeeSalary>().eq("employee_id", app.getEmployeeId())
                        .orderByDesc("id").last("LIMIT 1"));
        if (salary == null) {
            log.warn("转正薪资调整失败：员工 {} 无薪资档案", app.getEmployeeId());
            return;
        }
        java.math.BigDecimal oldBase = salary.getBaseSalary() != null
                ? salary.getBaseSalary() : java.math.BigDecimal.ZERO;
        salary.setBaseSalary(oldBase.add(app.getSalaryAdjustment()));
        employeeSalaryMapper.updateById(salary);
        log.info("转正薪资调整：employeeId={}, baseSalary {} → {}",
                app.getEmployeeId(), oldBase, salary.getBaseSalary());
    }

    private ProbationApplication getAppOrThrow(Long id) {
        ProbationApplication app = probationMapper.selectById(id);
        if (app == null) {
            log.warn("转正申请id为{}不存在", id);
            throw new BusinessException(ErrorCode.PROBATION_NOT_FOUND);
        }
        return app;
    }

    // ==================== 角色路由查询 ====================

    private Page<ProbationListVO> queryAllList(ProbationQuery query) {
        QueryWrapper<ProbationApplication> qw = buildQueryWrapper(query);
        qw.orderByDesc("create_time");
        Page<ProbationApplication> page = probationMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private Page<ProbationListVO> queryDeptList(Long deptId, ProbationQuery query) {
        // 部门的员工：通过 work_info 关联
        QueryWrapper<ProbationApplication> qw = buildQueryWrapper(query);
        qw.inSql("employee_id",
                "SELECT w.employee_id FROM employee_work_info w WHERE w.department_id = " + deptId);
        qw.orderByDesc("create_time");
        Page<ProbationApplication> page = probationMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private Page<ProbationListVO> queryPersonalList(Long employeeId, ProbationQuery query) {
        QueryWrapper<ProbationApplication> qw = buildQueryWrapper(query);
        qw.eq("applicant_id", employeeId);
        qw.orderByDesc("create_time");
        Page<ProbationApplication> page = probationMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private QueryWrapper<ProbationApplication> buildQueryWrapper(ProbationQuery query) {
        QueryWrapper<ProbationApplication> qw = new QueryWrapper<>();
        if (query.getStatus() != null) {
            qw.eq("status", query.getStatus());
        }
        if (query.getEmployeeId() != null) {
            qw.eq("employee_id", query.getEmployeeId());
        }
        if (query.getDepartmentId() != null) {
            qw.inSql("employee_id",
                "SELECT w.employee_id FROM employee_work_info w WHERE w.department_id = " + query.getDepartmentId());
        }
        if (StringUtils.isNotBlank(query.getKeyword())) {
            qw.and(w -> w
                .inSql("employee_id", "SELECT e.id FROM employee e " +
                    "INNER JOIN employee_personal_info pi ON pi.employee_id = e.id " +
                    "WHERE pi.name LIKE '%" + query.getKeyword() + "%' " +
                    "OR e.employee_no LIKE '%" + query.getKeyword() + "%'"));
        }
        return qw;
    }

    private Page<ProbationListVO> toListVOPage(Page<ProbationApplication> page) {
        List<ProbationApplication> apps = page.getRecords();
        // 批量收集
        Set<Long> empIds = new HashSet<>(), applicantIds = new HashSet<>();
        for (ProbationApplication a : apps) {
            if (a.getEmployeeId() != null) empIds.add(a.getEmployeeId());
            if (a.getApplicantId() != null) applicantIds.add(a.getApplicantId());
        }
        empIds.addAll(applicantIds);
        // 批量查 work_info
        Map<Long, EmployeeWorkInfo> wiMap = empIds.isEmpty() ? Collections.emptyMap() :
                workInfoMapper.selectList(new QueryWrapper<EmployeeWorkInfo>().in("employee_id", empIds))
                        .stream().collect(Collectors.toMap(EmployeeWorkInfo::getEmployeeId, w -> w, (a, b) -> a));
        // 批量查部门、职位
        Set<Long> deptIds = wiMap.values().stream().map(EmployeeWorkInfo::getDepartmentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> posIds = wiMap.values().stream().map(EmployeeWorkInfo::getPositionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptMap = deptIds.isEmpty() ? Collections.emptyMap() :
                departmentMapper.selectBatchIds(deptIds).stream().collect(Collectors.toMap(Department::getId, Department::getName));
        Map<Long, String> posMap = posIds.isEmpty() ? Collections.emptyMap() :
                positionMapper.selectBatchIds(posIds).stream().collect(Collectors.toMap(Position::getId, Position::getName));
        // 批量查工号、人名
        Map<Long, String> empNoMap = new HashMap<>(), empNameMap = new HashMap<>();
        for (Long eid : empIds) { empNoMap.put(eid, getEmployeeNo(eid)); empNameMap.put(eid, approverResolver.getEmployeeName(eid)); }

        List<ProbationListVO> records = apps.stream().map(app -> {
            EmployeeWorkInfo wi = wiMap.get(app.getEmployeeId());
            ProbationListVO vo = new ProbationListVO();
            vo.setId(app.getId());
            vo.setEmployeeId(app.getEmployeeId());
            vo.setEmployeeName(empNameMap.getOrDefault(app.getEmployeeId(), ""));
            vo.setEmployeeNo(empNoMap.getOrDefault(app.getEmployeeId(), ""));
            vo.setDepartmentName(wi != null ? deptMap.getOrDefault(wi.getDepartmentId(), "") : "");
            vo.setPositionName(wi != null ? posMap.getOrDefault(wi.getPositionId(), "") : "");
            vo.setProbationStartDate(app.getProbationStartDate());
            vo.setProbationEndDate(app.getProbationEndDate());
            vo.setSalaryAdjustment(app.getSalaryAdjustment());
            vo.setStatus(app.getStatus());
            vo.setStatusDesc(getStatusDesc(app.getStatus()));
            vo.setApplicantName(empNameMap.getOrDefault(app.getApplicantId(), ""));
            vo.setCreateTime(app.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<ProbationListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private ProbationDetailVO buildDetailVO(ProbationApplication app) {
        ProbationDetailVO vo = new ProbationDetailVO();
        vo.setId(app.getId());
        vo.setEmployeeId(app.getEmployeeId());
        vo.setEmployeeName(approverResolver.getEmployeeName(app.getEmployeeId()));
        vo.setEmployeeNo(getEmployeeNo(app.getEmployeeId()));
        vo.setDepartmentName(getDeptNameByEmployeeId(app.getEmployeeId()));
        vo.setPositionName(getPositionNameByEmployeeId(app.getEmployeeId()));
        vo.setJobLevel(getJobLevel(app.getEmployeeId()));
        vo.setProbationStartDate(app.getProbationStartDate());
        vo.setProbationEndDate(app.getProbationEndDate());
        vo.setPerformanceReview(app.getPerformanceReview());
        vo.setSalaryAdjustment(app.getSalaryAdjustment());
        vo.setResult(app.getResult());
        ProbationResult resultEnum = ProbationResult.fromCode(app.getResult());
        vo.setResultDesc(resultEnum != null ? resultEnum.getDesc() : null);
        vo.setExtendedEndDate(app.getExtendedEndDate());
        vo.setStatus(app.getStatus());
        vo.setStatusDesc(getStatusDesc(app.getStatus()));
        vo.setApprovalInstanceId(app.getApprovalInstanceId());
        vo.setApplicantId(app.getApplicantId());
        vo.setApplicantName(approverResolver.getEmployeeName(app.getApplicantId()));
        vo.setRemark(app.getRemark());
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());
        return vo;
    }

    private String getStatusDesc(Integer status) {
        ProbationStatus ps = ProbationStatus.fromCode(status);
        return ps != null ? ps.getDesc() : "";
    }

    private String getEmployeeNo(Long employeeId) {
        if (employeeId == null) return null;
        Employee emp = employeeMapper.selectById(employeeId);
        return emp != null ? emp.getEmployeeNo() : null;
    }

    private String getDeptNameByEmployeeId(Long employeeId) {
        if (employeeId == null) return null;
        EmployeeWorkInfo wi = workInfoMapper.selectOne(
                new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", employeeId));
        if (wi == null || wi.getDepartmentId() == null) return null;
        Department dept = departmentMapper.selectById(wi.getDepartmentId());
        return dept != null ? dept.getName() : null;
    }

    private String getPositionNameByEmployeeId(Long employeeId) {
        if (employeeId == null) return null;
        EmployeeWorkInfo wi = workInfoMapper.selectOne(
                new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", employeeId));
        if (wi == null || wi.getPositionId() == null) return null;
        Position pos = positionMapper.selectById(wi.getPositionId());
        return pos != null ? pos.getName() : null;
    }

    private String getJobLevel(Long employeeId) {
        if (employeeId == null) return null;
        EmployeeWorkInfo wi = workInfoMapper.selectOne(
                new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", employeeId));
        return wi != null ? wi.getJobLevel() : null;
    }
}
