package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.mapper.PositionMapper;
import com.limou.hrms.model.dto.position.PositionCreateRequest;
import com.limou.hrms.model.dto.position.PositionQueryRequest;
import com.limou.hrms.model.dto.position.PositionUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.PositionSequence;
import com.limou.hrms.model.vo.PositionVO;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    private final DepartmentService departmentService;

    private final EmployeeMapper employeeMapper;

    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;

    private final DataScopeContext dataScopeContext;

    // ==================== 创建职位 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Position createPosition(PositionCreateRequest dto) {
        PositionSequence sequence = PositionSequence.getByValue(dto.getSequence());
        if (sequence == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的职位序列");
        }
        sequence.validateLevelRange(dto.getLevelMin(), dto.getLevelMax());
        validateDepartment(dto.getDepartmentId());
        checkPositionNameUnique(dto.getName(), dto.getDepartmentId(), null);

        Position position = new Position();
        BeanUtils.copyProperties(dto, position);
        boolean saved = this.save(position);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建职位失败");
        }
        return position;
    }

    // ==================== 更新职位 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Position updatePosition(Long id, PositionUpdateRequest dto) {
        Position existPos = getUndeletedPositionOrThrow(id);

        PositionSequence sequence = null;
        if (dto.getSequence() != null) {
            sequence = PositionSequence.getByValue(dto.getSequence());
            if (sequence == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的职位序列");
            }
        } else {
            sequence = PositionSequence.getByValue(existPos.getSequence());
        }

        int effectiveMin = dto.getLevelMin() != null ? dto.getLevelMin() : existPos.getLevelMin();
        int effectiveMax = dto.getLevelMax() != null ? dto.getLevelMax() : existPos.getLevelMax();
        if (sequence != null) {
            sequence.validateLevelRange(effectiveMin, effectiveMax);
        }

        if (dto.getDepartmentId() != null) {
            validateDepartment(dto.getDepartmentId());
        }

        if (StringUtils.hasText(dto.getName())) {
            Long effectiveDeptId = dto.getDepartmentId() != null ? dto.getDepartmentId() : existPos.getDepartmentId();
            checkPositionNameUnique(dto.getName(), effectiveDeptId, id);
        }

        BeanUtils.copyProperties(dto, existPos, "id", "createTime", "updateTime", "isDeleted");
        existPos.setDepartmentId(dto.getDepartmentId());
        existPos.setDescription(dto.getDescription());

        boolean updated = this.updateById(existPos);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新职位失败");
        }
        return existPos;
    }

    // ==================== 删除职位 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePosition(Long id) {
        getUndeletedPositionOrThrow(id);

        List<EmployeeWorkInfo> workInfos = employeeWorkInfoMapper.selectList(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .eq(EmployeeWorkInfo::getPositionId, id));
        if (!workInfos.isEmpty()) {
            List<Long> employeeIds = workInfos.stream()
                    .map(EmployeeWorkInfo::getEmployeeId).collect(Collectors.toList());
            long activeCount = employeeMapper.selectCount(
                    Wrappers.<Employee>lambdaQuery()
                            .in(Employee::getId, employeeIds)
                            .in(Employee::getStatus, EmployeeStatus.getActiveValues()));
            if (activeCount > 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("employeeCount", activeCount);
                throw new BusinessException(ErrorCode.POSITION_HAS_EMPLOYEES.getCode(),
                        ErrorCode.POSITION_HAS_EMPLOYEES.getMessage());
            }
        }

        boolean removed = this.removeById(id);
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除职位失败");
        }
    }

    // ==================== 查询职位列表 ====================

    @Override
    public Page<PositionVO> getPositionList(PositionQueryRequest queryReq) {
        LambdaQueryWrapper<Position> query = Wrappers.<Position>lambdaQuery();
        if (StringUtils.hasText(queryReq.getKeyword())) {
            query.like(Position::getName, queryReq.getKeyword());
        }
        if (queryReq.getSequence() != null) {
            query.eq(Position::getSequence, queryReq.getSequence());
        }
        if (queryReq.getDepartmentId() != null) {
            query.eq(Position::getDepartmentId, queryReq.getDepartmentId());
        }

        // 部门主管数据权限：只能看管辖部门的职位 + 全公司通用职位
        DataScopeEnum scope = dataScopeContext.canManageOrganization();
        if (scope == DataScopeEnum.DEPT) {
            Set<Long> managedDeptIds = dataScopeContext.getManagedDepartmentIds();
            if (managedDeptIds == null || managedDeptIds.isEmpty()) {
                query.isNull(Position::getDepartmentId); // 只看全公司通用
            } else {
                query.and(w -> w.in(Position::getDepartmentId, managedDeptIds)
                        .or().isNull(Position::getDepartmentId));
            }
        } else if (scope == DataScopeEnum.NONE) {
            return new Page<>(queryReq.getCurrent(), queryReq.getPageSize(), 0);
        }

        query.orderByAsc(Position::getSequence, Position::getId);

        Page<Position> page = this.page(
                new Page<>(queryReq.getCurrent(), queryReq.getPageSize()), query);

        // 批量查部门名称
        List<Long> deptIds = page.getRecords().stream()
                .map(Position::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> deptNameMap;
        if (!deptIds.isEmpty()) {
            List<Department> depts = departmentService.listByIds(deptIds);
            deptNameMap = depts.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getName));
        } else {
            deptNameMap = Collections.emptyMap();
        }

        // 批量查询各职位在职员工数
        List<Long> positionIds = page.getRecords().stream()
                .map(Position::getId).collect(Collectors.toList());
        Map<Long, Integer> positionCountMap = loadPositionActiveCountMap(positionIds);

        List<PositionVO> voList = page.getRecords().stream().map(pos -> {
            PositionVO vo = new PositionVO();
            BeanUtils.copyProperties(pos, vo);
            PositionSequence seq = PositionSequence.getByValue(pos.getSequence());
            if (seq != null) {
                vo.setSequenceDesc(seq.getDesc() + seq.getPrefix());
                vo.setLevelRange(seq.getLevelRange(pos.getLevelMin(), pos.getLevelMax()));
            }
            if (pos.getDepartmentId() != null) {
                vo.setDepartmentName(deptNameMap.getOrDefault(pos.getDepartmentId(), ""));
            } else {
                vo.setDepartmentName("全公司通用");
            }
            vo.setEmployeeCount(positionCountMap.getOrDefault(pos.getId(), 0));
            return vo;
        }).collect(Collectors.toList());

        Page<PositionVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    // ==================== 查询职位详情 ====================

    @Override
    public PositionVO getPositionDetail(Long id) {
        Position pos = getUndeletedPositionOrThrow(id);
        PositionVO vo = new PositionVO();
        BeanUtils.copyProperties(pos, vo);

        PositionSequence seq = PositionSequence.getByValue(pos.getSequence());
        if (seq != null) {
            vo.setSequenceDesc(seq.getDesc() + seq.getPrefix());
            vo.setLevelRange(seq.getLevelRange(pos.getLevelMin(), pos.getLevelMax()));
        }
        if (pos.getDepartmentId() != null) {
            Department dept = departmentService.getById(pos.getDepartmentId());
            if (dept != null && dept.getIsDeleted() == 0) {
                vo.setDepartmentName(dept.getName());
            }
        } else {
            vo.setDepartmentName("全公司通用");
        }
        return vo;
    }

    // ==================== 序列枚举 ====================

    @Override
    public List<Map<String, Object>> getSequences() {
        return Arrays.stream(PositionSequence.values()).map(seq -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sequence", seq.getValue());
            map.put("sequenceName", seq.getPrefix() + seq.getDesc());
            return map;
        }).collect(Collectors.toList());
    }

    // ==================== 私有校验方法 ====================

    /**
     * 批量查询各职位的在职员工数
     */
    private Map<Long, Integer> loadPositionActiveCountMap(List<Long> positionIds) {
        if (positionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 一次 JOIN 查询：work_info + employee 在职过滤
        List<EmployeeWorkInfo> workInfos = employeeWorkInfoMapper.selectList(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .in(EmployeeWorkInfo::getPositionId, positionIds)
                        .inSql(EmployeeWorkInfo::getEmployeeId,
                                "SELECT e.id FROM employee e WHERE e.status IN ("
                                + EmployeeStatus.getActiveValues().stream()
                                        .map(String::valueOf).collect(Collectors.joining(","))
                                + ") AND e.is_deleted = 0"));
        if (workInfos.isEmpty()) {
            return Collections.emptyMap();
        }
        return workInfos.stream()
                .collect(Collectors.groupingBy(EmployeeWorkInfo::getPositionId, Collectors.summingInt(e -> 1)));
    }

    private Position getUndeletedPositionOrThrow(Long id) {
        Position pos = this.lambdaQuery().eq(Position::getId, id).one();
        if (pos == null) {
            throw new BusinessException(ErrorCode.POSITION_NOT_FOUND);
        }
        return pos;
    }

    private void validateDepartment(Long departmentId) {
        if (departmentId == null) return;
        Department dept = departmentService.getById(departmentId);
        if (dept == null || dept.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
    }

    private void checkPositionNameUnique(String name, Long departmentId, Long excludeId) {
        LambdaQueryWrapper<Position> query = Wrappers.<Position>lambdaQuery().eq(Position::getName, name);
        if (departmentId != null) {
            query.eq(Position::getDepartmentId, departmentId);
        } else {
            query.isNull(Position::getDepartmentId);
        }
        if (excludeId != null) {
            query.ne(Position::getId, excludeId);
        }
        if (this.count(query) > 0) {
            throw new BusinessException(ErrorCode.POSITION_NAME_DUPLICATE);
        }
    }
}
