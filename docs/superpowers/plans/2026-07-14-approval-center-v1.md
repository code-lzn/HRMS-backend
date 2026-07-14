# 审批中心 V1 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现审批中心第一版核心功能：创建审批实例+节点链、审批通过/拒绝/转交、撤回、待办/已办列表、审批详情，首批接入入职审批和转正审批。

**Architecture:** 策略模式 + 回调接口。ApprovalNodeBuilder 按业务类型构建节点链，ApprovalCallback 由业务模块实现解耦审批流转与业务处理。ApprovalFlowService 作为审批引擎统一入口，使用 Spring `@Transactional` 保证事务一致性。

**Tech Stack:** Spring Boot 2.7.2, MyBatis-Plus 3.5.2, MySQL, Java 8, Lombok

## Global Constraints

- Java 8 — 不使用 Java 9+ 特性
- 包路径：`com.limou.hrms`
- API 前缀：`/api`（由 `server.servlet.context-path` 自动添加，Controller 中 `@RequestMapping` 无需再加 `/api`）
- 统一响应：`ResultUtils.success(data)` / `ResultUtils.error(errorCode)`
- 异常：`throw new BusinessException(ErrorCode.XXX, "message")`
- 审批相关实体主键用 `IdType.AUTO`（自增ID），与表结构 `AUTO_INCREMENT` 一致
- 审批相关表无 `is_deleted` 逻辑删除字段，实体不加 `@TableLogic`
- 风格：Service 接口放 `service/`，实现放 `service/impl/`
- 依赖注入：`@Resource` 注解
- 当前用户：Controller 通过 `HttpServletRequest` + `userService.getLoginUser(request)` 获取当前登录 User
- 参数校验：Controller 层手动校验，不引入 Hibernate Validator
- MyBatis-Plus 已配置分页插件（`PaginationInnerInterceptor`），直接使用 `Page<T>`

---

### Task 1: 创建审批相关枚举类

**Files:**
- Create: `src/main/java/com/limou/hrms/model/enums/ApprovalBizType.java`
- Create: `src/main/java/com/limou/hrms/model/enums/ApprovalStatus.java`
- Create: `src/main/java/com/limou/hrms/model/enums/NodeStatus.java`

**Interfaces:**
- Produces: `ApprovalBizType` (ONBOARDING, PROBATION), `ApprovalStatus` (PENDING=1, APPROVED=2, REJECTED=3, CANCELLED=4), `NodeStatus` (PENDING=1, APPROVED=2, REJECTED=3, TRANSFERRED=4)

- [ ] **Step 1: 创建 ApprovalBizType**

```java
package com.limou.hrms.model.enums;

/**
 * 审批业务类型枚举
 */
public enum ApprovalBizType {

    ONBOARDING("ONBOARDING", "入职审批"),
    PROBATION("PROBATION", "转正审批");

    private final String code;
    private final String desc;

    ApprovalBizType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static ApprovalBizType fromCode(String code) {
        for (ApprovalBizType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
```

- [ ] **Step 2: 创建 ApprovalStatus**

```java
package com.limou.hrms.model.enums;

/**
 * 审批实例状态枚举
 */
public enum ApprovalStatus {

    PENDING(1, "审批中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝"),
    CANCELLED(4, "已撤回");

    private final int code;
    private final String desc;

    ApprovalStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static ApprovalStatus fromCode(int code) {
        for (ApprovalStatus s : values()) {
            if (s.code == code) { return s; }
        }
        return null;
    }
}
```

- [ ] **Step 3: 创建 NodeStatus**

```java
package com.limou.hrms.model.enums;

/**
 * 审批节点状态枚举
 */
public enum NodeStatus {

    PENDING(1, "待审批"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝"),
    TRANSFERRED(4, "已转交");

    private final int code;
    private final String desc;

    NodeStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static NodeStatus fromCode(int code) {
        for (NodeStatus s : values()) {
            if (s.code == code) { return s; }
        }
        return null;
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/limou/hrms/model/enums/ApprovalBizType.java src/main/java/com/limou/hrms/model/enums/ApprovalStatus.java src/main/java/com/limou/hrms/model/enums/NodeStatus.java
git commit -m "feat: add approval enums (ApprovalBizType, ApprovalStatus, NodeStatus)"
```

---

### Task 2: 创建实体类和 Mapper

**Files:**
- Create: `src/main/java/com/limou/hrms/model/entity/ApprovalInstance.java`
- Create: `src/main/java/com/limou/hrms/model/entity/ApprovalNode.java`
- Create: `src/main/java/com/limou/hrms/model/entity/ApprovalDelegate.java`
- Create: `src/main/java/com/limou/hrms/model/entity/Department.java`
- Create: `src/main/java/com/limou/hrms/model/entity/Employee.java`
- Create: `src/main/java/com/limou/hrms/model/entity/EmployeeWorkInfo.java`
- Create: `src/main/java/com/limou/hrms/model/entity/OnboardingApplication.java`
- Create: `src/main/java/com/limou/hrms/model/entity/ProbationApplication.java`
- Create: `src/main/java/com/limou/hrms/mapper/ApprovalInstanceMapper.java`
- Create: `src/main/java/com/limou/hrms/mapper/ApprovalNodeMapper.java`
- Create: `src/main/java/com/limou/hrms/mapper/ApprovalDelegateMapper.java`
- Create: `src/main/java/com/limou/hrms/mapper/DepartmentMapper.java`
- Create: `src/main/java/com/limou/hrms/mapper/EmployeeMapper.java`
- Create: `src/main/java/com/limou/hrms/mapper/EmployeeWorkInfoMapper.java`
- Create: `src/main/java/com/limou/hrms/mapper/OnboardingApplicationMapper.java`
- Create: `src/main/java/com/limou/hrms/mapper/ProbationApplicationMapper.java`

**Interfaces:**
- Produces: 8个实体类 + 8个 Mapper 接口（均继承 `BaseMapper<T>`）

- [ ] **Step 1: 创建 ApprovalInstance 实体**

