package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.model.enums.DataScopeEnum;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.EmploymentType;
import com.limou.hrms.model.enums.ContractType;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeDetailMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.PositionMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.dto.employee.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.service.EmployeeChangeLogService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.EmpSalaryProfileService;
import com.limou.hrms.service.PermissionService;
import com.limou.hrms.service.PositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工服务实现
 */
@Slf4j
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
        implements EmployeeService {
    @Resource
    private DepartmentService departmentService;
    @Resource
    private PositionService positionService;
    @Resource
    private EmpSalaryProfileService salaryProfileService;
    @Resource
    private EmployeeDetailMapper employeeDetailMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private EmployeeChangeLogService employeeChangeLogService;
    @Resource
    private UserMapper userMapper;

    @Autowired
    @Lazy
    private PermissionService permissionService;

    private static final Set<String> EDITABLE_FIELDS = Set.of("email", "currentAddress", "emergencyContactName","emergencyContactPhone");
    private static final Set<String> LOCKED_FIELDS = Set.of("phone", "idCard", "departmentId", "positionId", "jobLevel", "directReportId", "workLocation", "employmentType", "contractType", "contractExpireDate", "probationRatio", "baseSalary", "bankAccount", "bankName");
    private static final String SALT = "hrms";

    // ==================== 员工档案管理 ====================

    @Override
    public Page<EmployeeVO> listEmployees(EmployeeQueryRequest request, Long userId) {
        Integer dataScope = permissionService.getUserDataScope(userId);

        // SELF(5)：普通员工，列表无权限查看
        if (dataScope == DataScopeEnum.SELF.getCode()) {
            return new Page<>(request.getPage(), request.getSize(), 0);
        }

        LambdaQueryWrapper<Employee> wrapper = buildEmployeeQueryWrapper(request);

        // DEPARTMENT(3)：按可见部门过滤
        if (dataScope == DataScopeEnum.DEPARTMENT.getCode()) {
            List<Long> visibleDeptIds = departmentService.getVisibleDeptIds(userId);
            if (visibleDeptIds.isEmpty()) {
                return new Page<>(request.getPage(), request.getSize(), 0);
            }
            wrapper.in(Employee::getDepartmentId, visibleDeptIds);
        }
        // ALL(1,2) 和 SALARY(4)：不额外过滤

        Page<Employee> page = this.page(new Page<>(request.getPage(), request.getSize()), wrapper);
        Set<Long> deptIds = page.getRecords().stream().map(Employee::getDepartmentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> posIds = page.getRecords().stream().map(Employee::getPositionId).filter(Objects::nonNull).collect(Collectors.toSet());

        Page<EmployeeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(e -> {
            EmployeeVO vo = new EmployeeVO();
            BeanUtils.copyProperties(e, vo);
            vo.setStatusDesc(EmployeeStatus.getDesc(e.getStatus()));
            vo.setDepartmentName(getDeptName(e.getDepartmentId()));
            vo.setPositionName(getPosName(e.getPositionId()));
            vo.setEmploymentTypeDesc(EmploymentType.getDesc(e.getEmploymentType()));
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<EmployeeExcelVO> exportEmployees(EmployeeQueryRequest request, Long userId) {
        Integer dataScope = permissionService.getUserDataScope(userId);

        // SELF(5)：普通员工，无导出权限
        if (dataScope == DataScopeEnum.SELF.getCode()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Employee> wrapper = buildEmployeeQueryWrapper(request);

        // DEPARTMENT(3)：按可见部门过滤
        if (dataScope == DataScopeEnum.DEPARTMENT.getCode()) {
            List<Long> visibleDeptIds = departmentService.getVisibleDeptIds(userId);
            if (visibleDeptIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(Employee::getDepartmentId, visibleDeptIds);
        }

        List<Employee> list = this.list(wrapper);
        return list.stream().map(e -> {
            EmployeeExcelVO vo = new EmployeeExcelVO();
            vo.setEmployeeNo(e.getEmployeeNo());
            vo.setEmployeeName(e.getEmployeeName());
            vo.setGenderDesc(e.getGender() != null && e.getGender() == 1 ? "男" : "女");
            vo.setPhone(e.getPhone());
            vo.setEmail(e.getEmail());
            vo.setDepartmentName(getDeptName(e.getDepartmentId()));
            vo.setPositionName(getPosName(e.getPositionId()));
            vo.setJobLevel(e.getJobLevel());
            vo.setEmploymentTypeDesc(EmploymentType.getDesc(e.getEmploymentType()));
            vo.setStatusDesc(EmployeeStatus.getDesc(e.getStatus()));
            vo.setHireDate(e.getHireDate());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 构建员工查询的公共条件（复用：列表查询 / 导出）
     */
    private LambdaQueryWrapper<Employee> buildEmployeeQueryWrapper(EmployeeQueryRequest request) {
        return new LambdaQueryWrapper<Employee>()
                .and(StringUtils.hasText(request.getKeyword()),
                        w -> w.like(Employee::getEmployeeName, request.getKeyword())
                             .or().like(Employee::getEmployeeNo, request.getKeyword())
                             .or().like(Employee::getPhone, request.getKeyword()))
                .in(isNotEmpty(request.getDepartmentIds()), Employee::getDepartmentId, request.getDepartmentIds())
                .in(isNotEmpty(request.getPositionIds()), Employee::getPositionId, request.getPositionIds())
                .in(isNotEmpty(request.getStatuses()), Employee::getStatus, request.getStatuses())
                .in(isNotEmpty(request.getJobLevels()), Employee::getJobLevel, request.getJobLevels())
                .ge(request.getHireDateStart() != null, Employee::getHireDate, request.getHireDateStart())
                .le(request.getHireDateEnd() != null, Employee::getHireDate, request.getHireDateEnd())
                .last("ORDER BY FIELD(status, 2, 1, 3, 4), createTime DESC");
    }

    @Override
    public EmployeeDetailVO getDetail(Long id, Long userId) {
        Integer dataScope = permissionService.getUserDataScope(userId);

        // SELF(5)：普通员工不能查看员工档案，请使用个人中心
        if (dataScope == DataScopeEnum.SELF.getCode()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工不存在");
        }

        // DEPARTMENT(3)：只能查可见部门内的员工
        if (dataScope == DataScopeEnum.DEPARTMENT.getCode()) {
            Employee target = this.getById(id);
            if (target != null) {
                List<Long> visibleDeptIds = departmentService.getVisibleDeptIds(userId);
                if (visibleDeptIds.isEmpty() || target.getDepartmentId() == null
                        || !visibleDeptIds.contains(target.getDepartmentId())) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工不存在");
                }
            }
        }
        // ALL(1,2) 和 SALARY(4)：全部可见，不额外校验

        Employee emp = this.getById(id);
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工不存在");
        EmployeeDetail detail = getDetailByEmployeeId(id);
        EmployeeDetailVO vo = new EmployeeDetailVO();
        BeanUtils.copyProperties(emp, vo);
        if (detail != null) BeanUtils.copyProperties(detail, vo);
        vo.setStatusDesc(EmployeeStatus.getDesc(emp.getStatus()));
        vo.setGenderDesc(emp.getGender() != null && emp.getGender() == 1 ? "男" : "女");
        vo.setPhone(maskPhone(emp.getPhone()));
        vo.setEmploymentTypeDesc(EmploymentType.getDesc(emp.getEmploymentType()));
        vo.setIdCard(detail != null ? maskIdCard(detail.getIdCard()) : null);
        vo.setContractTypeDesc(detail != null && detail.getContractType() != null ? ContractType.getDesc(detail.getContractType()) : null);
        vo.setEmergencyContactPhone(maskPhone(detail != null ? detail.getEmergencyContactPhone() : null));
        vo.setDepartmentName(getDeptName(emp.getDepartmentId()));
        vo.setPositionName(getPosName(emp.getPositionId()));
        if (detail != null && detail.getDirectReportId() != null) {
            Employee report = this.getById(detail.getDirectReportId());
            vo.setDirectReportName(report != null ? report.getEmployeeName() : null);
        }

        // 按角色字段脱敏
        applyFieldMasking(vo, dataScope);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addEmployee(EmployeeAddRequest request) {
        String name = request.getEmployeeName() != null ? request.getEmployeeName().trim() : "";
        String phone = request.getPhone() != null ? request.getPhone().trim() : "";
        // 必填校验
        ThrowUtils.throwIf(name.isEmpty(), ErrorCode.PARAMS_ERROR, "姓名不能为空");
        ThrowUtils.throwIf(request.getGender() == null, ErrorCode.PARAMS_ERROR, "性别不能为空");
        ThrowUtils.throwIf(phone.isEmpty(), ErrorCode.PARAMS_ERROR, "手机号不能为空");
        ThrowUtils.throwIf(request.getEmploymentType() == null || request.getEmploymentType().isEmpty(), ErrorCode.PARAMS_ERROR, "录用类型不能为空");
        ThrowUtils.throwIf(request.getDepartmentId() == null, ErrorCode.PARAMS_ERROR, "所属部门不能为空");
        ThrowUtils.throwIf(request.getPositionId() == null, ErrorCode.PARAMS_ERROR, "职位不能为空");
        // 手机号格式
        ThrowUtils.throwIf(!phone.matches("^1[3-9]\\d{9}$"), ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        // 手机号唯一
        long phoneCount = this.count(new LambdaQueryWrapper<Employee>().eq(Employee::getPhone, phone));
        ThrowUtils.throwIf(phoneCount > 0, ErrorCode.OPERATION_ERROR, "手机号 " + phone + " 已被其他员工使用");

        // 身份证号唯一（存在 employee_detail 表）
        if (request.getIdCard() != null && !request.getIdCard().isEmpty()) {
            long idCardCount = employeeDetailMapper.selectCount(new LambdaQueryWrapper<EmployeeDetail>()
                    .eq(EmployeeDetail::getIdCard, request.getIdCard()));
            ThrowUtils.throwIf(idCardCount > 0, ErrorCode.OPERATION_ERROR, "身份证号已存在");
        }

        // 邮箱：允许为空，填了则验证格式和唯一性
        String email = null;
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            email = request.getEmail().trim();
            ThrowUtils.throwIf(!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$"), ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
            long emailCount = this.count(new LambdaQueryWrapper<Employee>().eq(Employee::getEmail, email));
            ThrowUtils.throwIf(emailCount > 0, ErrorCode.OPERATION_ERROR, "邮箱 " + email + " 已被其他员工使用");
        }
        // 部门/职位存在
        Department dept = departmentMapper.selectById(request.getDepartmentId());
        ThrowUtils.throwIf(dept == null, ErrorCode.NOT_FOUND_ERROR, "部门不存在");
        Position pos = positionMapper.selectById(request.getPositionId());
        ThrowUtils.throwIf(pos == null, ErrorCode.NOT_FOUND_ERROR, "职位不存在");
        // 业务校验
        if (request.getProbationRatio() != null)
            ThrowUtils.throwIf(request.getProbationRatio().compareTo(new java.math.BigDecimal("0.8")) < 0 || request.getProbationRatio().compareTo(new java.math.BigDecimal("1.0")) > 0, ErrorCode.PARAMS_ERROR, "试用期比例需在 0.8~1.0 之间");
        if (request.getBaseSalary() != null)
            ThrowUtils.throwIf(request.getBaseSalary().compareTo(java.math.BigDecimal.ZERO) < 0, ErrorCode.PARAMS_ERROR, "基本工资不能为负数");
        if (request.getContractType() != null && request.getContractType() == 1)
            ThrowUtils.throwIf(request.getContractExpireDate() == null, ErrorCode.PARAMS_ERROR, "固定期限合同到期日必填");

        String employeeNo = generateEmployeeNo(request.getDepartmentId());
        Employee emp = new Employee();
        emp.setEmployeeName(name);
        emp.setEmployeeNo(employeeNo);
        emp.setGender(request.getGender());
        emp.setPhone(phone);
        emp.setEmail(email);
        emp.setDepartmentId(request.getDepartmentId());
        emp.setPositionId(request.getPositionId());
        emp.setHireDate(request.getHireDate());
        emp.setEmploymentType(request.getEmploymentType());
        // 账号先用手机号占位，创建 User 后更新 userId
        emp.setAccount(phone);
        this.save(emp);

        EmployeeDetail detail = new EmployeeDetail();
        detail.setEmployeeId(emp.getId());
        detail.setIdCard(request.getIdCard());
        detail.setBirthday(request.getBirthday());
        detail.setRegisteredAddress(request.getRegisteredAddress());
        detail.setCurrentAddress(request.getCurrentAddress());
        detail.setDirectReportId(request.getDirectReportId());
        detail.setWorkLocation(request.getWorkLocation());
        detail.setContractType(request.getContractType());
        detail.setContractExpireDate(request.getContractExpireDate());
        detail.setProbationRatio(request.getProbationRatio());
        detail.setBaseSalary(request.getBaseSalary());
        detail.setBankAccount(request.getBankAccount());
        detail.setBankName(request.getBankName());
        detail.setJobLevel(request.getJobLevel());
        detail.setEmergencyContactName(request.getEmergencyContactName());
        detail.setEmergencyContactPhone(request.getEmergencyContactPhone());
        employeeDetailMapper.insert(detail);

        // 自动创建系统账号
        User user = new User();
        user.setUserAccount(phone);
        // 初始密码统一为 12345678
        String initPwd = "12345678";
        // 如需启用随机密码，取消下行注释即可（generateRandomPwd() 方法已保留在下方）
        // String initPwd = generateRandomPwd();
        user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + initPwd).getBytes()));
        user.setUserName(name);
        // roleId 不在此处设置，由管理员后续通过角色分配接口指定
        userMapper.insert(user);
        emp.setUserId(user.getId());
        emp.setAccount(phone);
        this.updateById(emp);

        log.info("员工创建成功: id={}, name={}, no={}, account={}", emp.getId(), name, employeeNo, phone);
        return emp.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(EmployeeUpdateRequest request) {
        Employee emp = this.getById(request.getId());
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工不存在");

        // 锁定字段拦截
        // 手机号可直接修改
        if (request.getDepartmentId() != null && !request.getDepartmentId().equals(emp.getDepartmentId()))
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "部门不可直接修改，需走调岗流程");
        if (request.getPositionId() != null && !request.getPositionId().equals(emp.getPositionId()))
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "职位不可直接修改，需走调岗流程");
        if (request.getJobLevel() != null && !Objects.equals(request.getJobLevel(), emp.getJobLevel()))
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "职级不可直接修改，需走调岗流程");
        if (request.getHireDate() != null && !request.getHireDate().equals(emp.getHireDate()))
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "入职日期不可直接修改，需走调岗流程");
        if (request.getEmploymentType() != null && !request.getEmploymentType().equals(emp.getEmploymentType()))
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "录用类型不可直接修改，需走调岗流程");
        if (request.getDirectReportId() != null) {
            EmployeeDetail ed = getDetailByEmployeeId(emp.getId());
            if (ed != null && !Objects.equals(request.getDirectReportId(), ed.getDirectReportId()))
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "直接汇报人不可直接修改，需走调岗流程");
        }
        if (request.getWorkLocation() != null) {
            EmployeeDetail ed = getDetailByEmployeeId(emp.getId());
            if (ed != null && !Objects.equals(request.getWorkLocation(), ed.getWorkLocation()))
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "工作地点不可直接修改，需走调岗流程");
        }

        // 记录变更历史（先于唯一性校验定义，供校验使用）
        Employee oldEmp = new Employee();
        BeanUtils.copyProperties(emp, oldEmp);
        EmployeeDetail oldDetail = getDetailByEmployeeId(emp.getId());

        // 唯一性校验
        if (request.getPhone() != null && !request.getPhone().equals(emp.getPhone())) {
            long phoneDup = this.count(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getPhone, request.getPhone())
                    .ne(Employee::getId, emp.getId()));
            ThrowUtils.throwIf(phoneDup > 0, ErrorCode.OPERATION_ERROR, "手机号 " + request.getPhone() + " 已被其他员工使用");
        }
        if (request.getIdCard() != null && !request.getIdCard().equals(oldDetail != null ? oldDetail.getIdCard() : null)) {
            long idCardDup = employeeDetailMapper.selectCount(new LambdaQueryWrapper<EmployeeDetail>()
                    .eq(EmployeeDetail::getIdCard, request.getIdCard())
                    .ne(oldDetail != null, EmployeeDetail::getId, oldDetail != null ? oldDetail.getId() : null));
            ThrowUtils.throwIf(idCardDup > 0, ErrorCode.OPERATION_ERROR, "身份证号已存在");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && !request.getEmail().equals(emp.getEmail())) {
            long emailDup = this.count(new LambdaQueryWrapper<Employee>()
                    .eq(Employee::getEmail, request.getEmail())
                    .ne(Employee::getId, emp.getId()));
            ThrowUtils.throwIf(emailDup > 0, ErrorCode.OPERATION_ERROR, "邮箱 " + request.getEmail() + " 已被其他员工使用");
        }
        if (request.getBankAccount() != null && !request.getBankAccount().equals(oldDetail != null ? oldDetail.getBankAccount() : null)) {
            long bankDup = employeeDetailMapper.selectCount(new LambdaQueryWrapper<EmployeeDetail>()
                    .eq(EmployeeDetail::getBankAccount, request.getBankAccount())
                    .ne(oldDetail != null, EmployeeDetail::getId, oldDetail != null ? oldDetail.getId() : null));
            ThrowUtils.throwIf(bankDup > 0, ErrorCode.OPERATION_ERROR, "银行账号已存在");
        }

        if (request.getEmployeeName() != null) emp.setEmployeeName(request.getEmployeeName().trim());
        if (request.getGender() != null) emp.setGender(request.getGender());
        if (request.getPhone() != null) {
            emp.setPhone(request.getPhone());
            emp.setAccount(request.getPhone());
            // 同步更新系统登录账号
            if (emp.getUserId() != null) {
                User sysUser = userMapper.selectById(emp.getUserId());
                if (sysUser != null) {
                    sysUser.setUserAccount(request.getPhone());
                    userMapper.updateById(sysUser);
                }
            }
        }
        if (request.getEmail() != null) emp.setEmail(request.getEmail());
        this.updateById(emp);

        EmployeeDetail detail = getOrCreateDetail(emp.getId());
        // 手动赋值详情表字段（禁止 BeanUtils.copyProperties，request.id 会覆盖 detail.id）
        if (request.getIdCard() != null) detail.setIdCard(request.getIdCard());
        if (request.getBirthday() != null) detail.setBirthday(request.getBirthday());
        if (request.getRegisteredAddress() != null) detail.setRegisteredAddress(request.getRegisteredAddress());
        if (request.getCurrentAddress() != null) detail.setCurrentAddress(request.getCurrentAddress());
        if (request.getDirectReportId() != null) detail.setDirectReportId(request.getDirectReportId());
        if (request.getWorkLocation() != null) detail.setWorkLocation(request.getWorkLocation());
        if (request.getContractType() != null) detail.setContractType(request.getContractType());
        if (request.getContractExpireDate() != null) detail.setContractExpireDate(request.getContractExpireDate());
        if (request.getProbationRatio() != null) detail.setProbationRatio(request.getProbationRatio());
        if (request.getBaseSalary() != null) detail.setBaseSalary(request.getBaseSalary());
        if (request.getBankAccount() != null) detail.setBankAccount(request.getBankAccount());
        if (request.getBankName() != null) detail.setBankName(request.getBankName());
        if (request.getEmergencyContactName() != null) detail.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) detail.setEmergencyContactPhone(request.getEmergencyContactPhone());
        employeeDetailMapper.updateById(detail);

        // 写变更日志
        logChanges(oldEmp, emp, oldDetail, detail, 1L);

        log.info("员工更新成功: id={}", emp.getId());
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee emp = this.getById(id);
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工不存在");
        this.removeById(id);
        log.info("员工删除成功: id={}", id);
    }

    @Override
    public String generateEmployeeNo(Long departmentId) {
        String year = new SimpleDateFormat("yyyy").format(new Date());
        String deptCode = "00";
        if (departmentId != null) {
            Department dept = departmentMapper.selectById(departmentId);
            if (dept != null) deptCode = dept.getDeptCode();
        }
        String prefix = year + deptCode;
        long maxSeq = 0;
        try {
            QueryWrapper<Employee> wrapper = new QueryWrapper<>();
            wrapper.likeRight("employeeNo", prefix).orderByDesc("employeeNo").last("LIMIT 1");
            Employee last = this.getOne(wrapper);
            if (last != null && last.getEmployeeNo().length() == 9)
                maxSeq = Long.parseLong(last.getEmployeeNo().substring(6));
        } catch (Exception e) { maxSeq = 0; }
        return prefix + String.format("%03d", maxSeq + 1);
    }

    @Override
    public FieldPermissionVO getFieldPermissions() {
        FieldPermissionVO vo = new FieldPermissionVO();
        vo.setEditableFields(new ArrayList<>(EDITABLE_FIELDS));
        vo.setLockedFields(new ArrayList<>(LOCKED_FIELDS));
        return vo;
    }
    public Page<EmployeeChangeLogVO> getChangeLogs(Long employeeId, int page, int size) {
        LambdaQueryWrapper<EmployeeChangeLog> wrapper = new LambdaQueryWrapper<EmployeeChangeLog>()
                .eq(EmployeeChangeLog::getEmployeeId, employeeId)
                .orderByDesc(EmployeeChangeLog::getCreateTime);
        Page<EmployeeChangeLog> logPage = employeeChangeLogService.page(new Page<>(page, size), wrapper);
        Page<EmployeeChangeLogVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        voPage.setRecords(logPage.getRecords().stream().map(log -> {
            EmployeeChangeLogVO vo = new EmployeeChangeLogVO();
            BeanUtils.copyProperties(log, vo);
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<EmployeeChangeLogVO> getMyChangeLogs(Long loginUserId, Long targetEmployeeId, int page, int size) {
        Long employeeId = targetEmployeeId;
        if (employeeId == null) {
            Employee emp = getByUserId(loginUserId);
            employeeId = emp != null ? emp.getId() : null;
        }
        return getChangeLogs(employeeId, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public java.util.Date updateMyDetail(Long loginUserId, MyDetailUpdateRequest request) {
        Employee loginEmp = getByUserId(loginUserId);
        ThrowUtils.throwIf(!loginEmp.getId().equals(request.getEmployeeId()),
                ErrorCode.NO_AUTH_ERROR, "只能修改本人的档案信息");

        EmployeeDetail detail = getOrCreateDetail(loginEmp.getId());

        EmployeeChangeLog log = new EmployeeChangeLog();
        log.setEmployeeId(loginEmp.getId());
        log.setOperatorId(loginEmp.getId());
        log.setChangeType("SELF_EDIT");

        checkAndLog(detail.getCurrentAddress(), request.getCurrentAddress(), "currentAddress", log);
        checkAndLog(detail.getEmergencyContactName(), request.getEmergencyContactName(), "emergencyContactName", log);
        checkAndLog(detail.getEmergencyContactPhone(), request.getEmergencyContactPhone(), "emergencyContactPhone", log);

        if (request.getCurrentAddress() != null) detail.setCurrentAddress(request.getCurrentAddress());
        if (request.getEmergencyContactName() != null) detail.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) detail.setEmergencyContactPhone(request.getEmergencyContactPhone());
        employeeDetailMapper.updateById(detail);

        return new java.util.Date();
    }

    @Override
    public MyProfileVO getMyFullProfile(Long loginUserId, Long targetEmployeeId, boolean isAdminOrHR) {
        Employee loginEmp = getByUserId(loginUserId);

        // 确定查询目标员工
        Employee targetEmp;
        if (targetEmployeeId == null || targetEmployeeId.equals(loginEmp.getId())) {
            targetEmp = loginEmp;
        } else {
            ThrowUtils.throwIf(!isAdminOrHR, ErrorCode.NO_AUTH_ERROR, "无权限查看他人档案");
            targetEmp = this.getById(targetEmployeeId);
            ThrowUtils.throwIf(targetEmp == null, ErrorCode.NOT_FOUND_ERROR, "目标员工不存在");
        }

        EmployeeDetail detail = getDetailByEmployeeId(targetEmp.getId());
        Department department = departmentService.getById(targetEmp.getDepartmentId());
        Position position = positionService.getById(targetEmp.getPositionId());
        EmpSalaryProfile salary = salaryProfileService.lambdaQuery()
                .eq(EmpSalaryProfile::getEmployeeId, targetEmp.getId()).one();

        MyProfileVO vo = new MyProfileVO();
        BeanUtils.copyProperties(targetEmp, vo);
        if (detail != null) {
            BeanUtils.copyProperties(detail, vo);
            vo.setIdCard(maskIdCard(detail.getIdCard()));
            vo.setEmergencyContactPhone(maskPhone(detail.getEmergencyContactPhone()));
        }
        if (department != null) vo.setDepartmentName(department.getDeptName());
        if (position != null) vo.setPositionName(position.getName());
        if (salary != null) vo.setBaseSalary(salary.getBaseSalary());
        vo.setPhone(maskPhone(targetEmp.getPhone()));
        vo.setIsSensitiveRole(isAdminOrHR);
        vo.setEditableFields(EDITABLE_FIELDS);

        return vo;
    }

    // ==================== 个人中心 ====================

    @Override
    public EmpProfileVO getProfile(Long userId) {
        Employee emp = getByUserId(userId);
        EmployeeDetail detail = getDetailByEmployeeId(emp.getId());

        Department department = departmentService.getById(emp.getDepartmentId());
        Position position = positionService.getById(emp.getPositionId());
        EmpSalaryProfile salary = salaryProfileService.lambdaQuery()
                .eq(EmpSalaryProfile::getEmployeeId, emp.getId()).one();
        EmpProfileVO vo = new EmpProfileVO();
        BeanUtils.copyProperties(emp, vo);
        if (detail != null) BeanUtils.copyProperties(detail, vo);
        if (department != null) vo.setDepartmentName(department.getDeptName());
        if (position != null) vo.setPositionName(position.getName());
        if (salary != null) vo.setBaseSalary(salary.getBaseSalary());
        vo.setIdCard(maskIdCard(detail != null ? detail.getIdCard() : null));
        vo.setPhone(maskPhone(emp.getPhone()));
        vo.setEditableFields(EDITABLE_FIELDS);
        //将employmentType字段设置为字符串
        vo.setEmploymentType(EmploymentType.getDesc(vo.getEmploymentType()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, EmpProfileUpdateRequest request) {
        Employee emp = getByUserId(userId);
        EmployeeDetail detail = getOrCreateDetail(emp.getId());
        if (request.getEmail() != null) { emp.setEmail(request.getEmail()); updateById(emp); }
        if (request.getCurrentAddress() != null) detail.setCurrentAddress(request.getCurrentAddress());
        if (request.getEmergencyContactName() != null) detail.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) detail.setEmergencyContactPhone(request.getEmergencyContactPhone());
        employeeDetailMapper.updateById(detail);
    }

    @Override
    public Employee getByUserId(Long userId) {
        Employee emp = this.lambdaQuery().eq(Employee::getUserId, userId).one();
        if (emp == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
        return emp;
    }

    // ==================== 私有辅助 ====================

    private EmployeeDetail getDetailByEmployeeId(Long employeeId) {
        return employeeDetailMapper.selectOne(new QueryWrapper<EmployeeDetail>().eq("employeeId", employeeId));
    }

    private EmployeeDetail getOrCreateDetail(Long employeeId) {
        EmployeeDetail detail = getDetailByEmployeeId(employeeId);
        if (detail == null) {
            detail = new EmployeeDetail();
            detail.setEmployeeId(employeeId);
            employeeDetailMapper.insert(detail);
        }
        return detail;
    }

    private String generateRandomPwd() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    private void logChanges(Employee oldEmp, Employee newEmp, EmployeeDetail oldDetail, EmployeeDetail newDetail, Long operatorId) {
        EmployeeChangeLog log = new EmployeeChangeLog();
        log.setEmployeeId(newEmp.getId());
        log.setOperatorId(operatorId);
        log.setChangeType("DIRECT_EDIT");
        checkAndLog(oldEmp.getEmployeeName(), newEmp.getEmployeeName(), "employeeName", log);
        checkAndLog(oldEmp.getGender(), newEmp.getGender(), "gender", log);
        checkAndLog(oldEmp.getEmail(), newEmp.getEmail(), "email", log);
        if (oldDetail != null && newDetail != null) {
            checkAndLog(oldDetail.getCurrentAddress(), newDetail.getCurrentAddress(), "currentAddress", log);
            checkAndLog(oldDetail.getEmergencyContactName(), newDetail.getEmergencyContactName(), "emergencyContactName", log);
            checkAndLog(oldDetail.getEmergencyContactPhone(), newDetail.getEmergencyContactPhone(), "emergencyContactPhone", log);
        }
    }

    private void checkAndLog(Object oldVal, Object newVal, String fieldName, EmployeeChangeLog log) {
        if (!Objects.equals(oldVal, newVal)) {
            log.setFieldName(fieldName);
            log.setOldValue(oldVal != null ? oldVal.toString() : null);
            log.setNewValue(newVal != null ? newVal.toString() : null);
            employeeChangeLogService.save(log);
        }
    }

    private String[] getNullPropertyNames(Object source) {
        final java.beans.PropertyDescriptor[] pds = org.springframework.beans.BeanUtils.getPropertyDescriptors(source.getClass());
        return java.util.Arrays.stream(pds)
                .filter(pd -> {
                    try { return pd.getReadMethod() != null && pd.getReadMethod().invoke(source) == null; }
                    catch (Exception e) { return false; }
                })
                .map(java.beans.PropertyDescriptor::getName)
                .toArray(String[]::new);
    }

    private boolean isNotEmpty(java.util.Collection<?> col) { return col != null && !col.isEmpty(); }

    private String getDeptName(Long deptId) {
        if (deptId == null) return null;
        Department d = departmentMapper.selectById(deptId);
        return d != null ? d.getDeptName() : null;
    }

    private String getPosName(Long posId) {
        if (posId == null) return null;
        Position p = positionMapper.selectById(posId);
        return p != null ? p.getName() : null;
    }

    /**
     * 按角色 dataScope 对员工详情进行字段脱敏
     */
    private void applyFieldMasking(EmployeeDetailVO vo, Integer dataScope) {
        // ALL(1) 和 ALL_EMPLOYEE(2)：已有的默认脱敏（手机号/身份证部分掩码）已够用
        if (dataScope == DataScopeEnum.ALL.getCode() ||
                dataScope == DataScopeEnum.ALL_EMPLOYEE.getCode()) {
            return;
        }

        if (dataScope == DataScopeEnum.DEPARTMENT.getCode()) {
            // 部门管理员：隐藏薪资合同、身份证等敏感信息
            vo.setIdCard(null);
            vo.setBirthday(null);
            vo.setRegisteredAddress(null);
            vo.setContractType(null);
            vo.setContractTypeDesc(null);
            vo.setContractExpireDate(null);
            vo.setProbationRatio(null);
            vo.setBaseSalary(null);
            vo.setBankAccount(null);
            vo.setBankName(null);
            return;
        }

        if (dataScope == DataScopeEnum.SALARY.getCode()) {
            // 财务专员：隐藏联系方式、住址、紧急联系人
            vo.setPhone(null);
            vo.setEmail(null);
            vo.setIdCard(null);
            vo.setBirthday(null);
            vo.setRegisteredAddress(null);
            vo.setCurrentAddress(null);
            vo.setEmergencyContactName(null);
            vo.setEmergencyContactPhone(null);
            return;
        }

        // SELF(5) 非本人的情况已经在 getDetail 中拦截，不会走到这里
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + idCard.substring(4, idCard.length() - 4).replaceAll(".", "*") + idCard.substring(idCard.length() - 4);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskBankAccount(String bankAccount) {
        if (bankAccount == null || bankAccount.length() < 4) return bankAccount;
        return "****" + bankAccount.substring(bankAccount.length() - 4);
    }
}
