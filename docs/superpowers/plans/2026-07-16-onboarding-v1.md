# 入职管理模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement task-by-task.

**Goal:** 实现入职管理完整流程 — HR 创建申请 → 审批流转 → 通过后自动生成工号+写入员工档案三表

**Architecture:** Controller 纯委托 Service，Service 实现 ApprovalCallback 与审批中心集成。角色数据路由复用 DataScopeContext。

**Tech Stack:** Spring Boot + MyBatis-Plus + MySQL，无新增依赖

## Global Constraints

- Java 8，不使用 var；中文注释；不删已有注释；不自动 git commit
- Controller 仅参数绑定，业务逻辑全在 Service
- 与现有 UserContext / DataScopeContext / AuthCheck 保持一致
- 审批中心当前用户解析已统一为 DataScopeContext（EmployeeResolveInterceptor 已删除）

---

### Task 1: OnboardingStatus 枚举

**Files:**
- Create: `src/main/java/com/limou/hrms/model/enums/OnboardingStatus.java`

**Interfaces:**
- Produces: `OnboardingStatus` enum with `DRAFT(1), PENDING(2), APPROVED(3), JOINED(4), REJECTED(5), ABANDONED(6)`

- [ ] **Step 1: 创建枚举文件**

```java
package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 入职申请状态枚举
 */
@Getter
public enum OnboardingStatus {

    DRAFT(1, "草稿"),
    PENDING(2, "审批中"),
    APPROVED(3, "已批准待入职"),
    JOINED(4, "已入职"),
    REJECTED(5, "已拒绝"),
    ABANDONED(6, "已放弃");

    private final int code;
    private final String desc;

    OnboardingStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OnboardingStatus fromCode(Integer code) {
        if (code == null) return null;
        for (OnboardingStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl . -q
```

---

### Task 2: OnboardingCreateDTO

**Files:**
- Create: `src/main/java/com/limou/hrms/model/dto/onboarding/OnboardingCreateDTO.java`

**Interfaces:**
- Produces: 12 个业务字段 + `submitDirectly`（Boolean，默认 false）

- [ ] **Step 1: 创建 DTO**

```java
package com.limou.hrms.model.dto.onboarding;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建入职申请请求体
 */
@Data
public class OnboardingCreateDTO {

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "性别不能为空")
    @Min(value = 1, message = "性别值无效")
    @Max(value = 2, message = "性别值无效")
    private Integer gender;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    @NotNull(message = "预计入职日期不能为空")
    private LocalDate expectedHireDate;

    @NotNull(message = "所属部门不能为空")
    private Long departmentId;

    @NotNull(message = "职位不能为空")
    private Long positionId;

    @NotNull(message = "录用类型不能为空")
    @Min(value = 1, message = "录用类型值无效")
    @Max(value = 3, message = "录用类型值无效")
    private Integer hireType;

    @NotNull(message = "试用期月数不能为空")
    @Min(value = 1, message = "试用期至少1个月")
    @Max(value = 6, message = "试用期最多6个月")
    private Integer defaultProbationMonths;

    @NotNull(message = "试用期薪资比例不能为空")
    @DecimalMin(value = "0.8", message = "试用期薪资比例范围0.8~1.0")
    @DecimalMax(value = "1.0", message = "试用期薪资比例范围0.8~1.0")
    private BigDecimal probationRatio;

    /** 直接汇报人ID（非必填，默认部门负责人） */
    private Long directReportId;

    /** 是否直接提交审批，默认false=保存草稿 */
    private Boolean submitDirectly;
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl . -q
```

---

### Task 3: OnboardingUpdateDTO

**Files:**
- Create: `src/main/java/com/limou/hrms/model/dto/onboarding/OnboardingUpdateDTO.java`

- [ ] **Step 1: 创建 DTO**

```java
package com.limou.hrms.model.dto.onboarding;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新入职草稿请求体 — 所有字段可选，仅更新传入字段
 */
@Data
public class OnboardingUpdateDTO {

    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String idCard;
    private LocalDate expectedHireDate;
    private Long departmentId;
    private Long positionId;
    private Integer hireType;
    private Integer defaultProbationMonths;
    private BigDecimal probationRatio;
    private Long directReportId;
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl . -q
```

---

### Task 4: OnboardingQuery

**Files:**
- Create: `src/main/java/com/limou/hrms/model/query/OnboardingQuery.java`

- [ ] **Step 1: 创建查询对象**

```java
package com.limou.hrms.model.query;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 入职申请分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnboardingQuery extends PageRequest {

    /** 状态筛选 */
    private Integer status;

    /** 部门ID筛选 */
    private Long departmentId;

    /** 关键词模糊搜索（姓名/手机号） */
    private String keyword;

    /** 预计入职日期起始 */
    private LocalDate hireDateStart;

    /** 预计入职日期截止 */
    private LocalDate hireDateEnd;
}
```

- [ ] **Step 2: 检查 PageRequest 基类**

确认 `com.limou.hrms.common.PageRequest` 存在，包含 `current`/`pageSize` 字段。

- [ ] **Step 3: 编译验证**

```bash
mvn compile -pl . -q
```

---