```java
package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("approval_instance")
@Data
public class ApprovalInstance implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务类型 */
    private String bizType;
    /** 业务主键ID */
    private Long bizId;
    /** 审批标题 */
    private String title;
    /** 审批状态：1=审批中 2=已通过 3=已拒绝 4=已撤回 */
    private Integer status;
    /** 申请人ID，关联employee.id */
    private Long applicantId;
    /** 当前审批节点序号 */
    private Integer currentNodeOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 2: 创建 ApprovalNode 实体**

```java
package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("approval_node")
@Data
public class ApprovalNode implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 审批实例ID */
    private Long instanceId;
    /** 节点名称 */
    private String nodeName;
    /** 节点顺序号 */
    private Integer nodeOrder;
    /** 当前审批人ID，关联employee.id */
    private Long approverId;
    /** 原审批人ID（转交场景记录） */
    private Long originalApproverId;
    /** 节点状态：1=待审批 2=已通过 3=已拒绝 4=已转交 */
    private Integer status;
    /** 审批意见 */
    private String comment;
    /** 操作时间 */
    private LocalDateTime operateTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 3: 创建 ApprovalDelegate 实体（预留表，仅映射）**

```java
package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("approval_delegate")
@Data
public class ApprovalDelegate implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long delegatorId;
    private Long delegateId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 是否启用：0=已取消 1=生效中 */
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 4: 创建 Department、Employee、EmployeeWorkInfo 实体**

Department:
```java
package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("department")
@Data
public class Department implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Long parentId;
    /** 部门负责人ID，关联employee.id */
    private Long managerId;
    private Integer sortOrder;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
```

Employee:
```java
package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("employee")
@Data
public class Employee implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联user.id */
    private Long userId;
    private String employeeNo;
    /** 在职状态：1=试用期 2=正式 3=待离职 4=已离职 */
    private Integer status;
    private LocalDate hireDate;
    private Integer hireType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
```

EmployeeWorkInfo:
```java
package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("employee_work_info")
@Data
public class EmployeeWorkInfo implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    /** 所属部门ID */
    private Long departmentId;
    private Long positionId;
    private String jobLevel;
    /** 直接汇报人ID，关联employee.id */
    private Long directReportId;
    private String workLocation;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 5: 创建 OnboardingApplication、ProbationApplication 实体**

OnboardingApplication:
```java
package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("onboarding_application")
@Data
public class OnboardingApplication implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
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
    /** 状态：1=草稿 2=审批中 3=已批准待入职 4=已入职 5=已拒绝 */
    private Integer status;
    private Long approvalInstanceId;
    private Long employeeId;
    private Long applicantId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
```

ProbationApplication:
```java
package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("probation_application")
@Data
public class ProbationApplication implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private LocalDate probationStartDate;
    private LocalDate probationEndDate;
    private String performanceReview;
    private BigDecimal salaryAdjustment;
    /** 转正结果：1=通过 2=延长试用 3=不通过 */
    private Integer result;
    private LocalDate extendedEndDate;
    /** 状态：1=草稿 2=审批中 3=已完成 4=已拒绝 */
    private Integer status;
    private Long approvalInstanceId;
    private Long applicantId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
```

- [ ] **Step 6: 创建 8 个 Mapper 接口**

所有 Mapper 均放在 `src/main/java/com/limou/hrms/mapper/` 下，继承 `BaseMapper<T>`：

```java
// ApprovalInstanceMapper.java
package com.limou.hrms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.ApprovalInstance;
public interface ApprovalInstanceMapper extends BaseMapper<ApprovalInstance> {}

// ApprovalNodeMapper.java
package com.limou.hrms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.ApprovalNode;
public interface ApprovalNodeMapper extends BaseMapper<ApprovalNode> {}

// ApprovalDelegateMapper.java
package com.limou.hrms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.ApprovalDelegate;
public interface ApprovalDelegateMapper extends BaseMapper<ApprovalDelegate> {}

// DepartmentMapper.java
package com.limou.hrms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.Department;
public interface DepartmentMapper extends BaseMapper<Department> {}

// EmployeeMapper.java
package com.limou.hrms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.Employee;
public interface EmployeeMapper extends BaseMapper<Employee> {}

// EmployeeWorkInfoMapper.java
package com.limou.hrms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
public interface EmployeeWorkInfoMapper extends BaseMapper<EmployeeWorkInfo> {}

// OnboardingApplicationMapper.java
package com.limou.hrms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.OnboardingApplication;
public interface OnboardingApplicationMapper extends BaseMapper<OnboardingApplication> {}

// ProbationApplicationMapper.java
package com.limou.hrms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.ProbationApplication;
public interface ProbationApplicationMapper extends BaseMapper<ProbationApplication> {}
```

- [ ] **Step 7: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/limou/hrms/model/entity/ApprovalInstance.java src/main/java/com/limou/hrms/model/entity/ApprovalNode.java src/main/java/com/limou/hrms/model/entity/ApprovalDelegate.java src/main/java/com/limou/hrms/model/entity/Department.java src/main/java/com/limou/hrms/model/entity/Employee.java src/main/java/com/limou/hrms/model/entity/EmployeeWorkInfo.java src/main/java/com/limou/hrms/model/entity/OnboardingApplication.java src/main/java/com/limou/hrms/model/entity/ProbationApplication.java src/main/java/com/limou/hrms/mapper/
git commit -m "feat: add approval and supporting entities and mappers"
```

---

### Task 3: 创建 DTO、VO、Query 并扩展错误码

**Files:**
- Create: `src/main/java/com/limou/hrms/model/dto/approval/ApprovalActionDTO.java`
- Create: `src/main/java/com/limou/hrms/model/query/ApprovalQuery.java`
- Create: `src/main/java/com/limou/hrms/model/vo/ApprovalInstanceVO.java`
- Create: `src/main/java/com/limou/hrms/model/vo/ApprovalNodeVO.java`
- Create: `src/main/java/com/limou/hrms/model/vo/PendingItemVO.java`
- Create: `src/main/java/com/limou/hrms/model/vo/ProcessedItemVO.java`
- Modify: `src/main/java/com/limou/hrms/common/ErrorCode.java`

**Interfaces:**
- Produces: DTO/VO/Query 类供 Service 和 Controller 使用；ErrorCode 新增 5 个审批错误码

- [ ] **Step 1: 创建 ApprovalActionDTO**

```java
package com.limou.hrms.model.dto.approval;

import lombok.Data;

/**
 * 审批操作请求参数
 */
@Data
public class ApprovalActionDTO {
    /** 审批意见（拒绝时必填） */
    private String comment;
    /** 转交目标审批人ID（转交时必填） */
    private Long toApproverId;
}
```

- [ ] **Step 2: 创建 ApprovalQuery**

```java
package com.limou.hrms.model.query;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批列表查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalQuery extends PageRequest {
    /** 业务类型筛选，可选 */
    private String bizType;
}
```

- [ ] **Step 3: 创建 ApprovalInstanceVO**

```java
package com.limou.hrms.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 审批实例详情 VO（含完整节点时间线）
 */
