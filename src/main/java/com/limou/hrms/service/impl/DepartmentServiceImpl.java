package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.EmployeePersonalInfoMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.dto.department.DepartmentCreateRequest;
import com.limou.hrms.model.dto.department.DepartmentUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.DeptEmployeeCount;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.EmployeePersonalInfo;
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
import org.springframework.aop.framework.AopContext;
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

    private final EmployeePersonalInfoMapper employeePersonalInfoMapper;

    private final UserMapper userMapper;

    private final DataScopeContext dataScopeContext;

    private static final int MAX_DEPTH = 4;

    // ==================== 创建部门 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Department createDepartment(DepartmentCreateRequest dto, User loginUser) {
        checkNameUnique(dto.getName(), dto.getParentId(), null);//判断是否存在该部门名称
        checkCodeUnique(dto.getCode(), null);//判断是否存在该部门编码
        checkSortOrderUnique(dto.getParentId(), dto.getSortOrder(), null);//判断是否存在该部门序号
        Department parent = validateParentAndDepth(dto.getParentId());//判断层级是否过深
        validateManagerActive(dto.getManagerId());//判断是否能够成为部门经理

        Department department = new Department();
        BeanUtils.copyProperties(dto, department);
        department.setLevel(parent.getLevel() + 1);
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

        // 父部门变更（用于后续同名/同序号校验）
        Long effectiveParentId = existDept.getParentId();
        if (dto.getParentId() != null && !dto.getParentId().equals(existDept.getParentId())) {//判断是否变更了父部门
            checkNotCircularRef(id, dto.getParentId());
            Department newParent = getUndeletedDeptOrThrow(dto.getParentId());
            effectiveParentId = dto.getParentId();

            // 最深子节点移动后不能超过最大层级
            int newRootLevel = newParent.getLevel() + 1;
            int levelDiff = newRootLevel - existDept.getLevel();
            int maxDescLevel = getMaxDescendantLevel(id);
            if (maxDescLevel + levelDiff > MAX_DEPTH) {
                throw new BusinessException(ErrorCode.DEPARTMENT_MAX_DEPTH_EXCEEDED);
            }

            // 级联更新当前部门及所有子部门的 level
            if (levelDiff != 0) {
                updateDescendantLevels(id, levelDiff);
            }
            existDept.setLevel(newRootLevel);
        }
        if (StringUtils.hasText(dto.getName())) {//判断部门名称是否重复
            checkNameUnique(dto.getName(), effectiveParentId, id);
        }
        if (StringUtils.hasText(dto.getCode())) {
            checkCodeUnique(dto.getCode(), id);
        }
        if (dto.getSortOrder() != null) {
            effectiveParentId = dto.getParentId() != null ? dto.getParentId() : existDept.getParentId();
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
        long childCount = this.lambdaQuery()
                .eq(Department::getParentId, id).count();
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.DEPARTMENT_HAS_CHILDREN);
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
                throw new BusinessException(ErrorCode.DEPARTMENT_HAS_EMPLOYEES,
                        "该部门下存在 " + activeCount + " 名在职员工，无法删除");
            }
        }

        // ③ 逻辑删除
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除部门失败");
        }
        log.info("用户 {} 删除了部门 ID: {}", loginUser.getId(), id);
    }

    // ==================== 部门查询（数据权限） ====================

    /**
     * HR/admin 全量查询所有部门（按层级+排序号保证父节点在子节点前）
     */
    private List<Department> queryAllDepartments() {
        return this.lambdaQuery()
                .orderByAsc(Department::getLevel, Department::getSortOrder).list();
    }

    /**
     * dept_head 管辖部门查询（SQL IN 过滤）
     */
    private List<Department> queryManagedDepartments() {
        Set<Long> allowedIds = dataScopeContext.getManagedDepartmentIds();
        if (allowedIds == null || allowedIds.isEmpty()) {
            return Collections.emptyList();
        }
        return this.lambdaQuery()
                .in(Department::getId, allowedIds)
                .orderByAsc(Department::getLevel, Department::getSortOrder).list();
    }

    // ==================== 部门树 ====================

    // ==================== 部门查询 ====================

    @Override
    public List<DepartmentTreeNode> queryDepartments(String keyword) {
        DataScopeEnum scope = dataScopeContext.canManageOrganization();
        if (scope == DataScopeEnum.NONE) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Department> query = Wrappers.lambdaQuery();

        // keyword 模糊匹配 name 或 code
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(Department::getName, keyword)
                    .or().like(Department::getCode, keyword));
        }

        // dept_head：只查管辖范围
        if (scope == DataScopeEnum.DEPT) {
            Set<Long> allowedIds = dataScopeContext.getManagedDepartmentIds();
            if (allowedIds == null || allowedIds.isEmpty()) {
                return Collections.emptyList();
            }
            query.in(Department::getId, allowedIds);
        }

        query.orderByAsc(Department::getLevel, Department::getSortOrder);
        List<Department> depts = this.list(query);

        if (depts.isEmpty()) {
            return Collections.emptyList();
        }

        // 员工数聚合 + 负责人姓名
        Map<Long, Integer> directCountMap = loadDirectCountMap();
        Map<Long, Integer> totalCountMap = aggregateEmployeeCount(depts, directCountMap);
        Map<Long, String> managerNameMap = loadManagerNameMap(depts);

        return depts.stream().map(d -> {
            DepartmentTreeNode node = new DepartmentTreeNode();
            node.setId(d.getId());
            node.setName(d.getName());
            node.setCode(d.getCode());
            node.setParentId(d.getParentId());
            node.setManagerId(d.getManagerId());
            node.setManagerName(managerNameMap.get(d.getManagerId()));
            node.setSortOrder(d.getSortOrder());
            node.setLevel(d.getLevel());
            node.setDescription(d.getDescription());
            node.setEmployeeCount(totalCountMap.getOrDefault(d.getId(), 0));
            return node;
        }).collect(Collectors.toList());
    }

    /**
     * 按 level 降序自底向上聚合员工数（叶子 → 根）
     */
    private Map<Long, Integer> aggregateEmployeeCount(List<Department> depts,
                                                       Map<Long, Integer> directCountMap) {
        List<Department> levelDesc = new ArrayList<>(depts);
        levelDesc.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));//降序排序

        Map<Long, Integer> totalMap = new HashMap<>();
        for (Department d : levelDesc) {
            int direct = directCountMap.getOrDefault(d.getId(), 0);
            int childrenSum = totalMap.getOrDefault(d.getId(), 0);
            int myTotal = direct + childrenSum;
            totalMap.put(d.getId(), myTotal);

            if (d.getParentId() != null) {
                totalMap.merge(d.getParentId(), myTotal, Integer::sum);
            }
        }
        return totalMap;
    }

    // ==================== 部门详情 ====================

    @Override
    public DepartmentVO getDepartmentDetail(Long id) {
        DataScopeEnum scope = dataScopeContext.canManageOrganization();
        if (scope == DataScopeEnum.NONE) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (scope == DataScopeEnum.DEPT) {
            //部门管理员逻辑
            return getDeptDetailForDeptHead(id);
        }
        //HR管理员逻辑
        return getDeptDetailForAdminHr(id);
    }

    /**
     * HR/admin：直接查详情
     */
    private DepartmentVO getDeptDetailForAdminHr(Long id) {
        Department dept = getUndeletedDeptOrThrow(id);
        List<Department> allDepts = this.lambdaQuery().list();

        //部门id->部门
        Map<Long, Department> deptMap = allDepts.stream()
                .collect(Collectors.toMap(Department::getId, d -> d));
        //查询每个部门的人数
        Map<Long, Integer> countMap = loadCountMap();
        //查询负责人姓名
        Map<Long, String> managerNameMap = loadManagerNameMap(allDepts);

        DepartmentVO vo = toDepartmentVO(dept, deptMap, countMap, managerNameMap);
        List<DepartmentVO> children = allDepts.stream()
                .filter(d -> id.equals(d.getParentId()))
                .map(d -> toDepartmentVO(d, deptMap, countMap, managerNameMap))
                .collect(Collectors.toList());
        vo.setChildren(children);
        return vo;
    }

    /**
     * 部门管理员：校验管辖范围后查详情
     */
    private DepartmentVO getDeptDetailForDeptHead(Long id) {
        Set<Long> allowedIds = dataScopeContext.getManagedDepartmentIds();
        if (allowedIds == null || !allowedIds.contains(id)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        Department dept = getUndeletedDeptOrThrow(id);
        List<Department> allDepts = this.lambdaQuery().list();
        Map<Long, Department> deptMap = allDepts.stream()
                .collect(Collectors.toMap(Department::getId, d -> d));//部门id->部门
        Map<Long, Integer> countMap = loadCountMap();//部门id->人数
        Map<Long, String> managerNameMap = loadManagerNameMap(allDepts);

        DepartmentVO vo = toDepartmentVO(dept, deptMap, countMap, managerNameMap);
        List<DepartmentVO> children = allDepts.stream()
                .filter(d -> id.equals(d.getParentId()))
                .map(d -> toDepartmentVO(d, deptMap, countMap, managerNameMap))
                .collect(Collectors.toList());
        vo.setChildren(children);
        return vo;
    }

    // ==================== 私有校验方法 ====================

    private Department getUndeletedDeptOrThrow(Long id) {
        Department dept = this.lambdaQuery().eq(Department::getId, id).one();
        if (dept == null) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        return dept;
    }

    private void checkNameUnique(String name, Long parentId, Long excludeId) {
        LambdaQueryWrapper<Department> query = Wrappers.<Department>lambdaQuery()
                .eq(Department::getName, name)
                .eq(Department::getParentId, parentId);
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

    private Department validateParentAndDepth(Long parentId) {
        if (parentId == null) {
            throw new BusinessException(ErrorCode.DEPARTMENT_PARENT_REQUIRED);
        }
        Department parent = getUndeletedDeptOrThrow(parentId);
        if (parent.getLevel() >= MAX_DEPTH) {
            throw new BusinessException(ErrorCode.DEPARTMENT_MAX_DEPTH_EXCEEDED);
        }
        return parent;
    }

    private void validateManagerActive(Long managerId) {
        if (managerId == null) return;
        Employee employee = employeeMapper.selectOne(
                Wrappers.<Employee>lambdaQuery()
                        .eq(Employee::getId, managerId)
                        .in(Employee::getStatus, EmployeeStatus.getActiveValues()));
        if (employee == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门负责人不存在或非在职状态");
        }
        // 校验角色：只有普通员工和部门负责人可以担任部门负责人
        User managerUser = userMapper.selectById(employee.getUserId());
        if (managerUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门负责人关联的账号不存在");
        }
        UserRoleEnum role = UserRoleEnum.getEnumByValue(managerUser.getUserRole());
        if (role != UserRoleEnum.USER && role != UserRoleEnum.DEPT_HEAD) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只有普通员工或部门负责人可以担任部门负责人，HR、管理员、财务专员不可以");
        }
        // 普通用户自动升级为部门负责人
        if (role == UserRoleEnum.USER) {
            managerUser.setUserRole(UserRoleEnum.DEPT_HEAD.getValue());
            userMapper.updateById(managerUser);
        }
    }

    private void checkNotCircularRef(Long deptId, Long newParentId) {
        if (deptId.equals(newParentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_CIRCULAR_REF);
        }
        Set<Long> descendants = collectDescendantIds(deptId);//递归获取所有子部门及子孙部门id
        if (descendants.contains(newParentId)) {//如果将新的父部门设置为当前部门的子部门或子孙部门，则无法完成
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

    /**
     * 获取当前部门及所有子孙部门中的最大 level
     */
    private int getMaxDescendantLevel(Long deptId) {
        Set<Long> ids = new HashSet<>(collectDescendantIds(deptId));
        ids.add(deptId);
        return this.lambdaQuery()
                .in(Department::getId, ids)
                .list().stream()
                .mapToInt(Department::getLevel)
                .max().orElse(0);
    }

    /**
     * 批量更新当前部门及所有子孙部门的 level（平移时使用）
     */
    private void updateDescendantLevels(Long deptId, int levelDiff) {
        Set<Long> ids = new HashSet<>(collectDescendantIds(deptId));
        ids.add(deptId);
        List<Department> subtree = this.lambdaQuery()
                .in(Department::getId, ids).list();
        for (Department d : subtree) {
            d.setLevel(d.getLevel() + levelDiff);
        }
        ((DepartmentService) AopContext.currentProxy()).updateBatchById(subtree);//解决事务失效
    }

    // ==================== 数据加载辅助 ====================

    /**
     * 批量加载各部门直属在职员工数
     */
    private Map<Long, Integer> loadDirectCountMap() {
        List<DeptEmployeeCount> countResults =
                employeeMapper.countActiveByDept(EmployeeStatus.getActiveValues());
        return countResults.stream()
                .collect(Collectors.toMap(
                        DeptEmployeeCount::getDepartmentId,
                        DeptEmployeeCount::getCnt
                ));
    }

    /**
     * 批量加载各部门在职员工数（HashMap，用于详情/列表）
     */
    private Map<Long, Integer> loadCountMap() {
        List<DeptEmployeeCount> countResults =
                employeeMapper.countActiveByDept(EmployeeStatus.getActiveValues());
        Map<Long, Integer> countMap = new HashMap<>();
        for (DeptEmployeeCount r : countResults) {
            countMap.put(r.getDepartmentId(), r.getCnt());
        }
        return countMap;
    }

    /**
     * 批量加载部门负责人姓名（employeeId → name）
     */
    private Map<Long, String> loadManagerNameMap(List<Department> depts) {
        Set<Long> managerIds = depts.stream()
                .map(Department::getManagerId)
                .filter(Objects::nonNull)// 过滤 null
                .collect(Collectors.toSet());
        if (managerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return employeePersonalInfoMapper.selectList(
                        Wrappers.<EmployeePersonalInfo>lambdaQuery()
                                .in(EmployeePersonalInfo::getEmployeeId, managerIds))
                .stream()
                .collect(Collectors.toMap(
                        EmployeePersonalInfo::getEmployeeId,
                        EmployeePersonalInfo::getName));
    }

    // ==================== VO 转换辅助 ====================

    private DepartmentVO toDepartmentVO(Department dept, Map<Long, Department> deptMap,
                                        Map<Long, Integer> countMap,
                                        Map<Long, String> managerNameMap) {
        DepartmentVO vo = new DepartmentVO();
        BeanUtils.copyProperties(dept, vo);
        if (dept.getParentId() != null && deptMap.containsKey(dept.getParentId())) {
            vo.setParentName(deptMap.get(dept.getParentId()).getName());
        }
        vo.setManagerName(managerNameMap.get(dept.getManagerId()));
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
