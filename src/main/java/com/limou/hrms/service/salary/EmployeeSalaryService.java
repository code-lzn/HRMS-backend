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
     * 查询员工当前薪资档案
     */
    EmployeeSalaryVO getEmployeeSalary(Long employeeId);

    /**
     * 更新员工薪资档案（自动记录调薪历史）
     */
    void updateEmployeeSalary(Long employeeId, EmployeeSalaryUpdateRequest request, Long operatorId);

    /**
     * 查询员工调薪历史
     */
    List<SalaryChangeHistoryVO> getSalaryHistory(Long employeeId);

    /**
     * 根据员工ID和生效日期获取适用的薪资档案（用于薪资核算时拉取数据）
     */
    EmployeeSalary getActiveSalary(Long employeeId);
}