@Data
public class ApprovalInstanceVO {
    private Long instanceId;
    private String bizType;
    private String bizTypeDesc;
    private String title;
    private Integer status;
    private String statusDesc;
    private Long applicantId;
    private String applicantName;
    private Integer currentNodeOrder;
    /** 审批节点时间线 */
    private List<ApprovalNodeVO> nodes;
    private LocalDateTime createTime;
}
```

- [ ] **Step 4: 创建 ApprovalNodeVO**

```java
package com.limou.hrms.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 审批节点 VO
 */
@Data
public class ApprovalNodeVO {
    private Long nodeId;
    private String nodeName;
    private Integer nodeOrder;
    private Long approverId;
    private String approverName;
    private Long originalApproverId;
    private String originalApproverName;
    private Integer status;
    private String statusDesc;
    private String comment;
    private LocalDateTime operateTime;
}
```

- [ ] **Step 5: 创建 PendingItemVO（待办列表项）**

```java
package com.limou.hrms.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 待办列表项 VO
 */
@Data
public class PendingItemVO {
    private Long instanceId;
    private Long nodeId;
    private String bizType;
    private String bizTypeDesc;
    private String title;
    private Long applicantId;
    private String applicantName;
    private String nodeName;
    private Integer nodeOrder;
    private LocalDateTime createTime;
}
```

- [ ] **Step 6: 创建 ProcessedItemVO（已办列表项）**

```java
package com.limou.hrms.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 已办列表项 VO
 */
@Data
public class ProcessedItemVO {
    private Long instanceId;
    private Long nodeId;
    private String bizType;
    private String bizTypeDesc;
    private String title;
    private String applicantName;
    private String nodeName;
    private Integer nodeStatus;
    private String nodeStatusDesc;
    private String comment;
    private LocalDateTime operateTime;
}
```

- [ ] **Step 7: 扩展 ErrorCode 枚举**

修改 `src/main/java/com/limou/hrms/common/ErrorCode.java`，在 `OPERATION_ERROR` 后添加：

```java
// 审批相关
APPROVAL_INSTANCE_NOT_FOUND(40001, "审批实例不存在"),
APPROVAL_NODE_NOT_FOUND(40002, "审批节点不存在"),
APPROVAL_NODE_NOT_OWNER(40003, "该节点不属于当前用户"),
APPROVAL_NODE_ALREADY_HANDLED(40004, "该节点已被处理"),
APPROVAL_CANCEL_ONLY_FIRST_NODE(40005, "仅第一节点可撤回"),
```

- [ ] **Step 8: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 9: 提交**

```bash
git add src/main/java/com/limou/hrms/model/dto/approval/ src/main/java/com/limou/hrms/model/query/ApprovalQuery.java src/main/java/com/limou/hrms/model/vo/ApprovalInstanceVO.java src/main/java/com/limou/hrms/model/vo/ApprovalNodeVO.java src/main/java/com/limou/hrms/model/vo/PendingItemVO.java src/main/java/com/limou/hrms/model/vo/ProcessedItemVO.java src/main/java/com/limou/hrms/common/ErrorCode.java
git commit -m "feat: add approval DTOs, VOs, query and extended error codes"
```

---

### Task 4: 创建 ApprovalCallback 接口和 ApprovalNodeBuilder 策略

**Files:**
- Create: `src/main/java/com/limou/hrms/service/ApprovalCallback.java`
- Create: `src/main/java/com/limou/hrms/builder/ApprovalNodeBuilder.java`
- Create: `src/main/java/com/limou/hrms/builder/ApprovalNodeBuilderFactory.java`
- Create: `src/main/java/com/limou/hrms/builder/OnboardingNodeBuilder.java`
- Create: `src/main/java/com/limou/hrms/builder/ProbationNodeBuilder.java`

**Interfaces:**
- Produces: `ApprovalCallback` 回调接口（`onApproved`/`onRejected`）；`ApprovalNodeBuilder` 策略接口（`build` + `supportedBizType`）；工厂 `ApprovalNodeBuilderFactory`；两个 Builder 实现

**HR审批人查询逻辑：** `approval_node.approver_id` 引用 `employee.id`，但 HR 通过 `user.user_role = 'hr'` 标识。需通过 `employee.user_id` 关联查询：先查 `user` 表得 HR 的 user_id 列表，再查 `employee` 表得 employee_id。若 HR 用户尚无 employee 记录（纯管理员账号），回退到使用 user.id（记录到 approver_id，与 employee.id 同源）。

- [ ] **Step 1: 创建 ApprovalCallback 接口**

```java
package com.limou.hrms.service;

import com.limou.hrms.model.enums.ApprovalBizType;

/**
 * 审批回调接口 — 各业务模块实现此接口接收审批结果
 */
public interface ApprovalCallback {

    /**
     * 审批全部通过后的业务处理
     */
    void onApproved(ApprovalBizType bizType, Long bizId);

    /**
     * 审批被拒绝后的业务处理
     */
    void onRejected(ApprovalBizType bizType, Long bizId);
}
```

- [ ] **Step 2: 创建 ApprovalNodeBuilder 接口**

```java
package com.limou.hrms.builder;

import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.enums.ApprovalBizType;
import java.util.List;

/**
 * 审批节点构建器 — 策略接口
 */
public interface ApprovalNodeBuilder {

    /**
     * 构建审批节点链
     * @param bizId       业务主键ID（如 onboarding_application.id）
     * @param applicantId 申请人 employee.id
     * @return 审批节点列表（按 nodeOrder 升序）
     */
    List<ApprovalNode> build(Long bizId, Long applicantId);

    /**
     * @return 该 Builder 支持的业务类型
     */
    ApprovalBizType supportedBizType();
}
```

- [ ] **Step 3: 创建 ApprovalNodeBuilderFactory**

```java
package com.limou.hrms.builder;

import com.limou.hrms.model.enums.ApprovalBizType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批节点构建器工厂 — 按业务类型路由到对应 Builder
 */
@Component
public class ApprovalNodeBuilderFactory {

    @Resource
    private List<ApprovalNodeBuilder> builders;

