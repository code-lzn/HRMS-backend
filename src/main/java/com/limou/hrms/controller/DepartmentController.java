package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.department.DepartmentAddRequest;
import com.limou.hrms.model.dto.department.DepartmentMergeRequest;
import com.limou.hrms.model.dto.department.DepartmentUpdateRequest;
import com.limou.hrms.model.vo.DepartmentMergeResultVO;
import com.limou.hrms.model.vo.DepartmentTreeVO;
import com.limou.hrms.service.DepartmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门管理接口
 */
@RestController
@RequestMapping("/v1/departments")
@Slf4j
@Api(tags = "部门管理")
public class DepartmentController {

    @Resource
    private DepartmentService departmentService;

    /**
     * 获取部门树
     */
    @GetMapping("/tree")
    @ApiOperation("获取部门树（含递归人数统计）")
    public BaseResponse<List<DepartmentTreeVO>> getDepartmentTree() {
        List<DepartmentTreeVO> tree = departmentService.getDepartmentTree();
        return ResultUtils.success(tree);
    }

    /**
     * 新增部门
     */
    @PostMapping
    @ApiOperation("新增部门")
    public BaseResponse<Map<String, Long>> addDepartment(@RequestBody DepartmentAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = departmentService.addDepartment(request);
        Map<String, Long> data = new HashMap<>();
        data.put("id", id);
        return ResultUtils.success(data);
    }

    /**
     * 更新部门
     */
    @PutMapping("/{id}")
    @ApiOperation("更新部门")
    public BaseResponse<Boolean> updateDepartment(
            @ApiParam("部门ID") @PathVariable Long id,
            @RequestBody DepartmentUpdateRequest request) {
        if (request == null || id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        request.setId(id);
        departmentService.updateDepartment(request);
        return ResultUtils.success(true);
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除部门（校验无子部门且无在职员工）")
    public BaseResponse<Boolean> deleteDepartment(@ApiParam("部门ID") @PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        departmentService.deleteDepartment(id);
        return ResultUtils.success(true);
    }

    /**
     * 合并部门
     */
    @PostMapping("/merge")
    @ApiOperation("合并部门（含员工批量转移）")
    public BaseResponse<DepartmentMergeResultVO> mergeDepartments(
            @RequestBody DepartmentMergeRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // TODO: 从登录上下文获取当前操作人ID
        Long operatorId = 1L;
        DepartmentMergeResultVO result = departmentService.mergeDepartments(request, operatorId);
        return ResultUtils.success(result);
    }
}
