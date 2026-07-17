package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.EmployeeLeaveBalanceMapper;
import com.limou.hrms.mapper.LeaveRequestMapper;
import com.limou.hrms.mapper.WorkCalendarMapper;
import com.limou.hrms.model.dto.leave.LeaveRequestSubmitDTO;
import com.limou.hrms.model.entity.EmployeeLeaveBalance;
import com.limou.hrms.model.entity.LeaveRequest;
import com.limou.hrms.model.entity.WorkCalendar;
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