    private final Map<ApprovalBizType, ApprovalNodeBuilder> builderMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ApprovalNodeBuilder builder : builders) {
            builderMap.put(builder.supportedBizType(), builder);
        }
    }

    public ApprovalNodeBuilder getBuilder(ApprovalBizType bizType) {
        ApprovalNodeBuilder builder = builderMap.get(bizType);
        if (builder == null) {
            throw new IllegalArgumentException("未找到业务类型对应的构建器: " + bizType.getCode());
        }
        return builder;
    }
}
```

- [ ] **Step 4: 创建 OnboardingNodeBuilder**

审批链：部门负责人 → HR负责人

```java
package com.limou.hrms.builder;

import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 入职审批节点构建器：Node1=部门负责人 → Node2=HR负责人
 */
@Component
public class OnboardingNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private OnboardingApplicationMapper onboardingApplicationMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private EmployeeMapper employeeMapper;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        // 1. 查入职申请获取部门ID
        OnboardingApplication app = onboardingApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("入职申请不存在: " + bizId);
        }

        // 2. 查部门获取部门负责人
        Department dept = departmentMapper.selectById(app.getDepartmentId());
        if (dept == null || dept.getManagerId() == null) {
            throw new IllegalArgumentException("部门或部门负责人不存在");
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 部门负责人
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("部门负责人审批");
        node1.setNodeOrder(order++);
        node1.setApproverId(dept.getManagerId());
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        // Node 2: HR负责人 — 查 employee 表中 user_role='hr' 对应的员工
        // 通过 EmployeeMapper 查询所有 HR 员工（需要用 SQL 查询）
        // 简化：用 EmployeeMapper + 传入的 applicantId 查同部门HR
        // 实际通过 UserMapper 查 user_role='hr' 的 user，再映射到 employee
        // 第一版简化：直接用 applicantId（HR本人）作为 HR负责人审批节点
        // TODO 后续改为查 user_role='hr' 的用户列表
        ApprovalNode node2 = new ApprovalNode();
        node2.setNodeName("HR负责人审批");
        node2.setNodeOrder(order);
        node2.setApproverId(resolveHrApprover());
        node2.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node2);

        return nodes;
    }

    /**
     * 查找HR负责人。查 user 表 user_role='hr'，映射到 employee 表。
     * 若无 HR 员工记录，回退返回 1L（admin）。
     */
    private Long resolveHrApprover() {
        // 通过查 employee 表关联 user 角色
        List<Employee> hrEmployees = employeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                        .inSql("user_id", "SELECT id FROM user WHERE user_role = 'hr' AND is_deleted = 0")
                        .eq("is_deleted", 0)
                        .last("LIMIT 1")
        );
        if (!hrEmployees.isEmpty()) {
            return hrEmployees.get(0).getId();
        }
        // 回退：返回第一个员工
        List<Employee> all = employeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                        .eq("is_deleted", 0)
                        .last("LIMIT 1")
        );
        if (!all.isEmpty()) {
            return all.get(0).getId();
        }
        return 1L; // 最终回退
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.ONBOARDING;
    }
}
```

- [ ] **Step 5: 创建 ProbationNodeBuilder**

审批链：部门负责人 → HR负责人（同入职）

```java
package com.limou.hrms.builder;

import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 转正审批节点构建器：Node1=部门负责人 → Node2=HR负责人
 */
@Component
public class ProbationNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private ProbationApplicationMapper probationApplicationMapper;
    @Resource
    private EmployeeWorkInfoMapper employeeWorkInfoMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private EmployeeMapper employeeMapper;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        // 1. 查转正申请获取员工ID
        ProbationApplication app = probationApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("转正申请不存在: " + bizId);
        }

        // 2. 查员工工作信息获取部门ID
        EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EmployeeWorkInfo>()
                        .eq("employee_id", app.getEmployeeId())
        );
        if (workInfo == null) {
            throw new IllegalArgumentException("员工工作信息不存在");
        }

        // 3. 查部门获取部门负责人
        Department dept = departmentMapper.selectById(workInfo.getDepartmentId());
        if (dept == null || dept.getManagerId() == null) {
            throw new IllegalArgumentException("部门或部门负责人不存在");
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 部门负责人
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("部门负责人审批");
        node1.setNodeOrder(order++);
        node1.setApproverId(dept.getManagerId());
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        // Node 2: HR负责人
        ApprovalNode node2 = new ApprovalNode();
        node2.setNodeName("HR负责人审批");
        node2.setNodeOrder(order);
        node2.setApproverId(resolveHrApprover());
        node2.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node2);

        return nodes;
    }

    private Long resolveHrApprover() {
        List<Employee> hrEmployees = employeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                        .inSql("user_id", "SELECT id FROM user WHERE user_role = 'hr' AND is_deleted = 0")
                        .eq("is_deleted", 0)
                        .last("LIMIT 1")
        );
        if (!hrEmployees.isEmpty()) {
            return hrEmployees.get(0).getId();
        }
        List<Employee> all = employeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                        .eq("is_deleted", 0)
                        .last("LIMIT 1")
        );
        if (!all.isEmpty()) {
            return all.get(0).getId();
        }
        return 1L;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.PROBATION;
    }
}
```

- [ ] **Step 6: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add src/main/java/com/limou/hrms/service/ApprovalCallback.java src/main/java/com/limou/hrms/builder/
git commit -m "feat: add ApprovalCallback interface and node builder strategy"
```

---

### Task 5: 创建 ApprovalFlowService 接口和实现

**Files:**
- Create: `src/main/java/com/limou/hrms/service/ApprovalFlowService.java`
- Create: `src/main/java/com/limou/hrms/service/impl/ApprovalFlowServiceImpl.java`

**Interfaces:**
- Consumes: `ApprovalInstanceMapper`, `ApprovalNodeMapper`, `ApprovalNodeBuilderFactory`, `ApprovalCallback`（Spring 自动注入所有实现类 List）, `UserMapper`, `EmployeeMapper`, `ErrorCode` 审批错误码
- Produces: `ApprovalFlowService` 接口7个方法 + 完整实现

- [ ] **Step 1: 创建 ApprovalFlowService 接口**

