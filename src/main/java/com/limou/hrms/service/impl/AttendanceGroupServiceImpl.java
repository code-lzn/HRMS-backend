package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.AttendanceGroupEmployeeMapper;
import com.limou.hrms.mapper.AttendanceGroupMapper;
import com.limou.hrms.model.dto.attendance.AttendanceGroupDTO;
import com.limou.hrms.model.entity.AttendanceGroup;
import com.limou.hrms.model.entity.AttendanceGroupEmployee;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.vo.AttendanceGroupVO;
import com.limou.hrms.service.AttendanceGroupService;
import cn.hutool.core.date.DateUtil;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AttendanceGroupServiceImpl extends ServiceImpl<AttendanceGroupMapper, AttendanceGroup>
        implements AttendanceGroupService {

    @Resource
    private AttendanceGroupEmployeeMapper groupEmployeeMapper;

    @Resource
    private EmployeeService employeeService;

    @Resource
    private DepartmentService departmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceGroupVO createGroup(AttendanceGroupDTO dto) {
        ThrowUtils.throwIf(dto.getGroupName() == null || dto.getGroupName().isEmpty(),
                ErrorCode.PARAMS_ERROR, "考勤组名称不能为空");
        ThrowUtils.throwIf(dto.getShiftType() == null, ErrorCode.PARAMS_ERROR, "班次类型不能为空");
        ThrowUtils.throwIf(dto.getWorkStartTime() == null, ErrorCode.PARAMS_ERROR, "上班时间不能为空");
        ThrowUtils.throwIf(dto.getWorkEndTime() == null, ErrorCode.PARAMS_ERROR, "下班时间不能为空");

        AttendanceGroup group = new AttendanceGroup();
        BeanUtils.copyProperties(dto, group);
        applyTimeFields(dto, group);
        if (group.getLateThreshold() == null) group.setLateThreshold(15);
        if (group.getEarlyThreshold() == null) group.setEarlyThreshold(15);
        if (group.getStatus() == null) group.setStatus(1);
        group.setCreateTime(new Date());
        group.setUpdateTime(new Date());

        boolean saved = this.save(group);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建考勤组失败");

        List<Long> allEmployeeIds = new ArrayList<>();
        if (dto.getDepartmentIds() != null && !dto.getDepartmentIds().isEmpty()) {
            List<Long> deptEmpIds = employeeService.lambdaQuery()
                    .in(Employee::getDepartmentId, dto.getDepartmentIds())
                    .list().stream().map(Employee::getId).collect(Collectors.toList());
            allEmployeeIds.addAll(deptEmpIds);
        }
        if (dto.getEmployeeIds() != null && !dto.getEmployeeIds().isEmpty()) {
            allEmployeeIds.addAll(dto.getEmployeeIds());
        }
        if (!allEmployeeIds.isEmpty()) {
            assignEmployees(group.getId(), allEmployeeIds.stream().distinct().collect(Collectors.toList()));
        }

        return convertToVO(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceGroupVO updateGroup(AttendanceGroupDTO dto) {
        ThrowUtils.throwIf(dto.getId() == null, ErrorCode.PARAMS_ERROR, "ID不能为空");

        AttendanceGroup group = this.getById(dto.getId());
        ThrowUtils.throwIf(group == null, ErrorCode.NOT_FOUND_ERROR, "考勤组不存在");

        BeanUtils.copyProperties(dto, group, "id", "createTime", "status");
        applyTimeFields(dto, group);
        group.setUpdateTime(new Date());

        boolean updated = this.updateById(group);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新考勤组失败");

        if (dto.getDepartmentIds() != null || dto.getEmployeeIds() != null) {
            groupEmployeeMapper.delete(new LambdaQueryWrapper<AttendanceGroupEmployee>()
                    .eq(AttendanceGroupEmployee::getGroupId, dto.getId()));

            List<Long> allEmployeeIds = new ArrayList<>();
            if (dto.getDepartmentIds() != null && !dto.getDepartmentIds().isEmpty()) {
                List<Long> deptEmpIds = employeeService.lambdaQuery()
                        .in(Employee::getDepartmentId, dto.getDepartmentIds())
                        .list().stream().map(Employee::getId).collect(Collectors.toList());
                allEmployeeIds.addAll(deptEmpIds);
            }
            if (dto.getEmployeeIds() != null && !dto.getEmployeeIds().isEmpty()) {
                allEmployeeIds.addAll(dto.getEmployeeIds());
            }
            if (!allEmployeeIds.isEmpty()) {
                assignEmployees(dto.getId(), allEmployeeIds.stream().distinct().collect(Collectors.toList()));
            }
        }

        return convertToVO(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        AttendanceGroup group = this.getById(id);
        ThrowUtils.throwIf(group == null, ErrorCode.NOT_FOUND_ERROR, "考勤组不存在");

        groupEmployeeMapper.delete(new LambdaQueryWrapper<AttendanceGroupEmployee>()
                .eq(AttendanceGroupEmployee::getGroupId, id));
        this.removeById(id);
    }

    @Override
    public AttendanceGroupVO getGroupDetail(Long id) {
        AttendanceGroup group = this.getById(id);
        ThrowUtils.throwIf(group == null, ErrorCode.NOT_FOUND_ERROR, "考勤组不存在");
        return convertToVO(group);
    }

    @Override
    public List<AttendanceGroupVO> getAllGroups() {
        List<AttendanceGroup> groups = this.lambdaQuery().orderByDesc(AttendanceGroup::getCreateTime).list();
        return groups.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public AttendanceGroup getGroupByEmployeeId(Long employeeId) {
        AttendanceGroupEmployee groupEmployee = groupEmployeeMapper.selectOne(new LambdaQueryWrapper<AttendanceGroupEmployee>()
                .eq(AttendanceGroupEmployee::getEmployeeId, employeeId));
        if (groupEmployee == null) {
            return this.lambdaQuery().eq(AttendanceGroup::getStatus, 1).one();
        }
        return this.getById(groupEmployee.getGroupId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignEmployees(Long groupId, List<Long> employeeIds) {
        ThrowUtils.throwIf(groupId == null, ErrorCode.PARAMS_ERROR, "考勤组ID不能为空");

        List<AttendanceGroupEmployee> existing = groupEmployeeMapper.selectList(new LambdaQueryWrapper<AttendanceGroupEmployee>()
                .eq(AttendanceGroupEmployee::getGroupId, groupId));
        List<Long> existingIds = existing.stream()
                .map(AttendanceGroupEmployee::getEmployeeId).collect(Collectors.toList());

        List<Long> toAdd = employeeIds.stream()
                .filter(id -> !existingIds.contains(id)).collect(Collectors.toList());

        for (Long empId : toAdd) {
            AttendanceGroupEmployee ge = new AttendanceGroupEmployee();
            ge.setGroupId(groupId);
            ge.setEmployeeId(empId);
            ge.setCreateTime(new Date());
            groupEmployeeMapper.insert(ge);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeEmployees(Long groupId, List<Long> employeeIds) {
        ThrowUtils.throwIf(groupId == null, ErrorCode.PARAMS_ERROR, "考勤组ID不能为空");

        groupEmployeeMapper.delete(new LambdaQueryWrapper<AttendanceGroupEmployee>()
                .eq(AttendanceGroupEmployee::getGroupId, groupId)
                .in(AttendanceGroupEmployee::getEmployeeId, employeeIds));
    }

    private void applyTimeFields(AttendanceGroupDTO dto, AttendanceGroup group) {
        if (dto.getWorkStartTime() != null && !dto.getWorkStartTime().isEmpty()) {
            group.setWorkStartTime(parseTimeString(dto.getWorkStartTime()));
        }
        if (dto.getWorkEndTime() != null && !dto.getWorkEndTime().isEmpty()) {
            group.setWorkEndTime(parseTimeString(dto.getWorkEndTime()));
        }
        if (dto.getLunchStartTime() != null && !dto.getLunchStartTime().isEmpty()) {
            group.setLunchStartTime(parseTimeString(dto.getLunchStartTime()));
        }
        if (dto.getLunchEndTime() != null && !dto.getLunchEndTime().isEmpty()) {
            group.setLunchEndTime(parseTimeString(dto.getLunchEndTime()));
        }
        if (dto.getFlexibleStart() != null && !dto.getFlexibleStart().isEmpty()) {
            group.setFlexibleStart(parseTimeString(dto.getFlexibleStart()));
        }
        if (dto.getFlexibleEnd() != null && !dto.getFlexibleEnd().isEmpty()) {
            group.setFlexibleEnd(parseTimeString(dto.getFlexibleEnd()));
        }
    }

    private Date parseTimeString(String time) {
        if (time == null || time.isEmpty()) return null;
        // 统一补全为 HH:mm:ss 格式
        String[] parts = time.trim().split(":");
        if (parts.length == 2) {
            time = time + ":00";
        }
        return DateUtil.parseTime(time);
    }

    private AttendanceGroupVO convertToVO(AttendanceGroup group) {
        AttendanceGroupVO vo = new AttendanceGroupVO();
        BeanUtils.copyProperties(group, vo);

        vo.setShiftTypeText(getShiftTypeText(group.getShiftType()));
        vo.setStatusText(Integer.valueOf(1).equals(group.getStatus()) ? "启用" : "禁用");

        List<AttendanceGroupEmployee> employees = groupEmployeeMapper.selectList(new LambdaQueryWrapper<AttendanceGroupEmployee>()
                .eq(AttendanceGroupEmployee::getGroupId, group.getId()));
        vo.setEmployeeCount(employees.size());
        vo.setEmployeeIds(employees.stream()
                .map(AttendanceGroupEmployee::getEmployeeId).collect(Collectors.toList()));

        if (!employees.isEmpty()) {
            List<Long> empIds = employees.stream()
                    .map(AttendanceGroupEmployee::getEmployeeId).collect(Collectors.toList());
            List<Employee> empList = employeeService.listByIds(empIds);
            Set<Long> deptIdSet = empList.stream()
                    .map(Employee::getDepartmentId).filter(Objects::nonNull).collect(Collectors.toSet());
            vo.setDepartmentIds(new ArrayList<>(deptIdSet));
            if (!deptIdSet.isEmpty()) {
                List<Department> depts = departmentService.listByIds(new ArrayList<>(deptIdSet));
                vo.setDepartmentNames(depts.stream()
                        .map(Department::getDeptName).filter(Objects::nonNull).collect(Collectors.toList()));
            } else {
                vo.setDepartmentNames(Collections.emptyList());
            }
        } else {
            vo.setDepartmentIds(Collections.emptyList());
            vo.setDepartmentNames(Collections.emptyList());
        }

        return vo;
    }

    private String getShiftTypeText(Integer shiftType) {
        if (shiftType == null) return "未知";
        switch (shiftType) {
            case 0: return "固定班";
            case 1: return "弹性班";
            case 2: return "排班制";
            default: return "未知";
        }
    }
}
