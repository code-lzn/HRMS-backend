package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.leave.LeaveCreateDTO;
import com.limou.hrms.model.dto.leave.LeaveUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.LeaveStatus;
import com.limou.hrms.model.query.LeaveQuery;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.model.vo.LeaveBalanceVO.BalanceItem;
import com.limou.hrms.model.vo.LeaveRequestVO;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 请假服务实现 — 含请假 CRUD + 审批回调 + 请假后处理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveServiceImpl
        extends ServiceImpl<LeaveRequestMapper, LeaveRequest>
        implements LeaveService, ApprovalCallback {

    private final LeaveRequestMapper leaveRequestMapper;
    private final ApprovalFlowService approvalFlowService;
    private final EmployeeLeaveBalanceMapper leaveBalanceMapper;
    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;
    private final EmployeePersonalInfoMapper employeePersonalInfoMapper;
    private final DepartmentService departmentService;
    private final WorkCalendarMapper workCalendarMapper;
    private final DataScopeContext dataScopeContext;

    private static final Set<Integer> ATTACHMENT_REQUIRED_TYPES = new HashSet<>(Arrays.asList(2, 4, 5));

    // ==================== 请假 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApplication(LeaveCreateDTO dto) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 基础校验
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间不能早于开始时间");
        }
        // 计算请假天数
        BigDecimal leaveDays = calculateLeaveDays(dto.getStartTime(), dto.getEndTime());
        // 附件校验
        validateAttachment(dto.getLeaveType(), leaveDays, dto.getAttachmentUrl());
        // 余额校验
        if (dto.getLeaveType() == 1 || dto.getLeaveType() == 7) {
            validateBalance(employeeId, dto.getLeaveType(), dto.getStartTime().getYear(), leaveDays);
        }

        LeaveRequest entity = new LeaveRequest();
        entity.setEmployeeId(employeeId);
        entity.setLeaveType(dto.getLeaveType());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setLeaveDays(leaveDays);
        entity.setReason(dto.getReason());
        entity.setHandoverEmployeeId(dto.getHandoverEmployeeId());
        entity.setAttachmentUrl(dto.getAttachmentUrl());
        entity.setStatus(LeaveStatus.DRAFT.getCode());
        this.save(entity);

        // 直接提交审批
        if (Boolean.TRUE.equals(dto.getSubmitDirectly())) {
            submitToApproval(entity.getId());
        }

        log.info("请假申请创建成功: id={}, employeeId={}, leaveType={}", entity.getId(), employeeId, dto.getLeaveType());
        return entity.getId();
    }

    @Override
    public void updateDraft(Long id, LeaveUpdateDTO dto) {
        LeaveRequest app = getAppOrThrow(id);
        if (app.getStatus() != LeaveStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅草稿状态可编辑");
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getEmployeeId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅申请人可编辑草稿");
        }
        if (dto.getLeaveType() != null) app.setLeaveType(dto.getLeaveType());
        if (dto.getStartTime() != null) app.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) app.setEndTime(dto.getEndTime());
        // 重新计算天数
        if (dto.getStartTime() != null || dto.getEndTime() != null) {
            app.setLeaveDays(calculateLeaveDays(app.getStartTime(), app.getEndTime()));
        }
        if (dto.getReason() != null) app.setReason(dto.getReason());
        if (dto.getHandoverEmployeeId() != null) app.setHandoverEmployeeId(dto.getHandoverEmployeeId());
        if (dto.getAttachmentUrl() != null) app.setAttachmentUrl(dto.getAttachmentUrl());
        this.updateById(app);
        log.info("请假草稿更新成功: id={}", id);
    }

    @Override
    public void deleteDraft(Long id) {
        LeaveRequest app = getAppOrThrow(id);
        if (app.getStatus() != LeaveStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅草稿状态可删除");
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getEmployeeId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅申请人可删除草稿");
        }
        this.removeById(id);
        log.info("请假草稿删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitToApproval(Long id) {
        LeaveRequest app = getAppOrThrow(id);
        if (app.getStatus() != LeaveStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅草稿状态可提交审批");
        }
        // 重新校验
        if (app.getEndTime().isBefore(app.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间不能早于开始时间");
        }
        BigDecimal leaveDays = calculateLeaveDays(app.getStartTime(), app.getEndTime());
        app.setLeaveDays(leaveDays);
        validateAttachment(app.getLeaveType(), leaveDays, app.getAttachmentUrl());
        validateFieldsComplete(app);

        // 创建审批实例
        ApprovalInstance instance = approvalFlowService.createInstance(
                ApprovalBizType.LEAVE, app.getId(), app.getEmployeeId());

        app.setStatus(LeaveStatus.PENDING.getCode());
        app.setApprovalInstanceId(instance.getId());
        this.updateById(app);

        log.info("请假申请已提交审批: id={}, instanceId={}", id, instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        LeaveRequest app = getAppOrThrow(id);
        if (app.getStatus() != LeaveStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅审批中状态可撤回");
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getEmployeeId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅申请人可撤回");
        }
        approvalFlowService.cancel(app.getApprovalInstanceId());

        app.setStatus(LeaveStatus.DRAFT.getCode());
        app.setApprovalInstanceId(null);
        this.updateById(app);

        log.info("请假申请已撤回: id={}", id);
    }

    // ==================== ApprovalCallback ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.LEAVE) return;
        LeaveRequest app = getAppOrThrow(bizId);
        app.setStatus(LeaveStatus.APPROVED.getCode());
        this.updateById(app);

        // 扣减假期余额（年假/调休）
        if (app.getLeaveType() == 1 || app.getLeaveType() == 7) {
            int year = app.getStartTime().getYear();
            EmployeeLeaveBalance balance = leaveBalanceMapper.selectOne(
                    Wrappers.<EmployeeLeaveBalance>lambdaQuery()
                            .eq(EmployeeLeaveBalance::getEmployeeId, app.getEmployeeId())
                            .eq(EmployeeLeaveBalance::getYear, year)
                            .eq(EmployeeLeaveBalance::getLeaveType, app.getLeaveType()));
            if (balance != null) {
                balance.setUsedDays(balance.getUsedDays().add(app.getLeaveDays()));
                balance.setRemainingDays(balance.getRemainingDays().subtract(app.getLeaveDays()));
                leaveBalanceMapper.updateById(balance);
            }
        }
        log.info("请假审批通过后处理完成: id={}", bizId);
    }

    @Override
    public void onRejected(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.LEAVE) return;
        LeaveRequest app = getAppOrThrow(bizId);
        app.setStatus(LeaveStatus.REJECTED.getCode());
        this.updateById(app);
        log.info("请假审批已拒绝: id={}", bizId);
    }

    // ==================== 查询列表 ====================

    @Override
    public Page<LeaveRequestVO> queryRequests(LeaveQuery query) {
        QueryWrapper<LeaveRequest> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");

        // 数据权限
        DataScopeEnum scope = dataScopeContext.getAttendanceScope();
        switch (scope) {
            case DEPT: {
                Set<Long> deptIds = dataScopeContext.getManagedDepartmentIds();
                if (deptIds == null || deptIds.isEmpty()) {
                    wrapper.eq("employee_id", dataScopeContext.getCurrentEmployeeId());
                } else {
                    wrapper.inSql("employee_id",
                            "SELECT e.id FROM employee e " +
                            "INNER JOIN employee_work_info ewi ON e.id = ewi.employee_id " +
                            "WHERE ewi.department_id IN (" + joinIds(deptIds) + ")");
                }
                break;
            }
            case SELF:
                wrapper.eq("employee_id", dataScopeContext.getCurrentEmployeeId());
                break;
        }

        // 关键字搜索（员工姓名）
        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.inSql("employee_id",
                    "SELECT epi.employee_id FROM employee_personal_info epi " +
                    "WHERE epi.name LIKE '%" + query.getKeyword() + "%'");
        }
        if (query.getLeaveType() != null) wrapper.eq("leave_type", query.getLeaveType());
        if (query.getStatus() != null) wrapper.eq("status", query.getStatus());
        if (query.getStartDate() != null) wrapper.ge("start_time", query.getStartDate().atStartOfDay());
        if (query.getEndDate() != null) wrapper.le("start_time", query.getEndDate().atTime(LocalTime.of(23, 59, 59)));

        Page<LeaveRequest> resultPage = this.page(
                new Page<>(query.getCurrent(), query.getPageSize()), wrapper);

        Set<Long> empIds = resultPage.getRecords().stream()
                .map(LeaveRequest::getEmployeeId).collect(Collectors.toSet());
        Map<Long, String> empNameMap = loadEmpNames(empIds);
        Map<Long, String> deptNameMap = loadDeptNames(empIds);

        List<LeaveRequestVO> voList = resultPage.getRecords().stream().map(r -> {
            LeaveRequestVO vo = new LeaveRequestVO();
            vo.setId(r.getId());
            vo.setEmployeeId(r.getEmployeeId());
            vo.setLeaveType(r.getLeaveType());
            vo.setStartTime(r.getStartTime());
            vo.setEndTime(r.getEndTime());
            vo.setLeaveDays(r.getLeaveDays());
            vo.setReason(r.getReason());
            vo.setHandoverEmployeeId(r.getHandoverEmployeeId());
            vo.setStatus(r.getStatus());
            vo.setCreateTime(r.getCreateTime());
            vo.setEmployeeName(empNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setDepartmentName(deptNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setLeaveTypeDesc(getLeaveTypeDesc(r.getLeaveType()));
            vo.setStatusDesc(getLeaveStatusDesc(r.getStatus()));
            return vo;
        }).collect(Collectors.toList());

        Page<LeaveRequestVO> result = new Page<>(query.getCurrent(), query.getPageSize(), resultPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    // ==================== 查询详情 ====================

    @Override
    public LeaveRequestVO getRequestDetail(Long id) {
        LeaveRequest request = getAppOrThrow(id);
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        DataScopeEnum scope = dataScopeContext.getAttendanceScope();

        if (scope == DataScopeEnum.SELF && !request.getEmployeeId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (scope == DataScopeEnum.DEPT) {
            Set<Long> deptIds = dataScopeContext.getManagedDepartmentIds();
            if (deptIds == null || deptIds.isEmpty()
                    || !isEmployeeInDepts(request.getEmployeeId(), deptIds)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        Map<Long, String> empNameMap = loadEmpNames(Collections.singleton(request.getEmployeeId()));
        Map<Long, String> deptNameMap = loadDeptNames(Collections.singleton(request.getEmployeeId()));

        LeaveRequestVO vo = new LeaveRequestVO();
        vo.setId(request.getId());
        vo.setEmployeeId(request.getEmployeeId());
        vo.setLeaveType(request.getLeaveType());
        vo.setStartTime(request.getStartTime());
        vo.setEndTime(request.getEndTime());
        vo.setLeaveDays(request.getLeaveDays());
        vo.setReason(request.getReason());
        vo.setHandoverEmployeeId(request.getHandoverEmployeeId());
        vo.setAttachmentUrl(request.getAttachmentUrl());
        vo.setStatus(request.getStatus());
        vo.setCreateTime(request.getCreateTime());
        vo.setEmployeeName(empNameMap.getOrDefault(request.getEmployeeId(), ""));
        vo.setDepartmentName(deptNameMap.getOrDefault(request.getEmployeeId(), ""));
        vo.setLeaveTypeDesc(getLeaveTypeDesc(request.getLeaveType()));
        vo.setStatusDesc(getLeaveStatusDesc(request.getStatus()));
        return vo;
    }

    // ==================== 查询余额 ====================

    @Override
    public LeaveBalanceVO getBalances(Long employeeId, Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        if (employeeId == null) employeeId = dataScopeContext.getCurrentEmployeeId();

        DataScopeEnum scope = dataScopeContext.getAttendanceScope();
        if (scope == DataScopeEnum.DEPT) {
            Set<Long> managedDeptIds = dataScopeContext.getManagedDepartmentIds();
            if (managedDeptIds == null || managedDeptIds.isEmpty()
                    || !isEmployeeInDepts(employeeId, managedDeptIds)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        if (scope == DataScopeEnum.SELF && !employeeId.equals(dataScopeContext.getCurrentEmployeeId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        List<EmployeeLeaveBalance> balances = leaveBalanceMapper.selectList(
                Wrappers.<EmployeeLeaveBalance>lambdaQuery()
                        .eq(EmployeeLeaveBalance::getEmployeeId, employeeId)
                        .eq(EmployeeLeaveBalance::getYear, year));

        List<BalanceItem> items = balances.stream().map(b -> {
            BalanceItem item = new BalanceItem();
            item.setLeaveType(b.getLeaveType());
            item.setLeaveTypeDesc(getLeaveTypeDesc(b.getLeaveType()));
            item.setTotalDays(b.getTotalDays());
            item.setUsedDays(b.getUsedDays());
            item.setRemainingDays(b.getRemainingDays());
            return item;
        }).collect(Collectors.toList());

        LeaveBalanceVO vo = new LeaveBalanceVO();
        vo.setEmployeeId(employeeId);
        vo.setBalances(items);
        return vo;
    }

    // ==================== 工具方法 ====================

    private LeaveRequest getAppOrThrow(Long id) {
        LeaveRequest app = this.getById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请假申请不存在");
        }
        return app;
    }

    private void validateAttachment(Integer leaveType, BigDecimal leaveDays, String attachmentUrl) {
        if (ATTACHMENT_REQUIRED_TYPES.contains(leaveType)) {
            if (leaveType == 2 && leaveDays.compareTo(BigDecimal.ONE) <= 0) return; // 病假≤1天
            if (attachmentUrl == null || attachmentUrl.isBlank()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该请假类型须上传证明材料");
            }
        }
    }

    private void validateBalance(Long employeeId, Integer leaveType, int year, BigDecimal leaveDays) {
        EmployeeLeaveBalance balance = leaveBalanceMapper.selectOne(
                Wrappers.<EmployeeLeaveBalance>lambdaQuery()
                        .eq(EmployeeLeaveBalance::getEmployeeId, employeeId)
                        .eq(EmployeeLeaveBalance::getYear, year)
                        .eq(EmployeeLeaveBalance::getLeaveType, leaveType));
        if (balance == null || balance.getRemainingDays().compareTo(leaveDays) < 0) {
            throw new BusinessException(ErrorCode.LEAVE_BALANCE_INSUFFICIENT);
        }
    }

    private void validateFieldsComplete(LeaveRequest app) {
        if (app.getLeaveType() == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "请假类型不能为空");
        if (app.getStartTime() == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始时间不能为空");
        if (app.getEndTime() == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间不能为空");
    }

    private Map<Long, String> loadEmpNames(Set<Long> empIds) {
        if (empIds.isEmpty()) return Collections.emptyMap();
        return employeePersonalInfoMapper.selectList(
                        Wrappers.<EmployeePersonalInfo>lambdaQuery()
                                .in(EmployeePersonalInfo::getEmployeeId, empIds))
                .stream()
                .collect(Collectors.toMap(EmployeePersonalInfo::getEmployeeId, EmployeePersonalInfo::getName));
    }

    private Map<Long, String> loadDeptNames(Set<Long> empIds) {
        if (empIds.isEmpty()) return Collections.emptyMap();
        List<EmployeeWorkInfo> workInfos = employeeWorkInfoMapper.selectList(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .in(EmployeeWorkInfo::getEmployeeId, empIds));
        Set<Long> deptIds = workInfos.stream()
                .map(EmployeeWorkInfo::getDepartmentId).collect(Collectors.toSet());
        Map<Long, String> deptNameMap = departmentService.listByIds(new ArrayList<>(deptIds)).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
        Map<Long, Long> empDeptMap = workInfos.stream()
                .collect(Collectors.toMap(EmployeeWorkInfo::getEmployeeId, EmployeeWorkInfo::getDepartmentId, (a, b) -> a));
        return empIds.stream()
                .collect(Collectors.toMap(id -> id, id -> deptNameMap.getOrDefault(empDeptMap.get(id), "")));
    }

    private boolean isEmployeeInDepts(Long employeeId, Set<Long> deptIds) {
        EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .eq(EmployeeWorkInfo::getEmployeeId, employeeId));
        return workInfo != null && deptIds.contains(workInfo.getDepartmentId());
    }

    private String getLeaveStatusDesc(Integer status) {
        LeaveStatus s = LeaveStatus.fromCode(status);
        return s != null ? s.getDesc() : "未知";
    }

    private String getLeaveTypeDesc(Integer leaveType) {
        if (leaveType == null) return "";
        switch (leaveType) {
            case 1: return "年假";
            case 2: return "病假";
            case 3: return "事假";
            case 4: return "婚假";
            case 5: return "产假";
            case 6: return "丧假";
            case 7: return "调休";
            default: return "未知";
        }
    }

    // ==================== 天数计算 ====================

    private BigDecimal calculateLeaveDays(LocalDateTime start, LocalDateTime end) {
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        List<WorkCalendar> calendars = workCalendarMapper.selectList(
                Wrappers.<WorkCalendar>lambdaQuery()
                        .between(WorkCalendar::getCalendarDate, startDate, endDate));
        Set<LocalDate> nonWorkDays = calendars.stream()
                .filter(c -> c.getDayType() == 2 || c.getDayType() == 3)
                .map(WorkCalendar::getCalendarDate)
                .collect(Collectors.toSet());

        if (startDate.equals(endDate)) {
            return isHalfDay(start, end) ? new BigDecimal("0.5") : BigDecimal.ONE;
        }

        BigDecimal total = BigDecimal.ZERO;
        if (!nonWorkDays.contains(startDate)) {
            total = total.add(isAfternoon(start) ? new BigDecimal("0.5") : BigDecimal.ONE);
        }
        LocalDate d = startDate.plusDays(1);
        while (d.isBefore(endDate)) {
            if (!nonWorkDays.contains(d)) total = total.add(BigDecimal.ONE);
            d = d.plusDays(1);
        }
        if (!d.isAfter(endDate) && !nonWorkDays.contains(endDate)) {
            total = total.add(isMorning(end) ? new BigDecimal("0.5") : BigDecimal.ONE);
        }
        return total.compareTo(BigDecimal.ZERO) == 0 ? new BigDecimal("0.5") : total;
    }

    private boolean isHalfDay(LocalDateTime start, LocalDateTime end) {
        return isMorning(end) || isAfternoon(start);
    }

    private boolean isMorning(LocalDateTime time) {
        return time.toLocalTime().isBefore(LocalTime.NOON) || time.toLocalTime().equals(LocalTime.of(12, 0));
    }

    private boolean isAfternoon(LocalDateTime time) {
        return time.toLocalTime().isAfter(LocalTime.NOON);
    }

    private String joinIds(Set<Long> ids) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Long id : ids) {
            if (i > 0) sb.append(",");
            sb.append(id);
            i++;
        }
        return sb.toString();
    }
}
