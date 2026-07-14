# HRMS-员工档案管理-后端开发计划书

## 变更记录

| **日期** | **版本** | **修订说明** | **作者** |
| --- | --- | --- | --- |
| 2026-07-13 | 1.0 | 初稿 | - |

---

## 一、开发目标

实现员工档案管理模块后端全部 9 个 API 接口，涵盖员工 CRUD、分页高级搜索、工号生成、字段权限、变更日志审计。

## 二、Git 分支

```
feature/20260710_HRMS3.01（当前分支，组织架构模块也在该分支上）
```

## 三、数据库变更

> 采用主表 + 详情表方案。已有 employee 表保持不动，新增 employee_detail 表。

### 3.1 已有 employee 表（保持不变）

employee 表已包含 20 列核心字段（id, employeeName, employeeNo, userId, account, status, gender, phone, email, departmentId, positionId, jobLevel, hireDate, hireType, salaryId, employmentType, createTime, updateTime, isDeleted），无需修改。

### 3.2 新建 employee_detail 表

详见 `sql/employee_restructure.sql`，包含：
- 敏感字段：`idCard`, `bankAccount`
- 大文本字段：`currentAddress`, `registeredAddress`
- 工作补充：`jobLevel`, `directReportId`, `workLocation`
- 薪资合同：`contractType`, `contractExpireDate`, `probationRatio`, `baseSalary`
- 紧急联系人：`emergencyContactName`, `emergencyContactPhone`
- 其他：`account`, `birthday`, `bankName`

### 3.3 新建 employee_change_log 表

```sql
CREATE TABLE IF NOT EXISTS `employee_change_log` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`     BIGINT UNSIGNED NOT NULL COMMENT '员工ID',
    `field_name`      VARCHAR(64)     NOT NULL COMMENT '变更字段名',
    `old_value`       VARCHAR(512)    NULL COMMENT '变更前值',
    `new_value`       VARCHAR(512)    NULL COMMENT '变更后值',
    `change_type`     VARCHAR(32)     NOT NULL COMMENT 'DIRECT_EDIT/FLOW_CHANGE/SYSTEM',
    `operator_id`     BIGINT UNSIGNED NULL COMMENT '操作人ID',
    `remark`          VARCHAR(256)    NULL COMMENT '备注',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_employee_id` (`employee_id`),
    KEY `idx_create_time` (`create_time`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '员工档案变更日志表';
```

---

## 四、代码文件清单

### 4.1 需要新建的文件（16 个）

| # | 文件 | 说明 |
|---|---|---|
| 1 | `constant/EmployeeConstant.java` | 员工状态枚举、录用类型枚举、合同类型枚举 |
| 2 | `mapper/EmployeeChangeLogMapper.java` | 变更日志 Mapper |
| 3 | `model/entity/EmployeeChangeLog.java` | 变更日志实体 |
| 4 | `model/dto/employee/EmployeeAddRequest.java` | 新增员工请求 |
| 5 | `model/dto/employee/EmployeeUpdateRequest.java` | 更新员工请求 |
| 6 | `model/dto/employee/EmployeeQueryRequest.java` | 员工列表查询请求 |
| 7 | `model/vo/EmployeeVO.java` | 员工列表 VO |
| 8 | `model/vo/EmployeeDetailVO.java` | 员工详情 VO（四分区嵌套） |
| 9 | `model/vo/EmployeeChangeLogVO.java` | 变更日志 VO |
| 10 | `model/vo/FieldPermissionVO.java` | 字段权限 VO |
| 11 | `service/EmployeeService.java` | 员工服务接口 |
| 12 | `service/EmployeeChangeLogService.java` | 变更日志服务接口 |
| 13 | `service/impl/EmployeeServiceImpl.java` | 员工服务实现 |
| 14 | `service/impl/EmployeeChangeLogServiceImpl.java` | 变更日志服务实现 |
| 15 | `controller/EmployeeController.java` | 员工管理接口（9 个端点） |
| 16 | `sql/employee_update.sql` | employee 表 DDL 更新脚本 |

### 4.2 需要更新已有文件（1 个）