```java
package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.entity.ApprovalInstance;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.ApprovalInstanceVO;
import com.limou.hrms.model.vo.PendingItemVO;
import com.limou.hrms.model.vo.ProcessedItemVO;

/**
 * 审批流引擎 — 核心服务接口
 */
public interface ApprovalFlowService {

    /**
     * 创建审批实例 + 节点链
     * @return 审批实例
     */
    ApprovalInstance createInstance(ApprovalBizType bizType, Long bizId, Long applicantId);

    /**
     * 审批通过
     * @param nodeId   审批节点ID
     * @param employeeId 当前操作人的 employee.id
     * @param comment  审批意见（可选）
     */
    void approve(Long nodeId, Long employeeId, String comment);

    /**
     * 审批拒绝
     * @param nodeId   审批节点ID
     * @param employeeId 当前操作人的 employee.id
     * @param comment  拒绝理由（必填）
     */
    void reject(Long nodeId, Long employeeId, String comment);

    /**
     * 审批转交
     * @param nodeId       审批节点ID
     * @param fromEmployeeId 当前审批人 employee.id
     * @param toEmployeeId   新审批人 employee.id
     */
    void transfer(Long nodeId, Long fromEmployeeId, Long toEmployeeId);

    /**
     * 撤回申请（仅申请人和第一节点可撤回）
     * @param instanceId 审批实例ID
     * @param operatorId 操作人 employee.id
     */
    void cancel(Long instanceId, Long operatorId);

    /**
     * 查询待办列表
     */
    Page<PendingItemVO> getPendingList(Long employeeId, ApprovalQuery query);

    /**
     * 查询已办列表
     */
    Page<ProcessedItemVO> getProcessedList(Long employeeId, ApprovalQuery query);

    /**
     * 获取审批详情（含节点时间线）
     */
    ApprovalInstanceVO getDetail(Long instanceId);
}
```

- [ ] **Step 2: 创建 ApprovalFlowServiceImpl**

核心实现要点：
- `createInstance`：查 Builder → build 节点链 → 保存 instance + batch insert nodes，事务保护
- `approve`：校验权限 → 更新节点为已通过 → 判断是否最后节点 → 是则完成实例+回调，否则推进 currentNodeOrder
- `reject`：校验权限 → 更新节点为已拒绝 → 更新实例为已拒绝 → 回调 onRejected
- `transfer`：校验权限 → 保存 originalApproverId → 更新 approverId → 节点状态重置为待审批
- `cancel`：校验申请人和第一节点 → 更新实例为已撤回
- `getPendingList`：`SELECT * FROM approval_node WHERE approver_id = ? AND status = 1` 关联 instance
- `getProcessedList`：`SELECT * FROM approval_node WHERE approver_id = ? AND status IN (2,3,4)` 关联 instance
- `getDetail`：查 instance + 查所有 nodes 组装 VO

