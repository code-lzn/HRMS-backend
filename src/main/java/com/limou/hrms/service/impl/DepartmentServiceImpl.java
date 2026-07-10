package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.OrgConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.model.dto.department.DepartmentAddRequest;
import com.limou.hrms.model.dto.department.DepartmentMergeRequest;
import com.limou.hrms.model.dto.department.DepartmentUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.DepartmentMergeLog;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.vo.DepartmentMergeResultVO;
import com.limou.hrms.model.vo.DepartmentTreeVO;
import com.limou.hrms.service.DepartmentMergeLogService;
import com.limou.hrms.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 */
@Slf4j
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private DepartmentMergeLogService departmentMergeLogService;

    @Override
    public List<DepartmentTreeVO> getDepartmentTree() {
        // 一次性查询所有未删除部门
        List<Department> allDepts = this.list(
                new LambdaQueryWrapper<Department>()
                        .orderByAsc(Department::getParentId)
                        .orderByAsc(Department::getSortOrder)
        );

        if (allDepts.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有部门ID，用于批量查询员工数
        Map<Long, Long> deptEmployeeCountMap = buildEmployeeCountMap(allDepts);

        // 收集所有负责人ID，批量查询负责人姓名
        Set<Long> managerIds = allDepts.stream()
                .map(Department::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> managerNameMap = getManagerNameMap(managerIds);

        // 按 parentId 分组
        Map<Long, List<Department>> parentIdMap = allDepts.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getParentId() == null ? 0L : d.getParentId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 构建树，根部门的 parentId 为 null
        List<Department> roots = parentIdMap.getOrDefault(0L, Collections.emptyList());
        return roots.stream()
                .map(dept -> buildTreeVO(dept, parentIdMap, deptEmployeeCountMap, managerNameMap))
                .collect(Collectors.toList());
    }

    @Override
    public Long addDepartment(DepartmentAddRequest request) {
        String code = request.getCode().trim();
        String name = request.getName().trim();
        Long parentId = request.getParentId();

        // 1. 校验编码唯一（含已删除记录，编码删除后不可回收复用）
        long codeCount = this.baseMapper.countByCodeIgnoreDelete(code);
        ThrowUtils.throwIf(codeCount > 0, ErrorCode.OPERATION_ERROR, "部门编码 " + code + " 已存在（含已删除部门，编码不可复用）");

        // 2. 校验层级深度
        if (parentId != null) {
            int parentDepth = calculateDepth(parentId);
            ThrowUtils.throwIf(parentDepth + 1 > OrgConstant.MAX_DEPT_DEPTH,
                    ErrorCode.OPERATION_ERROR, "已达到最大层级深度 " + OrgConstant.MAX_DEPT_DEPTH + " 级");
        }

        // 3. 校验同级部门名称不重复
        long nameCount = this.count(new LambdaQueryWrapper<Department>()
                .eq(parentId == null, Department::getParentId, parentId)
                .eq(Department::getName, name));
        ThrowUtils.throwIf(nameCount > 0, ErrorCode.OPERATION_ERROR, "同级部门名称 " + name + " 已存在");

        // 4. 校验部门负责人是否存在
        if (request.getManagerId() != null) {
            Employee manager = employeeMapper.selectById(request.getManagerId());
            ThrowUtils.throwIf(manager == null, ErrorCode.NOT_FOUND_ERROR, "部门负责人ID " + request.getManagerId() + " 不存在");
        }

        // 5. 创建部门
        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setParentId(parentId);
        department.setManagerId(request.getManagerId());
        department.setSortOrder(request.getSortOrder());
        department.setDescription(request.getDescription());

        boolean saved = this.save(department);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "部门创建失败");

        log.info("部门创建成功: id={}, name={}, code={}", department.getId(), name, code);
        return department.getId();
    }

    @Override
    public void updateDepartment(DepartmentUpdateRequest request) {
        Department dept = this.getById(request.getId());
        ThrowUtils.throwIf(dept == null, ErrorCode.NOT_FOUND_ERROR, "部门不存在");

        // 不允许修改编码和上级部门（编码创建后锁定，上级部门变更需走合并流程）
        String name = request.getName().trim();

        // 校验同级部门名称不重复（排除自身）
        long nameCount = this.count(new LambdaQueryWrapper<Department>()
                .eq(dept.getParentId() == null, Department::getParentId, dept.getParentId())
                .eq(Department::getName, name)
                .ne(Department::getId, request.getId()));
        ThrowUtils.throwIf(nameCount > 0, ErrorCode.OPERATION_ERROR, "同级部门名称 " + name + " 已存在");

        // 校验部门负责人是否存在
        if (request.getManagerId() != null) {
            Employee manager = employeeMapper.selectById(request.getManagerId());
            ThrowUtils.throwIf(manager == null, ErrorCode.NOT_FOUND_ERROR, "部门负责人ID " + request.getManagerId() + " 不存在");
        }

        dept.setName(name);
        dept.setManagerId(request.getManagerId());
        dept.setSortOrder(request.getSortOrder());
        dept.setDescription(request.getDescription());

        boolean updated = this.updateById(dept);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "部门更新失败");

        log.info("部门更新成功: id={}, name={}", dept.getId(), name);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department dept = this.getById(id);
        ThrowUtils.throwIf(dept == null, ErrorCode.NOT_FOUND_ERROR, "部门不存在");

        // 1. 校验无子部门
        long childCount = this.count(new LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, id));
        ThrowUtils.throwIf(childCount > 0, ErrorCode.OPERATION_ERROR, "存在 " + childCount + " 个子部门，请先删除子部门");

        // 2. 校验无在职员工
        long employeeCount = getEmployeeCount(id);
        ThrowUtils.throwIf(employeeCount > 0, ErrorCode.OPERATION_ERROR, "该部门及子部门还有 " + employeeCount + " 名在职员工，请先转移员工至其他部门");

        // 3. 软删除
        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.OPERATION_ERROR, "部门删除失败");

        log.info("部门删除成功: id={}, name={}", id, dept.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentMergeResultVO mergeDepartments(DepartmentMergeRequest request, Long operatorId) {
        Long sourceDeptId = request.getSourceDeptId();
        Long targetDeptId = request.getTargetDeptId();

        // 1. 校验源部门和目标部门存在
        Department sourceDept = this.getById(sourceDeptId);
        Department targetDept = this.getById(targetDeptId);
        ThrowUtils.throwIf(sourceDept == null, ErrorCode.NOT_FOUND_ERROR, "源部门不存在");
        ThrowUtils.throwIf(targetDept == null, ErrorCode.NOT_FOUND_ERROR, "目标部门不存在");

        // 2. 校验源部门 != 目标部门
        ThrowUtils.throwIf(sourceDeptId.equals(targetDeptId),
                ErrorCode.OPERATION_ERROR, "不能合并到自身");

        // 3. 校验目标部门不是源部门的子部门
        List<Long> childIds = collectChildDeptIds(sourceDeptId);
        ThrowUtils.throwIf(childIds.contains(targetDeptId),
                ErrorCode.OPERATION_ERROR, "不能合并到源部门的子部门");

        // 收集源部门及其所有子部门ID
        Set<Long> sourceDeptIdSet = new HashSet<>(childIds);
        sourceDeptIdSet.add(sourceDeptId);

        // 4. 批量转移员工
        long transferredEmployees = 0;
        try {
            transferredEmployees = employeeMapper.batchUpdateDeptId(sourceDeptIdSet, targetDeptId);
        } catch (Exception e) {
            log.warn("员工转移失败（employee表可能未初始化）: {}", e.getMessage());
        }

        // 5. 批量转移子部门
        List<Department> childDepts = this.list(new LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, sourceDeptId));
        int transferredChildDepts = 0;
        for (Department child : childDepts) {
            child.setParentId(targetDeptId);
            this.updateById(child);
            transferredChildDepts++;
        }

        // 6. 软删除源部门
        this.removeById(sourceDeptId);

        // 7. 记录合并日志
        DepartmentMergeLog logEntity = new DepartmentMergeLog();
        logEntity.setSourceDeptId(sourceDeptId);
        logEntity.setSourceDeptName(sourceDept.getName());
        logEntity.setTargetDeptId(targetDeptId);
        logEntity.setTargetDeptName(targetDept.getName());
        logEntity.setTransferredEmployees((int) transferredEmployees);
        logEntity.setOperatorId(operatorId);
        departmentMergeLogService.save(logEntity);

        log.info("部门合并成功: source={}({}), target={}({}), 转移员工={}, 转移子部门={}",
                sourceDept.getName(), sourceDeptId, targetDept.getName(), targetDeptId,
                transferredEmployees, transferredChildDepts);

        DepartmentMergeResultVO result = new DepartmentMergeResultVO();
        result.setTransferredEmployees((int) transferredEmployees);
        result.setTransferredChildDepts(transferredChildDepts);
        return result;
    }

    @Override
    public long getEmployeeCount(Long deptId) {
        try {
            Set<Long> deptIds = new HashSet<>(collectChildDeptIds(deptId));
            deptIds.add(deptId);
            if (deptIds.isEmpty()) {
                return 0;
            }
            return employeeMapper.countActiveByDeptIds(deptIds);
        } catch (Exception e) {
            log.warn("查询员工数失败（employee表可能未初始化）: {}", e.getMessage());
            return 0;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建部门树节点
     */
    private DepartmentTreeVO buildTreeVO(Department dept,
                                         Map<Long, List<Department>> parentIdMap,
                                         Map<Long, Long> deptEmployeeCountMap,
                                         Map<Long, String> managerNameMap) {
        DepartmentTreeVO vo = new DepartmentTreeVO();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setCode(dept.getCode());
        vo.setParentId(dept.getParentId());
        vo.setManagerId(dept.getManagerId());
        vo.setManagerName(managerNameMap.get(dept.getManagerId()));
        vo.setSortOrder(dept.getSortOrder());
        vo.setDescription(dept.getDescription());
        vo.setEmployeeCount(deptEmployeeCountMap.getOrDefault(dept.getId(), 0L).intValue());

        // 递归构建子节点
        List<Department> children = parentIdMap.getOrDefault(dept.getId(), Collections.emptyList());
        for (Department child : children) {
            vo.getChildren().add(buildTreeVO(child, parentIdMap, deptEmployeeCountMap, managerNameMap));
        }

        return vo;
    }

    /**
     * 计算部门层级深度
     */
    private int calculateDepth(Long deptId) {
        int depth = 0;
        Long currentId = deptId;
        while (currentId != null) {
            depth++;
            Department dept = this.getById(currentId);
            if (dept == null) break;
            currentId = dept.getParentId();
            if (depth > OrgConstant.MAX_DEPT_DEPTH + 1) break; // 防止死循环
        }
        return depth;
    }

    /**
     * 收集部门的所有子孙部门ID
     */
    private List<Long> collectChildDeptIds(Long parentId) {
        List<Long> result = new ArrayList<>();
        List<Department> children = this.list(new LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, parentId));
        for (Department child : children) {
            result.add(child.getId());
            result.addAll(collectChildDeptIds(child.getId()));
        }
        return result;
    }

    /**
     * 构建各部门在职员工数映射
     */
    private Map<Long, Long> buildEmployeeCountMap(List<Department> allDepts) {
        Map<Long, Long> map = new LinkedHashMap<>();
        // 按 parentId 分组用于递归收集子部门
        Map<Long, List<Department>> parentIdMap = allDepts.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getParentId() == null ? 0L : d.getParentId()
                ));

        for (Department dept : allDepts) {
            Set<Long> deptIds = new HashSet<>();
            collectChildIds(dept.getId(), parentIdMap, deptIds);
            deptIds.add(dept.getId());

            try {
                long count = employeeMapper.countActiveByDeptIds(deptIds);
                map.put(dept.getId(), count);
            } catch (Exception e) {
                map.put(dept.getId(), 0L);
            }
        }
        return map;
    }

    private void collectChildIds(Long parentId, Map<Long, List<Department>> parentIdMap, Set<Long> result) {
        List<Department> children = parentIdMap.getOrDefault(parentId, Collections.emptyList());
        for (Department child : children) {
            result.add(child.getId());
            collectChildIds(child.getId(), parentIdMap, result);
        }
    }

    /**
     * 获取负责人姓名映射
     */
    private Map<Long, String> getManagerNameMap(Set<Long> managerIds) {
        Map<Long, String> map = new HashMap<>();
        if (managerIds.isEmpty()) return map;
        try {
            List<Employee> employees = employeeMapper.selectBatchIds(managerIds);
            for (Employee emp : employees) {
                map.put(emp.getId(), emp.getName());
            }
        } catch (Exception e) {
            log.warn("查询负责人姓名失败: {}", e.getMessage());
        }
        return map;
    }
}
