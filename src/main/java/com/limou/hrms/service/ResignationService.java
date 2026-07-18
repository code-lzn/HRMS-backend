package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.resignation.ResignationAddRequest;
import com.limou.hrms.model.entity.HrResignation;
import com.limou.hrms.model.vo.ResignationVO;

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

    /** 定时任务：处理离职日期到达的员工 */
    void processDailyResignations();

    /** 获取各状态统计数量 */
    java.util.Map<String, Long> getStats();
}