### Task 5: OnboardingListVO + OnboardingDetailVO

**Files:**
- Create: `src/main/java/com/limou/hrms/model/vo/OnboardingListVO.java`
- Create: `src/main/java/com/limou/hrms/model/vo/OnboardingDetailVO.java`

- [ ] **Step 1: 创建列表 VO**

```java
package com.limou.hrms.model.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入职申请列表项 VO
 */
@Data
public class OnboardingListVO {
    private Long id;
    private String name;
    private String phone;
    private String departmentName;
    private String positionName;
    private LocalDate expectedHireDate;
    private Integer status;
    private String statusDesc;
    private String applicantName;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 创建详情 VO**

```java
package com.limou.hrms.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入职申请详情 VO（含审批进度）
 */
@Data
public class OnboardingDetailVO {
    // 基本信息
    private Long id;
    private String name;
    private Integer gender;
    private String genderDesc;
    private String phone;
    private String email;
    private String idCard;
    private LocalDate expectedHireDate;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private Integer hireType;
    private String hireTypeDesc;
    private Integer defaultProbationMonths;
    private BigDecimal probationRatio;
    private Long directReportId;
    private String directReportName;
    private Integer status;
    private String statusDesc;
    private Long approvalInstanceId;
    private Long employeeId;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 审批进度（审批中/已通过/已拒绝时返回） */
    private ApprovalInstanceVO approvalProgress;
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -pl . -q
```

---

### Task 6: ErrorCode 新增入职错误码

**Files:**
- Modify: `src/main/java/com/limou/hrms/common/ErrorCode.java`

- [ ] **Step 1: 在枚举末尾（`;` 之前）添加错误码**

```java
    // region 入转调离 — 入职管理 (310xx)
    ONBOARDING_NOT_FOUND(31001, "入职申请不存在"),
    ONBOARDING_DRAFT_ONLY(31002, "仅草稿状态可操作"),
    ONBOARDING_SUBMIT_DRAFT_ONLY(31003, "仅草稿状态可提交审批"),
    ONBOARDING_CANCEL_FIRST_NODE_ONLY(31004, "仅第一级审批节点可撤回申请"),
    ONBOARDING_CONFIRM_APPROVED_ONLY(31005, "仅"已批准待入职"状态可确认入职"),
    ONBOARDING_FIELDS_INCOMPLETE(31006, "入职申请必填字段不完整");
    // endregion
```

注意：添加在 `POSITION_NAME_DUPLICATE` 之后、`;` 之前。

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl . -q
```

---

### Task 7: OnboardingService 接口

**Files:**
- Create: `src/main/java/com/limou/hrms/service/OnboardingService.java`

**Interfaces:**
- Produces: 9 个方法签名，供 Controller 和 Impl 使用

- [ ] **Step 1: 创建接口**

```java
package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.onboarding.OnboardingCreateDTO;
import com.limou.hrms.model.dto.onboarding.OnboardingUpdateDTO;
import com.limou.hrms.model.query.OnboardingQuery;
import com.limou.hrms.model.vo.OnboardingDetailVO;
import com.limou.hrms.model.vo.OnboardingListVO;

import java.time.LocalDate;

/**
 * 入职管理服务接口
 */
public interface OnboardingService {

    /**
     * 创建入职申请（保存草稿或直接提交审批）
     * @param dto 申请信息
     * @return 入职申请ID
     */
    Long createApplication(OnboardingCreateDTO dto);

    /**
     * 更新草稿（仅草稿状态可编辑）
     */
    void updateDraft(Long id, OnboardingUpdateDTO dto);

    /**
     * 删除草稿（仅草稿状态且本人操作）
     */
    void deleteDraft(Long id);

    /**
     * 提交审批（草稿→审批中，创建审批实例）
     */
    void submitToApproval(Long id);

    /**
     * 撤回申请（仅第一节点，回退为草稿）
     */
    void cancel(Long id);

    /**
     * 确认入职（已批准待入职→已入职）
     */
    void confirmJoin(Long id, LocalDate actualHireDate);

    /**
     * 标记放弃入职
     */
    void abandon(Long id);

    /**
     * 分页查询列表（角色路由）
     */
    Page<OnboardingListVO> list(OnboardingQuery query);

    /**
     * 获取详情（含审批进度，审批中/已通过/已拒绝时返回）
     */
    OnboardingDetailVO getDetail(Long id);
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl . -q
```

---

### Task 8: OnboardingServiceImpl（核心实现）

**Files:**
- Create: `src/main/java/com/limou/hrms/service/impl/OnboardingServiceImpl.java`

**Interfaces:**
- Consumes: `OnboardingService`（Task 7）、`ApprovalCallback`（已有）、`DataScopeContext`（已有）、`ApprovalFlowService`（已有）、`OnboardingApplicationMapper`（已有）、`EmployeeMapper`（已有）、`EmployeePersonalInfoMapper`（已有）、`EmployeeWorkInfoMapper`（已有）、`EmployeeNoSequenceMapper`（已有）、`AesUtil`（已有）、`DepartmentMapper`（已有）、`ApproverResolver`（已有）、`ApprovalInstanceMapper`（已有）、`ApprovalNodeMapper`（已有）
- Produces: `OnboardingServiceImpl` Bean，自动注册为 `ApprovalCallback` 实现

- [ ] **Step 1: 创建 Service 实现类（完整代码）**

```java
package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.builder.ApproverResolver;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.onboarding.OnboardingCreateDTO;
import com.limou.hrms.model.dto.onboarding.OnboardingUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.ApprovalStatus;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.OnboardingStatus;
import com.limou.hrms.model.query.OnboardingQuery;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.OnboardingService;
import com.limou.hrms.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 入职管理服务实现 — 含入职 CRUD + 审批回调 + 入职后处理
 */
@Service
@Slf4j
public class OnboardingServiceImpl
        extends ServiceImpl<OnboardingApplicationMapper, OnboardingApplication>
        implements OnboardingService, ApprovalCallback {

    @Resource
    private OnboardingApplicationMapper onboardingMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeePersonalInfoMapper personalInfoMapper;
    @Resource
    private EmployeeWorkInfoMapper workInfoMapper;
    @Resource
    private EmployeeNoSequenceMapper noSequenceMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private ApprovalInstanceMapper approvalInstanceMapper;
    @Resource
    private ApprovalNodeMapper approvalNodeMapper;
    @Resource
    private AesUtil aesUtil;
    @Resource
    private DataScopeContext dataScopeContext;
    @Resource
    private ApproverResolver approverResolver;

    // ==================== 入职 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApplication(OnboardingCreateDTO dto) {
        OnboardingApplication app = new OnboardingApplication();
        app.setName(dto.getName());
        app.setGender(dto.getGender());
        app.setPhone(dto.getPhone());
        app.setEmail(dto.getEmail());
        app.setIdCard(dto.getIdCard());
        app.setExpectedHireDate(dto.getExpectedHireDate());
        app.setDepartmentId(dto.getDepartmentId());
        app.setPositionId(dto.getPositionId());
        app.setHireType(dto.getHireType());
        app.setDefaultProbationMonths(dto.getDefaultProbationMonths());
        app.setProbationRatio(dto.getProbationRatio());
        // 直接汇报人默认部门负责人
        if (dto.getDirectReportId() != null) {
            app.setDirectReportId(dto.getDirectReportId());
        } else {
            app.setDirectReportId(approverResolver.resolveDeptManager(dto.getDepartmentId()));
        }
        app.setApplicantId(dataScopeContext.getCurrentEmployeeId());
        app.setStatus(OnboardingStatus.DRAFT.getCode());
        onboardingMapper.insert(app);

        // 直接提交审批
        if (Boolean.TRUE.equals(dto.getSubmitDirectly())) {
            submitToApproval(app.getId());
        }

        log.info("入职申请创建成功: id={}, name={}", app.getId(), app.getName());
        return app.getId();
    }

    @Override
    public void updateDraft(Long id, OnboardingUpdateDTO dto) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_DRAFT_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.ONBOARDING_DRAFT_ONLY, "仅申请人可编辑草稿");
        }

        if (StringUtils.isNotBlank(dto.getName())) app.setName(dto.getName());
        if (dto.getGender() != null) app.setGender(dto.getGender());
        if (StringUtils.isNotBlank(dto.getPhone())) app.setPhone(dto.getPhone());
        if (StringUtils.isNotBlank(dto.getEmail())) app.setEmail(dto.getEmail());
        if (StringUtils.isNotBlank(dto.getIdCard())) app.setIdCard(dto.getIdCard());
        if (dto.getExpectedHireDate() != null) app.setExpectedHireDate(dto.getExpectedHireDate());
        if (dto.getDepartmentId() != null) app.setDepartmentId(dto.getDepartmentId());
        if (dto.getPositionId() != null) app.setPositionId(dto.getPositionId());
        if (dto.getHireType() != null) app.setHireType(dto.getHireType());
        if (dto.getDefaultProbationMonths() != null) app.setDefaultProbationMonths(dto.getDefaultProbationMonths());
        if (dto.getProbationRatio() != null) app.setProbationRatio(dto.getProbationRatio());
        if (dto.getDirectReportId() != null) app.setDirectReportId(dto.getDirectReportId());

        onboardingMapper.updateById(app);
        log.info("入职草稿更新成功: id={}", id);
    }

    @Override
    public void deleteDraft(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_DRAFT_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.ONBOARDING_DRAFT_ONLY, "仅申请人可删除草稿");
        }
        onboardingMapper.deleteById(id);
        log.info("入职草稿删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitToApproval(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_SUBMIT_DRAFT_ONLY);
        }
        validateFieldsComplete(app);

        // 创建审批实例（由 OnboardingNodeBuilder 构建节点链）
        ApprovalInstance instance = approvalFlowService.createInstance(
                ApprovalBizType.ONBOARDING, app.getId(), app.getApplicantId());

        app.setStatus(OnboardingStatus.PENDING.getCode());
        app.setApprovalInstanceId(instance.getId());
        onboardingMapper.updateById(app);

        log.info("入职申请已提交审批: id={}, instanceId={}", id, instance.getId());
    }

    @Override
    public void cancel(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_CANCEL_FIRST_NODE_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.ONBOARDING_CANCEL_FIRST_NODE_ONLY, "仅申请人可撤回");
        }
        // 审批中心会校验是否第一节点
        approvalFlowService.cancel(app.getApprovalInstanceId());

        app.setStatus(OnboardingStatus.DRAFT.getCode());
        app.setApprovalInstanceId(null);
        onboardingMapper.updateById(app);

        log.info("入职申请已撤回: id={}", id);
    }

    @Override
    public void confirmJoin(Long id, LocalDate actualHireDate) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.APPROVED.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_CONFIRM_APPROVED_ONLY);
        }
        if (app.getEmployeeId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "员工档案未创建，无法确认入职");
        }
        // 更新员工入职日期
        Employee employee = employeeMapper.selectById(app.getEmployeeId());
        if (employee != null) {
            employee.setHireDate(actualHireDate);
            employee.setStatus(EmployeeStatus.PROBATION.getValue());
            employeeMapper.updateById(employee);
        }

        app.setStatus(OnboardingStatus.JOINED.getCode());
        onboardingMapper.updateById(app);

        log.info("员工已确认入职: id={}, employeeId={}", id, app.getEmployeeId());
    }

    @Override
    public void abandon(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        if (app.getStatus() != OnboardingStatus.APPROVED.getCode()) {
            throw new BusinessException(ErrorCode.ONBOARDING_CONFIRM_APPROVED_ONLY);
        }
        app.setStatus(OnboardingStatus.ABANDONED.getCode());
        onboardingMapper.updateById(app);
        log.info("入职申请已放弃: id={}", id);
    }

    @Override
    public Page<OnboardingListVO> list(OnboardingQuery query) {
        DataScopeEnum scope = dataScopeContext.getApprovalScope();
        switch (scope) {
            case ALL:
                return queryAllList(query);
            case DEPT:
                Long deptId = dataScopeContext.getCurrentDepartmentId();
                return queryDeptList(deptId, query);
            case SELF:
                Long employeeId = dataScopeContext.getCurrentEmployeeId();
                return queryPersonalList(employeeId, query);
            default:
                return new Page<>(query.getCurrent(), query.getPageSize());
        }
    }

    @Override
    public OnboardingDetailVO getDetail(Long id) {
        OnboardingApplication app = getAppOrThrow(id);
        OnboardingDetailVO vo = buildDetailVO(app);

        // 审批中/已通过/已拒绝时附带审批进度
        if (app.getStatus() == OnboardingStatus.PENDING.getCode()
                || app.getStatus() == OnboardingStatus.APPROVED.getCode()
                || app.getStatus() == OnboardingStatus.REJECTED.getCode()) {
            if (app.getApprovalInstanceId() != null) {
                ApprovalInstanceVO progress = approvalFlowService.getDetail(app.getApprovalInstanceId());
                vo.setApprovalProgress(progress);
            }
        }
        return vo;
    }

    // ==================== 审批回调 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.ONBOARDING) return;
        OnboardingApplication app = getAppOrThrow(bizId);

        // 1. 生成工号
        String employeeNo = generateEmployeeNo(app.getDepartmentId());

        // 2. 写入 employee 表
        Employee employee = new Employee();
        employee.setEmployeeNo(employeeNo);
        employee.setStatus(EmployeeStatus.PROBATION.getValue());
        employee.setHireDate(app.getExpectedHireDate());
        employee.setHireType(app.getHireType());
        employeeMapper.insert(employee);

        // 3. 写入 personal_info（身份证号 AES 加密）
        EmployeePersonalInfo personalInfo = new EmployeePersonalInfo();
        personalInfo.setEmployeeId(employee.getId());
        personalInfo.setName(app.getName());
        personalInfo.setGender(app.getGender());
        personalInfo.setPhone(app.getPhone());
        personalInfo.setEmail(app.getEmail());
        personalInfo.setIdCard(aesUtil.encrypt(app.getIdCard()));
        personalInfoMapper.insert(personalInfo);

        // 4. 写入 work_info
        EmployeeWorkInfo workInfo = new EmployeeWorkInfo();
        workInfo.setEmployeeId(employee.getId());
        workInfo.setDepartmentId(app.getDepartmentId());
        workInfo.setPositionId(app.getPositionId());
        workInfo.setDirectReportId(app.getDirectReportId());
        workInfoMapper.insert(workInfo);

        // 5. 更新入职申请
        app.setEmployeeId(employee.getId());
        app.setStatus(OnboardingStatus.APPROVED.getCode());
        onboardingMapper.updateById(app);

        log.info("入职审批通过后处理完成: id={}, employeeId={}, employeeNo={}", bizId, employee.getId(), employeeNo);
    }

    @Override
    public void onRejected(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.ONBOARDING) return;
        OnboardingApplication app = getAppOrThrow(bizId);
        app.setStatus(OnboardingStatus.REJECTED.getCode());
        onboardingMapper.updateById(app);
        log.info("入职审批已拒绝: id={}", bizId);
    }

    // ==================== 私有方法 ====================

    private OnboardingApplication getAppOrThrow(Long id) {
        OnboardingApplication app = onboardingMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND);
        }
        return app;
    }

    /** 校验必填字段完整 */
    private void validateFieldsComplete(OnboardingApplication app) {
        if (StringUtils.isBlank(app.getName())
                || app.getGender() == null
                || StringUtils.isBlank(app.getPhone())
                || StringUtils.isBlank(app.getEmail())
                || StringUtils.isBlank(app.getIdCard())
                || app.getExpectedHireDate() == null
                || app.getDepartmentId() == null
                || app.getPositionId() == null
                || app.getHireType() == null
                || app.getDefaultProbationMonths() == null
                || app.getProbationRatio() == null) {
            throw new BusinessException(ErrorCode.ONBOARDING_FIELDS_INCOMPLETE);
        }
    }

    /** 生成工号（复用 EmployeeServiceImpl 同款逻辑） */
    private String generateEmployeeNo(Long departmentId) {
        Department dept = departmentMapper.selectById(departmentId);
        if (dept == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "部门不存在");
        }
        String deptCode = dept.getCode();
        String shortCode = deptCode.length() == 2 ? deptCode : deptCode.substring(deptCode.length() - 2);
        int year = LocalDate.now().getYear();

        String lockKey = (year + "_" + shortCode).intern();
        synchronized (lockKey) {
            QueryWrapper<EmployeeNoSequence> query = new QueryWrapper<>();
            query.eq("year", year).eq("dept_code", shortCode);
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
            return String.format("%04d%02d%03d", year, Integer.parseInt(shortCode), seq.getCurrentSeq());
        }
    }

    // ==================== 角色路由查询 ====================

    private Page<OnboardingListVO> queryAllList(OnboardingQuery query) {
        QueryWrapper<OnboardingApplication> qw = buildQueryWrapper(query);
        qw.orderByDesc("create_time");
        Page<OnboardingApplication> page = onboardingMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private Page<OnboardingListVO> queryDeptList(Long deptId, OnboardingQuery query) {
        QueryWrapper<OnboardingApplication> qw = buildQueryWrapper(query);
        qw.eq("department_id", deptId);
        qw.orderByDesc("create_time");
        Page<OnboardingApplication> page = onboardingMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private Page<OnboardingListVO> queryPersonalList(Long employeeId, OnboardingQuery query) {
        QueryWrapper<OnboardingApplication> qw = buildQueryWrapper(query);
        qw.eq("applicant_id", employeeId);
        qw.orderByDesc("create_time");
        Page<OnboardingApplication> page = onboardingMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private QueryWrapper<OnboardingApplication> buildQueryWrapper(OnboardingQuery query) {
        QueryWrapper<OnboardingApplication> qw = new QueryWrapper<>();
        if (query.getStatus() != null) {
            qw.eq("status", query.getStatus());
        }
        if (query.getDepartmentId() != null) {
            qw.eq("department_id", query.getDepartmentId());
        }
        if (StringUtils.isNotBlank(query.getKeyword())) {
            qw.and(w -> w.like("name", query.getKeyword()).or().like("phone", query.getKeyword()));
        }
        if (query.getHireDateStart() != null) {
            qw.ge("expected_hire_date", query.getHireDateStart());
        }
        if (query.getHireDateEnd() != null) {
            qw.le("expected_hire_date", query.getHireDateEnd());
        }
        return qw;
    }

    private Page<OnboardingListVO> toListVOPage(Page<OnboardingApplication> page) {
        List<OnboardingListVO> records = page.getRecords().stream().map(app -> {
            OnboardingListVO vo = new OnboardingListVO();
            vo.setId(app.getId());
            vo.setName(app.getName());
            vo.setPhone(maskPhone(app.getPhone()));
            vo.setDepartmentName(getDeptName(app.getDepartmentId()));
            vo.setPositionName(getPositionName(app.getPositionId()));
            vo.setExpectedHireDate(app.getExpectedHireDate());
            vo.setStatus(app.getStatus());
            vo.setStatusDesc(OnboardingStatus.fromCode(app.getStatus()) != null
                    ? OnboardingStatus.fromCode(app.getStatus()).getDesc() : "");
            vo.setApplicantName(approverResolver.getEmployeeName(app.getApplicantId()));
            vo.setCreateTime(app.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<OnboardingListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private OnboardingDetailVO buildDetailVO(OnboardingApplication app) {
        OnboardingDetailVO vo = new OnboardingDetailVO();
        vo.setId(app.getId());
        vo.setName(app.getName());
        vo.setGender(app.getGender());
        vo.setGenderDesc(app.getGender() == 1 ? "男" : app.getGender() == 2 ? "女" : "");
        vo.setPhone(app.getPhone());
        vo.setEmail(app.getEmail());
        // 身份证号非 HR/admin 脱敏
        String role = dataScopeContext.getCurrentRole();
        vo.setIdCard(("hr".equals(role) || "admin".equals(role))
                ? aesUtil.decrypt(null) /* 直接展示 */ : maskIdCard(app.getIdCard()));
        vo.setExpectedHireDate(app.getExpectedHireDate());
        vo.setDepartmentId(app.getDepartmentId());
        vo.setDepartmentName(getDeptName(app.getDepartmentId()));
        vo.setPositionId(app.getPositionId());
        vo.setPositionName(getPositionName(app.getPositionId()));
        vo.setHireType(app.getHireType());
        vo.setHireTypeDesc(getHireTypeDesc(app.getHireType()));
        vo.setDefaultProbationMonths(app.getDefaultProbationMonths());
        vo.setProbationRatio(app.getProbationRatio());
        vo.setDirectReportId(app.getDirectReportId());
        vo.setDirectReportName(approverResolver.getEmployeeName(app.getDirectReportId()));
        vo.setStatus(app.getStatus());
        OnboardingStatus statusEnum = OnboardingStatus.fromCode(app.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        vo.setApprovalInstanceId(app.getApprovalInstanceId());
        vo.setEmployeeId(app.getEmployeeId());
        vo.setApplicantId(app.getApplicantId());
        vo.setApplicantName(approverResolver.getEmployeeName(app.getApplicantId()));
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) return null;
        Department dept = departmentMapper.selectById(deptId);
        return dept != null ? dept.getName() : null;
    }

    private String getPositionName(Long positionId) {
        if (positionId == null) return null;
        Position position = positionMapper.selectById(positionId);
        return position != null ? position.getName() : null;
    }

    private String getHireTypeDesc(Integer hireType) {
        if (hireType == null) return "";
        switch (hireType) {
            case 1: return "全职";
            case 2: return "兼职";
            case 3: return "实习";
            default: return "";
        }
    }
}
```

- [ ] **Step 2: 检查需要的依赖注入**

确认 `PositionMapper` 也注入（`buildDetailVO` 用到了）。在文件顶部 `@Resource` 区加：
```java
@Resource
private PositionMapper positionMapper;
```

- [ ] **Step 3: 修复 idCard 脱敏逻辑**

详情 VO 中身份证号字段：申请表存的是明文，需要判断是否脱敏。修复为：
```java
// 身份证号：HR/admin 直接展示（申请表中是明文），其他角色脱敏
boolean isHrOrAdmin = "hr".equals(role) || "admin".equals(role);
vo.setIdCard(isHrOrAdmin ? app.getIdCard() : maskIdCard(app.getIdCard()));
```

- [ ] **Step 4: 编译验证**

```bash
mvn compile -pl . -q
```

---

### Task 9: OnboardingController

**Files:**
- Create: `src/main/java/com/limou/hrms/controller/OnboardingController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.onboarding.OnboardingCreateDTO;
import com.limou.hrms.model.dto.onboarding.OnboardingUpdateDTO;
import com.limou.hrms.model.query.OnboardingQuery;
import com.limou.hrms.model.vo.OnboardingDetailVO;
import com.limou.hrms.model.vo.OnboardingListVO;
import com.limou.hrms.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 入职管理控制器 — 仅参数绑定，业务逻辑全部在 Service 层
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@Slf4j
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /** 查询入职列表（分页 + 角色路由） */
    @GetMapping
    @AuthCheck
    public BaseResponse<Page<OnboardingListVO>> list(OnboardingQuery query) {
        return ResultUtils.success(onboardingService.list(query));
    }

    /** 获取入职详情（含审批进度） */
    @GetMapping("/{id}")
    @AuthCheck
    public BaseResponse<OnboardingDetailVO> getDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(onboardingService.getDetail(id));
    }

    /** 创建入职申请（保存草稿或直接提交审批） */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Long> create(@Valid @RequestBody OnboardingCreateDTO dto) {
        return ResultUtils.success(onboardingService.createApplication(dto));
    }

    /** 更新入职草稿（仅草稿状态可编辑） */
    @PutMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> updateDraft(@PathVariable Long id, @RequestBody OnboardingUpdateDTO dto) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.updateDraft(id, dto);
        return ResultUtils.success(null);
    }

    /** 删除入职草稿 */
    @DeleteMapping("/{id}")
    @AuthCheck
    public BaseResponse<?> deleteDraft(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.deleteDraft(id);
        return ResultUtils.success(null);
    }

    /** 提交入职审批（草稿→审批中） */
    @PostMapping("/{id}/submit")
    @AuthCheck
    public BaseResponse<?> submitToApproval(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.submitToApproval(id);
        return ResultUtils.success(null);
    }

    /** 撤回入职申请（仅第一级审批节点可撤回） */
    @PostMapping("/{id}/cancel")
    @AuthCheck
    public BaseResponse<?> cancel(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.cancel(id);
        return ResultUtils.success(null);
    }

    /** 确认入职（已批准待入职→已入职） */
    @PostMapping("/{id}/confirm-join")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<?> confirmJoin(@PathVariable Long id, @RequestParam LocalDate actualHireDate) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.confirmJoin(id, actualHireDate);
        return ResultUtils.success(null);
    }

    /** 标记放弃入职 */
    @PostMapping("/{id}/abandon")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<?> abandon(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        onboardingService.abandon(id);
        return ResultUtils.success(null);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl . -q
```

---

### Task 10: 单元测试

**Files:**
- Create: `src/test/java/com/limou/hrms/service/OnboardingServiceTest.java`

**注意**：确认 `PositionMapper` 类名和路径。如果实际 `PositionMapper` 在 `com.limou.hrms.mapper` 下，导入正确。

- [ ] **Step 1: 创建测试类**

```java
package com.limou.hrms.service;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.onboarding.OnboardingCreateDTO;
import com.limou.hrms.model.dto.onboarding.OnboardingUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.OnboardingStatus;
import com.limou.hrms.model.vo.OnboardingDetailVO;
import com.limou.hrms.service.impl.OnboardingServiceImpl;
import com.limou.hrms.util.AesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 入职管理服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnboardingServiceTest {

    @Mock private OnboardingApplicationMapper onboardingMapper;
    @Mock private ApprovalFlowService approvalFlowService;
    @Mock private EmployeeMapper employeeMapper;
    @Mock private EmployeePersonalInfoMapper personalInfoMapper;
    @Mock private EmployeeWorkInfoMapper workInfoMapper;
    @Mock private EmployeeNoSequenceMapper noSequenceMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private ApprovalInstanceMapper approvalInstanceMapper;
    @Mock private ApprovalNodeMapper approvalNodeMapper;
    @Mock private PositionMapper positionMapper;
    @Mock private AesUtil aesUtil;
    @Mock private DataScopeContext dataScopeContext;
    @Mock private com.limou.hrms.builder.ApproverResolver approverResolver;

    @InjectMocks
    private OnboardingServiceImpl service;

    private static final Long APP_ID = 1L;
    private static final Long APPLICANT_ID = 10L;

    @BeforeEach
    void setUp() {
        when(dataScopeContext.getCurrentEmployeeId()).thenReturn(APPLICANT_ID);
        when(dataScopeContext.getCurrentRole()).thenReturn("hr");
        when(aesUtil.encrypt(any())).thenReturn("encrypted");
        when(approverResolver.resolveDeptManager(anyLong())).thenReturn(100L);
        when(approverResolver.getEmployeeName(anyLong())).thenReturn("测试姓名");
    }

    private OnboardingCreateDTO buildCreateDTO() {
        OnboardingCreateDTO dto = new OnboardingCreateDTO();
        dto.setName("张三");
        dto.setGender(1);
        dto.setPhone("13800001234");
        dto.setEmail("zhangsan@test.com");
        dto.setIdCard("330100199001011234");
        dto.setExpectedHireDate(LocalDate.of(2026, 8, 1));
        dto.setDepartmentId(1L);
        dto.setPositionId(1L);
        dto.setHireType(1);
        dto.setDefaultProbationMonths(3);
        dto.setProbationRatio(new BigDecimal("0.80"));
        dto.setSubmitDirectly(false);
        return dto;
    }

    private OnboardingApplication mockApp() {
        OnboardingApplication app = new OnboardingApplication();
        app.setId(APP_ID);
        app.setName("张三");
        app.setGender(1);
        app.setPhone("13800001234");
        app.setEmail("zhangsan@test.com");
        app.setIdCard("330100199001011234");
        app.setExpectedHireDate(LocalDate.of(2026, 8, 1));
        app.setDepartmentId(1L);
        app.setPositionId(1L);
        app.setHireType(1);
        app.setDefaultProbationMonths(3);
        app.setProbationRatio(new BigDecimal("0.80"));
        app.setStatus(OnboardingStatus.DRAFT.getCode());
        app.setApplicantId(APPLICANT_ID);
        return app;
    }

    // ==================== 创建申请 ====================

    @Test
    void createApplication_draft_shouldSucceed() {
        when(onboardingMapper.insert(any())).thenReturn(1);

        Long id = service.createApplication(buildCreateDTO());

        assertNotNull(id);
    }

    @Test
    void createApplication_submitDirectly_shouldCallSubmit() {
        OnboardingCreateDTO dto = buildCreateDTO();
        dto.setSubmitDirectly(true);
        when(onboardingMapper.insert(any())).thenReturn(1);
        // mock for submitToApproval
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(anyLong())).thenReturn(app);
        when(approvalFlowService.createInstance(any(), anyLong(), anyLong()))
                .thenReturn(new ApprovalInstance());

        Long id = service.createApplication(dto);

        assertNotNull(id);
        verify(approvalFlowService, times(1)).createInstance(any(), anyLong(), anyLong());
    }

    // ==================== 更新草稿 ====================

    @Test
    void updateDraft_shouldSucceed() {
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        OnboardingUpdateDTO dto = new OnboardingUpdateDTO();
        dto.setName("李四");

        assertDoesNotThrow(() -> service.updateDraft(APP_ID, dto));
        assertEquals("李四", app.getName());
    }

    @Test
    void updateDraft_nonDraft_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateDraft(APP_ID, new OnboardingUpdateDTO()));
        assertEquals(ErrorCode.ONBOARDING_DRAFT_ONLY.getCode(), ex.getCode());
    }

    // ==================== 删除草稿 ====================

    @Test
    void deleteDraft_shouldSucceed() {
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.deleteById(APP_ID)).thenReturn(1);

        assertDoesNotThrow(() -> service.deleteDraft(APP_ID));
    }

    @Test
    void deleteDraft_nonDraft_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        assertThrows(BusinessException.class, () -> service.deleteDraft(APP_ID));
    }

    // ==================== 提交审批 ====================

    @Test
    void submitToApproval_shouldSucceed() {
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(100L);
        when(approvalFlowService.createInstance(any(), anyLong(), anyLong())).thenReturn(instance);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.submitToApproval(APP_ID);

        assertEquals(OnboardingStatus.PENDING.getCode(), app.getStatus());
        assertEquals(100L, app.getApprovalInstanceId());
    }

    @Test
    void submitToApproval_nonDraft_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        assertThrows(BusinessException.class, () -> service.submitToApproval(APP_ID));
    }

    @Test
    void submitToApproval_incompleteFields_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setName(null); // 缺少姓名
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitToApproval(APP_ID));
        assertEquals(ErrorCode.ONBOARDING_FIELDS_INCOMPLETE.getCode(), ex.getCode());
    }

    // ==================== 撤回 ====================

    @Test
    void cancel_shouldSucceed() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        app.setApprovalInstanceId(100L);
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.cancel(APP_ID);

        assertEquals(OnboardingStatus.DRAFT.getCode(), app.getStatus());
        assertNull(app.getApprovalInstanceId());
        verify(approvalFlowService, times(1)).cancel(100L);
    }

    @Test
    void cancel_notApplicant_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.PENDING.getCode());
        app.setApplicantId(999L); // 不是当前用户
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        assertThrows(BusinessException.class, () -> service.cancel(APP_ID));
    }

    // ==================== 确认入职 ====================

    @Test
    void confirmJoin_shouldSucceed() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.APPROVED.getCode());
        app.setEmployeeId(100L);
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        Employee employee = new Employee();
        employee.setId(100L);
        when(employeeMapper.selectById(100L)).thenReturn(employee);
        when(employeeMapper.updateById(any())).thenReturn(1);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.confirmJoin(APP_ID, LocalDate.of(2026, 8, 1));

        assertEquals(OnboardingStatus.JOINED.getCode(), app.getStatus());
    }

    @Test
    void confirmJoin_notApproved_shouldThrow() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.DRAFT.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        assertThrows(BusinessException.class, () ->
                service.confirmJoin(APP_ID, LocalDate.now()));
    }

    // ==================== 放弃入职 ====================

    @Test
    void abandon_shouldSucceed() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.APPROVED.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.abandon(APP_ID);

        assertEquals(OnboardingStatus.ABANDONED.getCode(), app.getStatus());
    }

    // ==================== 审批回调 ====================

    @Test
    void onApproved_shouldWriteEmployeeAndThreeTables() {
        OnboardingApplication app = mockApp();
        app.setDepartmentId(1L);
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        // mock department for employeeNo generation
        Department dept = new Department();
        dept.setCode("01");
        when(departmentMapper.selectById(1L)).thenReturn(dept);
        // mock noSequence
        EmployeeNoSequence seq = new EmployeeNoSequence();
        seq.setCurrentSeq(5);
        when(noSequenceMapper.selectOne(any())).thenReturn(seq);
        when(noSequenceMapper.updateById(any())).thenReturn(1);
        when(employeeMapper.insert(any())).thenReturn(1);
        when(personalInfoMapper.insert(any())).thenReturn(1);
        when(workInfoMapper.insert(any())).thenReturn(1);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.onApproved(ApprovalBizType.ONBOARDING, APP_ID);

        assertEquals(OnboardingStatus.APPROVED.getCode(), app.getStatus());
        assertNotNull(app.getEmployeeId());
        verify(employeeMapper, times(1)).insert(any());
        verify(personalInfoMapper, times(1)).insert(any());
        verify(workInfoMapper, times(1)).insert(any());
    }

    @Test
    void onRejected_shouldUpdateStatus() {
        OnboardingApplication app = mockApp();
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);
        when(onboardingMapper.updateById(any())).thenReturn(1);

        service.onRejected(ApprovalBizType.ONBOARDING, APP_ID);

        assertEquals(OnboardingStatus.REJECTED.getCode(), app.getStatus());
    }

    // ==================== 获取详情 ====================

    @Test
    void getDetail_shouldReturnVO() {
        OnboardingApplication app = mockApp();
        app.setStatus(OnboardingStatus.DRAFT.getCode());
        when(onboardingMapper.selectById(APP_ID)).thenReturn(app);

        OnboardingDetailVO vo = service.getDetail(APP_ID);

        assertNotNull(vo);
        assertEquals(app.getName(), vo.getName());
    }

    // ==================== 边界条件 ====================

    @Test
    void getDetail_notFound_shouldThrow() {
        when(onboardingMapper.selectById(APP_ID)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getDetail(APP_ID));
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
mvn test -pl . -Dtest="com.limou.hrms.service.OnboardingServiceTest" -DfailIfNoTests=false
```

预期：全部通过（约16个测试）

- [ ] **Step 3: 运行全部审批中心测试确保没有回归**

```bash
mvn test -pl . -Dtest="com.limou.hrms.builder.LeaveNodeBuilderTest,com.limou.hrms.service.ApprovalDelegateServiceTest,com.limou.hrms.service.ApprovalFlowServiceTest,com.limou.hrms.service.OnboardingServiceTest" -DfailIfNoTests=false
```
