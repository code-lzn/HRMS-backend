package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.limou.hrms.common.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.model.dto.leave.LeaveRequestSubmitDTO;
import com.limou.hrms.model.entity.EmployeeLeaveBalance;
import com.limou.hrms.model.entity.EmployeePersonalInfo;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.entity.LeaveRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.WorkCalendar;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.model.vo.LeaveBalanceVO.BalanceItem;
import com.limou.hrms.model.vo.LeaveRequestVO;
import com.limou.hrms.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 请假服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestMapper leaveRequestMapper;
    private final EmployeeLeaveBalanceMapper leaveBalanceMapper;
    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;
    private final EmployeePersonalInfoMapper employeePersonalInfoMapper;
    private final DepartmentService departmentService;
    private final WorkCalendarMapper workCalendarMapper;
    private final DataScopeContext dataScopeContext;

    private static final Set<Integer> ATTACHMENT_REQUIRED_TYPES = new HashSet<>(Arrays.asList(2, 4, 5));

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveRequestVO submitLeaveRequest(LeaveRequestSubmitDTO dto) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // ① 基础校验
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间不能早于开始时间");
        }

        // ② 计算请假天数（排除休息日和节假日）
        BigDecimal leaveDays = calculateLeaveDays(dto.getStartTime(), dto.getEndTime());

        // ③ 附件校验（病假>1天 / 婚假 / 产假必传）
        if (ATTACHMENT_REQUIRED_TYPES.contains(dto.getLeaveType())) {
            if (dto.getLeaveType() == 2 && leaveDays.compareTo(BigDecimal.ONE) <= 0) {
                // 病假 ≤1 天不强制附件
            } else if (dto.getAttachmentUrl() == null || dto.getAttachmentUrl().isBlank()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该请假类型须上传证明材料");
            }
        }

        // ④ 余额校验（年假/调休）
        if (dto.getLeaveType() == 1 || dto.getLeaveType() == 7) {
            int year = dto.getStartTime().getYear();
            EmployeeLeaveBalance balance = leaveBalanceMapper.selectOne(
                    Wrappers.<EmployeeLeaveBalance>lambdaQuery()
                            .eq(EmployeeLeaveBalance::getEmployeeId, employeeId)
                            .eq(EmployeeLeaveBalance::getYear, year)
                            .eq(EmployeeLeaveBalance::getLeaveType, dto.getLeaveType()));
            if (balance == null || balance.getRemainingDays().compareTo(leaveDays) < 0) {
                throw new BusinessException(ErrorCode.LEAVE_BALANCE_INSUFFICIENT);
            }
        }

        // ⑤ 保存
        LeaveRequest entity = new LeaveRequest();
        BeanUtils.copyProperties(dto, entity);
        entity.setEmployeeId(employeeId);
        entity.setLeaveDays(leaveDays);
        entity.setStatus(2); // 审批中

        boolean saved = leaveRequestMapper.insert(entity) > 0;
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交请假申请失败");
        }

        log.info("员工 {} 提交了{}请假申请 (id={}), 天数={}",
                employeeId, getLeaveTypeDesc(dto.getLeaveType()), entity.getId(), leaveDays);

        LeaveRequestVO vo = new LeaveRequestVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setLeaveTypeDesc(getLeaveTypeDesc(entity.getLeaveType()));
        vo.setStatusDesc("审批中");
        return vo;
    }

    // ==================== 查询列表 ====================

    @Override
    public Page<LeaveRequestVO> queryRequests(Long employeeId, Integer leaveType, Integer status,
                                               LocalDate startDate, LocalDate endDate, int page, int size) {
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

        if (employeeId != null) wrapper.eq("employee_id", employeeId);
        if (leaveType != null) wrapper.eq("leave_type", leaveType);
        if (status != null) wrapper.eq("status", status);
        if (startDate != null) wrapper.ge("start_time", startDate.atStartOfDay());
        if (endDate != null) wrapper.le("start_time", endDate.atTime(LocalTime.of(23, 59, 59)));

        Page<LeaveRequest> resultPage = leaveRequestMapper.selectPage(new Page<>(page, size), wrapper);

        Set<Long> empIds = resultPage.getRecords().stream()
                .map(LeaveRequest::getEmployeeId).collect(Collectors.toSet());
        Map<Long, String> empNameMap = loadEmpNames(empIds);
        Map<Long, String> deptNameMap = loadDeptNames(empIds);

        List<LeaveRequestVO> voList = resultPage.getRecords().stream().map(r -> {
            LeaveRequestVO vo = new LeaveRequestVO();
            BeanUtils.copyProperties(r, vo);
            vo.setEmployeeName(empNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setDepartmentName(deptNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setLeaveTypeDesc(getLeaveTypeDesc(r.getLeaveType()));
            vo.setStatusDesc(getLeaveStatusDesc(r.getStatus()));
            return vo;
        }).collect(Collectors.toList());

        Page<LeaveRequestVO> result = new Page<>(page, size, resultPage.getTotal());
        result.setRecords(voList);
        return result;
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

    private String getLeaveStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 1: return "草稿";
            case 2: return "审批中";
            case 3: return "已通过";
            case 4: return "已拒绝";
            case 5: return "已取消";
            default: return "未知";
        }
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

    // ==================== 查询详情 ====================

    @Override
    public LeaveRequestVO getRequestDetail(Long id) {
        LeaveRequest request = leaveRequestMapper.selectById(id);
        if (request == null) {
            throw new BusinessException(ErrorCode.LEAVE_NOT_FOUND);
        }

        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        DataScopeEnum scope = dataScopeContext.getAttendanceScope();

        // 权限校验
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

        // 查员工名和部门名
        Map<Long, String> empNameMap = loadEmpNames(Collections.singleton(request.getEmployeeId()));
        Map<Long, String> deptNameMap = loadDeptNames(Collections.singleton(request.getEmployeeId()));

        LeaveRequestVO vo = new LeaveRequestVO();
        BeanUtils.copyProperties(request, vo);
        vo.setEmployeeName(empNameMap.getOrDefault(request.getEmployeeId(), ""));
        vo.setDepartmentName(deptNameMap.getOrDefault(request.getEmployeeId(), ""));
        vo.setLeaveTypeDesc(getLeaveTypeDesc(request.getLeaveType()));
        vo.setStatusDesc(getLeaveStatusDesc(request.getStatus()));
        return vo;
    }

    // ==================== 查询余额 ====================

    @Override
    public LeaveBalanceVO getBalances(Long employeeId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        if (employeeId == null) {
            employeeId = dataScopeContext.getCurrentEmployeeId();
        }

        DataScopeEnum scope = dataScopeContext.getAttendanceScope();

        // dept_head：只能查管辖部门员工
        if (scope == DataScopeEnum.DEPT) {
            Set<Long> managedDeptIds = dataScopeContext.getManagedDepartmentIds();
            if (managedDeptIds == null || managedDeptIds.isEmpty()
                    || !isEmployeeInDepts(employeeId, managedDeptIds)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // user：只能查自己
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

    private boolean isEmployeeInDepts(Long employeeId, Set<Long> deptIds) {
        EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .eq(EmployeeWorkInfo::getEmployeeId, employeeId));
        return workInfo != null && deptIds.contains(workInfo.getDepartmentId());
    }

    // ==================== 天数计算 ====================

    /**
     * 计算请假天数，排除休息日和节假日
     */
    private BigDecimal calculateLeaveDays(LocalDateTime start, LocalDateTime end) {
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        // 查日历
        List<WorkCalendar> calendars = workCalendarMapper.selectList(
                Wrappers.<WorkCalendar>lambdaQuery()
                        .between(WorkCalendar::getCalendarDate, startDate, endDate));
        Set<LocalDate> nonWorkDays = calendars.stream()
                .filter(c -> c.getDayType() == 2 || c.getDayType() == 3)
                .map(WorkCalendar::getCalendarDate)
                .collect(Collectors.toSet());

        if (startDate.equals(endDate)) {
            // 同一天
            return isHalfDay(start, end) ? new BigDecimal("0.5") : BigDecimal.ONE;
        }

        BigDecimal total = BigDecimal.ZERO;

        // 第一天
        if (!nonWorkDays.contains(startDate)) {
            total = total.add(isAfternoon(start) ? new BigDecimal("0.5") : BigDecimal.ONE);
        }

        // 中间完整天
        LocalDate d = startDate.plusDays(1);
        while (d.isBefore(endDate)) {
            if (!nonWorkDays.contains(d)) {
                total = total.add(BigDecimal.ONE);
            }
            d = d.plusDays(1);
        }

        // 最后一天
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

    // ==================== 工具 ====================

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
}
