package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.attendance.OvertimeApplyRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.OvertimeVO;
import com.limou.hrms.service.OvertimeService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/attendance/overtime")
@Slf4j
public class OvertimeController {

    @Resource
    private OvertimeService overtimeService;

    @Resource
    private UserService userService;

    /**
     * 申请加班
     */
    @PostMapping("/apply")
    public BaseResponse<OvertimeVO> apply(@RequestBody OvertimeApplyRequest request,
                                          HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        OvertimeVO vo = overtimeService.apply(loginUser.getId(),
                request.getOvertimeDate(), request.getStartTime(), request.getEndTime(),
                request.getOvertimeHours(), request.getOvertimeType(), request.getReason());
        return ResultUtils.success(vo);
    }

    /**
     * 获取我的加班记录
     */
    @GetMapping("/my")
    public BaseResponse<List<OvertimeVO>> getMyOvertimes(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<OvertimeVO> list = overtimeService.getMyOvertimes(loginUser.getId());
        return ResultUtils.success(list);
    }

    /**
     * 撤回加班申请
     */
    @PostMapping("/cancel/{id}")
    public BaseResponse<Boolean> cancel(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        overtimeService.cancel(id, loginUser.getId());
        return ResultUtils.success(true);
    }
}
