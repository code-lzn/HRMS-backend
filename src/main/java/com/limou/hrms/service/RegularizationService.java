package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.regularization.RegularizationAddRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.HrRegularization;
import com.limou.hrms.model.vo.RegularizationVO;

import java.util.Date;
import java.util.List;

public interface RegularizationService extends IService<HrRegularization> {

    HrRegularization addRegularization(RegularizationAddRequest request, Long hrEmployeeId, boolean submitNow);

    void submitForApproval(Long id, Long hrEmployeeId);

    void updateRegularization(Long id, RegularizationAddRequest request, Long hrEmployeeId);

    void deleteRegularization(Long id, Long hrEmployeeId);

    Page<RegularizationVO> listRegularization(String keyword, List<String> statuses, int page, int size);

    RegularizationVO getRegularizationDetail(Long id);

    void onApprovalPassed(Long businessId);

    void onApprovalRejected(Long businessId);

    List<Employee> getProbationEndingEmployees();

    /** 获取各状态统计数量 */
    java.util.Map<String, Long> getStats();

    /** 撤回审批中的申请 */
    void revokeRegularization(Long id, Long hrEmployeeId);

    /** 放弃已批准的转正申请 */
    void abandonRegularization(Long id, Long hrEmployeeId);

    /** 修改转正日期（已批准待转正） */
    void updateRegularizationDate(Long id, Date newDate, Long hrEmployeeId);

    /** 确认转正（立即生效） */
    void confirmRegularization(Long id, Long hrEmployeeId);

    /** 重新发起已拒绝的申请 */
    void resubmitRegularization(Long id, Long hrEmployeeId);
}
