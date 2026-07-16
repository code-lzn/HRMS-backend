package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.regularization.RegularizationAddRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.HrRegularization;
import com.limou.hrms.model.vo.RegularizationVO;

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
}
