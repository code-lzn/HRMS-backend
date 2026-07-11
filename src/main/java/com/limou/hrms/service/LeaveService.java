package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.Leave;
import com.limou.hrms.model.vo.LeaveVO;

import java.util.List;

/**
 * 请假服务
 */
public interface LeaveService extends IService<Leave> {

    /**
     * 申请请假
     */
    LeaveVO apply(Long userId, Integer leaveType, String startDate, String endDate, String reason);

    /**
     * 审批请假
     */
    LeaveVO approve(Long requestId, Integer result, String comment, Long approverId);

    /**
     * 获取我的请假记录
     */
    List<LeaveVO> getMyLeaves(Long userId);

    /**
     * 撤销请假
     */
    void cancel(Long requestId, Long userId);
}
