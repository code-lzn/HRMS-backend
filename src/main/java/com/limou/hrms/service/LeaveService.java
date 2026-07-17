package com.limou.hrms.service;

import com.limou.hrms.model.dto.leave.LeaveRequestSubmitDTO;
import com.limou.hrms.model.vo.LeaveRequestVO;

/**
 * 请假服务
 */
public interface LeaveService {

    /**
     * 提交请假申请
     */
    LeaveRequestVO submitLeaveRequest(LeaveRequestSubmitDTO dto);
}
