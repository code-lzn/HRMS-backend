package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.resignation.ResignationAddRequest;
import com.limou.hrms.model.entity.HrResignation;
import com.limou.hrms.model.vo.ResignationVO;

import java.util.Date;
import java.util.List;

public interface ResignationService extends IService<HrResignation> {

    HrResignation addResignation(ResignationAddRequest request, Long hrEmployeeId, boolean submitNow);

    void submitForApproval(Long id, Long hrEmployeeId);

    void updateResignation(Long id, ResignationAddRequest request, Long hrEmployeeId);

    void deleteResignation(Long id, Long hrEmployeeId);

    Page<ResignationVO> listResignation(String keyword, List<String> statuses, int page, int size);

    ResignationVO getResignationDetail(Long id);

    void onApprovalPassed(Long businessId);

    void onApprovalRejected(Long businessId);

    /** 撤回审批中的申请 */
    void revokeResignation(Long id, Long hrEmployeeId);

    /** 放弃已批准的离职申请 */
    void abandonResignation(Long id, Long hrEmployeeId);

    /** 修改离职日期（已批准待离职） */
    void updateResignDate(Long id, Date newDate, Long hrEmployeeId);

    /** 确认离职（立即生效） */
    void confirmResignation(Long id, Long hrEmployeeId);

    /** 重新发起已拒绝的申请 */
    void resubmitResignation(Long id, Long hrEmployeeId);

    /** 定时任务：处理离职日期到达的员工 */
    void processDailyResignations();

    /** 获取各状态统计数量 */
    java.util.Map<String, Long> getStats();
}