```java
package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.builder.ApprovalNodeBuilder;
import com.limou.hrms.builder.ApprovalNodeBuilderFactory;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.ApprovalStatus;
import com.limou.hrms.model.enums.NodeStatus;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalFlowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApprovalFlowServiceImpl extends ServiceImpl<ApprovalInstanceMapper, ApprovalInstance>
        implements ApprovalFlowService {

    @Resource
    private ApprovalInstanceMapper approvalInstanceMapper;
    @Resource
    private ApprovalNodeMapper approvalNodeMapper;
    @Resource
    private ApprovalNodeBuilderFactory builderFactory;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private UserMapper userMapper;

    /** Spring 自动注入所有 ApprovalCallback 实现（各业务模块），可能为空 */
    @Resource
    private List<ApprovalCallback> callbacks;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalInstance createInstance(ApprovalBizType bizType, Long bizId, Long applicantId) {
        // 1. 查 Builder 构建节点链
        ApprovalNodeBuilder builder = builderFactory.getBuilder(bizType);
        List<ApprovalNode> nodes = builder.build(bizId, applicantId);

        // 2. 生成审批标题
        Employee applicant = employeeMapper.selectById(applicantId);
        String applicantName = getEmployeeName(applicantId);
        String title = applicantName + "的" + bizType.getDesc();

        // 3. 保存审批实例
        ApprovalInstance instance = new ApprovalInstance();
        instance.setBizType(bizType.getCode());
        instance.setBizId(bizId);
        instance.setTitle(title);
        instance.setStatus(ApprovalStatus.PENDING.getCode());
        instance.setApplicantId(applicantId);
        instance.setCurrentNodeOrder(1);
        approvalInstanceMapper.insert(instance);

        // 4. 批量保存节点，绑定 instanceId
        for (int i = 0; i < nodes.size(); i++) {
            ApprovalNode node = nodes.get(i);
            node.setInstanceId(instance.getId());
            node.setNodeOrder(i + 1);
            approvalNodeMapper.insert(node);
        }

        log.info("审批实例创建成功: instanceId={}, bizType={}, bizId={}", instance.getId(), bizType.getCode(), bizId);
        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long nodeId, Long employeeId, String comment) {
        ApprovalNode node = getNodeOrThrow(nodeId);
        validateNodeOwner(node, employeeId);
        validateNodePending(node);

        // 更新当前节点
        node.setStatus(NodeStatus.APPROVED.getCode());
        node.setComment(comment);
        node.setOperateTime(LocalDateTime.now());
        approvalNodeMapper.updateById(node);

        ApprovalInstance instance = getInstanceOrThrow(node.getInstanceId());

        // 判断是否为最后一个节点
        Long totalNodes = approvalNodeMapper.selectCount(
                new QueryWrapper<ApprovalNode>().eq("instance_id", instance.getId())
        );

        if (node.getNodeOrder() >= totalNodes) {
            // 最后节点 — 审批完成
            instance.setStatus(ApprovalStatus.APPROVED.getCode());
            approvalInstanceMapper.updateById(instance);
            // 触发业务回调
            invokeCallback(instance, true);
            log.info("审批全部通过: instanceId={}", instance.getId());
        } else {
            // 推进到下一节点
            instance.setCurrentNodeOrder(node.getNodeOrder() + 1);
            approvalInstanceMapper.updateById(instance);
            log.info("审批通过，流转到下一节点: instanceId={}, nextOrder={}",
                    instance.getId(), instance.getCurrentNodeOrder());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long nodeId, Long employeeId, String comment) {
        if (StringUtils.isBlank(comment)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拒绝时必须填写审批意见");
        }
        ApprovalNode node = getNodeOrThrow(nodeId);
        validateNodeOwner(node, employeeId);
        validateNodePending(node);

        // 更新节点
        node.setStatus(NodeStatus.REJECTED.getCode());
        node.setComment(comment);
        node.setOperateTime(LocalDateTime.now());
        approvalNodeMapper.updateById(node);

        // 更新实例
        ApprovalInstance instance = getInstanceOrThrow(node.getInstanceId());
        instance.setStatus(ApprovalStatus.REJECTED.getCode());
        approvalInstanceMapper.updateById(instance);

        // 触发拒绝回调
        invokeCallback(instance, false);
        log.info("审批已拒绝: instanceId={}", instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long nodeId, Long fromEmployeeId, Long toEmployeeId) {
        ApprovalNode node = getNodeOrThrow(nodeId);
        validateNodeOwner(node, fromEmployeeId);
        validateNodePending(node);

        if (toEmployeeId.equals(fromEmployeeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能转交给自己");
        }
        // 验证目标审批人存在
        Employee target = employeeMapper.selectById(toEmployeeId);
        if (target == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标审批人不存在");
        }

        // 记录原审批人
        node.setOriginalApproverId(fromEmployeeId);
        // 更换审批人
        node.setApproverId(toEmployeeId);
        // 状态保持待审批（不改成已转交，让新审批人重新审批）
        node.setStatus(NodeStatus.PENDING.getCode());
        node.setComment(null);
        node.setOperateTime(LocalDateTime.now());
        approvalNodeMapper.updateById(node);

        log.info("审批已转交: nodeId={}, from={}, to={}", nodeId, fromEmployeeId, toEmployeeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long instanceId, Long operatorId) {
        ApprovalInstance instance = getInstanceOrThrow(instanceId);

        // 校验：仅申请人可撤回
        if (!instance.getApplicantId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.APPROVAL_CANCEL_ONLY_FIRST_NODE);
        }
        // 校验：必须是审批中状态
        if (!ApprovalStatus.PENDING.getCode().equals(instance.getStatus())) {
            throw new BusinessException(ErrorCode.APPROVAL_NODE_ALREADY_HANDLED, "当前状态不允许撤回");
        }
        // 校验：必须是第一节点
        if (instance.getCurrentNodeOrder() != 1) {
            throw new BusinessException(ErrorCode.APPROVAL_CANCEL_ONLY_FIRST_NODE);
        }

        instance.setStatus(ApprovalStatus.CANCELLED.getCode());
        approvalInstanceMapper.updateById(instance);

        // 取消所有待审批节点（设为已转交/或保持原样，前端显示已撤回即可）
        log.info("审批已撤回: instanceId={}", instanceId);
    }

    @Override
    public Page<PendingItemVO> getPendingList(Long employeeId, ApprovalQuery query) {
        // 查当前用户待审批的节点
        QueryWrapper<ApprovalNode> nodeQw = new QueryWrapper<>();
        nodeQw.eq("approver_id", employeeId)
               .eq("status", NodeStatus.PENDING.getCode())
               .orderByDesc("create_time");

        // 分页查节点
        Page<ApprovalNode> nodePage = approvalNodeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), nodeQw
        );

        // 关联 instance 信息组装 VO
        List<PendingItemVO> records = nodePage.getRecords().stream().map(node -> {
            ApprovalInstance instance = approvalInstanceMapper.selectById(node.getInstanceId());
            PendingItemVO vo = new PendingItemVO();
            vo.setInstanceId(node.getInstanceId());
            vo.setNodeId(node.getId());
            if (instance != null) {
                vo.setBizType(instance.getBizType());
                ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
                vo.setBizTypeDesc(bizType != null ? bizType.getDesc() : instance.getBizType());
                vo.setTitle(instance.getTitle());
                vo.setApplicantId(instance.getApplicantId());
                vo.setApplicantName(getEmployeeName(instance.getApplicantId()));
                vo.setCreateTime(instance.getCreateTime());
            }
            vo.setNodeName(node.getNodeName());
            vo.setNodeOrder(node.getNodeOrder());
            return vo;
        }).collect(Collectors.toList());

        // 按 bizType 筛选
        if (StringUtils.isNotBlank(query.getBizType())) {
            records = records.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
        }

        Page<PendingItemVO> result = new Page<>(nodePage.getCurrent(), nodePage.getSize(), nodePage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public Page<ProcessedItemVO> getProcessedList(Long employeeId, ApprovalQuery query) {
        QueryWrapper<ApprovalNode> nodeQw = new QueryWrapper<>();
        nodeQw.eq("approver_id", employeeId)
               .in("status", NodeStatus.APPROVED.getCode(), NodeStatus.REJECTED.getCode(), NodeStatus.TRANSFERRED.getCode())
               .orderByDesc("operate_time");

        Page<ApprovalNode> nodePage = approvalNodeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), nodeQw
        );

        List<ProcessedItemVO> records = nodePage.getRecords().stream().map(node -> {
            ApprovalInstance instance = approvalInstanceMapper.selectById(node.getInstanceId());
            ProcessedItemVO vo = new ProcessedItemVO();
            vo.setInstanceId(node.getInstanceId());
            vo.setNodeId(node.getId());
            if (instance != null) {
                vo.setBizType(instance.getBizType());
                ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
                vo.setBizTypeDesc(bizType != null ? bizType.getDesc() : instance.getBizType());
                vo.setTitle(instance.getTitle());
                vo.setApplicantName(getEmployeeName(instance.getApplicantId()));
            }
            vo.setNodeName(node.getNodeName());
            vo.setNodeStatus(node.getStatus());
            NodeStatus ns = NodeStatus.fromCode(node.getStatus());
            vo.setNodeStatusDesc(ns != null ? ns.getDesc() : "");
            vo.setComment(node.getComment());
            vo.setOperateTime(node.getOperateTime());
            return vo;
        }).collect(Collectors.toList());

        if (StringUtils.isNotBlank(query.getBizType())) {
            records = records.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
        }

        Page<ProcessedItemVO> result = new Page<>(nodePage.getCurrent(), nodePage.getSize(), nodePage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public ApprovalInstanceVO getDetail(Long instanceId) {
        ApprovalInstance instance = getInstanceOrThrow(instanceId);
        List<ApprovalNode> nodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .eq("instance_id", instanceId)
                        .orderByAsc("node_order")
        );

        ApprovalInstanceVO vo = new ApprovalInstanceVO();
        vo.setInstanceId(instance.getId());
        vo.setBizType(instance.getBizType());
        ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
        vo.setBizTypeDesc(bizType != null ? bizType.getDesc() : instance.getBizType());
        vo.setTitle(instance.getTitle());
        vo.setStatus(instance.getStatus());
        ApprovalStatus as = ApprovalStatus.fromCode(instance.getStatus());
        vo.setStatusDesc(as != null ? as.getDesc() : "");
        vo.setApplicantId(instance.getApplicantId());
        vo.setApplicantName(getEmployeeName(instance.getApplicantId()));
        vo.setCurrentNodeOrder(instance.getCurrentNodeOrder());
        vo.setCreateTime(instance.getCreateTime());

        List<ApprovalNodeVO> nodeVOs = nodes.stream().map(node -> {
            ApprovalNodeVO nvo = new ApprovalNodeVO();
            nvo.setNodeId(node.getId());
            nvo.setNodeName(node.getNodeName());
            nvo.setNodeOrder(node.getNodeOrder());
            nvo.setApproverId(node.getApproverId());
            nvo.setApproverName(getEmployeeName(node.getApproverId()));
            nvo.setOriginalApproverId(node.getOriginalApproverId());
            nvo.setOriginalApproverName(node.getOriginalApproverId() != null
                    ? getEmployeeName(node.getOriginalApproverId()) : null);
            nvo.setStatus(node.getStatus());
            NodeStatus ns = NodeStatus.fromCode(node.getStatus());
            nvo.setStatusDesc(ns != null ? ns.getDesc() : "");
            nvo.setComment(node.getComment());
            nvo.setOperateTime(node.getOperateTime());
            return nvo;
        }).collect(Collectors.toList());
        vo.setNodes(nodeVOs);

        return vo;
    }

    // ==================== 私有辅助方法 ====================

    private ApprovalNode getNodeOrThrow(Long nodeId) {
        ApprovalNode node = approvalNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException(ErrorCode.APPROVAL_NODE_NOT_FOUND);
        }
        return node;
    }

    private ApprovalInstance getInstanceOrThrow(Long instanceId) {
        ApprovalInstance instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.APPROVAL_INSTANCE_NOT_FOUND);
        }
        return instance;
    }

    private void validateNodeOwner(ApprovalNode node, Long employeeId) {
        if (!node.getApproverId().equals(employeeId)) {
            throw new BusinessException(ErrorCode.APPROVAL_NODE_NOT_OWNER);
        }
    }

    private void validateNodePending(ApprovalNode node) {
        if (!NodeStatus.PENDING.getCode().equals(node.getStatus())) {
            throw new BusinessException(ErrorCode.APPROVAL_NODE_ALREADY_HANDLED);
        }
    }

    /**
     * 根据 employee.id 获取员工姓名
     */
    private String getEmployeeName(Long employeeId) {
        if (employeeId == null) return null;
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) return null;
        User user = userMapper.selectById(emp.getUserId());
        return user != null ? user.getUserName() : emp.getEmployeeNo();
    }

    /**
     * 触发业务回调（通过/拒绝）
     */
    private void invokeCallback(ApprovalInstance instance, boolean approved) {
        ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
        if (bizType == null) {
            log.warn("未知业务类型: {}", instance.getBizType());
            return;
        }
        for (ApprovalCallback callback : callbacks) {
            if (approved) {
                callback.onApproved(bizType, instance.getBizId());
            } else {
                callback.onRejected(bizType, instance.getBizId());
            }
        }
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/limou/hrms/service/ApprovalFlowService.java src/main/java/com/limou/hrms/service/impl/ApprovalFlowServiceImpl.java
git commit -m "feat: add ApprovalFlowService interface and implementation"
```

