package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.employee.EmpProfileUpdateRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.vo.EmpProfileVO;

public interface EmployeeService extends IService<Employee> {

    /**
     * 获取当前登录员工的档案（含敏感字段脱敏、字段锁定标记）
     */
    EmpProfileVO getProfile(Long userId);

    /**
     * 更新当前登录员工的可编辑字段
     */
    void updateProfile(Long userId, EmpProfileUpdateRequest request);

    /**
     * 通过用户ID获取员工信息
     */
     Employee getByUserId(Long userId);
}