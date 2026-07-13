package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.OrgConstant.PositionSequence;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.PositionMapper;
import com.limou.hrms.model.dto.position.PositionAddRequest;
import com.limou.hrms.model.dto.position.PositionQueryRequest;
import com.limou.hrms.model.dto.position.PositionUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.vo.PositionVO;
import com.limou.hrms.model.vo.SequenceLevelVO;
import com.limou.hrms.service.PositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 职位服务实现
 */
@Slf4j
@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public List<PositionVO> listPositions(PositionQueryRequest request) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<Position>()
                .orderByAsc(Position::getSequence)
                .orderByAsc(Position::getId);

        if (request.getSequence() != null) {
            wrapper.eq(Position::getSequence, request.getSequence());
        }
        if (request.getDepartmentId() != null) {
            wrapper.and(w -> w.isNull(Position::getDepartmentId)
                    .or().eq(Position::getDepartmentId, request.getDepartmentId()));
        }

        List<Position> positions = this.list(wrapper);

        Set<Long> deptIds = positions.stream()
                .map(Position::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = getDeptNameMap(deptIds);

        return positions.stream()
                .map(p -> convertToVO(p, deptNameMap))
                .collect(Collectors.toList());
    }

    @Override
    public Long addPosition(PositionAddRequest request) {
        String name = request.getName().trim();
        Integer sequence = request.getSequence();
        String levelMin = request.getLevelMin().trim();
        String levelMax = request.getLevelMax().trim();

        // 1. 校验职位名称不为空
        ThrowUtils.throwIf(name.isEmpty(), ErrorCode.PARAMS_ERROR, "职位名称不能为空");

        // 2. 校验序列有效性
        PositionSequence seq = PositionSequence.fromCode(sequence);
        ThrowUtils.throwIf(seq == null, ErrorCode.PARAMS_ERROR, "无效的职位序列: " + sequence);

        // 2. 校验职级在序列范围内
        ThrowUtils.throwIf(!seq.inRange(levelMin),
                ErrorCode.OPERATION_ERROR, "职级下限 " + levelMin + " 不在 " + seq.getPrefix() + " 序列范围内");
        ThrowUtils.throwIf(!seq.inRange(levelMax),
                ErrorCode.OPERATION_ERROR, "职级上限 " + levelMax + " 不在 " + seq.getPrefix() + " 序列范围内");

        // 3. 校验下限 <= 上限
        List<String> levels = Arrays.asList(seq.getLevels());
        int minIdx = levels.indexOf(levelMin);
        int maxIdx = levels.indexOf(levelMax);
        ThrowUtils.throwIf(minIdx > maxIdx, ErrorCode.OPERATION_ERROR,
                "职级下限 " + levelMin + " 不能高于上限 " + levelMax);

        // 4. 校验所属部门存在
        if (request.getDepartmentId() != null) {
            Department dept = departmentMapper.selectById(request.getDepartmentId());
            ThrowUtils.throwIf(dept == null, ErrorCode.NOT_FOUND_ERROR, "所属部门不存在或已被删除");
        }

        // 5. 校验同一序列+名称+部门唯一
        LambdaQueryWrapper<Position> existWrapper = new LambdaQueryWrapper<Position>()
                .eq(Position::getSequence, sequence)
                .eq(Position::getName, name);
        if (request.getDepartmentId() == null) {
            existWrapper.isNull(Position::getDepartmentId);
        } else {
            existWrapper.eq(Position::getDepartmentId, request.getDepartmentId());
        }
        long existCount = this.count(existWrapper);
        ThrowUtils.throwIf(existCount > 0, ErrorCode.OPERATION_ERROR, "已存在相同职位");

        // 5. 创建职位
        Position position = new Position();
        position.setName(name);
        position.setSequence(sequence);
        position.setDepartmentId(request.getDepartmentId());
        position.setLevelMin(levelMin);
        position.setLevelMax(levelMax);
        position.setDefaultProbationMonths(request.getDefaultProbationMonths());
        position.setDescription(request.getDescription());

        boolean saved = this.save(position);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "职位创建失败");

        log.info("职位创建成功: id={}, name={}, sequence={}", position.getId(), name, sequence);
        return position.getId();
    }

    @Override
    public void updatePosition(PositionUpdateRequest request) {
        Position position = this.getById(request.getId());
        ThrowUtils.throwIf(position == null, ErrorCode.NOT_FOUND_ERROR, "职位不存在");

        Integer sequence = request.getSequence();
        String name = request.getName().trim();
        String levelMin = request.getLevelMin().trim();
        String levelMax = request.getLevelMax().trim();

        // 1. 校验职位名称不为空
        ThrowUtils.throwIf(name.isEmpty(), ErrorCode.PARAMS_ERROR, "职位名称不能为空");

        // 2. 校验序列有效性
        PositionSequence seq = PositionSequence.fromCode(sequence);
        ThrowUtils.throwIf(seq == null, ErrorCode.PARAMS_ERROR, "无效的职位序列: " + sequence);

        // 2. 校验职级在序列范围内
        ThrowUtils.throwIf(!seq.inRange(levelMin),
                ErrorCode.OPERATION_ERROR, "职级下限 " + levelMin + " 不在 " + seq.getPrefix() + " 序列范围内");
        ThrowUtils.throwIf(!seq.inRange(levelMax),
                ErrorCode.OPERATION_ERROR, "职级上限 " + levelMax + " 不在 " + seq.getPrefix() + " 序列范围内");

        // 3. 校验下限 <= 上限
        List<String> levels = Arrays.asList(seq.getLevels());
        int minIdx = levels.indexOf(levelMin);
        int maxIdx = levels.indexOf(levelMax);
        ThrowUtils.throwIf(minIdx > maxIdx, ErrorCode.OPERATION_ERROR,
                "职级下限 " + levelMin + " 不能高于上限 " + levelMax);

        // 4. 校验所属部门存在
        if (request.getDepartmentId() != null) {
            Department dept = departmentMapper.selectById(request.getDepartmentId());
            ThrowUtils.throwIf(dept == null, ErrorCode.NOT_FOUND_ERROR, "所属部门不存在或已被删除");
        }

        // 5. 校验唯一性（排除自身）
        LambdaQueryWrapper<Position> existWrapper = new LambdaQueryWrapper<Position>()
                .eq(Position::getSequence, sequence)
                .eq(Position::getName, name)
                .ne(Position::getId, request.getId());
        if (request.getDepartmentId() == null) {
            existWrapper.isNull(Position::getDepartmentId);
        } else {
            existWrapper.eq(Position::getDepartmentId, request.getDepartmentId());
        }
        long existCount = this.count(existWrapper);
        ThrowUtils.throwIf(existCount > 0, ErrorCode.OPERATION_ERROR, "已存在相同职位");

        position.setName(name);
        position.setSequence(sequence);
        position.setDepartmentId(request.getDepartmentId());
        position.setLevelMin(levelMin);
        position.setLevelMax(levelMax);
        position.setDefaultProbationMonths(request.getDefaultProbationMonths());
        position.setDescription(request.getDescription());

        boolean updated = this.updateById(position);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "职位更新失败");

        log.info("职位更新成功: id={}, name={}", position.getId(), request.getName());
    }

    @Override
    public void deletePosition(Long id) {
        Position position = this.getById(id);
        ThrowUtils.throwIf(position == null, ErrorCode.NOT_FOUND_ERROR, "职位不存在");

        // 校验无员工引用该职位
        long employeeCount = employeeMapper.countActiveByPositionId(id);
        ThrowUtils.throwIf(employeeCount > 0,
                ErrorCode.OPERATION_ERROR, "该职位被 " + employeeCount + " 名在职员工引用，无法删除");

        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "职位删除失败");

        log.info("职位删除成功: id={}, name={}", id, position.getName());
    }

    @Override
    public List<SequenceLevelVO> getSequences() {
        List<SequenceLevelVO> result = new ArrayList<>();
        for (PositionSequence seq : PositionSequence.values()) {
            SequenceLevelVO vo = new SequenceLevelVO();
            vo.setSequence(seq.getCode());
            vo.setSequenceName(seq.getName());
            vo.setSequenceCode(seq.getPrefix());
            vo.setLevels(Arrays.asList(seq.getLevels()));
            result.add(vo);
        }
        return result;
    }

    // ==================== 私有辅助方法 ====================

    private PositionVO convertToVO(Position position, Map<Long, String> deptNameMap) {
        PositionVO vo = new PositionVO();
        vo.setId(position.getId());
        vo.setName(position.getName());
        vo.setSequence(position.getSequence());

        PositionSequence seq = PositionSequence.fromCode(position.getSequence());
        vo.setSequenceName(seq != null ? seq.getName() : "未知");

        vo.setDepartmentId(position.getDepartmentId());
        vo.setDepartmentName(position.getDepartmentId() != null
                ? deptNameMap.getOrDefault(position.getDepartmentId(), "未知部门")
                : "全公司通用");

        vo.setLevelMin(position.getLevelMin());
        vo.setLevelMax(position.getLevelMax());
        vo.setLevelRange(position.getLevelMin() + "-" + position.getLevelMax());
        vo.setDefaultProbationMonths(position.getDefaultProbationMonths());
        vo.setDescription(position.getDescription());
        vo.setCreateTime(position.getCreateTime());

        return vo;
    }

    private Map<Long, String> getDeptNameMap(Set<Long> deptIds) {
        if (deptIds.isEmpty()) return Collections.emptyMap();
        try {
            List<Department> depts = departmentMapper.selectBatchIds(deptIds);
            return depts.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
        } catch (Exception e) {
            log.warn("查询部门名称失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
