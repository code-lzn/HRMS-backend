package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest;
import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest.RuleDTO;
import com.limou.hrms.model.dto.attendance.AttendanceGroupUpdateRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.vo.AttendanceGroupListVO;
import com.limou.hrms.model.vo.AttendanceGroupVO;
import com.limou.hrms.service.AttendanceGroupService;
import com.limou.hrms.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
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

    private final EmployeePersonalInfoMapper employeePersonalInfoMapper;

    private final DepartmentService departmentService;

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

        List<RuleDTO> ruleList = dto.getRules();

        // ① 批量校验：重复性 + 部门管理员权限
        validateRulesBatch(ruleList, isDeptHead, managedDeptIds, null);

        // ② 校验班次时间：弹性班(startTime/endTime 可为空)，其余必须填
        boolean isFlex = dto.getShiftType() != null && dto.getShiftType() == 2;
        if (!isFlex) {
            if (!StringUtils.hasText(dto.getStartTime()))
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "固定班/排班制必须设置上班时间");
            if (!StringUtils.hasText(dto.getEndTime()))
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "固定班/排班制必须设置下班时间");
        }

        // ③ 保存考勤组
        AttendanceGroup group = new AttendanceGroup();
        BeanUtils.copyProperties(dto, group, "rules");
        if (dto.getStartTime() != null) group.setStartTime(LocalTime.parse(dto.getStartTime()));
        if (dto.getEndTime() != null) group.setEndTime(LocalTime.parse(dto.getEndTime()));
        if (dto.getRestStartTime() != null) group.setRestStartTime(LocalTime.parse(dto.getRestStartTime()));
        if (dto.getRestEndTime() != null) group.setRestEndTime(LocalTime.parse(dto.getRestEndTime()));
        if (dto.getFlexStartTime() != null) group.setFlexStartTime(LocalTime.parse(dto.getFlexStartTime()));
        if (dto.getFlexEndTime() != null) group.setFlexEndTime(LocalTime.parse(dto.getFlexEndTime()));
        if (dto.getCoreStartTime() != null) group.setCoreStartTime(LocalTime.parse(dto.getCoreStartTime()));
        if (dto.getCoreEndTime() != null) group.setCoreEndTime(LocalTime.parse(dto.getCoreEndTime()));

        boolean saved = this.save(group);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建考勤组失败");
        }

        // ③ 批量保存规则（MetaObjectHandler 自动填充 createTime）
        List<AttendanceGroupRule> rules = ruleList.stream().map(r -> {
            AttendanceGroupRule rule = new AttendanceGroupRule();
            rule.setAttendanceGroupId(group.getId());
            rule.setRuleType(r.getRuleType());
            rule.setTargetId(r.getTargetId());
            return rule;
        }).collect(Collectors.toList());
        this.batchInsertRules(rules);

        log.info("创建了考勤组: {} (id={}), 规则数={}", group.getName(), group.getId(), rules.size());
        return toCreateVO(group, ruleList);
    }

    // ==================== 更新考勤组 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceGroupVO updateAttendanceGroup(Long id, AttendanceGroupUpdateRequest dto) {
        AttendanceGroup existGroup = this.lambdaQuery()
                .eq(AttendanceGroup::getId, id).one();
        if (existGroup == null) {
            throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_NOT_FOUND);
        }

        DataScopeEnum scope = dataScopeContext.canManageOrganization();
        if (scope == DataScopeEnum.NONE) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean isDeptHead = (scope == DataScopeEnum.DEPT);
        Set<Long> managedDeptIds = isDeptHead ? dataScopeContext.getManagedDepartmentIds() : null;

        // 更新基本字段（null = 不更新）
        if (dto.getName() != null) {
            existGroup.setName(dto.getName());
        }
        if (dto.getShiftType() != null) {
            existGroup.setShiftType(dto.getShiftType());
        }
        if (dto.getStartTime() != null) {
            existGroup.setStartTime(LocalTime.parse(dto.getStartTime()));
        }
        if (dto.getEndTime() != null) {
            existGroup.setEndTime(LocalTime.parse(dto.getEndTime()));
        }
        if (dto.getRestStartTime() != null) {
            existGroup.setRestStartTime(LocalTime.parse(dto.getRestStartTime()));
        }
        if (dto.getRestEndTime() != null) {
            existGroup.setRestEndTime(LocalTime.parse(dto.getRestEndTime()));
        }
        if (dto.getFlexStartTime() != null) {
            existGroup.setFlexStartTime(LocalTime.parse(dto.getFlexStartTime()));
        }
        if (dto.getFlexEndTime() != null) {
            existGroup.setFlexEndTime(LocalTime.parse(dto.getFlexEndTime()));
        }
        if (dto.getLateThreshold() != null) {
            existGroup.setLateThreshold(dto.getLateThreshold());
        }
        if (dto.getEarlyLeaveThreshold() != null) {
            existGroup.setEarlyLeaveThreshold(dto.getEarlyLeaveThreshold());
        }
        if (dto.getIpWhitelist() != null) {
            existGroup.setIpWhitelist(dto.getIpWhitelist());
        }
        if (dto.getCoreStartTime() != null) {
            existGroup.setCoreStartTime(LocalTime.parse(dto.getCoreStartTime()));
        }
        if (dto.getCoreEndTime() != null) {
            existGroup.setCoreEndTime(LocalTime.parse(dto.getCoreEndTime()));
        }
        if (dto.getWorkHours() != null) {
            existGroup.setWorkHours(dto.getWorkHours());
        }

        this.updateById(existGroup);

        // rules：传则全量替换
        if (dto.getRules() != null) {
            // 批量校验
            validateRulesBatch(dto.getRules(), isDeptHead, managedDeptIds, id);

            // 删除旧规则 + 批量插入新规则
            attendanceGroupRuleMapper.delete(
                    Wrappers.<AttendanceGroupRule>lambdaQuery()
                            .eq(AttendanceGroupRule::getAttendanceGroupId, id));

            List<AttendanceGroupRule> entities = dto.getRules().stream().map(r -> {
                AttendanceGroupRule entity = new AttendanceGroupRule();
                entity.setAttendanceGroupId(id);
                entity.setRuleType(r.getRuleType());
                entity.setTargetId(r.getTargetId());
                return entity;
            }).collect(Collectors.toList());
            this.batchInsertRules(entities);
        }

        log.info("更新了考勤组: {} (id={})", existGroup.getName(), id);
        return toCreateVO(existGroup, dto.getRules());
    }

    // ==================== 查询列表 ====================

    @Override
    public Page<AttendanceGroupListVO> queryAttendanceGroups(String keyword, Integer shiftType, int page, int size) {
        DataScopeEnum scope = dataScopeContext.canManageOrganization();
        if (scope == DataScopeEnum.NONE) {
            return new Page<>(page, size, 0);
        }

        LambdaQueryWrapper<AttendanceGroup> query = Wrappers.<AttendanceGroup>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            query.like(AttendanceGroup::getName, keyword);
        }
        if (shiftType != null) {
            query.eq(AttendanceGroup::getShiftType, shiftType);
        }

        // dept_head：只查可见的考勤组
        if (scope == DataScopeEnum.DEPT) {
            Set<Long> visibleIds = resolveVisibleGroupIds();
            if (visibleIds.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            query.in(AttendanceGroup::getId, visibleIds);
        }

        query.orderByDesc(AttendanceGroup::getCreateTime);

        Page<AttendanceGroup> groupPage = this.page(new Page<>(page, size), query);

        // 批量查规则，用于构建 ruleSummary
        Set<Long> groupIds = groupPage.getRecords().stream()
                .map(AttendanceGroup::getId).collect(Collectors.toSet());
        Map<Long, List<AttendanceGroupRule>> rulesMap = loadRulesMap(groupIds);
        Map<Long, String> summaryMap = buildRuleSummaries(rulesMap);

        List<AttendanceGroupListVO> voList = groupPage.getRecords().stream().map(g -> {
            AttendanceGroupListVO vo = new AttendanceGroupListVO();
            vo.setId(g.getId());
            vo.setName(g.getName());
            vo.setShiftType(g.getShiftType());
            vo.setShiftTypeDesc(getShiftTypeDesc(g.getShiftType()));
            if (g.getStartTime() != null) vo.setStartTime(g.getStartTime().toString());
            if (g.getEndTime() != null) vo.setEndTime(g.getEndTime().toString());
            if (g.getCoreStartTime() != null) vo.setCoreStartTime(g.getCoreStartTime().toString());
            if (g.getCoreEndTime() != null) vo.setCoreEndTime(g.getCoreEndTime().toString());
            vo.setWorkHours(g.getWorkHours());
            vo.setLateThreshold(g.getLateThreshold());
            vo.setEarlyLeaveThreshold(g.getEarlyLeaveThreshold());
            vo.setRuleSummary(summaryMap.getOrDefault(g.getId(), ""));
            vo.setCreateTime(g.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<AttendanceGroupListVO> result = new Page<>(page, size, groupPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    /**
     * 解析部管可见的考勤组 ID 集合
     */
    private Set<Long> resolveVisibleGroupIds() {
        Set<Long> managedDeptIds = dataScopeContext.getManagedDepartmentIds();
        if (managedDeptIds == null || managedDeptIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> visibleIds = new HashSet<>();

        // ruleType=1：targetId 就是部门ID，直接在管辖范围内即匹配
        List<AttendanceGroupRule> deptRules = attendanceGroupRuleMapper.selectList(
                Wrappers.<AttendanceGroupRule>lambdaQuery()
                        .eq(AttendanceGroupRule::getRuleType, 1)
                        .in(AttendanceGroupRule::getTargetId, managedDeptIds));
        deptRules.forEach(r -> visibleIds.add(r.getAttendanceGroupId()));

        // ruleType=2：按职位
        List<AttendanceGroupRule> posRules = attendanceGroupRuleMapper.selectList(
                Wrappers.<AttendanceGroupRule>lambdaQuery()
                        .eq(AttendanceGroupRule::getRuleType, 2));
        for (AttendanceGroupRule rule : posRules) {
            Position pos = positionMapper.selectById(rule.getTargetId());
            if (pos == null) continue;
            if (pos.getDepartmentId() != null) {
                if (managedDeptIds.contains(pos.getDepartmentId())) {
                    visibleIds.add(rule.getAttendanceGroupId());
                }
            } else {
                // 全公司通用：查管辖区是否有员工持有此职位
                Long count = employeeWorkInfoMapper.selectCount(
                        Wrappers.<EmployeeWorkInfo>lambdaQuery()
                                .eq(EmployeeWorkInfo::getPositionId, pos.getId())
                                .in(EmployeeWorkInfo::getDepartmentId, managedDeptIds));
                if (count != null && count > 0) {
                    visibleIds.add(rule.getAttendanceGroupId());
                }
            }
        }

        // ruleType=3：按个人 — 查员工所在部门是否在管辖区
        List<AttendanceGroupRule> empRules = attendanceGroupRuleMapper.selectList(
                Wrappers.<AttendanceGroupRule>lambdaQuery()
                        .eq(AttendanceGroupRule::getRuleType, 3));
        Set<Long> empIds = empRules.stream()
                .map(AttendanceGroupRule::getTargetId).collect(Collectors.toSet());
        if (!empIds.isEmpty()) {
            List<EmployeeWorkInfo> workInfos = employeeWorkInfoMapper.selectList(
                    Wrappers.<EmployeeWorkInfo>lambdaQuery()
                            .in(EmployeeWorkInfo::getEmployeeId, empIds)
                            .in(EmployeeWorkInfo::getDepartmentId, managedDeptIds));
            Set<Long> validEmpIds = workInfos.stream()
                    .map(EmployeeWorkInfo::getEmployeeId).collect(Collectors.toSet());
            empRules.forEach(r -> {
                if (validEmpIds.contains(r.getTargetId())) {
                    visibleIds.add(r.getAttendanceGroupId());
                }
            });
        }

        return visibleIds;
    }

    /**
     * 批量加载考勤组的规则
     */
    private Map<Long, List<AttendanceGroupRule>> loadRulesMap(Set<Long> groupIds) {
        if (groupIds.isEmpty()) return Collections.emptyMap();
        List<AttendanceGroupRule> rules = attendanceGroupRuleMapper.selectList(
                Wrappers.<AttendanceGroupRule>lambdaQuery()
                        .in(AttendanceGroupRule::getAttendanceGroupId, groupIds));
        return rules.stream()
                .collect(Collectors.groupingBy(AttendanceGroupRule::getAttendanceGroupId));
    }

    /**
     * 构建 ruleSummary
     */
    private Map<Long, String> buildRuleSummaries(Map<Long, List<AttendanceGroupRule>> rulesMap) {
        Map<Long, String> result = new HashMap<>();
        if (rulesMap.isEmpty()) return result;

        // 收集所有 targetId 按类型分组 → 批量查名称
        Set<Long> deptIds = new HashSet<>();
        Set<Long> posIds = new HashSet<>();
        Set<Long> empIds = new HashSet<>();
        for (List<AttendanceGroupRule> rules : rulesMap.values()) {
            for (AttendanceGroupRule r : rules) {
                if (r.getRuleType() == 1) deptIds.add(r.getTargetId());
                else if (r.getRuleType() == 2) posIds.add(r.getTargetId());
                else if (r.getRuleType() == 3) empIds.add(r.getTargetId());
            }
        }

        Map<Long, String> deptNameMap = loadDeptNames(deptIds);
        Map<Long, String> posNameMap = loadPosNames(posIds);
        Map<Long, String> empNameMap = loadEmpNames(empIds);

        for (Map.Entry<Long, List<AttendanceGroupRule>> entry : rulesMap.entrySet()) {
            List<String> names = new ArrayList<>();
            for (AttendanceGroupRule r : entry.getValue()) {
                if (r.getRuleType() == 1) {
                    String name = deptNameMap.get(r.getTargetId());
                    if (name != null) names.add(name);
                } else if (r.getRuleType() == 2) {
                    String name = posNameMap.get(r.getTargetId());
                    if (name != null) names.add(name);
                } else if (r.getRuleType() == 3) {
                    String name = empNameMap.get(r.getTargetId());
                    if (name != null) names.add(name);
                }
            }
            String summary = names.isEmpty() ? "" :
                    String.join("、", names) + "（" + names.size() + "个）";
            result.put(entry.getKey(), summary);
        }
        return result;
    }

    private Map<Long, String> loadDeptNames(Set<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        return departmentService.listByIds(new ArrayList<>(ids)).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }

    private Map<Long, String> loadPosNames(Set<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        return positionMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Position::getId, Position::getName));
    }

    private Map<Long, String> loadEmpNames(Set<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        return employeePersonalInfoMapper.selectList(
                        Wrappers.<EmployeePersonalInfo>lambdaQuery()
                                .in(EmployeePersonalInfo::getEmployeeId, ids))
                .stream()
                .collect(Collectors.toMap(EmployeePersonalInfo::getEmployeeId, EmployeePersonalInfo::getName));
    }

    private static String getShiftTypeDesc(Integer shiftType) {
        if (shiftType == null) return "";
        switch (shiftType) {
            case 1: return "固定班";
            case 2: return "弹性班";
            case 3: return "排班制";
            default: return "未知";
        }
    }

    // ==================== 查询详情 ====================

    @Override
    public AttendanceGroupVO getAttendanceGroupDetail(Long id) {
        AttendanceGroup group = this.lambdaQuery()
                .eq(AttendanceGroup::getId, id).one();
        if (group == null) {
            throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_NOT_FOUND);
        }

        // dept_head：校验管辖范围
        DataScopeEnum scope = dataScopeContext.canManageOrganization();
        if (scope == DataScopeEnum.DEPT) {
            Set<Long> visibleIds = resolveVisibleGroupIds();
            if (!visibleIds.contains(id)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        // 加载规则
        List<AttendanceGroupRule> rules = attendanceGroupRuleMapper.selectList(
                Wrappers.<AttendanceGroupRule>lambdaQuery()
                        .eq(AttendanceGroupRule::getAttendanceGroupId, id));
        List<RuleDTO> ruleDTOs = rules.stream().map(r -> {
            RuleDTO dto = new RuleDTO();
            dto.setRuleType(r.getRuleType());
            dto.setTargetId(r.getTargetId());
            return dto;
        }).collect(Collectors.toList());

        AttendanceGroupVO vo = toCreateVO(group, ruleDTOs);
        vo.setShiftTypeDesc(getShiftTypeDesc(group.getShiftType()));
        return vo;
    }

    // ==================== 删除 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttendanceGroup(Long id) {
        AttendanceGroup existGroup = this.lambdaQuery()
                .eq(AttendanceGroup::getId, id).one();
        if (existGroup == null) {
            throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_NOT_FOUND);
        }

        DataScopeEnum scope = dataScopeContext.canManageOrganization();
        if (scope == DataScopeEnum.NONE) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // dept_head：只能删自己管辖范围内的考勤组
        if (scope == DataScopeEnum.DEPT) {
            Set<Long> visibleIds = resolveVisibleGroupIds();
            if (!visibleIds.contains(id)) {
                throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DEPT_OUT_OF_SCOPE);
            }
        }

        // 删除前校验：是否还有适用规则
        Long ruleCount = attendanceGroupRuleMapper.selectCount(
                Wrappers.<AttendanceGroupRule>lambdaQuery()
                        .eq(AttendanceGroupRule::getAttendanceGroupId, id));
        if (ruleCount != null && ruleCount > 0) {
            throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_HAS_EMPLOYEES);
        }

        boolean removed = this.removeById(id);
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除考勤组失败");
        }
        log.info("删除了考勤组: {} (id={})", existGroup.getName(), id);
    }

    // ==================== 权限校验 ====================

    /**
     * 批量校验所有规则：一次查询完成重复检查 + 关联数据加载，替代 N 次单条 SQL
     */
    private void validateRulesBatch(List<RuleDTO> ruleList, boolean isDeptHead,
                                     Set<Long> managedDeptIds, Long excludeGroupId) {
        if (ruleList.isEmpty()) return;

        // ① 重复校验：一次性查出所有匹配的已有规则，用 Set 判重
        Set<String> dtoKeys = new HashSet<>();
        Set<String> dupSet = new HashSet<>();
        for (RuleDTO r : ruleList) {
            String key = r.getRuleType() + "_" + r.getTargetId();
            if (!dtoKeys.add(key)) {
                throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DUPLICATE,
                        "同一请求中存在重复的规则");
            }
        }

        LambdaQueryWrapper<AttendanceGroupRule> dupQuery = Wrappers.<AttendanceGroupRule>lambdaQuery();
        // 构造 OR 条件：(ruleType=1 AND targetId=101) OR (ruleType=2 AND targetId=201) ...
        dupQuery.and(w -> {
            for (int i = 0; i < ruleList.size(); i++) {
                RuleDTO r = ruleList.get(i);
                if (i > 0) w.or();
                w.eq(AttendanceGroupRule::getRuleType, r.getRuleType())
                 .eq(AttendanceGroupRule::getTargetId, r.getTargetId());
            }
        });
        if (excludeGroupId != null) {
            dupQuery.ne(AttendanceGroupRule::getAttendanceGroupId, excludeGroupId);
        }
        List<AttendanceGroupRule> existingRules = attendanceGroupRuleMapper.selectList(dupQuery);
        for (AttendanceGroupRule existing : existingRules) {
            String key = existing.getRuleType() + "_" + existing.getTargetId();
            if (dtoKeys.contains(key)) {
                throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DUPLICATE);
            }
        }

        // ② dept_head 权限校验：按类型批量预加载关联数据
        if (isDeptHead) {
            // 收集需要查询的 positionId 和 employeeId
            Set<Long> posIds = new HashSet<>();
            Set<Long> empIds = new HashSet<>();
            for (RuleDTO r : ruleList) {
                if (r.getRuleType() == 2) posIds.add(r.getTargetId());
                else if (r.getRuleType() == 3) empIds.add(r.getTargetId());
            }

            // 批量查职位
            Map<Long, Position> posMap = Collections.emptyMap();
            if (!posIds.isEmpty()) {
                posMap = positionMapper.selectBatchIds(posIds).stream()
                        .collect(Collectors.toMap(Position::getId, p -> p));
            }
            // 批量查员工工作信息
            Map<Long, EmployeeWorkInfo> workInfoMap = Collections.emptyMap();
            if (!empIds.isEmpty()) {
                workInfoMap = employeeWorkInfoMapper.selectList(
                        Wrappers.<EmployeeWorkInfo>lambdaQuery()
                                .in(EmployeeWorkInfo::getEmployeeId, empIds))
                        .stream()
                        .collect(Collectors.toMap(EmployeeWorkInfo::getEmployeeId, wi -> wi, (a, b) -> a));
            }

            // 内存校验
            for (RuleDTO r : ruleList) {
                switch (r.getRuleType()) {
                    case 1:
                        if (!managedDeptIds.contains(r.getTargetId())) {
                            throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DEPT_OUT_OF_SCOPE);
                        }
                        break;
                    case 2:
                        validatePositionScopeFromCache(r.getTargetId(), managedDeptIds, posMap);
                        break;
                    case 3:
                        validateEmployeeScopeFromCache(r.getTargetId(), managedDeptIds, workInfoMap);
                        break;
                }
            }
        }
    }

    private void validatePositionScopeFromCache(Long positionId, Set<Long> managedDeptIds,
                                                  Map<Long, Position> posMap) {
        Position position = posMap.get(positionId);
        if (position == null) {
            throw new BusinessException(ErrorCode.POSITION_NOT_FOUND);
        }
        if (position.getDepartmentId() != null) {
            if (!managedDeptIds.contains(position.getDepartmentId())) {
                throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DEPT_OUT_OF_SCOPE);
            }
        } else {
            Long count = employeeWorkInfoMapper.selectCount(
                    Wrappers.<EmployeeWorkInfo>lambdaQuery()
                            .eq(EmployeeWorkInfo::getPositionId, positionId)
                            .in(EmployeeWorkInfo::getDepartmentId, managedDeptIds));
            if (count == null || count == 0) {
                throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_POSITION_NO_EMPLOYEE);
            }
        }
    }

    private void validateEmployeeScopeFromCache(Long employeeId, Set<Long> managedDeptIds,
                                                  Map<Long, EmployeeWorkInfo> workInfoMap) {
        EmployeeWorkInfo workInfo = workInfoMap.get(employeeId);
        if (workInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "员工工作信息不存在");
        }
        if (!managedDeptIds.contains(workInfo.getDepartmentId())) {
            throw new BusinessException(ErrorCode.ATTENDANCE_GROUP_RULE_DEPT_OUT_OF_SCOPE);
        }
    }

    /**
     * 批量插入规则（复用当前事务，MetaObjectHandler 自动填充 createTime）
     */
    private void batchInsertRules(List<AttendanceGroupRule> rules) {
        for (AttendanceGroupRule rule : rules) {
            attendanceGroupRuleMapper.insert(rule);
        }
    }

    // ==================== VO 转换 ====================

    private AttendanceGroupVO toCreateVO(AttendanceGroup group, List<RuleDTO> rules) {
        AttendanceGroupVO vo = new AttendanceGroupVO();
        BeanUtils.copyProperties(group, vo);
        if (group.getStartTime() != null) {
            vo.setStartTime(group.getStartTime().toString());
        }
        if (group.getEndTime() != null) {
            vo.setEndTime(group.getEndTime().toString());
        }
        if (group.getRestStartTime() != null) {
            vo.setRestStartTime(group.getRestStartTime().toString());
        }
        if (group.getRestEndTime() != null) {
            vo.setRestEndTime(group.getRestEndTime().toString());
        }
        if (group.getFlexStartTime() != null) {
            vo.setFlexStartTime(group.getFlexStartTime().toString());
        }
        if (group.getFlexEndTime() != null) {
            vo.setFlexEndTime(group.getFlexEndTime().toString());
        }
        if (group.getCoreStartTime() != null) {
            vo.setCoreStartTime(group.getCoreStartTime().toString());
        }
        if (group.getCoreEndTime() != null) {
            vo.setCoreEndTime(group.getCoreEndTime().toString());
        }
        vo.setShiftTypeDesc(getShiftTypeDesc(group.getShiftType()));
        vo.setRules(rules);
        return vo;
    }
}
