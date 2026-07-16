package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.employee.*;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.vo.*;

public interface EmployeeService extends IService<Employee> {

    // ==================== 员工档案管理 ====================

    /** 员工列表（分页 + 高级搜索） */
    Page<EmployeeVO> listEmployees(EmployeeQueryRequest request, Long userId);

    /** 员工详情 */
    EmployeeDetailVO getDetail(Long id, Long userId);

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

    // ==================== 我的档案（新版） ====================

    /**
     * 获取我的档案详情（联表 employee + employee_detail）
     * <p>
     * 普通员工仅查本人，HR可查任意员工；
     * 根据角色自动脱敏/隐藏敏感字段
     *
     * @param loginUserId       当前登录用户ID
     * @param targetEmployeeId  目标员工ID（不传/null则默认当前登录员工）
     * @param isAdminOrHR       是否为HR/管理员角色
     * @return 完整档案信息
     */
    MyProfileVO getMyFullProfile(Long loginUserId, Long targetEmployeeId, boolean isAdminOrHR);

    /**
     * 修改个人可编辑档案信息（仅 currentAddress / emergencyContactName / emergencyContactPhone）
     * <p>
     * 自动对比新旧值，循环写入 change_log
     *
     * @param loginUserId 当前登录用户ID
     * @param request     更新请求
     * @return 更新时间
     */
    java.util.Date updateMyDetail(Long loginUserId, MyDetailUpdateRequest request);

    /**
     * 分页查询个人档案修改日志
     *
     * @param loginUserId       当前登录用户ID
     * @param targetEmployeeId  目标员工ID（不传/null则默认当前登录员工）
     * @param page              页码
     * @param size              每页条数
     * @return 分页日志
     */
    Page<EmployeeChangeLogVO> getMyChangeLogs(Long loginUserId, Long targetEmployeeId, int page, int size);
}
