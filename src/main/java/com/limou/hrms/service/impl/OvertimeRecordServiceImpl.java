package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.overtime.OvertimeRecordCreateDTO;
import com.limou.hrms.model.dto.overtime.OvertimeRecordUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.vo.OvertimeRecordListVO;
import com.limou.hrms.model.vo.OvertimeRecordVO;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.service.OvertimeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 加班记录服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OvertimeRecordServiceImpl implements OvertimeRecordService {

    private final OvertimeRecordMapper overtimeRecordMapper;
    private final EmployeeLeaveBalanceMapper leaveBalanceMapper;
    private final EmployeePersonalInfoMapper employeePersonalInfoMapper;
    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;
    private final DepartmentService departmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OvertimeRecordVO createOvertimeRecord(OvertimeRecordCreateDTO dto) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间不能早于开始时间");
        }

        // 保存加班记录
        OvertimeRecord record = new OvertimeRecord();
        BeanUtils.copyProperties(dto, record);
        record.setIsUsed(0);

        // 有效期：加班当月 + 次月末
        LocalDate overtimeDate = dto.getOvertimeDate();
        LocalDate expireDate = overtimeDate.plusMonths(1).withDayOfMonth(
                overtimeDate.plusMonths(1).lengthOfMonth());
        record.setExpireDate(expireDate);

        boolean saved = overtimeRecordMapper.insert(record) > 0;
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建加班记录失败");
        }

        // 自动 1:1 转入调休余额
        int year = overtimeDate.getYear();
        EmployeeLeaveBalance balance = leaveBalanceMapper.selectOne(
                Wrappers.<EmployeeLeaveBalance>lambdaQuery()
                        .eq(EmployeeLeaveBalance::getEmployeeId, dto.getEmployeeId())
                        .eq(EmployeeLeaveBalance::getYear, year)
                        .eq(EmployeeLeaveBalance::getLeaveType, 7));
        if (balance == null) {
            balance = new EmployeeLeaveBalance();
            balance.setEmployeeId(dto.getEmployeeId());
            balance.setYear(year);
            balance.setLeaveType(7); // 调休
            balance.setTotalDays(dto.getHours());
            balance.setUsedDays(BigDecimal.ZERO);
            balance.setRemainingDays(dto.getHours());
            leaveBalanceMapper.insert(balance);
        } else {
            balance.setTotalDays(balance.getTotalDays().add(dto.getHours()));
            balance.setRemainingDays(balance.getRemainingDays().add(dto.getHours()));
            leaveBalanceMapper.updateById(balance);
        }

        log.info("HR 创建了加班记录 (id={}), 员工={}, {}小时, 转入调休{}.0天, 有效期至{}",
                record.getId(), dto.getEmployeeId(), dto.getHours(), dto.getHours(), expireDate);

        OvertimeRecordVO vo = new OvertimeRecordVO();
        BeanUtils.copyProperties(record, vo);
        vo.setCompTimeAdded(dto.getHours());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OvertimeRecordVO updateOvertimeRecord(Long id, OvertimeRecordUpdateDTO dto) {
        OvertimeRecord exist = overtimeRecordMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "加班记录不存在");
        }

        BigDecimal oldHours = exist.getHours();

        if (dto.getOvertimeDate() != null) {
            exist.setOvertimeDate(dto.getOvertimeDate());
            // 重新计算有效期
            LocalDate d = dto.getOvertimeDate();
            exist.setExpireDate(d.plusMonths(1).withDayOfMonth(d.plusMonths(1).lengthOfMonth()));
        }
        if (dto.getStartTime() != null) exist.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) exist.setEndTime(dto.getEndTime());
        if (dto.getHours() != null) exist.setHours(dto.getHours());

        overtimeRecordMapper.updateById(exist);

        // 差额调整调休余额
        if (dto.getHours() != null) {
            BigDecimal delta = dto.getHours().subtract(oldHours);
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                int year = exist.getOvertimeDate().getYear();
                EmployeeLeaveBalance balance = leaveBalanceMapper.selectOne(
                        Wrappers.<EmployeeLeaveBalance>lambdaQuery()
                                .eq(EmployeeLeaveBalance::getEmployeeId, exist.getEmployeeId())
                                .eq(EmployeeLeaveBalance::getYear, year)
                                .eq(EmployeeLeaveBalance::getLeaveType, 7));
                if (balance != null) {
                    balance.setTotalDays(balance.getTotalDays().add(delta));
                    balance.setRemainingDays(balance.getRemainingDays().add(delta));
                    leaveBalanceMapper.updateById(balance);
                }
            }
        }

        log.info("HR 更新了加班记录 (id={}), 原{}.0h → {}.0h", id, oldHours, exist.getHours());

        OvertimeRecordVO vo = new OvertimeRecordVO();
        BeanUtils.copyProperties(exist, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOvertimeRecord(Long id) {
        OvertimeRecord exist = overtimeRecordMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "加班记录不存在");
        }

        int year = exist.getOvertimeDate().getYear();
        EmployeeLeaveBalance balance = leaveBalanceMapper.selectOne(
                Wrappers.<EmployeeLeaveBalance>lambdaQuery()
                        .eq(EmployeeLeaveBalance::getEmployeeId, exist.getEmployeeId())
                        .eq(EmployeeLeaveBalance::getYear, year)
                        .eq(EmployeeLeaveBalance::getLeaveType, 7));
        if (balance != null) {
            BigDecimal newRemaining = balance.getRemainingDays().subtract(exist.getHours());
            if (newRemaining.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该加班记录对应的调休已被使用，无法删除");
            }
            balance.setTotalDays(balance.getTotalDays().subtract(exist.getHours()));
            balance.setRemainingDays(newRemaining);
            leaveBalanceMapper.updateById(balance);
        }

        overtimeRecordMapper.deleteById(id);
        log.info("HR 删除了加班记录 (id={}), 员工={}, 扣减调休{}.0h", id, exist.getEmployeeId(), exist.getHours());
    }

    @Override
    public Page<OvertimeRecordListVO> queryRecords(Long employeeId, LocalDate startDate, LocalDate endDate,
                                                    Integer isUsed, int page, int size) {
        QueryWrapper<OvertimeRecord> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");

        if (employeeId != null) wrapper.eq("employee_id", employeeId);
        if (startDate != null) wrapper.ge("overtime_date", startDate);
        if (endDate != null) wrapper.le("overtime_date", endDate);
        if (isUsed != null) wrapper.eq("is_used", isUsed);

        Page<OvertimeRecord> resultPage = overtimeRecordMapper.selectPage(new Page<>(page, size), wrapper);

        Set<Long> empIds = resultPage.getRecords().stream()
                .map(OvertimeRecord::getEmployeeId).collect(Collectors.toSet());
        Map<Long, String> empNameMap = loadEmpNames(empIds);
        Map<Long, String> deptNameMap = loadDeptNames(empIds);

        List<OvertimeRecordListVO> voList = resultPage.getRecords().stream().map(r -> {
            OvertimeRecordListVO vo = new OvertimeRecordListVO();
            BeanUtils.copyProperties(r, vo);
            vo.setEmployeeName(empNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setDepartmentName(deptNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setIsUsedDesc(r.getIsUsed() != null && r.getIsUsed() == 1 ? "已使用" : "未使用");
            return vo;
        }).collect(Collectors.toList());

        Page<OvertimeRecordListVO> result = new Page<>(page, size, resultPage.getTotal());
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
}