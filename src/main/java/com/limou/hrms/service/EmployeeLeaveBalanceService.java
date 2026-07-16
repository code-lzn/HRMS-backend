package com.limou.hrms.service;

import com.limou.hrms.model.entity.EmployeeLeaveBalance;
import com.limou.hrms.model.vo.LeaveBalanceVO;

import java.math.BigDecimal;

public interface EmployeeLeaveBalanceService {

    /**
     * 获取员工当年请假余额
     */
    LeaveBalanceVO getCurrentYearBalance(Long employeeId);

    /**
     * 获取或创建员工当年余额记录
     */
    EmployeeLeaveBalance getOrCreateBalance(Long employeeId, int year);

    /**
     * 扣减请假额度
     *
     * @param employeeId 员工ID
     * @param leaveType  请假类型：1=病假 2=年假 6=调休
     * @param days       扣减天数
     */
    void deduct(Long employeeId, Integer leaveType, BigDecimal days);

    /**
     * 还原请假额度
     *
     * @param employeeId 员工ID
     * @param leaveType  请假类型：1=病假 2=年假 6=调休
     * @param days       还原天数
     */
    void restore(Long employeeId, Integer leaveType, BigDecimal days);
}
