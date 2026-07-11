package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.transfer.TransferApplicationAddRequest;
import com.limou.hrms.model.dto.transfer.TransferApplicationQueryRequest;
import com.limou.hrms.model.entity.TransferApplication;
import com.limou.hrms.model.vo.TransferApplicationVO;
import com.limou.hrms.service.TransferApplicationService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 调岗申请控制器
 */
@RestController
@RequestMapping("/v1/transfer-applications")
@Slf4j
public class TransferApplicationController {

    @Resource
    private TransferApplicationService transferApplicationService;

    @Resource
    private UserService userService;

    /**
     * 创建调岗申请
     */
    @PostMapping
    public BaseResponse<Long> createApplication(@RequestBody TransferApplicationAddRequest addRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = userService.getLoginUser(request).getId();
        Long id = transferApplicationService.createApplication(addRequest, userId);
        return ResultUtils.success(id);
    }

    /**
     * 根据ID获取调岗申请详情
     */
    @GetMapping("/{id}")
    public BaseResponse<TransferApplicationVO> getById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        TransferApplication entity = transferApplicationService.getById(id);
        ThrowUtils.throwIf(entity == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(transferApplicationService.getVO(entity));
    }

    /**
     * 分页查询调岗申请列表
     */
    @GetMapping
    public BaseResponse<Page<TransferApplicationVO>> listByPage(TransferApplicationQueryRequest queryRequest) {
        if (queryRequest == null) {
            queryRequest = new TransferApplicationQueryRequest();
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<TransferApplication> page = transferApplicationService.page(
                new Page<>(current, size),
                transferApplicationService.getQueryWrapper(queryRequest));
        Page<TransferApplicationVO> voPage = new Page<>(current, size, page.getTotal());
        List<TransferApplicationVO> voList = transferApplicationService.getVOList(page.getRecords());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
