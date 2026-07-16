package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.transfer.TransferAddRequest;
import com.limou.hrms.model.entity.HrTransfer;
import com.limou.hrms.model.vo.TransferVO;

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
}
