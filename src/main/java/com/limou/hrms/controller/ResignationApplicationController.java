package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.resignation.ResignationApplicationAddRequest;
import com.limou.hrms.model.dto.resignation.ResignationApplicationQueryRequest;
import com.limou.hrms.model.entity.ResignationApplication;
import com.limou.hrms.model.vo.ResignationApplicationVO;
import com.limou.hrms.service.ResignationApplicationService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 离职申请控制器
 */
@RestController
@RequestMapping("/v1/resignation-applications")
@Slf4j
public class ResignationApplicationController {

    @Resource
    private ResignationApplicationService resignationApplicationService;

    @Resource
    private UserService userService;

    /**
     * 创建离职申请
     */
    @PostMapping
    public BaseResponse<Long> createApplication(@RequestBody ResignationApplicationAddRequest addRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = userService.getLoginUser(request).getId();
        Long id = resignationApplicationService.createApplication(addRequest, userId);
        return ResultUtils.success(id);
    }

    /**
     * 根据ID获取离职申请详情
     */
    @GetMapping("/{id}")
    public BaseResponse<ResignationApplicationVO> getById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ResignationApplication entity = resignationApplicationService.getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(resignationApplicationService.getVO(entity));
    }

    /**
     * 分页查询离职申请列表
     */
    @GetMapping
    public BaseResponse<Page<ResignationApplicationVO>> listByPage(ResignationApplicationQueryRequest queryRequest) {
        if (queryRequest == null) {
            queryRequest = new ResignationApplicationQueryRequest();
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<ResignationApplication> page = resignationApplicationService.page(
                new Page<>(current, size),
                resignationApplicationService.getQueryWrapper(queryRequest));
        Page<ResignationApplicationVO> voPage = new Page<>(current, size, page.getTotal());
        List<ResignationApplicationVO> voList = resignationApplicationService.getVOList(page.getRecords());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 查询员工异动历史（入职/转正/调岗/离职）
     */
    @GetMapping("/employees/{employeeId}/change-history")
    public BaseResponse<List<ResignationApplicationVO>> getChangeHistory(@PathVariable Long employeeId) {
        ThrowUtils.throwIf(employeeId == null || employeeId <= 0, ErrorCode.PARAMS_ERROR);
        // todo: 综合查询员工的所有异动记录（入职、转正、调岗、离职），按时间倒序
        // 目前先返回离职记录
        ResignationApplicationQueryRequest queryRequest = new ResignationApplicationQueryRequest();
        queryRequest.setEmployeeId(employeeId);
        List<ResignationApplication> list = resignationApplicationService.list(
                resignationApplicationService.getQueryWrapper(queryRequest));
        return ResultUtils.success(resignationApplicationService.getVOList(list));
    }
}
