package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.AttendanceConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.AttendanceMapper;
import com.limou.hrms.model.entity.Attendance;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.enums.AttendanceStatusEnum;
import com.limou.hrms.model.vo.AttendanceCalendarVO;
import com.limou.hrms.model.vo.AttendanceVO;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考勤打卡服务实现
 */
@Service
@Slf4j
public class AttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance>
        implements AttendanceService {

    @Resource
    private EmployeeService employeeService;

    /**
     * 上下班打卡
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceVO punch(Long userId, Integer punchType, String location) {
        Employee emp = employeeService.getByUserId(userId);
        Date now = new Date();
        String today = DateUtil.formatDate(now);

        // 查今天的打卡记录
        Attendance record = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .eq(Attendance::getAttendanceDate, DateUtil.parseDate(today))
                .one();

        boolean isPunchIn = (punchType == null || punchType == AttendanceConstant.PUNCH_TYPE_WEB);

        if (record == null) {
            // 首次打卡，新建记录
            record = new Attendance();
            record.setEmployeeId(emp.getId());
            record.setUserId(userId);
            record.setAttendanceDate(DateUtil.parseDate(today));
            record.setStatus(AttendanceConstant.ATTENDANCE_STATUS_MISSING); // 默认缺卡
            record.setPunchInType(AttendanceConstant.PUNCH_TYPE_WEB);
            record.setPunchOutType(AttendanceConstant.PUNCH_TYPE_WEB);
        }

        if (isPunchIn) {
            ThrowUtils.throwIf(record.getPunchInTime() != null,
                    ErrorCode.ATTENDANCE_DUPLICATE_ERROR);
            record.setPunchInTime(now);
            record.setPunchInLocation(location);

            // 判断是否迟到
            int hour = DateUtil.hour(now, true);
            int minute = DateUtil.minute(now);
            int thresholdMinutes = AttendanceConstant.DEFAULT_WORK_START_HOUR * 60 + AttendanceConstant.LATE_GRACE_MINUTES;
            if (hour * 60 + minute > thresholdMinutes) {
                record.setStatus(AttendanceConstant.ATTENDANCE_STATUS_LATE);
            }
        } else {
            ThrowUtils.throwIf(record.getPunchOutTime() != null,
                    ErrorCode.ATTENDANCE_DUPLICATE_ERROR);
            record.setPunchOutTime(now);
            record.setPunchOutLocation(location);

            // 如果上班打卡正常、下班也正常打卡，则状态改为正常
            if (record.getStatus() != AttendanceConstant.ATTENDANCE_STATUS_LATE) {
                record.setStatus(AttendanceConstant.ATTENDANCE_STATUS_NORMAL);
            }
        }

        boolean saved = this.saveOrUpdate(record);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "打卡失败");

        return convertToVO(record);
    }

    @Override
    public AttendanceCalendarVO getCalendar(Long userId, String month) {
        Employee emp = employeeService.getByUserId(userId);
        // month 格式: yyyy-MM
        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        List<Attendance> records = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(monthStart))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(monthEnd))
                .orderByAsc(Attendance::getAttendanceDate)
                .list();

        AttendanceCalendarVO vo = new AttendanceCalendarVO();
        vo.setMonth(month);
        vo.setNormalDays(0);
        vo.setLateDays(0);
        vo.setLeaveDays(0);
        vo.setMissingDays(0);

        Map<String, Integer> dailyStatus = new LinkedHashMap<>();
        List<String> makeupDates = new ArrayList<>();

        for (Attendance r : records) {
            String dateStr = DateUtil.formatDate(r.getAttendanceDate());
            dailyStatus.put(dateStr, r.getStatus());

            switch (r.getStatus()) {
                case 0: vo.setNormalDays(vo.getNormalDays() + 1); break;
                case 1: vo.setLateDays(vo.getLateDays() + 1); break;
                case 4: vo.setLeaveDays(vo.getLeaveDays() + 1); break;
                // 缺卡---可以进行补卡
                case 3:
                    vo.setMissingDays(vo.getMissingDays() + 1);
                    makeupDates.add(dateStr);
                    break;
                default: break;
            }
        }

        vo.setDailyStatus(dailyStatus);
        vo.setMakeupAvailableDates(makeupDates);
        return vo;
    }

    @Override
    public List<AttendanceVO> getMonthRecords(Long userId, String month) {
        Employee emp = employeeService.getByUserId(userId);
        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        List<Attendance> records = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(monthStart))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(monthEnd))
                .orderByDesc(Attendance::getAttendanceDate)
                .list();

        return records.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public AttendanceVO getTodayStatus(Long userId) {
        Employee emp = employeeService.getByUserId(userId);
        String today = DateUtil.formatDate(new Date());

        Attendance record = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .eq(Attendance::getAttendanceDate, DateUtil.parseDate(today))
                .one();

        if (record == null) {
            AttendanceVO vo = new AttendanceVO();
            vo.setAttendanceDate(DateUtil.parseDate(today));
            vo.setStatus(AttendanceConstant.ATTENDANCE_STATUS_MISSING);
            vo.setStatusText("未打卡");
            return vo;
        }
        return convertToVO(record);
    }

    // ========== 私有方法 ==========

//    private Employee getEmployee(Long userId) {
//        Employee emp = employeeService.lambdaQuery().eq(Employee::getUserId, userId).one();
//        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
//        return emp;
//    }

    private AttendanceVO convertToVO(Attendance record) {
        AttendanceVO vo = new AttendanceVO();
        BeanUtils.copyProperties(record, vo);
        AttendanceStatusEnum status = AttendanceStatusEnum.getEnumByValue(record.getStatus());
        vo.setStatusText(status != null ? status.getText() : "未知");
        return vo;
    }
}