| # | 文件 | 变更 |
|---|---|---|
| 1 | `model/entity/Employee.java` | 已有最小结构，需补全所有新字段 + `@TableField` 映射（30+ 字段） |

---

## 五、开发阶段

### Phase 1：数据库变更（0.5天）

| 步骤 | 内容 |
|---|---|
| 1.1 | 编写 `sql/employee_restructure.sql`（建 employee_detail + 迁移数据 + 建 employee_change_log） |
| 1.2 | 在 MySQL 执行脚本 |

### Phase 2：Entity + Mapper + 常量（0.5天）

| 步骤 | 内容 |
|---|---|
| 2.1 | 更新 `model/entity/Employee.java`（补全 30+ 字段 + `@TableField` 映射） |
| 2.2 | 新建 `model/entity/EmployeeChangeLog.java` |
| 2.3 | 新建 `mapper/EmployeeMapper.java`（更新已有，补充分页查询 SQL） |
| 2.4 | 新建 `mapper/EmployeeChangeLogMapper.java` |
| 2.5 | 新建 `constant/EmployeeConstant.java`（状态/录用类型/合同类型枚举） |

### Phase 3：DTO + VO（0.5天）

| 步骤 | 内容 |
|---|---|
| 3.1 | `EmployeeAddRequest.java`（全字段，含校验注解） |
| 3.2 | `EmployeeUpdateRequest.java`（id + 部分可编辑字段） |
| 3.3 | `EmployeeQueryRequest.java`（高级搜索字段：多选 + 日期范围） |
| 3.4 | `EmployeeVO.java`（列表展示字段，含 departmentName / positionName / statusDesc） |
| 3.5 | `EmployeeDetailVO.java`（四分区嵌套 VO） |
| 3.6 | `EmployeeChangeLogVO.java` |
| 3.7 | `FieldPermissionVO.java` |

### Phase 4：Service 业务层（1.5天）

| 步骤 | 内容 |
|---|---|
| 4.1 | `EmployeeService` 接口：list / detail / add / update / delete / generateNo / getPermissions / getChangeLogs / export |
| 4.2 | `EmployeeServiceImpl.listEmployees()`：分页 + 高级搜索 + 关联查询部门名/职位名 |
| 4.3 | `EmployeeServiceImpl.addEmployee()`：参数校验 + 部门/职位存在校验 + 工号生成 + 系统账号创建 |
| 4.4 | `EmployeeServiceImpl.updateEmployee()`：字段权限校验 + 变更日志记录 |
| 4.5 | `EmployeeServiceImpl.deleteEmployee()`：软删除 + 关联数据校验 |
| 4.6 | `EmployeeServiceImpl.generateEmployeeNo()`：工号生成（年份+部门编码+序号） |
| 4.7 | `EmployeeServiceImpl.getFieldPermissions()`：按当前用户角色返回权限 |
| 4.8 | `EmployeeServiceImpl.getChangeLogs()`：分页查询变更日志 |
| 4.9 | `EmployeeServiceImpl.exportEmployees()`：Excel 导出（EasyExcel） |
| 4.10 | `EmployeeChangeLogService` + 实现 |

### Phase 5：Controller 接口层（0.5天）

| 步骤 | 内容 |
|---|---|
| 5.1 | `EmployeeController`：9 个端点，统一路径 `/employees` |
| 5.2 | 添加 Knife4j `@Api` + `@ApiOperation` 注解 |
| 5.3 | 参数校验（空值、类型） |

### Phase 6：编译验证 + Swagger 测试（0.5天）

| 步骤 | 内容 |
|---|---|
| 6.1 | `mvn clean compile` 编译通过 |
| 6.2 | 启动项目，Swagger 逐个接口测试 |
| 6.3 | 验证高级搜索多选参数 |
| 6.4 | 验证字段权限返回值 |
| 6.5 | 验证工号唯一性 |

---

## 六、接口开发顺序（推荐）

