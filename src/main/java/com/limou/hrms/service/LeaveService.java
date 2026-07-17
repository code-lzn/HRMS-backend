package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.leave.LeaveRequestSubmitDTO;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.model.vo.LeaveRequestVO;

import java.time.LocalDate;

/**
 * 请假服务
 */
public interface LeaveService {

    /**
     * 提交请假申请
     */
    LeaveRequestVO submitLeaveRequest(LeaveRequestSubmitDTO dto);

    /**
     * 查询假期余额
     */
    LeaveBalanceVO getBalances(Long employeeId, Integer year);

    /**
     * 查询请假申请列表（分页 + 数据权限）
     */
    Page<LeaveRequestVO> queryRequests(Long employeeId, Integer leaveType, Integer status,
                                       LocalDate startDate, LocalDate endDate, int page, int size);

    /**
     * 查询请假申请详情
     */
    LeaveRequestVO getRequestDetail(Long id);

    /**
     * 取消请假申请（仅审批中状态可取消）
     */
    void cancelLeaveRequest(Long id, Long employeeId);
}