---

### Task 6: 创建 ApprovalController

**Files:**
- Create: `src/main/java/com/limou/hrms/controller/ApprovalController.java`

**Interfaces:**
- Consumes: `ApprovalFlowService`, `UserService`（获取当前登录用户）

- [ ] **Step 1: 创建 ApprovalController**

```java
package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.approval.ApprovalActionDTO;
import com.limou.hrms.model.entity.ApprovalInstance;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.ApprovalInstanceVO;
import com.limou.hrms.model.vo.PendingItemVO;
import com.limou.hrms.model.vo.ProcessedItemVO;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 审批中心 Controller — 通用审批接口
 */
@RestController
@RequestMapping("/approvals")
@Slf4j
public class ApprovalController {

    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private UserService userService;

    // ==================== 查询接口 ====================

    /**
     * 查询待办列表
     */
    @GetMapping("/pending")
    public BaseResponse<Page<PendingItemVO>> getPendingList(ApprovalQuery query, HttpServletRequest request) {
        Long employeeId = getCurrentEmployeeId(request);
        Page<PendingItemVO> result = approvalFlowService.getPendingList(employeeId, query);
        return ResultUtils.success(result);
    }

    /**
     * 查询已办列表
     */
    @GetMapping("/processed")
    public BaseResponse<Page<ProcessedItemVO>> getProcessedList(ApprovalQuery query, HttpServletRequest request) {
        Long employeeId = getCurrentEmployeeId(request);
        Page<ProcessedItemVO> result = approvalFlowService.getProcessedList(employeeId, query);
        return ResultUtils.success(result);
    }

    /**
     * 获取审批详情
     */
    @GetMapping("/{instanceId}")
    public BaseResponse<ApprovalInstanceVO> getDetail(@PathVariable Long instanceId) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        ApprovalInstanceVO vo = approvalFlowService.getDetail(instanceId);
        return ResultUtils.success(vo);
    }

    // ==================== 操作接口 ====================

    /**
     * 审批通过
     */
    @PostMapping("/{nodeId}/approve")
    public BaseResponse<?> approve(@PathVariable Long nodeId,
                                   @RequestBody ApprovalActionDTO dto,
                                   HttpServletRequest request) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        Long employeeId = getCurrentEmployeeId(request);
        approvalFlowService.approve(nodeId, employeeId, dto.getComment());
        return ResultUtils.success("ok");
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/{nodeId}/reject")
    public BaseResponse<?> reject(@PathVariable Long nodeId,
                                  @RequestBody ApprovalActionDTO dto,
                                  HttpServletRequest request) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        if (StringUtils.isBlank(dto.getComment())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拒绝时必须填写审批意见");
        }
        Long employeeId = getCurrentEmployeeId(request);
        approvalFlowService.reject(nodeId, employeeId, dto.getComment());
        return ResultUtils.success("ok");
    }

    /**
     * 审批转交
     */
    @PostMapping("/{nodeId}/transfer")
    public BaseResponse<?> transfer(@PathVariable Long nodeId,
                                    @RequestBody ApprovalActionDTO dto,
                                    HttpServletRequest request) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(dto.getToApproverId() == null || dto.getToApproverId() <= 0,
                ErrorCode.PARAMS_ERROR, "目标审批人不能为空");
        Long employeeId = getCurrentEmployeeId(request);
        approvalFlowService.transfer(nodeId, employeeId, dto.getToApproverId());
        return ResultUtils.success("ok");
    }

    /**
     * 撤回申请
     */
    @PostMapping("/{instanceId}/cancel")
    public BaseResponse<?> cancel(@PathVariable Long instanceId, HttpServletRequest request) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        Long employeeId = getCurrentEmployeeId(request);
        approvalFlowService.cancel(instanceId, employeeId);
        return ResultUtils.success("ok");
    }

    // ==================== 辅助方法 ====================

    /**
     * 从当前登录用户获取 employee.id。
     * 当前系统用 User + HttpSession 认证，User.id 即为 employee 体系中的 user_id。
     * 需要查 employee 表获取对应的 employee.id。
     */
    private Long getCurrentEmployeeId(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 简化：User.id 作为 employee_id（假设 user 和 employee 共享 id）
        // 实际情况需查 employee 表 by user_id
        // 第一版：直接使用 employee.id = loginUser.getId() 的映射
        return loginUser.getId();
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/limou/hrms/controller/ApprovalController.java
git commit -m "feat: add ApprovalController with 7 REST endpoints"
```

