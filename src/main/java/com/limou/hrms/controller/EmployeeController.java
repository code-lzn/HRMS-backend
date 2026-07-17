package com.limou.hrms.controller;

import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.exception.BusinessException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.employee.EmployeeCreateRequest;
import com.limou.hrms.model.dto.employee.EmployeeQueryRequest;
import com.limou.hrms.model.vo.EmployeeCreateVO;
import com.limou.hrms.model.vo.EmployeeDetailVO;
import com.limou.hrms.model.vo.EmployeeListVO;
import com.limou.hrms.model.vo.EmployeeUpdateVO;
import com.limou.hrms.model.vo.FieldPermissionsVO;
import com.limou.hrms.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 员工档案管理控制器
 */
@RestController
@RequestMapping("/employees")
@Slf4j
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 获取在职状态枚举
     * <p>
     * 供前端下拉筛选框使用，HR/管理员/部门主管可调用。
     */
    @GetMapping("/statuses")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<List<Map<String, Object>>> getStatuses() {
        log.info("{} 获取在职状态枚举", UserContext.getCurrentUser());
        List<Map<String, Object>> statuses = employeeService.getStatuses();
        return ResultUtils.success(statuses);
    }

    /**
     * 获取字段级权限配置
     */
    @GetMapping("/field-permissions")
    public BaseResponse<FieldPermissionsVO> getFieldPermissions() {
        log.info("{} 获取字段权限配置", UserContext.getCurrentUser());
        FieldPermissionsVO vo = employeeService.getFieldPermissions(UserContext.getCurrentUser());
        return ResultUtils.success(vo);
    }

    /**
     * 创建员工档案
     * <p>
     * 由入转调离模块在入职审批通过后调用，或 HR/管理员手动创建。
     * 自动生成工号、创建系统账号、AES 加密敏感字段，五表事务写入。
     */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<EmployeeCreateVO> createEmployee(@Valid @RequestBody EmployeeCreateRequest dto) {
        log.info("{} 创建员工档案, name={}", UserContext.getCurrentUser(), dto.getName());
        EmployeeCreateVO vo = employeeService.createEmployee(dto, UserContext.getCurrentUser());
        return ResultUtils.success(vo);
    }

    /**
     * 更新员工档案
     * <p>
     * 请求体为平铺的可选字段 Map，不传保持原值、传 null 清空。
     * 后端逐字段校验权限，允许的更新并记变更日志，不允许的返回 flowRequiredFields。
     */
    @PutMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<EmployeeUpdateVO> updateEmployee(@PathVariable Long id,
                                                          @RequestBody Map<String, Object> fields) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("{} 更新员工档案, id={}", UserContext.getCurrentUser(), id);
        EmployeeUpdateVO vo = employeeService.updateEmployee(id, fields, UserContext.getCurrentUser());
        return ResultUtils.success(vo);
    }

    /**
     * 查询员工列表（分页 + 高级搜索 + 数据权限）
     */
    @GetMapping
    public BaseResponse<Page<EmployeeListVO>> getEmployeeList(EmployeeQueryRequest query) {
        log.info("{} 查询员工列表, keyword={}", UserContext.getCurrentUser(), query.getKeyword());
        Page<EmployeeListVO> page = employeeService.listEmployees(query, UserContext.getCurrentUser());
        return ResultUtils.success(page);
    }

    /**
     * 获取员工档案详情
     * <p>
     * 按角色自动脱敏敏感字段、控制薪资信息可见性。
     */
    @GetMapping("/{id}")
    public BaseResponse<EmployeeDetailVO> getEmployeeDetail(@PathVariable Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("{} 查询员工详情, id={}", UserContext.getCurrentUser(), id);
        EmployeeDetailVO vo = employeeService.getEmployeeDetail(id, UserContext.getCurrentUser());
        return ResultUtils.success(vo);
    }
}