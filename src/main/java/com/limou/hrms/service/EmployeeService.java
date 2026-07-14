package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.employee.*;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.vo.*;

public interface EmployeeService extends IService<Employee> {

    // ==================== 员工档案管理 ====================

    /** 员工列表（分页 + 高级搜索） */
    Page<EmployeeVO> listEmployees(EmployeeQueryRequest request);

    /** 员工详情 */
    EmployeeDetailVO getDetail(Long id);

    /** 新增员工 + 生成工号 */
    Long addEmployee(EmployeeAddRequest request);

    /** 更新员工 */
    void updateEmployee(EmployeeUpdateRequest request);

    /** 删除员工（软删除） */
    void deleteEmployee(Long id);

    /** 生成工号 */
    String generateEmployeeNo(Long departmentId);

    /** 获取字段权限 */
    FieldPermissionVO getFieldPermissions();

    /** 员工变更历史 */
    Page<EmployeeChangeLogVO> getChangeLogs(Long employeeId, int page, int size);

    // ==================== 个人中心 ====================

    /** 获取当前登录员工档案 */
    EmpProfileVO getProfile(Long userId);

    /** 更新当前登录员工可编辑字段 */
    void updateProfile(Long userId, EmpProfileUpdateRequest request);

    /** 通过用户ID获取员工 */
    Employee getByUserId(Long userId);
}
