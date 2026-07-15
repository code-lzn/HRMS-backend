package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.department.DepartmentCreateRequest;
import com.limou.hrms.model.dto.department.DepartmentQueryRequest;
import com.limou.hrms.model.dto.department.DepartmentUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.DepartmentTreeNode;
import com.limou.hrms.model.vo.DepartmentVO;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 部门管理控制器
 */
@RestController
@RequestMapping("/api/v1/departments")
@Slf4j
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    private final UserService userService;

    /**
     * 查询部门树（含人数统计）
     */
    @GetMapping("/tree")
    public BaseResponse<List<DepartmentTreeNode>> getDepartmentTree(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<DepartmentTreeNode> tree = departmentService.getDepartmentTree(loginUser);
        return ResultUtils.success(tree);
    }

    /**
     * 查询部门列表（平铺/分页）
     */
    @GetMapping
    public BaseResponse<Page<DepartmentVO>> getDepartmentList(DepartmentQueryRequest queryReq,
                                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<DepartmentVO> page = departmentService.getDepartmentList(queryReq, loginUser);
        return ResultUtils.success(page);
    }

    /**
     * 查询部门详情
     */
    @GetMapping("/{id}")
    public BaseResponse<DepartmentVO> getDepartmentDetail(@PathVariable Long id,
                                                           HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        DepartmentVO vo = departmentService.getDepartmentDetail(id, loginUser);
        return ResultUtils.success(vo);
    }

    /**
     * 创建部门
     */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Department> createDepartment(@Valid @RequestBody DepartmentCreateRequest dto,
                                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Department department = departmentService.createDepartment(dto, loginUser);
        return ResultUtils.success(department);
    }

    /**
     * 更新部门
     */
    @PutMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Department> updateDepartment(@PathVariable Long id,
                                                      @RequestBody DepartmentUpdateRequest dto,
                                                      HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Department department = departmentService.updateDepartment(id, dto, loginUser);
        return ResultUtils.success(department);
    }

    /**
     * 删除部门（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Void> deleteDepartment(@PathVariable Long id,
                                                HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        departmentService.deleteDepartment(id, loginUser);
        return ResultUtils.success(null);
    }
}