| 优先级 | 接口 | 依赖 |
|---|---|---|
| P0 | `GET /employees/list` | EmployeeMapper 分页查询 |
| P0 | `POST /employees/generate-employee-no` | 部门编码查询 |
| P0 | `POST /employees/add` | 部门/职位校验 |
| P1 | `GET /employees/detail` | 关联查询 |
| P1 | `PUT /employees/update` | 字段权限校验 |
| P1 | `POST /employees/delete` | 关联数据校验 |
| P2 | `GET /employees/field-permissions` | 角色判断 |
| P2 | `GET /employees/change-logs` | EmployeeChangeLogMapper |
| P3 | `GET /employees/export` | EasyExcel |

---

## 七、关键技术点

### 7.1 工号生成

```java
String year = String.valueOf(Year.now().getValue());          // "2025"
String deptCode = department.getDeptCode();                    // "01"
int maxSeq = employeeMapper.getMaxSeq(year, deptCode);         // 查询同年同部门最大序号
String seq = String.format("%03d", maxSeq + 1);               // "005"
String employeeNo = year + deptCode + seq;                     // "202501005"
```

### 7.2 高级搜索 SQL

使用 MyBatis-Plus `LambdaQueryWrapper` 动态构建：
- `like` 模糊匹配 keyword（employeeName / employeeNo / phone）
- `in` 多选筛选（departmentIds / positionIds / statuses / jobLevels）
- `between` 日期范围（hireDateStart ~ hireDateEnd）

### 7.3 字段权限

`GET /api/employees/field-permissions` 根据当前登录用户角色返回：
```json
{
  "editableFields": ["email", "currentAddress", "birthday"],
  "readonlyFields": ["phone", "idCard", "departmentId", "positionId"],
  "hiddenFields": ["contractType", "baseSalary", "bankAccount"]
}
```

### 7.4 变更日志

更新员工时比对 old/new 值，只记录实际变更的字段：
```java
if (!Objects.equals(oldEmail, newEmail)) {
    changeLog.setFieldName("email");
    changeLog.setOldValue(oldEmail);
    changeLog.setNewValue(newEmail);
    changeLog.setChangeType("DIRECT_EDIT");
    changeLogService.save(changeLog);
}
```

### 7.5 详情四分区 VO

```java
public class EmployeeDetailVO {
    // 基础信息
    private Long id;
    private String employeeNo;
    private String account;
    private Integer status;
    // 个人信息
    private PersonalInfo personalInfo;
    // 工作信息
    private WorkInfo workInfo;
    // 薪资信息
    private SalaryInfo salaryInfo;
}
```

### 7.6 系统账号创建

```java
// 新增员工后自动创建
User user = new User();
user.setUserAccount(request.getPhone());         // 账号=手机号
String randomPwd = RandomStringUtils.randomAlphanumeric(8); // 随机8位
user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + randomPwd).getBytes()));
userService.save(user);
employee.setUserId(user.getId());
employee.setAccount(request.getPhone());
```

---

## 八、注意事项

| # | 说明 |
|---|---|
| 1 | **不修改已有代码**：只新增文件，不动现有的 Department/Position 等代码 |
| 2 | **employee 表字段兼容**：已有 `name` 列需确认是重命名为 `employee_name` 还是保留 |
| 3 | **DK 8/11 编译**：确保 `JAVA_HOME=D:\jdk17`（pom.xml 已改为 Java 11） |
| 4 | **`@TableField` 映射**：DB 列名下划线 + `map-underscore-to-camel-case: false`，所有下划线列名必须用 `@TableField` |
| 5 | **字段权限先简化**：Phase 1 先不做复杂角色判断，返回默认 HR 全权限 |
| 6 | **变更日志先记录**：Phase 1 只记录字段名+新旧值，不做复杂比对 |

---

## 九、排期总览

| 阶段 | 内容 | 预估 |
|---|---|---|
| Phase 1 | 数据库变更 | 0.5天 |
| Phase 2 | Entity + Mapper + 常量 | 0.5天 |
| Phase 3 | DTO + VO | 0.5天 |
| Phase 4 | Service 业务层 | 1.5天 |
| Phase 5 | Controller 接口层 | 0.5天 |
| Phase 6 | 编译 + Swagger 测试 | 0.5天 |
| **合计** | | **4 天** |
