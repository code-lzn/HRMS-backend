package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.AttendanceConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.MakeupPunchMapper;
import com.limou.hrms.model.entity.Attendance;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.MakeupPunch;
import com.limou.hrms.model.enums.ApprovalStatusEnum;
import com.limou.hrms.model.enums.MakeupPunchTypeEnum;
import com.limou.hrms.model.vo.MakeupPunchVO;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.MakeupPunchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 补卡服务实现
 */
@Service
@Slf4j
public class MakeupPunchServiceImpl extends ServiceImpl<MakeupPunchMapper, MakeupPunch>
        implements MakeupPunchService {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private AttendanceService attendanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MakeupPunchVO apply(Long userId, String punchDate, Integer punchType,
                                       String punchTime, String reason) {
        Employee emp = getEmployee(userId);

        MakeupPunch request = new MakeupPunch();
        request.setEmployeeId(emp.getId());
        request.setUserId(userId);
        request.setPunchDate(DateUtil.parseDate(punchDate));
        request.setPunchType(punchType);
        request.setPunchTime(DateUtil.parseDateTime(punchTime));
        request.setReason(reason);
        request.setStatus(AttendanceConstant.APPROVAL_STATUS_PENDING);

        boolean saved = this.save(request);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "补卡申请失败");

        return convertToVO(request, emp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MakeupPunchVO approve(Long requestId, Integer result, String comment, Long approverId) {
        MakeupPunch request = this.getById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "补卡申请不存在");
        ThrowUtils.throwIf(request.getStatus() != AttendanceConstant.APPROVAL_STATUS_PENDING,
                ErrorCode.APPROVAL_NOT_PENDING_ERROR);

        Date now = new Date();
        request.setStatus(result);
        request.setApproverId(approverId);
        request.setApproveTime(now);
        request.setApproveComment(comment);

        boolean updated = this.updateById(request);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "审批失败");

        // 审批通过后，更新打卡记录
        if (Objects.equals(result, AttendanceConstant.APPROVAL_STATUS_APPROVED)) {
            updateAttendanceRecord(request);
        }

        Employee emp = employeeService.lambdaQuery().eq(Employee::getId, request.getEmployeeId()).one();
        return convertToVO(request, emp);
    }

    @Override
    public List<MakeupPunchVO> getMyMakeupPunches(Long userId) {
        Employee emp = getEmployee(userId);
        List<MakeupPunch> list = this.lambdaQuery()
                .eq(MakeupPunch::getEmployeeId, emp.getId())
                .orderByDesc(MakeupPunch::getCreateTime)
                .list();

        return list.stream().map(r -> convertToVO(r, emp)).collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    private Employee getEmployee(Long userId) {
        Employee emp = employeeService.lambdaQuery().eq(Employee::getUserId, userId).one();
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
        return emp;
    }

    private MakeupPunchVO convertToVO(MakeupPunch request, Employee emp) {
        MakeupPunchVO vo = new MakeupPunchVO();
        BeanUtils.copyProperties(request, vo);
        vo.setEmployeeName(emp != null ? emp.getEmployeeName() : null);
        MakeupPunchTypeEnum punchType = MakeupPunchTypeEnum.getEnumByValue(request.getPunchType());
        vo.setPunchTypeText(punchType != null ? punchType.getText() : "未知");
        ApprovalStatusEnum status = ApprovalStatusEnum.getEnumByValue(request.getStatus());
        vo.setStatusText(status != null ? status.getText() : "未知");
        return vo;
    }

    /**
     * 补卡通过后，更新打卡记录
     */
    private void updateAttendanceRecord(MakeupPunch makeup) {
        Attendance record = attendanceService.lambdaQuery()
                .eq(Attendance::getEmployeeId, makeup.getEmployeeId())
                .eq(Attendance::getAttendanceDate, makeup.getPunchDate())
                .one();

        if (record == null) {
            record = new Attendance();
            record.setEmployeeId(makeup.getEmployeeId());
            record.setUserId(makeup.getUserId());
            record.setAttendanceDate(makeup.getPunchDate());
            record.setStatus(AttendanceConstant.ATTENDANCE_STATUS_NORMAL);
            record.setPunchInType(AttendanceConstant.PUNCH_TYPE_WEB);
            record.setPunchOutType(AttendanceConstant.PUNCH_TYPE_WEB);
        }

        if (Objects.equals(makeup.getPunchType(), AttendanceConstant.MAKEUP_TYPE_PUNCH_IN)) {
            record.setPunchInTime(makeup.getPunchTime());
        } else {
            record.setPunchOutTime(makeup.getPunchTime());
        }

        // 补卡后状态恢复正常
        if (record.getPunchInTime() != null && record.getPunchOutTime() != null) {
            record.setStatus(AttendanceConstant.ATTENDANCE_STATUS_NORMAL);
        }

        attendanceService.saveOrUpdate(record);
    }
}
