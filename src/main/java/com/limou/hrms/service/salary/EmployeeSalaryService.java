package com.limou.hrms.service.salary;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.salary.EmployeeSalaryUpdateRequest;
import com.limou.hrms.model.entity.EmployeeSalary;
import com.limou.hrms.model.vo.salary.EmployeeSalaryVO;
import com.limou.hrms.model.vo.salary.SalaryChangeHistoryVO;
import java.util.List;

/**
 * 员工薪资档案服务接口
 */
public interface EmployeeSalaryService extends IService<EmployeeSalary> {

    /**
     * 获取员工当前有效薪资档案
     */
    EmployeeSalaryVO getEmployeeSalary(Long employeeId);

    /**
     * 更新员工薪资档案（自动记录调薪历史）
     */
    void updateEmployeeSalary(Long employeeId, EmployeeSalaryUpdateRequest request, Long operatorId);

    /**
     * 获取员工调薪历史
     */
    List<SalaryChangeHistoryVO> getSalaryHistory(Long employeeId);

    /**
     * 获取员工当前有效薪资档案（原始实体，供计算引擎使用）
     */
    EmployeeSalary getActiveSalary(Long employeeId);
}