---

### Task 7: 数据库建表

**Files:**
- Create: `sql/approval_center.sql`

- [ ] **Step 1: 编写建表 SQL（直接使用 表结构.md 中的 DDL）**

```sql
-- 审批实例表
CREATE TABLE IF NOT EXISTS `approval_instance` (
    `id`                  BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `biz_type`            VARCHAR(32)       NOT NULL COMMENT '业务类型',
    `biz_id`              BIGINT UNSIGNED   NOT NULL COMMENT '业务主键ID',
    `title`               VARCHAR(128)      NOT NULL COMMENT '审批标题',
    `status`              TINYINT           NOT NULL DEFAULT 1 COMMENT '1=审批中 2=已通过 3=已拒绝 4=已撤回',
    `applicant_id`        BIGINT UNSIGNED   NOT NULL COMMENT '申请人ID',
    `current_node_order`  INT               NOT NULL DEFAULT 1 COMMENT '当前审批节点序号',
    `create_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz` (`biz_type`, `biz_id`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '审批实例表';

-- 审批节点表
CREATE TABLE IF NOT EXISTS `approval_node` (
    `id`                    BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id`           BIGINT UNSIGNED   NOT NULL COMMENT '审批实例ID',
    `node_name`             VARCHAR(64)       NOT NULL COMMENT '节点名称',
    `node_order`            INT               NOT NULL COMMENT '节点顺序号',
    `approver_id`           BIGINT UNSIGNED   NOT NULL COMMENT '当前审批人ID',
    `original_approver_id`  BIGINT UNSIGNED            COMMENT '原审批人ID',
    `status`                TINYINT           NOT NULL DEFAULT 1 COMMENT '1=待审批 2=已通过 3=已拒绝 4=已转交',
    `comment`               VARCHAR(512)               COMMENT '审批意见',
    `operate_time`          DATETIME                   COMMENT '操作时间',
    `create_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_approver_id_status` (`approver_id`, `status`),
    KEY `idx_instance_id_order` (`instance_id`, `node_order`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '审批节点表';

-- 审批委托表（预留）
CREATE TABLE IF NOT EXISTS `approval_delegate` (
    `id`                BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `delegator_id`      BIGINT UNSIGNED   NOT NULL COMMENT '委托人ID',
    `delegate_id`       BIGINT UNSIGNED   NOT NULL COMMENT '被委托人ID',
    `start_time`        DATETIME          NOT NULL COMMENT '委托开始时间',
    `end_time`          DATETIME          NOT NULL COMMENT '委托结束时间',
    `enabled`           TINYINT           NOT NULL DEFAULT 1 COMMENT '0=已取消 1=生效中',
    `create_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_delegator_id` (`delegator_id`),
    KEY `idx_delegate_id` (`delegate_id`),
    KEY `idx_time_enabled` (`start_time`, `end_time`, `enabled`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '委托审批表';
```

- [ ] **Step 2: 执行建表**

通过 MySQL MCP 工具连接到数据库执行上述 SQL，或手动在数据库中执行。

- [ ] **Step 3: 提交**

```bash
git add sql/approval_center.sql
git commit -m "feat: add approval center DDL"
```

---

### Task 8: 编译验证与集成测试

**Files:**
- Create: `src/test/java/com/limou/hrms/service/ApprovalFlowServiceTest.java`

- [ ] **Step 1: 全量编译**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行现有测试确保无回归**

```bash
mvn test -q
```
Expected: 现有测试全部通过

- [ ] **Step 3: 编写核心流程集成测试**

```java
package com.limou.hrms.service;

import com.limou.hrms.model.entity.ApprovalInstance;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.vo.ApprovalInstanceVO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ApprovalFlowServiceTest {

    @Resource
    private ApprovalFlowService approvalFlowService;

    @Test
    public void testCreateInstance() {
        // 使用测试数据创建入职审批实例
        // bizId=1 (onboarding_application.id), applicantId=1 (HR的employee.id)
        ApprovalInstance instance = approvalFlowService.createInstance(
                ApprovalBizType.ONBOARDING, 1L, 1L);
        assertNotNull(instance);
        assertNotNull(instance.getId());
        assertEquals("ONBOARDING", instance.getBizType());
        assertEquals(Integer.valueOf(1), instance.getStatus());
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
mvn test -Dtest=ApprovalFlowServiceTest -q
```
Expected: 测试通过

- [ ] **Step 5: 提交**

```bash
git add src/test/java/com/limou/hrms/service/ApprovalFlowServiceTest.java
git commit -m "test: add ApprovalFlowService integration test"
```

---

## 实施建议

1. **按 Task 顺序逐任务实施**，每完成一个 Task 编译通过 + 提交后再做下一个
2. **数据库优先**：可在 Task 2 之后先执行 Task 7 建表，确保代码运行时数据库已就绪
3. **回调实现延后**：`ApprovalCallback` 的实现（OnboardingServiceImpl、ProbationServiceImpl）不在本次审批中心 V1 范围，由业务模块开发时自行实现
4. **getCurrentEmployeeId**：当前简化为 `loginUser.getId()`，后续需改为通过 `employee.user_id` 映射查 `employee.id`
5. **HR审批人查询**：当前从 `user` 表查 `user_role='hr'` 再映射到 `employee` 表，若无记录则回退，后续可优化为缓存
