package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.vo.MutationLogVO;
import com.limou.hrms.service.MutationLogService;
import com.limou.hrms.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/mutation-logs")
public class MutationLogController {

    @Resource
    private MutationLogService mutationLogService;

    @Resource
    private UserService userService;

    @GetMapping("/my")
    public BaseResponse<List<MutationLogVO>> getMyLogs(HttpServletRequest request) {
        Long userId = userService.getLoginUser(request).getId();
        return ResultUtils.success(mutationLogService.getMyMutationLogs(userId));
    }
}
