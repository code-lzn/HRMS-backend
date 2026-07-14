package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.DeleteRequest;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.employee.EmployeeAddRequest;
import com.limou.hrms.model.dto.employee.EmployeeQueryRequest;
import com.limou.hrms.model.dto.employee.EmployeeUpdateRequest;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.EmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工档案管理接口
 */
@RestController
@RequestMapping("/employees")
@Slf4j
public class EmployeesController {

    @Resource
    private EmployeeService employeeService;

    /**
     * 员工列表（分页 + 高级搜索）
     */
    @GetMapping("/list")
    public BaseResponse<Page<EmployeeVO>> listEmployees(
            String keyword,
            List<Long> departmentIds,
            List<Long> positionIds,
            List<Integer> statuses, List<String> jobLevels,
            String hireDateStart,
            String hireDateEnd,
            Integer page,
            Integer size) {
        EmployeeQueryRequest request = new EmployeeQueryRequest();
        request.setKeyword(keyword);
        request.setDepartmentIds(departmentIds);
        request.setPositionIds(positionIds);
        request.setStatuses(statuses);
        request.setJobLevels(jobLevels);
        request.setHireDateStart(parseDate(hireDateStart));
        request.setHireDateEnd(parseDate(hireDateEnd));
        request.setPage(page);
        request.setSize(size);
        Page<EmployeeVO> result = employeeService.listEmployees(request);
        return ResultUtils.success(result);
    }

    /**
     * 员工详情
     */
    @GetMapping("/detail")
    public BaseResponse<EmployeeDetailVO> getDetail( Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        EmployeeDetailVO detail = employeeService.getDetail(id);
        return ResultUtils.success(detail);
    }

    /**
     * 新增员工
     */
    @PostMapping("/add")
    public BaseResponse<Map<String, Object>> addEmployee(@RequestBody EmployeeAddRequest request) {
        if (request == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Long id = employeeService.addEmployee(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        return ResultUtils.success(data);
    }

    /**
     * 更新员工
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updateEmployee(@RequestBody EmployeeUpdateRequest request) {
        if (request == null || request.getId() == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        employeeService.updateEmployee(request);
        return ResultUtils.success(true);
    }

    /**
     * 删除员工
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteEmployee(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        employeeService.deleteEmployee(request.getId());
        return ResultUtils.success(true);
    }

    /**
     * 员工变更历史
     */
    @GetMapping("/change-logs")
    public BaseResponse<Page<EmployeeChangeLogVO>> getChangeLogs(Long employeeId, Integer page,Integer size) {
        ThrowUtils.throwIf(employeeId == null || employeeId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(employeeService.getChangeLogs(employeeId, page, size));
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try { return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr); }
        catch (Exception e) { return null; }
    }
}
