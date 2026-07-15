package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.model.dto.department.DepartmentCreateRequest;
import com.limou.hrms.model.dto.department.DepartmentQueryRequest;
import com.limou.hrms.model.dto.department.DepartmentUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.DeptEmployeeCount;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.UserRoleEnum;
import com.limou.hrms.model.vo.DepartmentTreeNode;
import com.limou.hrms.model.vo.DepartmentVO;
import com.limou.hrms.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    private final EmployeeMapper employeeMapper;

    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;

    private static final int MAX_DEPTH = 5;

    // ==================== 创建部门 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Department createDepartment(DepartmentCreateRequest dto, User loginUser) {
        checkNameUnique(dto.getName(), null);
        checkCodeUnique(dto.getCode(), null);
        checkSortOrderUnique(dto.getParentId(), dto.getSortOrder(), null);
        validateParentAndDepth(dto.getParentId());
        validateManagerActive(dto.getManagerId());

        Department department = new Department();
        BeanUtils.copyProperties(dto, department);
        boolean saved = this.save(department);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建部门失败");
        }
        log.info("用户 {} 创建了部门: {}", loginUser.getId(), department.getName());
        return department;
    }

    // ==================== 更新部门 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Department updateDepartment(Long id, DepartmentUpdateRequest dto, User loginUser) {
        Department existDept = getUndeletedDeptOrThrow(id);

        if (StringUtils.hasText(dto.getName())) {
            checkNameUnique(dto.getName(), id);
        }
        if (StringUtils.hasText(dto.getCode())) {
            checkCodeUnique(dto.getCode(), id);
        }
        if (dto.getParentId() != null) {
            checkNotCircularRef(id, dto.getParentId());
            int level = calculateLevel(dto.getParentId());
            if (level >= MAX_DEPTH) {
                throw new BusinessException(ErrorCode.DEPARTMENT_MAX_DEPTH_EXCEEDED);
            }
        }
        if (dto.getSortOrder() != null) {
            Long effectiveParentId = dto.getParentId() != null ? dto.getParentId() : existDept.getParentId();
            checkSortOrderUnique(effectiveParentId, dto.getSortOrder(), id);
        }
        if (dto.getManagerId() != null) {
            validateManagerActive(dto.getManagerId());
        }

        BeanUtils.copyProperties(dto, existDept, "id", "createTime", "updateTime", "isDeleted");
        existDept.setParentId(dto.getParentId());
        existDept.setManagerId(dto.getManagerId());
        existDept.setDescription(dto.getDescription());

        boolean updated = this.updateById(existDept);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新部门失败");
        }
        log.info("用户 {} 更新了部门: {}", loginUser.getId(), existDept.getName());
        return existDept;
    }

    // ==================== 删除部门 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id, User loginUser) {
        getUndeletedDeptOrThrow(id);

        // ① 检查是否有子部门
        List<Department> children = this.lambdaQuery()
                .eq(Department::getParentId, id).list();
        if (!children.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("childDepartments", children.stream()
                    .map(d -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", d.getId());
                        m.put("name", d.getName());
                        return m;
                    }).collect(Collectors.toList()));
            throw new BusinessException(ErrorCode.DEPARTMENT_HAS_CHILDREN.getCode(),
                    ErrorCode.DEPARTMENT_HAS_CHILDREN.getMessage());
        }

        // ② 检查是否有在职员工
        List<EmployeeWorkInfo> workInfos = employeeWorkInfoMapper.selectList(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .eq(EmployeeWorkInfo::getDepartmentId, id));
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
                throw new BusinessException(ErrorCode.DEPARTMENT_HAS_EMPLOYEES.getCode(),
                        ErrorCode.DEPARTMENT_HAS_EMPLOYEES.getMessage());
            }
        }

        // ③ 逻辑删除
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除部门失败");
        }
        log.info("用户 {} 删除了部门 ID: {}", loginUser.getId(), id);
    }

    // ==================== 部门树 ====================

    @Override
    public List<DepartmentTreeNode> getDepartmentTree(User loginUser) {
        List<Department> allDepts = this.lambdaQuery()
                .orderByAsc(Department::getSortOrder).list();//查询出来所有的部门集合

        if (allDepts.isEmpty()) {
            return Collections.emptyList();//如果没有部门，则返回空集合
        }

        // 批量统计在职员工数
        List<DeptEmployeeCount> countResults =
                employeeMapper.countActiveByDept(EmployeeStatus.getActiveValues());

        Map<Long, Integer> directCountMap = countResults.stream()
                .collect(Collectors.toMap(
                        DeptEmployeeCount::getDepartmentId,
                        DeptEmployeeCount::getCnt
                ));

        // 数据权限过滤
        Set<Long> allowedDeptIds = resolveDataScope(loginUser, allDepts);
        if (allowedDeptIds != null) {
            Set<Long> withAncestors = new HashSet<>(allowedDeptIds);
            for (Department dept : allDepts) {
                if (allowedDeptIds.contains(dept.getId())) {
                    addAncestors(dept, allDepts, withAncestors);
                }
            }
            allDepts = allDepts.stream()
                    .filter(d -> withAncestors.contains(d.getId()))
                    .collect(Collectors.toList());
        }

        // 构建父子关系 Map
        Map<Long, List<Department>> childrenMap = allDepts.stream()
                .collect(Collectors.groupingBy(d ->
                        d.getParentId() == null ? 0L : d.getParentId()));

        // dept_head 以本人管理的部门为根构建子树
        if (allowedDeptIds != null && loginUser != null) {
            UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
            if (role == UserRoleEnum.DEPT_HEAD) {
                Employee employee = employeeMapper.selectByUserId(loginUser.getId());
                if (employee != null) {
                    Department rootDept = allDepts.stream()
                            .filter(d -> employee.getId().equals(d.getManagerId()))
                            .findFirst().orElse(null);
                    if (rootDept != null) {
                        List<DepartmentTreeNode> result = new ArrayList<>();
                        result.add(buildNode(rootDept, childrenMap, directCountMap));
                        return result;
                    }
                }
                return Collections.emptyList();
            }
        }

        // 从根部门出发递归组装
        return childrenMap.getOrDefault(0L, Collections.emptyList()).stream()
                .map(dept -> buildNode(dept, childrenMap, directCountMap))
                .collect(Collectors.toList());
    }

    // ==================== 部门列表（平铺） ====================

    @Override
    public Page<DepartmentVO> getDepartmentList(DepartmentQueryRequest queryReq, User loginUser) {
        checkReadPermission(loginUser);

        LambdaQueryWrapper<Department> query = Wrappers.<Department>lambdaQuery();
        if (StringUtils.hasText(queryReq.getKeyword())) {
            query.like(Department::getName, queryReq.getKeyword());
        }
        if (queryReq.getParentId() != null) {
            query.eq(Department::getParentId, queryReq.getParentId());
        }
        query.orderByAsc(Department::getSortOrder);

        Page<Department> page = this.page(
                new Page<>(queryReq.getCurrent(), queryReq.getPageSize()), query);

        // 部门主管只返回管辖范围内的部门
        UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (role == UserRoleEnum.DEPT_HEAD) {
            List<Department> allDepts = this.lambdaQuery().list();
            Set<Long> allowedIds = resolveDataScope(loginUser, allDepts);
            if (allowedIds == null || allowedIds.isEmpty()) {
                return new Page<>(queryReq.getCurrent(), queryReq.getPageSize(), 0);
            }
            List<Department> filtered = page.getRecords().stream()
                    .filter(d -> allowedIds.contains(d.getId()))
                    .collect(Collectors.toList());
            page.setRecords(filtered);
            page.setTotal(filtered.size());
        }

        // 批量查人数
        List<DeptEmployeeCount> countResults =
                employeeMapper.countActiveByDept(EmployeeStatus.getActiveValues());

        Map<Long, Integer> countMap = countResults.stream()
                .collect(Collectors.toMap(DeptEmployeeCount::getDepartmentId, DeptEmployeeCount::getCnt));

        // 全量部门（用于查 parentName、level、childCount）
        List<Department> allDepts = this.lambdaQuery().list();
        Map<Long, Department> deptMap = allDepts.stream()
                .collect(Collectors.toMap(Department::getId, d -> d));

        // 统计子部门数
        Map<Long, Long> childCountMap = allDepts.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(Department::getParentId, Collectors.counting()));

        List<DepartmentVO> voList = page.getRecords().stream().map(dept -> {
            DepartmentVO vo = new DepartmentVO();
            BeanUtils.copyProperties(dept, vo);
            if (dept.getParentId() != null && deptMap.containsKey(dept.getParentId())) {
                vo.setParentName(deptMap.get(dept.getParentId()).getName());
            }
            vo.setLevel(calculateLevelFromMap(dept, deptMap));
            vo.setEmployeeCount(countMap.getOrDefault(dept.getId(), 0));
            vo.setChildCount(childCountMap.getOrDefault(dept.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());

        Page<DepartmentVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    // ==================== 部门详情 ====================

    @Override
    public DepartmentVO getDepartmentDetail(Long id, User loginUser) {
        checkReadPermission(loginUser);
        Department dept = getUndeletedDeptOrThrow(id);

        // 部门主管只能查看自己管辖范围内的部门
        UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (role == UserRoleEnum.DEPT_HEAD) {
            List<Department> allDepts = this.lambdaQuery().list();
            Set<Long> allowedIds = resolveDataScope(loginUser, allDepts);
            if (allowedIds == null || !allowedIds.contains(id)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        List<Department> allDepts = this.lambdaQuery()
                .orderByAsc(Department::getSortOrder).list();
        Map<Long, Department> deptMap = allDepts.stream()
                .collect(Collectors.toMap(Department::getId, d -> d));

        // 批量查人数
        List<DeptEmployeeCount> countResults =
                employeeMapper.countActiveByDept(EmployeeStatus.getActiveValues());
        Map<Long, Integer> countMap = new HashMap<>();
        for (DeptEmployeeCount r : countResults) {
            countMap.put(r.getDepartmentId(), r.getCnt());
        }

        DepartmentVO vo = toDepartmentVO(dept, deptMap, countMap);

        // 直接子部门简要信息
        List<DepartmentVO> children = allDepts.stream()
                .filter(d -> id.equals(d.getParentId()))
                .map(d -> toDepartmentVO(d, deptMap, countMap))
                .collect(Collectors.toList());
        vo.setChildren(children);

        return vo;
    }

    // ==================== 私有校验方法 ====================

    private void checkReadPermission(User loginUser) {
        UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (role != UserRoleEnum.ADMIN && role != UserRoleEnum.HR && role != UserRoleEnum.DEPT_HEAD) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    private Department getUndeletedDeptOrThrow(Long id) {
        Department dept = this.lambdaQuery().eq(Department::getId, id).one();
        if (dept == null) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        return dept;
    }

    private void checkNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<Department> query = Wrappers.<Department>lambdaQuery().eq(Department::getName, name);
        if (excludeId != null) {
            query.ne(Department::getId, excludeId);
        }
        if (this.count(query) > 0) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NAME_DUPLICATE);
        }
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<Department> query = Wrappers.<Department>lambdaQuery().eq(Department::getCode, code);
        if (excludeId != null) {
            query.ne(Department::getId, excludeId);
        }
        if (this.count(query) > 0) {
            throw new BusinessException(ErrorCode.DEPARTMENT_CODE_DUPLICATE);
        }
    }

    private void checkSortOrderUnique(Long parentId, Integer sortOrder, Long excludeId) {
        LambdaQueryWrapper<Department> query = Wrappers.<Department>lambdaQuery().eq(Department::getSortOrder, sortOrder);
        if (parentId != null) {
            query.eq(Department::getParentId, parentId);
        } else {
            query.isNull(Department::getParentId);
        }
        if (excludeId != null) {
            query.ne(Department::getId, excludeId);
        }
        if (this.count(query) > 0) {
            throw new BusinessException(ErrorCode.DEPARTMENT_SORT_ORDER_DUPLICATE);
        }
    }

    private int validateParentAndDepth(Long parentId) {
        if (parentId == null) {
            return 1;
        }
        getUndeletedDeptOrThrow(parentId);
        int level = calculateLevel(parentId);
        if (level >= MAX_DEPTH) {
            throw new BusinessException(ErrorCode.DEPARTMENT_MAX_DEPTH_EXCEEDED);
        }
        return level + 1;
    }

    private int calculateLevel(Long parentId) {
        if (parentId == null) return 0;
        int level = 1;
        Long currentId = parentId;
        while (currentId != null) {
            Department parent = this.lambdaQuery()
                    .eq(Department::getId, currentId).one();
            if (parent == null || parent.getParentId() == null) {
                break;
            }
            level++;
            if (level > MAX_DEPTH) break;
            currentId = parent.getParentId();
        }
        return level;
    }

    private void validateManagerActive(Long managerId) {
        if (managerId == null) return;
        long count = employeeMapper.selectCount(
                Wrappers.<Employee>lambdaQuery()
                        .eq(Employee::getId, managerId)
                        .in(Employee::getStatus, EmployeeStatus.getActiveValues()));
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门负责人不存在或非在职状态");
        }
    }

    private void checkNotCircularRef(Long deptId, Long newParentId) {
        if (deptId.equals(newParentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_CIRCULAR_REF);
        }
        Set<Long> descendants = collectDescendantIds(deptId);
        if (descendants.contains(newParentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_CIRCULAR_REF);
        }
    }

    private Set<Long> collectDescendantIds(Long deptId) {
        Set<Long> result = new HashSet<>();
        List<Department> children = this.lambdaQuery()
                .eq(Department::getParentId, deptId).list();
        for (Department child : children) {
            result.add(child.getId());
            result.addAll(collectDescendantIds(child.getId()));
        }
        return result;
    }

    // ==================== 树构建辅助 ====================

    private DepartmentTreeNode buildNode(Department dept,
                                         Map<Long, List<Department>> childrenMap,
                                         Map<Long, Integer> directCountMap) {
        DepartmentTreeNode node = new DepartmentTreeNode();
        BeanUtils.copyProperties(dept, node);
        node.setChildren(new ArrayList<>());

        List<DepartmentTreeNode> children = childrenMap
                .getOrDefault(dept.getId(), Collections.emptyList()).stream()
                .map(child -> buildNode(child, childrenMap, directCountMap))
                .collect(Collectors.toList());
        node.setChildren(children);

        int directCount = directCountMap.getOrDefault(dept.getId(), 0);
        int childrenCount = children.stream()
                .mapToInt(DepartmentTreeNode::getEmployeeCount).sum();
        node.setEmployeeCount(directCount + childrenCount);

        return node;
    }

    // ==================== 数据权限辅助 ====================

    private Set<Long> resolveDataScope(User loginUser, List<Department> allDepts) {
        if (loginUser == null) return Collections.emptySet();

        UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (role == null) return Collections.emptySet();

        // admin / hr → 全量
        if (role == UserRoleEnum.ADMIN || role == UserRoleEnum.HR) {
            return null;
        }
        // dept_head → 本人管理的部门及所有子部门
        if (role == UserRoleEnum.DEPT_HEAD) {
            Employee employee = employeeMapper.selectByUserId(loginUser.getId());
            if (employee == null) return Collections.emptySet();
            Set<Long> managedDeptIds = allDepts.stream()
                    .filter(d -> employee.getId().equals(d.getManagerId()))
                    .map(Department::getId)
                    .collect(Collectors.toSet());
            if (managedDeptIds.isEmpty()) return Collections.emptySet();
            // 包含所有子部门
            Set<Long> expanded = new HashSet<>(managedDeptIds);
            for (Long deptId : managedDeptIds) {
                expanded.addAll(collectDescendantIds(deptId));
            }
            return expanded;
        }
        return Collections.emptySet();
    }

    private void addAncestors(Department dept, List<Department> allDepts, Set<Long> result) {
        Long parentId = dept.getParentId();
        while (parentId != null) {
            result.add(parentId);
            Long finalPid = parentId;
            Department parent = allDepts.stream()
                    .filter(d -> d.getId().equals(finalPid)).findFirst().orElse(null);
            parentId = parent != null ? parent.getParentId() : null;
        }
    }

    // ==================== VO 转换辅助 ====================

    private DepartmentVO toDepartmentVO(Department dept, Map<Long, Department> deptMap,
                                        Map<Long, Integer> countMap) {
        DepartmentVO vo = new DepartmentVO();
        BeanUtils.copyProperties(dept, vo);
        if (dept.getParentId() != null && deptMap.containsKey(dept.getParentId())) {
            vo.setParentName(deptMap.get(dept.getParentId()).getName());
        }
        vo.setLevel(calculateLevelFromMap(dept, deptMap));
        vo.setEmployeeCount(countMap.getOrDefault(dept.getId(), 0));
        return vo;
    }

    private int calculateLevelFromMap(Department dept, Map<Long, Department> deptMap) {
        int level = 1;
        Long currentId = dept.getParentId();
        while (currentId != null) {
            Department parent = deptMap.get(currentId);
            if (parent == null) break;
            level++;
            currentId = parent.getParentId();
        }
        return level;
    }
}
