package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.transfer.TransferCreateDTO;
import com.limou.hrms.model.dto.transfer.TransferUpdateDTO;
import com.limou.hrms.model.query.TransferQuery;
import com.limou.hrms.common.PageRequest;
import com.limou.hrms.model.vo.TransferDetailVO;
import com.limou.hrms.model.vo.TransferHistoryVO;
import com.limou.hrms.model.vo.TransferListVO;

/**
 * 调岗管理服务接口
 */
public interface TransferService {

    Long createApplication(TransferCreateDTO dto);
    void updateDraft(Long id, TransferUpdateDTO dto);
    void deleteDraft(Long id);
    void submitToApproval(Long id);
    void cancel(Long id);
    Page<TransferListVO> list(TransferQuery query);
    TransferDetailVO getDetail(Long id);

    /** 查询员工调岗历史 */
    Page<TransferHistoryVO> getHistory(Long employeeId, PageRequest page);
}
