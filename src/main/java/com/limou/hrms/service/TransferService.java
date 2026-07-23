package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.transfer.TransferAddRequest;
import com.limou.hrms.model.entity.HrTransfer;
import com.limou.hrms.model.vo.TransferVO;

import java.util.Date;
import java.util.List;

public interface TransferService extends IService<HrTransfer> {

    HrTransfer addTransfer(TransferAddRequest request, Long hrEmployeeId, boolean submitNow);

    void submitForApproval(Long id, Long hrEmployeeId);

    void updateTransfer(Long id, TransferAddRequest request, Long hrEmployeeId);

    void deleteTransfer(Long id, Long hrEmployeeId);

    Page<TransferVO> listTransfer(String keyword, List<String> statuses, int page, int size);

    TransferVO getTransferDetail(Long id);

    void onApprovalPassed(Long businessId);

    void onApprovalRejected(Long businessId);

    /** 获取各状态统计数量 */
    java.util.Map<String, Long> getStats();

    /** 撤回审批中的调岗申请（仅第一级审批前） */
    void revokeTransfer(Long id, Long hrEmployeeId);

    /** 放弃已批准的调岗申请 */
    void abandonTransfer(Long id, Long hrEmployeeId);

    /** 确认调岗生效（执行实际调岗操作） */
    void confirmTransfer(Long id, Long hrEmployeeId);

    /** 修改调岗生效日期 */
    void updateTransferDate(Long id, Date newDate, Long hrEmployeeId);

    /** 重新发起已拒绝的调岗申请 */
    void resubmitTransfer(Long id, Long hrEmployeeId);
}
