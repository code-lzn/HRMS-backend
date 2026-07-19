package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.leave.LeaveCreateDTO;
import com.limou.hrms.model.dto.leave.LeaveUpdateDTO;
import com.limou.hrms.model.query.LeaveQuery;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.model.vo.LeaveRequestVO;

/**
 * 请假服务
 */
public interface LeaveService {

    // ==================== 请假 CRUD ====================

    /** 创建请假草稿（可一键提交审批） */
    Long createApplication(LeaveCreateDTO dto);

    /** 更新草稿 */
    void updateDraft(Long id, LeaveUpdateDTO dto);

    /** 删除草稿 */
    void deleteDraft(Long id);

    /** 提交审批 */
    void submitToApproval(Long id);

    /** 撤回申请 */
    void cancel(Long id);

    // ==================== 查询 ====================

    /** 分页查询请假列表（含关键字搜索 + 数据权限） */
    Page<LeaveRequestVO> queryRequests(LeaveQuery query);

    /** 查询请假详情 */
    LeaveRequestVO getRequestDetail(Long id);

    /** 查询假期余额 */
    LeaveBalanceVO getBalances(Long employeeId, Integer year);
}
