package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.regularization.RegularizationApplicationAddRequest;
import com.limou.hrms.model.dto.regularization.RegularizationApplicationQueryRequest;
import com.limou.hrms.model.entity.RegularizationApplication;
import com.limou.hrms.model.vo.RegularizationApplicationVO;
import com.limou.hrms.service.RegularizationApplicationService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 转正申请控制器
 */
@RestController
@RequestMapping("/v1/regularization-applications")
@Slf4j
public class RegularizationApplicationController {

    @Resource
    private RegularizationApplicationService regularizationApplicationService;

    @Resource
    private UserService userService;

    /**
     * 创建转正申请
     */
    @PostMapping
    public BaseResponse<Long> createApplication(@RequestBody RegularizationApplicationAddRequest addRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = userService.getLoginUser(request).getId();
        Long id = regularizationApplicationService.createApplication(addRequest, userId);
        return ResultUtils.success(id);
    }

    /**
     * 获取待转正员工列表
     */
    @GetMapping("/pending")
    public BaseResponse<List<RegularizationApplicationVO>> getPendingList() {
        List<RegularizationApplicationVO> pendingList = regularizationApplicationService.getPendingList();
        return ResultUtils.success(pendingList);
    }

    /**
     * 根据ID获取转正申请详情
     */
    @GetMapping("/{id}")
    public BaseResponse<RegularizationApplicationVO> getById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        RegularizationApplication entity = regularizationApplicationService.getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(regularizationApplicationService.getVO(entity));
    }

    /**
     * 分页查询转正申请列表
     */
    @GetMapping
    public BaseResponse<Page<RegularizationApplicationVO>> listByPage(
            RegularizationApplicationQueryRequest queryRequest) {
        if (queryRequest == null) {
            queryRequest = new RegularizationApplicationQueryRequest();
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<RegularizationApplication> page = regularizationApplicationService.page(
                new Page<>(current, size),
                regularizationApplicationService.getQueryWrapper(queryRequest));
        Page<RegularizationApplicationVO> voPage = new Page<>(current, size, page.getTotal());
        List<RegularizationApplicationVO> voList = regularizationApplicationService.getVOList(page.getRecords());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
