package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.attendance.ApprovalRequest;
import com.limou.hrms.model.dto.attendance.MakeupPunchApplyRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.MakeupPunchProgressVO;
import com.limou.hrms.model.vo.MakeupPunchVO;
import com.limou.hrms.service.MakeupPunchService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 补卡接口
 */
@RestController
@RequestMapping("/attendance/makeup")
@Slf4j
public class MakeupPunchController {

    @Resource
    private MakeupPunchService makeupPunchService;

    @Resource
    private UserService userService;

    /**
     * 申请补卡
     */
    @PostMapping("/apply")
    public BaseResponse<MakeupPunchVO> apply(@RequestBody MakeupPunchApplyRequest applyRequest,
                                                     HttpServletRequest request) {
        if (applyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        MakeupPunchVO vo = makeupPunchService.apply(loginUser.getId(),
                applyRequest.getPunchDate(), applyRequest.getPunchType(),
                applyRequest.getPunchTime(), applyRequest.getReason());
        return ResultUtils.success(vo);
    }

    /**
     * 审批补卡----统一审批中心进行审批
     */
//    @PostMapping("/approve")
//    public BaseResponse<MakeupPunchVO> approve(@RequestBody ApprovalRequest approvalRequest,
//                                                       HttpServletRequest request) {
//        if (approvalRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        MakeupPunchVO vo = makeupPunchService.approve(approvalRequest.getId(),
//                approvalRequest.getResult(), approvalRequest.getComment(), loginUser.getId());
//        return ResultUtils.success(vo);
//    }

    /**
     * 获取我的补卡记录
     */
    @GetMapping("/my")
    public BaseResponse<List<MakeupPunchVO>> getMyMakeupPunches(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<MakeupPunchVO> list = makeupPunchService.getMyMakeupPunches(loginUser.getId());
        return ResultUtils.success(list);
    }

    /**
     * 获取补卡审批进度
     */
    @GetMapping("/{id}/progress")
    public BaseResponse<MakeupPunchProgressVO> getApprovalProgress(
            @PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        MakeupPunchProgressVO vo = makeupPunchService.getApprovalProgress(id, loginUser.getId());
        return ResultUtils.success(vo);
    }

    /**
     * 撤回补卡申请
     */
    @PostMapping("/cancel/{id}")
    public BaseResponse<Boolean> cancel(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        makeupPunchService.cancel(id, loginUser.getId());
        return ResultUtils.success(true);
    }
}
