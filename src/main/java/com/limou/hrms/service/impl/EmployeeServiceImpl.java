package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.employee.EmployeeCreateRequest;
import com.limou.hrms.model.dto.employee.EmployeeQueryRequest;
import com.limou.hrms.model.dto.employee.EmployeeUpdateRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.UserRoleEnum;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.PositionService;
import com.limou.hrms.service.UserService;
import com.limou.hrms.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工档案服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final EmployeePersonalInfoMapper personalInfoMapper;
    private final EmployeeWorkInfoMapper workInfoMapper;
    private final EmployeeSalaryInfoMapper salaryInfoMapper;
    private final EmployeeSalaryMapper salaryMapper;
    private final EmployeeNoSequenceMapper noSequenceMapper;
    private final EmployeeChangeLogMapper employeeChangeLogMapper;
    private final AesUtil aesUtil;

    private static final String SALT = "limou";
    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ==================== 字段权限常量 ====================
    // ... (unchanged)
    private static final List<String> ALL_VIEWABLE = Arrays.asList(
            "employeeNo", "name", "gender", "phone", "email", "idCard", "birthday",
            "registeredAddress", "currentAddress", "emergencyContactName", "emergencyContactPhone",
            "departmentName", "positionName", "jobLevel", "directReportName", "workLocation",
            "status", "hireDate", "hireType", "contractType", "contractExpireDate",
            "probationRatio", "salaryAccountName", "baseSalary", "bankAccount", "bankName"
    );

    private static final List<String> DEPT_HEAD_VIEWABLE = Arrays.asList(
            "employeeNo", "name", "gender", "phone", "email",
            "departmentName", "positionName", "jobLevel", "directReportName", "workLocation",
            "status", "hireDate", "hireType"
    );

    private static final List<String> FINANCE_VIEWABLE = Arrays.asList(
            "employeeNo", "name", "departmentName", "positionName",
            "contractType", "contractExpireDate", "probationRatio",
            "salaryAccountName", "baseSalary", "bankAccount", "bankName"
    );

    private static final List<String> USER_VIEWABLE = Arrays.asList(
            "employeeNo", "name", "gender", "phone", "email",
            "departmentName", "positionName", "jobLevel", "directReportName", "workLocation",
            "status", "hireDate", "hireType"
    );

    private static final List<String> ADMIN_HR_EDITABLE = Arrays.asList(
            "name", "gender", "phone", "email", "birthday",
            "registeredAddress", "currentAddress",
            "emergencyContactName", "emergencyContactPhone"
    );

    private static final List<String> USER_EDITABLE = Arrays.asList(
            "email", "birthday", "registeredAddress", "currentAddress",
            "emergencyContactName", "emergencyContactPhone"
    );

    private static final List<String> FLOW_REQUIRED = Arrays.asList(
            "departmentId", "positionId", "jobLevel", "directReportId",
            "phone", "workLocation"
    );

    // ==================== 枚举/权限接口 ====================

    @Override
    public List<Map<String, Object>> getStatuses() {
        return Arrays.stream(EmployeeStatus.values()).map(status -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("status", status.getValue());
            map.put("statusName", status.getDesc());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public FieldPermissionsVO getFieldPermissions(User loginUser) {
        UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (role == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        List<String> viewableFields;
        List<String> editableFields;

        switch (role) {
            case ADMIN:
            case HR:
                viewableFields = ALL_VIEWABLE;
                editableFields = ADMIN_HR_EDITABLE;
                break;
            case DEPT_HEAD:
                viewableFields = DEPT_HEAD_VIEWABLE;
                editableFields = Collections.emptyList();
                break;
            case FINANCE:
                viewableFields = FINANCE_VIEWABLE;
                editableFields = Collections.emptyList();
                break;
            case USER:
                viewableFields = USER_VIEWABLE;
                editableFields = USER_EDITABLE;
                break;
            default:
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        return FieldPermissionsVO.builder()
                .viewableFields(viewableFields)
                .editableFields(editableFields)
                .flowRequiredFields(FLOW_REQUIRED)
                .build();
    }

    // ==================== 创建员工档案 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeCreateVO createEmployee(EmployeeCreateRequest dto, User loginUser) {
        // 1. 校验必填关联数据
        validateDepartment(dto.getDepartmentId());
        validatePosition(dto.getPositionId());

        // 2. 校验固定期限合同必须填到期日
        if (dto.getContractType() == 1 && dto.getContractExpireDate() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "固定期限合同必须填写合同到期日");
        }

        // 3. 生成工号
        String employeeNo = generateEmployeeNo(dto.getDepartmentId());

        // 4. 创建系统用户账号
        String initialPassword = generateRandomPassword();
        User user = buildUser(dto, initialPassword);
        userService.save(user);
        Long userId = user.getId();

        // 5. 写入 employee 主表
        Employee employee = buildEmployee(dto, employeeNo, userId);
        this.save(employee);
        Long employeeId = employee.getId();

        // 6. 写入 personal_info（身份证号 AES 加密）
        EmployeePersonalInfo personalInfo = buildPersonalInfo(employeeId, dto);
        personalInfoMapper.insert(personalInfo);

        // 7. 写入 work_info
        EmployeeWorkInfo workInfo = buildWorkInfo(employeeId, dto);
        workInfoMapper.insert(workInfo);

        // 8. 写入 employee_salary（基本工资）
        EmployeeSalary salary = buildSalary(employeeId, dto);
        salaryMapper.insert(salary);

        // 9. 写入 salary_info（银行卡号 AES 加密，关联 employee_salary.id）
        EmployeeSalaryInfo salaryInfo = buildSalaryInfo(employeeId, salary.getId(), dto);
        salaryInfoMapper.insert(salaryInfo);

        log.info("员工档案创建成功: employeeId={}, employeeNo={}, account={}", employeeId, employeeNo, dto.getPhone());

        return EmployeeCreateVO.builder()
                .id(employeeId)
                .employeeNo(employeeNo)
                .account(dto.getPhone())
                .initialPassword(initialPassword)
                .build();
    }

    // ==================== 查询员工详情 ====================

    @Override
    public EmployeeDetailVO getEmployeeDetail(Long id, User loginUser) {
        // 1. 查询员工
        Employee employee = this.lambdaQuery().eq(Employee::getId, id).one();
        if (employee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工不存在");
        }

        // 2. 数据权限校验
        UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (role == UserRoleEnum.USER) {
            // 普通员工只能查自己
            Employee selfEmp = this.lambdaQuery()
                    .eq(Employee::getUserId, loginUser.getId())
                    .one();
            if (selfEmp == null || !selfEmp.getId().equals(id)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该员工档案");
            }
        } else if (role == UserRoleEnum.DEPT_HEAD) {
            // 部门主管只能查本部门及下属
            validateDeptHeadScope(loginUser, employee.getId());
        }
        // ADMIN / HR / FINANCE 无数据范围限制

        // 3. 查询关联数据
        EmployeePersonalInfo personalInfo = personalInfoMapper.selectOne(
                Wrappers.<EmployeePersonalInfo>lambdaQuery()
                        .eq(EmployeePersonalInfo::getEmployeeId, id));
        EmployeeWorkInfo workInfo = workInfoMapper.selectOne(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .eq(EmployeeWorkInfo::getEmployeeId, id));
        EmployeeSalaryInfo salaryInfo = salaryInfoMapper.selectOne(
                Wrappers.<EmployeeSalaryInfo>lambdaQuery()
                        .eq(EmployeeSalaryInfo::getEmployeeId, id));

        // 4. 解析基础信息
        User employeeUser = userService.getById(employee.getUserId());

        EmployeeDetailVO.EmployeeDetailVOBuilder builder = EmployeeDetailVO.builder()
                .id(employee.getId())
                .employeeNo(employee.getEmployeeNo())
                .account(employeeUser != null ? employeeUser.getUserAccount() : null)
                .status(employee.getStatus())
                .statusDesc(resolveStatusDesc(employee.getStatus()))
                .hireDate(employee.getHireDate() != null ? employee.getHireDate().toString() : null)
                .hireType(employee.getHireType())
                .hireTypeDesc(resolveHireTypeDesc(employee.getHireType()))
                .createTime(employee.getCreateTime());

        // 5. 构建个人信息（脱敏）
        if (personalInfo != null) {
            // phone 和 email 是明文存储，身份证号是 AES 加密存储
            String rawPhone = personalInfo.getPhone();
            String rawIdCard = personalInfo.getIdCard();

            builder.personalInfo(PersonalInfoVO.builder()
                    .name(personalInfo.getName())
                    .gender(personalInfo.getGender())
                    .genderDesc(personalInfo.getGender() == 1 ? "男" : "女")
                    .phone(maskIfNeeded(rawPhone, role))
                    .email(personalInfo.getEmail())
                    .idCard(maskIdCard(rawIdCard, role))
                    .birthday(personalInfo.getBirthday() != null ? personalInfo.getBirthday().toString() : null)
                    .registeredAddress(personalInfo.getRegisteredAddress())
                    .currentAddress(personalInfo.getCurrentAddress())
                    .emergencyContactName(isFieldViewable(role, "emergencyContactName")
                            ? personalInfo.getEmergencyContactName() : null)
                    .emergencyContactPhone(isFieldViewable(role, "emergencyContactPhone")
                            ? maskIfNeeded(personalInfo.getEmergencyContactPhone(), role) : null)
                    .build());
        }

        // 6. 构建工作信息
        if (workInfo != null) {
            Department dept = departmentService.getById(workInfo.getDepartmentId());
            Position pos = positionService.getById(workInfo.getPositionId());
            String directReportName = null;
            if (workInfo.getDirectReportId() != null) {
                EmployeePersonalInfo reportInfo = personalInfoMapper.selectOne(
                        Wrappers.<EmployeePersonalInfo>lambdaQuery()
                                .eq(EmployeePersonalInfo::getEmployeeId, workInfo.getDirectReportId()));
                directReportName = reportInfo != null ? reportInfo.getName() : null;
            }

            builder.workInfo(WorkInfoVO.builder()
                    .departmentId(workInfo.getDepartmentId())
                    .departmentName(dept != null ? dept.getName() : null)
                    .positionId(workInfo.getPositionId())
                    .positionName(pos != null ? pos.getName() : null)
                    .jobLevel(workInfo.getJobLevel())
                    .directReportId(workInfo.getDirectReportId())
                    .directReportName(directReportName)
                    .workLocation(workInfo.getWorkLocation())
                    .build());
        }

        // 7. 构建薪资信息（仅 HR/ADMIN/FINANCE 可见）
        if (role == UserRoleEnum.HR || role == UserRoleEnum.ADMIN || role == UserRoleEnum.FINANCE) {
            if (salaryInfo != null) {
                EmployeeSalary salary = salaryMapper.selectOne(
                        Wrappers.<EmployeeSalary>lambdaQuery()
                                .eq(EmployeeSalary::getEmployeeId, id));
                String bankAccount = salaryInfo.getBankAccount();
                builder.salaryInfo(SalaryInfoVO.builder()
                        .contractType(salaryInfo.getContractType())
                        .contractTypeDesc(resolveContractTypeDesc(salaryInfo.getContractType()))
                        .contractExpireDate(salaryInfo.getContractExpireDate() != null
                                ? salaryInfo.getContractExpireDate().toString() : null)
                        .probationRatio(salaryInfo.getProbationRatio())
                        .salaryAccountId(salaryInfo.getSalaryAccountId())
                        .baseSalary(salary != null ? salary.getBaseSalary() : null)
                        .bankAccount(maskBankAccount(bankAccount, role))
                        .bankName(salaryInfo.getBankName())
                        .build());
            }
        }

        return builder.build();
    }

    // ==================== 员工列表查询 ====================

    @Override
    public Page<EmployeeListVO> listEmployees(EmployeeQueryRequest query, User loginUser) {
        UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        List<Long> deptIds = null;
        Long selfEmployeeId = null;

        switch (role) {
            case ADMIN:
            case HR:
                // 全量，不设限制
                break;
            case DEPT_HEAD:
                // 仅本部门及下属，除非 all=true（委托审批等场景需搜全员）
                if (Boolean.TRUE.equals(query.getAll())) {
                    break;
                }
                deptIds = resolveManagedDeptIds(loginUser);
                if (deptIds == null || deptIds.isEmpty()) {
                    return new Page<>(query.getCurrent(), query.getPageSize(), 0);
                }
                break;
            case USER:
                // 仅本人，除非 all=true（委托审批等场景需搜全员）
                if (Boolean.TRUE.equals(query.getAll())) {
                    break;
                }
                Employee selfEmp = this.lambdaQuery()
                        .eq(Employee::getUserId, loginUser.getId())
                        .one();
                selfEmployeeId = selfEmp != null ? selfEmp.getId() : -1L;
                break;
            default:
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        Page<EmployeeListVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        Page<EmployeeListVO> result = this.baseMapper.selectEmployeePage(
                page,
                query.getKeyword(),
                query.getDepartmentIds() != null ? Arrays.asList(query.getDepartmentIds()) : null,
                query.getPositionIds() != null ? Arrays.asList(query.getPositionIds()) : null,
                query.getStatuses() != null ? Arrays.asList(query.getStatuses()) : null,
                query.getJobLevels() != null ? Arrays.asList(query.getJobLevels()) : null,
                query.getHireDateStart() != null ? query.getHireDateStart().toString() : null,
                query.getHireDateEnd() != null ? query.getHireDateEnd().toString() : null,
                deptIds,
                selfEmployeeId);

        // 填充状态描述
        for (EmployeeListVO vo : result.getRecords()) {
            vo.setStatusDesc(resolveStatusDesc(vo.getStatus()));
        }
        return result;
    }

    // ==================== 导出员工 ====================

    @Override
    public List<EmployeeListVO> exportEmployees(EmployeeQueryRequest query, User loginUser) {
        UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        List<Long> deptIds = null;
        Long selfEmployeeId = null;

        switch (role) {
            case ADMIN:
            case HR:
                break;
            case DEPT_HEAD:
                deptIds = resolveManagedDeptIds(loginUser);
                if (deptIds == null || deptIds.isEmpty()) {
                    return Collections.emptyList();
                }
                break;
            case USER:
                Employee selfEmp = this.lambdaQuery()
                        .eq(Employee::getUserId, loginUser.getId())
                        .one();
                selfEmployeeId = selfEmp != null ? selfEmp.getId() : -1L;
                break;
            default:
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        List<EmployeeListVO> list = this.baseMapper.selectEmployeeList(
                query.getKeyword(),
                query.getDepartmentIds() != null ? Arrays.asList(query.getDepartmentIds()) : null,
                query.getPositionIds() != null ? Arrays.asList(query.getPositionIds()) : null,
                query.getStatuses() != null ? Arrays.asList(query.getStatuses()) : null,
                query.getJobLevels() != null ? Arrays.asList(query.getJobLevels()) : null,
                query.getHireDateStart() != null ? query.getHireDateStart().toString() : null,
                query.getHireDateEnd() != null ? query.getHireDateEnd().toString() : null,
                deptIds,
                selfEmployeeId);

        // 填充状态描述
        for (EmployeeListVO vo : list) {
            vo.setStatusDesc(resolveStatusDesc(vo.getStatus()));
        }
        return list;
    }

    // ==================== 员工快速搜索 ====================

    @Override
    public List<Map<String, Object>> searchEmployees(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return Collections.emptyList();
        List<Employee> emps = this.lambdaQuery()
                .list()
                .stream()
                .filter(e -> e.getIsDeleted() == null || e.getIsDeleted() == 0)
                .collect(Collectors.toList());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee e : emps) {
            EmployeePersonalInfo info = personalInfoMapper.selectOne(
                    new QueryWrapper<EmployeePersonalInfo>().eq("employee_id", e.getId()).last("LIMIT 1"));
            String name = info != null ? info.getName() : "";
            if (!name.contains(keyword) && !e.getEmployeeNo().contains(keyword)) continue;
            EmployeeWorkInfo wi = workInfoMapper.selectOne(
                    new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", e.getId()).last("LIMIT 1"));
            String posName = "";
            if (wi != null && wi.getPositionId() != null) {
                Position pos = positionService.getById(wi.getPositionId());
                posName = pos != null ? pos.getName() : "";
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", e.getId());
            map.put("name", name);
            map.put("positionName", posName);
            result.add(map);
            if (result.size() >= 20) break;
        }
        return result;
    }

    // ==================== 更新员工档案 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeUpdateVO updateEmployee(Long id, EmployeeUpdateRequest dto, User loginUser) {
        // 1. 查询员工
        Employee employee = this.lambdaQuery().eq(Employee::getId, id).one();
        if (employee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工不存在");
        }

        // 2. 数据权限校验
        UserRoleEnum role = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (role == UserRoleEnum.USER) {
            Employee selfEmp = this.lambdaQuery()
                    .eq(Employee::getUserId, loginUser.getId())
                    .one();
            if (selfEmp == null || !selfEmp.getId().equals(id)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权编辑该员工档案");
            }
        } else if (role == UserRoleEnum.DEPT_HEAD) {
            validateDeptHeadScope(loginUser, id);
        }

        // 3. 获取当前角色的 editableFields
        Set<String> editableFields = new HashSet<>(resolveEditableFields(role));
        List<String> updatedFields = new ArrayList<>();
        List<String> flowRequiredFields = new ArrayList<>();

        // 4. 查询个人信息
        EmployeePersonalInfo personalInfo = personalInfoMapper.selectOne(
                Wrappers.<EmployeePersonalInfo>lambdaQuery()
                        .eq(EmployeePersonalInfo::getEmployeeId, id));
        if (personalInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工个人信息不存在");
        }

        // 5. 逐字段校验并更新
        updateFieldIfNotNull("name", dto.getName(), editableFields,
                personalInfo::getName, personalInfo::setName,
                updatedFields, flowRequiredFields,
                id, loginUser.getId());
        updateFieldIfNotNull("gender", dto.getGender(), editableFields,
                () -> personalInfo.getGender() != null ? personalInfo.getGender().toString() : null,
                v -> personalInfo.setGender(v != null ? Integer.valueOf(v) : null),
                updatedFields, flowRequiredFields,
                id, loginUser.getId());
        updateFieldIfNotNull("phone", dto.getPhone(), editableFields,
                personalInfo::getPhone, personalInfo::setPhone,
                updatedFields, flowRequiredFields,
                id, loginUser.getId());
        updateFieldIfNotNull("email", dto.getEmail(), editableFields,
                personalInfo::getEmail, personalInfo::setEmail,
                updatedFields, flowRequiredFields,
                id, loginUser.getId());
        updateFieldIfNotNull("birthday", dto.getBirthday(), editableFields,
                () -> personalInfo.getBirthday() != null ? personalInfo.getBirthday().toString() : null,
                v -> personalInfo.setBirthday(v),
                updatedFields, flowRequiredFields,
                id, loginUser.getId());
        updateFieldIfNotNull("registeredAddress", dto.getRegisteredAddress(), editableFields,
                personalInfo::getRegisteredAddress, personalInfo::setRegisteredAddress,
                updatedFields, flowRequiredFields,
                id, loginUser.getId());
        updateFieldIfNotNull("currentAddress", dto.getCurrentAddress(), editableFields,
                personalInfo::getCurrentAddress, personalInfo::setCurrentAddress,
                updatedFields, flowRequiredFields,
                id, loginUser.getId());
        updateFieldIfNotNull("emergencyContactName", dto.getEmergencyContactName(), editableFields,
                personalInfo::getEmergencyContactName, personalInfo::setEmergencyContactName,
                updatedFields, flowRequiredFields,
                id, loginUser.getId());
        updateFieldIfNotNull("emergencyContactPhone", dto.getEmergencyContactPhone(), editableFields,
                personalInfo::getEmergencyContactPhone, personalInfo::setEmergencyContactPhone,
                updatedFields, flowRequiredFields,
                id, loginUser.getId());

        // 6. 保存个人信息变更
        if (!updatedFields.isEmpty()) {
            personalInfoMapper.updateById(personalInfo);
        }

        return EmployeeUpdateVO.builder()
                .updatedFields(updatedFields)
                .flowRequiredFields(flowRequiredFields)
                .build();
    }

    /**
     * 字段更新辅助：新值不为 null 时校验权限并更新，为 null 时不处理（保留原值）
     */
    private <T> void updateFieldIfNotNull(String fieldName, T newValue,
                                           Set<String> editableFields,
                                           java.util.function.Supplier<String> oldValueSupplier,
                                           java.util.function.Consumer<T> setter,
                                           List<String> updatedFields,
                                           List<String> flowRequiredFields,
                                           Long employeeId, Long operatorId) {
        if (newValue == null) {
            return;
        }
        if (!editableFields.contains(fieldName)) {
            flowRequiredFields.add(fieldName);
            return;
        }
        String oldValueStr = oldValueSupplier.get();
        String newValueStr = newValue.toString();
        setter.accept(newValue);
        updatedFields.add(fieldName);

        // 记变更日志
        EmployeeChangeLog changeLog = EmployeeChangeLog.builder()
                .employeeId(employeeId)
                .fieldName(fieldName)
                .oldValue(oldValueStr)
                .newValue(newValueStr)
                .changeType("DIRECT_EDIT")
                .operatorId(operatorId)
                .createTime(LocalDateTime.now())
                .build();
        employeeChangeLogMapper.insert(changeLog);
    }

    // ==================== 私有方法 ====================

    /**
     * 生成工号（格式：年份4位 + 部门编码2位 + 序号3位，如 202601005）
     */
    private String generateEmployeeNo(Long departmentId) {
        Department dept = departmentService.getById(departmentId);
        if (dept == null || dept.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门不存在或已删除");
        }
        String deptCode = dept.getCode();
        if (deptCode == null || deptCode.length() < 2) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部门编码格式不正确，工号生成失败");
        }
        // 取部门编码后2位
        String shortCode = deptCode.length() <= 2 ? String.format("%2s", deptCode).replace(' ', '0') : deptCode.substring(deptCode.length() - 2);
        int year = LocalDate.now().getYear();

        String lockKey = (year + "_" + shortCode).intern();
        synchronized (lockKey) {
            LambdaQueryWrapper<EmployeeNoSequence> query = Wrappers.<EmployeeNoSequence>lambdaQuery()
                    .eq(EmployeeNoSequence::getYear, year)
                    .eq(EmployeeNoSequence::getDeptCode, shortCode);
            EmployeeNoSequence seq = noSequenceMapper.selectOne(query);
            if (seq == null) {
                seq = new EmployeeNoSequence();
                seq.setYear(year);
                seq.setDeptCode(shortCode);
                seq.setCurrentSeq(1);
                noSequenceMapper.insert(seq);
            } else {
                seq.setCurrentSeq(seq.getCurrentSeq() + 1);
                noSequenceMapper.updateById(seq);
            }
            return String.format("%04d%s%03d", year, shortCode, seq.getCurrentSeq());
        }
    }

    /**
     * 生成随机初始密码（8位，含大小写字母+数字）
     */
    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private User buildUser(EmployeeCreateRequest dto, String initialPassword) {
        User user = new User();
        user.setUserAccount(dto.getPhone());
        user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + initialPassword).getBytes()));
        user.setUserName(dto.getName());
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setCreateTime(new Date());
        return user;
    }

    private Employee buildEmployee(EmployeeCreateRequest dto, String employeeNo, Long userId) {
        Employee employee = new Employee();
        employee.setUserId(userId);
        employee.setEmployeeNo(employeeNo);
        employee.setStatus(EmployeeStatus.PROBATION.getValue());
        employee.setHireDate(dto.getHireDate());
        employee.setHireType(dto.getHireType());
        employee.setCreateTime(LocalDateTime.now());
        return employee;
    }

    private EmployeePersonalInfo buildPersonalInfo(Long employeeId, EmployeeCreateRequest dto) {
        EmployeePersonalInfo info = new EmployeePersonalInfo();
        info.setEmployeeId(employeeId);
        info.setName(dto.getName());
        info.setGender(dto.getGender());
        info.setPhone(dto.getPhone());
        info.setEmail(dto.getEmail());
        info.setIdCard(dto.getIdCard());
        info.setCreateTime(LocalDateTime.now());
        return info;
    }

    private EmployeeWorkInfo buildWorkInfo(Long employeeId, EmployeeCreateRequest dto) {
        EmployeeWorkInfo info = new EmployeeWorkInfo();
        info.setEmployeeId(employeeId);
        info.setDepartmentId(dto.getDepartmentId());
        info.setPositionId(dto.getPositionId());
        info.setJobLevel(dto.getJobLevel());
        info.setDirectReportId(dto.getDirectReportId());
        info.setWorkLocation(dto.getWorkLocation());
        info.setCreateTime(LocalDateTime.now());
        return info;
    }

    private EmployeeSalary buildSalary(Long employeeId, EmployeeCreateRequest dto) {
        EmployeeSalary salary = new EmployeeSalary();
        salary.setEmployeeId(employeeId);
        salary.setAccountId(dto.getSalaryAccountId());
        salary.setBaseSalary(dto.getBaseSalary());
        salary.setEffectiveDate(dto.getHireDate());
        salary.setCreateTime(LocalDateTime.now());
        return salary;
    }

    private EmployeeSalaryInfo buildSalaryInfo(Long employeeId, Long employeeSalaryId,
                                                EmployeeCreateRequest dto) {
        EmployeeSalaryInfo info = new EmployeeSalaryInfo();
        info.setEmployeeId(employeeId);
        info.setEmployeeSalaryId(employeeSalaryId);
        info.setContractType(dto.getContractType());
        info.setContractExpireDate(dto.getContractExpireDate());
        info.setProbationRatio(dto.getProbationRatio());
        info.setSalaryAccountId(dto.getSalaryAccountId());
        info.setBankAccount(dto.getBankAccount() != null ? aesUtil.encrypt(dto.getBankAccount()) : null);
        info.setBankName(dto.getBankName());
        info.setCreateTime(LocalDateTime.now());
        return info;
    }

    private void validateDepartment(Long departmentId) {
        if (departmentId == null) return;
        Department dept = departmentService.getById(departmentId);
        if (dept == null || dept.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门不存在或已删除");
        }
    }

    private void validatePosition(Long positionId) {
        if (positionId == null) return;
        Position pos = positionService.getById(positionId);
        if (pos == null || pos.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "职位不存在或已删除");
        }
    }

    // ==================== 数据权限 ====================

    /**
     * 校验部门主管的数据范围：只能看本部门及下属部门的员工
     */
    private void validateDeptHeadScope(User loginUser, Long targetEmployeeId) {
        List<Long> managedDeptIds = resolveManagedDeptIds(loginUser);
        if (managedDeptIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无法确定您的管辖范围");
        }

        EmployeeWorkInfo targetWorkInfo = workInfoMapper.selectOne(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .eq(EmployeeWorkInfo::getEmployeeId, targetEmployeeId));
        if (targetWorkInfo == null
                || !managedDeptIds.contains(targetWorkInfo.getDepartmentId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看该员工档案");
        }
    }

    private void collectChildDeptIds(List<Department> allDepts, Long parentId, Set<Long> result) {
        for (Department dept : allDepts) {
            if (parentId.equals(dept.getParentId())) {
                result.add(dept.getId());
                collectChildDeptIds(allDepts, dept.getId(), result);
            }
        }
    }

    /**
     * 获取指定角色的可编辑字段列表
     */
    private List<String> resolveEditableFields(UserRoleEnum role) {
        switch (role) {
            case ADMIN:
            case HR:
                return ADMIN_HR_EDITABLE;
            case USER:
                return USER_EDITABLE;
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 获取部门主管管辖的所有部门ID（含子部门）
     */
    /**
     * 按 department.manager_id 解析部门主管管辖的部门（含子部门）
     */
    private List<Long> resolveManagedDeptIds(User loginUser) {
        List<Employee> emps = this.lambdaQuery()
                .eq(Employee::getUserId, loginUser.getId())
                .list();
        if (emps.isEmpty()) return Collections.emptyList();
        List<Long> empIds = emps.stream().map(Employee::getId).collect(Collectors.toList());

        // 查所有部门，找 managerId 匹配的
        List<Department> allDepts = departmentService.list();
        Set<Long> deptIds = new HashSet<>();
        for (Department d : allDepts) {
            if (d.getManagerId() != null && empIds.contains(d.getManagerId())) {
                deptIds.add(d.getId());
                collectChildDeptIds(allDepts, d.getId(), deptIds);
            }
        }
        return new ArrayList<>(deptIds);
    }

    // ==================== 字段脱敏 ====================

    /**
     * 身份证号脱敏：HR/管理员可见完整，其余保留前4后2
     */
    private String maskIdCard(String idCard, UserRoleEnum role) {
        if (idCard == null) return null;
        if (role == UserRoleEnum.HR || role == UserRoleEnum.ADMIN) {
            return idCard;
        }
        if (idCard.length() >= 6) {
            return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 2);
        }
        return "****";
    }

    /**
     * 手机号脱敏：HR/部门主管可见完整，其余保留前3后4
     */
    private String maskIfNeeded(String phone, UserRoleEnum role) {
        if (phone == null) return null;
        if (role == UserRoleEnum.HR || role == UserRoleEnum.ADMIN
                || role == UserRoleEnum.DEPT_HEAD) {
            return phone;
        }
        if (phone.length() >= 7) {
            return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        }
        return "****";
    }

    /**
     * 银行卡号脱敏：HR/财务可见完整，其余仅后4位
     */
    private String maskBankAccount(String bankAccount, UserRoleEnum role) {
        if (bankAccount == null) return null;
        // 尝试解密，解密失败（明文存储）则直接用原值
        String plain = aesUtil.decrypt(bankAccount);
        if (plain == null) plain = bankAccount;
        if (role == UserRoleEnum.HR || role == UserRoleEnum.ADMIN
                || role == UserRoleEnum.FINANCE) {
            return plain;
        }
        if (plain.length() >= 4) {
            return "****" + plain.substring(plain.length() - 4);
        }
        return "****";
    }

    // ==================== 字段可见性 ====================

    private boolean isFieldViewable(UserRoleEnum role, String fieldName) {
        FieldPermissionsVO perms = getFieldPermissionsForRole(role);
        return perms.getViewableFields().contains(fieldName);
    }

    private FieldPermissionsVO getFieldPermissionsForRole(UserRoleEnum role) {
        List<String> viewableFields;
        switch (role) {
            case ADMIN:
            case HR:
                viewableFields = ALL_VIEWABLE;
                break;
            case DEPT_HEAD:
                viewableFields = DEPT_HEAD_VIEWABLE;
                break;
            case FINANCE:
                viewableFields = FINANCE_VIEWABLE;
                break;
            case USER:
                viewableFields = USER_VIEWABLE;
                break;
            default:
                viewableFields = Collections.emptyList();
        }
        return FieldPermissionsVO.builder().viewableFields(viewableFields).build();
    }

    // ==================== 枚举描述 ====================

    private String resolveStatusDesc(Integer status) {
        EmployeeStatus es = EmployeeStatus.getByValue(status);
        return es != null ? es.getDesc() : null;
    }

    private String resolveHireTypeDesc(Integer hireType) {
        if (hireType == null) return null;
        switch (hireType) {
            case 1: return "全职";
            case 2: return "兼职";
            case 3: return "实习";
            default: return null;
        }
    }

    private String resolveContractTypeDesc(Integer contractType) {
        if (contractType == null) return null;
        switch (contractType) {
            case 1: return "固定期限";
            case 2: return "无固定期限";
            case 3: return "劳务合同";
            default: return null;
        }
    }
}