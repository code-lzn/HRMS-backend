package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.employee.EmployeeCreateRequest;
import com.limou.hrms.model.dto.employee.EmployeeQueryRequest;
import com.limou.hrms.model.dto.employee.EmployeeUpdateRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.EmployeeCreateVO;
import com.limou.hrms.model.vo.EmployeeDetailVO;
import com.limou.hrms.model.vo.EmployeeListVO;
import com.limou.hrms.model.vo.EmployeeUpdateVO;
import com.limou.hrms.model.vo.FieldPermissionsVO;

import java.util.List;
import java.util.Map;

/**
 * 员工档案服务
 */
public interface EmployeeService extends IService<Employee> {

    /**
     * 获取在职状态枚举列表
     */
    List<Map<String, Object>> getStatuses();

    /**
     * 获取字段级权限配置
     * <p>
     * 按当前登录用户角色返回 viewableFields / editableFields / flowRequiredFields。
     */
    FieldPermissionsVO getFieldPermissions(User loginUser);

    /**
     * 创建员工档案
     * <p>
     * 四表（employee + personal_info + work_info + salary_info）事务写入，
     * 自动生成工号、创建系统账号、AES 加密敏感字段。
     *
     * @param dto       创建请求
     * @param loginUser 当前登录用户
     * @return 创建结果（含工号、账号、初始密码）
     */
    EmployeeCreateVO createEmployee(EmployeeCreateRequest dto, User loginUser);

    /**
     * 获取员工档案详情
     * <p>
     * 按角色进行字段脱敏（身份证号、手机号、银行卡号）、
     * 薪资信息可见性控制、数据范围权限校验。
     */
    EmployeeDetailVO getEmployeeDetail(Long id, User loginUser);

    /**
     * 分页查询员工列表
     * <p>
     * 支持高级搜索（关键词/部门/职位/状态/职级/入职日期） + 数据权限过滤。
     */
    Page<EmployeeListVO> listEmployees(EmployeeQueryRequest query, User loginUser);

    /**
     * 更新员工档案（逐字段权限校验）
     * <p>
     * DTO 中 null 字段不更新、传值则更新，后端按角色 editableFields 逐字段校验。
     *
     * @param id        员工ID
     * @param dto       更新请求（所有字段可选）
     * @param loginUser 当前登录用户
     * @return updatedFields + flowRequiredFields
     */
    EmployeeUpdateVO updateEmployee(Long id, EmployeeUpdateRequest dto, User loginUser);

    /**
     * 导出员工档案列表为 Excel
     *
     * @param query     筛选条件
     * @param loginUser 当前登录用户
     * @return 员工列表数据（含状态描述）
     */
    List<EmployeeListVO> exportEmployees(EmployeeQueryRequest query, User loginUser);
}