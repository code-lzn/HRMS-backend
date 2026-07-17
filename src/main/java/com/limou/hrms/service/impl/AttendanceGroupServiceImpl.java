package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.AttendanceGroupMapper;
import com.limou.hrms.mapper.AttendanceGroupRuleMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.mapper.PositionMapper;
import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest;
import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest.RuleDTO;
import com.limou.hrms.model.entity.AttendanceGroup;
import com.limou.hrms.model.entity.AttendanceGroupRule;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.vo.AttendanceGroupVO;
import com.limou.hrms.service.AttendanceGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考勤组服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AttendanceGroupServiceImpl extends ServiceImpl<AttendanceGroupMapper, AttendanceGroup>
        implements AttendanceGroupService {

    private final AttendanceGroupRuleMapper attendanceGroupRuleMapper;

    private final PositionMapper positionMapper;

    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;

    private final DataScopeContext dataScopeContext;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceGroupVO createAttendanceGroup(AttendanceGroupCreateRequest dto) {
        DataScopeEnum scope = dataScopeContext.canManageOrganization();
        if (scope == DataScopeEnum.NONE) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean isDeptHead = (scope == DataScopeEnum.DEPT);
        Set<Long> managedDeptIds = isDeptHead ? dataScopeContext.getManagedDepartmentIds() : null;

        // 校验每条规则的权限和重复性
        for (RuleDTO rule : dto.getRules()) {
            int ruleType = rule.getRuleType();
            Long targetId = rule.getTargetId();

            // 重复校验：(ruleType, targetId) 全局唯一
            checkRuleNotExists(ruleType, targetId);

            if (isDeptHead) {
                validateDeptHeadRule(ruleType, targetId, managedDeptIds);
            }
        }

        // 保存考勤组
        AttendanceGroup group = new AttendanceGroup();
        BeanUtils.copyProperties(dto, group, "rules");
        group.setStartTime(LocalTime.parse(dto.getStartTime()));
        group.setEndTime(LocalTime.parse(dto.getEndTime()));
        if (dto.getRestStartTime() != null) {
            group.setRestStartTime(LocalTime.parse(dto.getRestStartTime()));
        }
        if (dto.getRestEndTime() != null) {
            group.setRestEndTime(LocalTime.parse(dto.getRestEndTime()));
        }
        if (dto.getFlexStartTime() != null) {
            group.setFlexStartTime(LocalTime.parse(dto.getFlexStartTime()));
        }
        if (dto.getFlexEndTime() != null) {
            group.setFlexEndTime(LocalTime.parse(dto.getFlexEndTime()));
        }
        if (dto.getCoreStartTime() != null) {
            group.setCoreStartTime(LocalTime.parse(dto.getCoreStartTime()));
        }
        if (dto.getCoreEndTime() != null) {
            group.setCoreEndTime(LocalTime.parse(dto.getCoreEndTime()));
        }

        boolean saved = this.save(group);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建考勤组失败");
        }

        // 批量保存规则
        List<AttendanceGroupRule> rules = dto.getRules().stream().map(r -> {
            AttendanceGroupRule rule = new AttendanceGroupRule();
            rule.setAttendanceGroupId(group.getId());
            rule.setRuleType(r.getRuleType());
            rule.setTargetId(r.getTargetId());
            return rule;
        }).collect(Collectors.toList());
        for (AttendanceGroupRule rule : rules) {
            attendanceGroupRuleMapper.insert(rule);
        }

        log.info("创建了考勤组: {} (id={}), 规则数={}", group.getName(), group.getId(), rules.size());

        // 返回 VO
        AttendanceGroupVO vo = new AttendanceGroupVO();
        BeanUtils.copyProperties(group, vo);
        vo.setStartTime(dto.getStartTime());
        vo.setEndTime(dto.getEndTime());
        vo.setRules(dto.getRules());
        return vo;
    }

    // ==================== 权限校验 ====================

    /**
     * 部门管理员校验单条规则的权限
     */
    private void validateDeptHeadRule(int ruleType, Long targetId, Set<Long> managedDeptIds) {
        switch (ruleType) {
            case 1:
                // 按部门：targetId 必须在管辖范围内
                if (!managedDeptIds.contains(targetId)) {
                    throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DEPT_OUT_OF_SCOPE);
                }
                break;
            case 2:
                // 按职位
                validatePositionScope(targetId, managedDeptIds);
                break;
            case 3:
                // 按个人：员工的 department_id 必须在管辖范围内
                validateEmployeeScope(targetId, managedDeptIds);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的适用类型");
        }
    }

    /**
     * 校验职位是否在管辖范围内
     */
    private void validatePositionScope(Long positionId, Set<Long> managedDeptIds) {
        Position position = positionMapper.selectById(positionId);
        if (position == null) {
            throw new BusinessException(ErrorCode.POSITION_NOT_FOUND);
        }
        if (position.getDepartmentId() != null) {
            // 部门专属职位：部门必须在管辖范围内
            if (!managedDeptIds.contains(position.getDepartmentId())) {
                throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DEPT_OUT_OF_SCOPE);
            }
        } else {
            // 全公司通用职位：校验管辖范围内是否有在职员工持有此职位
            Long count = employeeWorkInfoMapper.selectCount(
                    Wrappers.<EmployeeWorkInfo>lambdaQuery()
                            .eq(EmployeeWorkInfo::getPositionId, positionId)
                            .in(EmployeeWorkInfo::getDepartmentId, managedDeptIds));
            if (count == null || count == 0) {
                throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_POSITION_NO_EMPLOYEE);
            }
        }
    }

    /**
     * 校验员工是否在管辖范围内
     */
    private void validateEmployeeScope(Long employeeId, Set<Long> managedDeptIds) {
        EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .eq(EmployeeWorkInfo::getEmployeeId, employeeId));
        if (workInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "员工工作信息不存在");
        }
        if (!managedDeptIds.contains(workInfo.getDepartmentId())) {
            throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DEPT_OUT_OF_SCOPE);
        }
    }

    // ==================== 重复校验 ====================

    /**
     * 检查 (ruleType, targetId) 是否已存在
     */
    private void checkRuleNotExists(int ruleType, Long targetId) {
        Long count = attendanceGroupRuleMapper.selectCount(
                Wrappers.<AttendanceGroupRule>lambdaQuery()
                        .eq(AttendanceGroupRule::getRuleType, ruleType)
                        .eq(AttendanceGroupRule::getTargetId, targetId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DUPLICATE);
        }
    }
}
