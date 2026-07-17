package com.limou.hrms.controller;

import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.department.DepartmentCreateRequest;
import com.limou.hrms.model.dto.department.DepartmentUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.DepartmentTreeNode;
import com.limou.hrms.model.vo.DepartmentVO;
import com.limou.hrms.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 部门管理控制器 — 组织架构 CRUD
 */
@RestController
@RequestMapping("/api/v1/departments")
@Slf4j
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * GET /api/v1/departments?keyword= — 查询部门（平铺返回，前端按 parentId 自行组装树）
     */
    @GetMapping("tree")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<List<DepartmentTreeNode>> getDepartmentList(@RequestParam(required = false) String keyword) {
        log.info("{} 查询部门列表, keyword={}", UserContext.getCurrentUser(), keyword);
        List<DepartmentTreeNode> list = departmentService.queryDepartments(keyword);
        return ResultUtils.success(list);
    }

    /**
     * GET /api/v1/departments/{id} — 查询部门详情（含子部门简要信息）
     */
    @GetMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<DepartmentVO> getDepartmentDetail(@PathVariable Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("{} 查询部门详情, id={}", UserContext.getCurrentUser(), id);
        DepartmentVO vo = departmentService.getDepartmentDetail(id);
        return ResultUtils.success(vo);
    }

    /**
     * POST /api/v1/departments — 创建部门
     */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Department> createDepartment(@Valid @RequestBody DepartmentCreateRequest dto) {
        User loginUser = UserContext.getCurrentUser();
        log.info("{} 创建部门, name={}", loginUser, dto.getName());
        Department department = departmentService.createDepartment(dto, loginUser);
        return ResultUtils.success(department);
    }

    /**
     * PUT /api/v1/departments/{id} — 更新部门（部分更新）
     */
    @PutMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Department> updateDepartment(@PathVariable Long id,
                                                      @Valid @RequestBody DepartmentUpdateRequest dto) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = UserContext.getCurrentUser();
        log.info("{} 更新部门, id={}", loginUser, id);
        Department department = departmentService.updateDepartment(id, dto, loginUser);
        return ResultUtils.success(department);
    }

    /**
     * DELETE /api/v1/departments/{id} — 删除部门（逻辑删除，需先清空子部门和员工）
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Void> deleteDepartment(@PathVariable Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = UserContext.getCurrentUser();
        log.info("{} 删除部门, id={}", loginUser, id);
        departmentService.deleteDepartment(id, loginUser);
        return ResultUtils.success(null);
    }
}