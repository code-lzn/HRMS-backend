package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.model.enums.DataScopeEnum;
import com.limou.hrms.model.enums.OrgEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.RoleMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.dto.department.DepartmentAddRequest;
import com.limou.hrms.model.dto.department.DepartmentMergeRequest;
import com.limou.hrms.model.dto.department.DepartmentUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.DepartmentMergeLog;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.Role;
import com.limou.hrms.model.entity.User;
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
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private DepartmentMergeLogService departmentMergeLogService;

    @Override
    public List<DepartmentTreeVO> getDepartmentTree(Long userId) {
        List<Long> visibleIds = getVisibleDeptIds(userId);
        log.info("数据权限调试: userId={}, dataScope={}, visibleIds={}",
                userId, userId != null ? getDataScopeFromUser(userId) : "null",
                visibleIds != null ? visibleIds.size() + "个部门" : "null(全部)");
        if (visibleIds != null && visibleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Department> allDepts = this.list(
                new LambdaQueryWrapper<Department>()
                        .orderByAsc(Department::getParentId)
                        .orderByAsc(Department::getSortOrder)
        );

        if (allDepts.isEmpty()) {
            return Collections.emptyList();
        }

        // 按数据范围过滤部门
        if (visibleIds != null) {
            allDepts = allDepts.stream()
                    .filter(d -> visibleIds.contains(d.getId()))
                    .collect(Collectors.toList());
            if (allDepts.isEmpty()) {
                return Collections.emptyList();
            }
        }

        Map<Long, Long> deptEmployeeCountMap = buildEmployeeCountMap(allDepts);

        Set<Long> managerIds = allDepts.stream()
                .map(Department::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> managerNameMap = getManagerNameMap(managerIds);

        // 收集所有存在的部门ID
        Set<Long> allDeptIds = allDepts.stream().map(Department::getId).collect(Collectors.toSet());

        // 按 parentId 分组，parentId 为 null 或父部门已删除的统一归为根节点
        Map<Long, List<Department>> parentIdMap = allDepts.stream()
                .collect(Collectors.groupingBy(
                        d -> (d.getParentId() != null && allDeptIds.contains(d.getParentId()))
                                ? d.getParentId() : 0L,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 检测并告警孤儿子部门
        allDepts.stream()
                .filter(d -> d.getParentId() != null && !allDeptIds.contains(d.getParentId()))
                .forEach(d -> log.warn("部门 [{}(id={})] 的上级部门 id={} 不存在或已删除，已移至根层级",
                        d.getDeptName(), d.getId(), d.getParentId()));

        List<Department> roots = parentIdMap.getOrDefault(0L, Collections.emptyList());
        return roots.stream()
                .map(dept -> buildTreeVO(dept, parentIdMap, deptEmployeeCountMap, managerNameMap))
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentTreeVO getDepartmentDetail(Long id, Long userId) {
        // 校验可见性（统一返回"部门不存在"，避免暴露存在但无权查看的信息）
        List<Long> visibleIds = getVisibleDeptIds(userId);
        if (visibleIds != null && !visibleIds.contains(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "部门不存在");
        }

        Department dept = this.getById(id);
        ThrowUtils.throwIf(dept == null, ErrorCode.NOT_FOUND_ERROR, "部门不存在");

        // 查负责人姓名
        Map<Long, String> managerNameMap = Collections.emptyMap();
        if (dept.getManagerId() != null) {
            managerNameMap = getManagerNameMap(new HashSet<>(Collections.singletonList(dept.getManagerId())));
        }

        DepartmentTreeVO vo = new DepartmentTreeVO();
        vo.setId(dept.getId());
        vo.setName(dept.getDeptName());
        vo.setCode(dept.getDeptCode());
        vo.setParentId(dept.getParentId());
        vo.setManagerId(dept.getManagerId());
        vo.setManagerName(managerNameMap.get(dept.getManagerId()));
        vo.setSortOrder(dept.getSortOrder());
        vo.setDescription(dept.getDescription());
        vo.setEmployeeCount((int) getEmployeeCount(dept.getId()));
        vo.setChildren(new ArrayList<>());

        return vo;
    }

    @Override
    public Long addDepartment(DepartmentAddRequest request) {
        String code = request.getCode().trim();
        String name = request.getName().trim();
        Long parentId = request.getParentId();

        // 1. 校验部门名称不为空
        ThrowUtils.throwIf(name.isEmpty(), ErrorCode.PARAMS_ERROR, "部门名称不能为空");

        // 2. 校验编码唯一（含已删除记录，编码删除后不可回收复用）
        long codeCount = this.baseMapper.countByCodeIgnoreDelete(code);
        ThrowUtils.throwIf(codeCount > 0, ErrorCode.OPERATION_ERROR, "部门编码 " + code + " 已存在（含已删除部门，编码不可复用）");

        // 3. 校验上级部门存在且未被删除
        if (parentId != null) {
            Department parentDept = this.getById(parentId);
            ThrowUtils.throwIf(parentDept == null, ErrorCode.NOT_FOUND_ERROR, "上级部门不存在或已被删除");
            int parentDepth = calculateDepth(parentId);
            ThrowUtils.throwIf(parentDepth + 1 > OrgEnum.MAX_DEPT_DEPTH.getCode(),
                    ErrorCode.OPERATION_ERROR, "已达到最大层级深度 " + OrgEnum.MAX_DEPT_DEPTH.getCode() + " 级");
        }

        // 4. 校验同级部门名称不重复
        LambdaQueryWrapper<Department> nameWrapper = new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptName, name);
        if (parentId == null) {
            nameWrapper.isNull(Department::getParentId);
        } else {
            nameWrapper.eq(Department::getParentId, parentId);
        }
        long nameCount = this.count(nameWrapper);
        ThrowUtils.throwIf(nameCount > 0, ErrorCode.OPERATION_ERROR, "同级部门名称 " + name + " 已存在");

        // 4. 校验部门负责人是否存在且角色合规
        if (request.getManagerId() != null) {
            validateManagerRole(request.getManagerId());
        }

        // 5. 创建部门
        Department department = new Department();
        department.setDeptName(name);
        department.setDeptCode(code);
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

        ThrowUtils.throwIf(request.getName() == null, ErrorCode.PARAMS_ERROR, "部门名称不能为空");
        String name = request.getName().trim();

        // 校验部门名称不为空
        ThrowUtils.throwIf(name.isEmpty(), ErrorCode.PARAMS_ERROR, "部门名称不能为空");

        // 校验同级部门名称不重复（排除自身）
        LambdaQueryWrapper<Department> nameWrapper = new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptName, name)
                .ne(Department::getId, request.getId());
        if (dept.getParentId() == null) {
            nameWrapper.isNull(Department::getParentId);
        } else {
            nameWrapper.eq(Department::getParentId, dept.getParentId());
        }
        long nameCount = this.count(nameWrapper);
        ThrowUtils.throwIf(nameCount > 0, ErrorCode.OPERATION_ERROR, "同级部门名称 " + name + " 已存在");

        // 校验部门负责人是否存在且角色合规
        if (request.getManagerId() != null) {
            validateManagerRole(request.getManagerId());
        }

        dept.setDeptName(name);
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

        log.info("部门删除成功: id={}, name={}", id, dept.getDeptName());
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
        long transferredEmployees = employeeMapper.batchUpdateDeptId(sourceDeptIdSet, targetDeptId);

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
        logEntity.setSourceDeptName(sourceDept.getDeptName());
        logEntity.setTargetDeptId(targetDeptId);
        logEntity.setTargetDeptName(targetDept.getDeptName());
        logEntity.setTransferredEmployees((int) transferredEmployees);
        logEntity.setOperatorId(operatorId);
        departmentMergeLogService.save(logEntity);

        log.info("部门合并成功: source={}({}), target={}({}), 转移员工={}, 转移子部门={}",
                sourceDept.getDeptName(), sourceDeptId, targetDept.getDeptName(), targetDeptId,
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
            return employeeMapper.countActiveByDeptIds(deptIds);
        } catch (Exception e) {
            log.warn("查询员工数失败（employee表可能未初始化）: {}", e.getMessage());
            return 0;
        }
    }

    // ==================== 数据权限 ====================

    @Override
    public List<Long> getVisibleDeptIds(Long userId) {
        Integer dataScope = getDataScopeFromUser(userId);

        // SELF(5) 和 SALARY(4)：无权限查看组织架构
        if (dataScope == DataScopeEnum.SELF.getCode() ||
                dataScope == DataScopeEnum.SALARY.getCode()) {
            return Collections.emptyList();
        }

        // ALL(1) 和 ALL_EMPLOYEE(2)：全部可见
        if (dataScope == DataScopeEnum.ALL.getCode() ||
                dataScope == DataScopeEnum.ALL_EMPLOYEE.getCode()) {
            return null;
        }

        // DEPARTMENT(3)：本部门及下属
        Employee emp = employeeMapper.selectOne(
                new LambdaQueryWrapper<Employee>().eq(Employee::getUserId, userId));
        if (emp == null) {
            return Collections.emptyList();
        }

        // 先找该员工管理的部门
        List<Department> managedDepts = this.list(
                new LambdaQueryWrapper<Department>().eq(Department::getManagerId, emp.getId()));

        Set<Long> visibleIds = new HashSet<>();
        if (!managedDepts.isEmpty()) {
            for (Department dept : managedDepts) {
                visibleIds.add(dept.getId());
                visibleIds.addAll(collectChildDeptIds(dept.getId()));
            }
        } else if (emp.getDepartmentId() != null) {
            // 没有管理任何部门，则以其所属部门为根
            visibleIds.add(emp.getDepartmentId());
            visibleIds.addAll(collectChildDeptIds(emp.getDepartmentId()));
        }

        return new ArrayList<>(visibleIds);
    }

    /** 允许担任部门负责人的角色ID：1=系统管理员 2=HR专员 3=部门主管 */
    private static final Set<Long> MANAGER_ROLE_IDS = Set.of(1L, 2L, 3L);

    /**
     * 校验部门负责人：员工存在且其系统角色为系统管理员、HR专员或部门主管
     */
    private void validateManagerRole(Long managerId) {
        Employee manager = employeeMapper.selectById(managerId);
        ThrowUtils.throwIf(manager == null, ErrorCode.NOT_FOUND_ERROR, "部门负责人不存在");
        if (manager.getUserId() != null) {
            User managerUser = userMapper.selectById(manager.getUserId());
            ThrowUtils.throwIf(managerUser == null || managerUser.getRoleId() == null
                            || !MANAGER_ROLE_IDS.contains(managerUser.getRoleId()),
                    ErrorCode.PARAMS_ERROR, "部门负责人必须是系统管理员、HR专员或部门主管");
        }
    }

    /**
     * 直接通过 Mapper 查用户角色 dataScope（不经过 PermissionService，避免循环依赖）
     */
    private Integer getDataScopeFromUser(Long userId) {
        if (userId == null) return DataScopeEnum.SELF.getCode();
        User user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) return DataScopeEnum.SELF.getCode();
        Role role = roleMapper.selectById(user.getRoleId());
        if (role == null || role.getStatus() == null || role.getStatus() != 1 || role.getDataScope() == null)
            return DataScopeEnum.SELF.getCode();
        return role.getDataScope();
    }

    // ==================== 私有辅助方法 ====================

    private DepartmentTreeVO buildTreeVO(Department dept,
                                         Map<Long, List<Department>> parentIdMap,
                                         Map<Long, Long> deptEmployeeCountMap,
                                         Map<Long, String> managerNameMap) {
        DepartmentTreeVO vo = new DepartmentTreeVO();
        vo.setId(dept.getId());
        vo.setName(dept.getDeptName());
        vo.setCode(dept.getDeptCode());
        vo.setParentId(dept.getParentId());
        vo.setManagerId(dept.getManagerId());
        vo.setManagerName(managerNameMap.get(dept.getManagerId()));
        vo.setSortOrder(dept.getSortOrder());
        vo.setDescription(dept.getDescription());
        vo.setEmployeeCount(deptEmployeeCountMap.getOrDefault(dept.getId(), 0L).intValue());

        List<Department> children = parentIdMap.getOrDefault(dept.getId(), Collections.emptyList());
        for (Department child : children) {
            vo.getChildren().add(buildTreeVO(child, parentIdMap, deptEmployeeCountMap, managerNameMap));
        }

        return vo;
    }

    private int calculateDepth(Long deptId) {
        int depth = 0;
        Long currentId = deptId;
        while (currentId != null) {
            depth++;
            Department dept = this.getById(currentId);
            if (dept == null) break;
            currentId = dept.getParentId();
            if (depth > OrgEnum.MAX_DEPT_DEPTH.getCode() + 1) break;
        }
        return depth;
    }

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

    private Map<Long, Long> buildEmployeeCountMap(List<Department> allDepts) {
        Map<Long, Long> map = new LinkedHashMap<>();
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

    private Map<Long, String> getManagerNameMap(Set<Long> managerIds) {
        Map<Long, String> map = new HashMap<>();
        if (managerIds.isEmpty()) return map;
        try {
            List<Employee> employees = employeeMapper.selectBatchIds(managerIds);
            for (Employee emp : employees) {
                map.put(emp.getId(), emp.getEmployeeName());
            }
        } catch (Exception e) {
            log.warn("查询负责人姓名失败: {}", e.getMessage());
        }
        return map;
    }
}
