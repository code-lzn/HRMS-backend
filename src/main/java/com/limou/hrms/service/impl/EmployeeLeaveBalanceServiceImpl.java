package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.EmployeeLeaveBalanceMapper;
import com.limou.hrms.model.entity.EmployeeLeaveBalance;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.service.EmployeeLeaveBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Calendar;

@Slf4j
@Service
public class EmployeeLeaveBalanceServiceImpl extends ServiceImpl<EmployeeLeaveBalanceMapper, EmployeeLeaveBalance>
        implements EmployeeLeaveBalanceService {

    @Override
    public LeaveBalanceVO getCurrentYearBalance(Long employeeId) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        EmployeeLeaveBalance balance = getOrCreateBalance(employeeId, year);

        LeaveBalanceVO vo = new LeaveBalanceVO();
        vo.setAnnualRemaining(balance.getAnnualTotal().subtract(balance.getAnnualUsed()));
        vo.setSickRemaining(balance.getSickTotal().subtract(balance.getSickUsed()));
        vo.setCompRemaining(balance.getCompTotal().subtract(balance.getCompUsed()));

        BigDecimal total = vo.getAnnualRemaining()
                .add(vo.getSickRemaining())
                .add(vo.getCompRemaining());
        vo.setTotalRemaining(total);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeLeaveBalance getOrCreateBalance(Long employeeId, int year) {
        EmployeeLeaveBalance balance = this.lambdaQuery()
                .eq(EmployeeLeaveBalance::getEmployeeId, employeeId)
                .eq(EmployeeLeaveBalance::getYear, year)
                .one();
        if (balance == null) {
            balance = new EmployeeLeaveBalance();
            balance.setEmployeeId(employeeId);
            balance.setYear(year);
            balance.setAnnualTotal(BigDecimal.ZERO);
            balance.setAnnualUsed(BigDecimal.ZERO);
            balance.setSickTotal(BigDecimal.ZERO);
            balance.setSickUsed(BigDecimal.ZERO);
            balance.setCompTotal(BigDecimal.ZERO);
            balance.setCompUsed(BigDecimal.ZERO);
            this.save(balance);
        }
        return balance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long employeeId, Integer leaveType, BigDecimal days) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        EmployeeLeaveBalance balance = getOrCreateBalance(employeeId, year);

        switch (leaveType) {
            case 1: // 病假
                balance.setSickUsed(balance.getSickUsed().add(days));
                break;
            case 2: // 年假
                balance.setAnnualUsed(balance.getAnnualUsed().add(days));
                break;
            case 6: // 调休
                balance.setCompUsed(balance.getCompUsed().add(days));
                break;
            default:
                return;
        }
        this.updateById(balance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long employeeId, Integer leaveType, BigDecimal days) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        EmployeeLeaveBalance balance = this.lambdaQuery()
                .eq(EmployeeLeaveBalance::getEmployeeId, employeeId)
                .eq(EmployeeLeaveBalance::getYear, year)
                .one();
        if (balance == null) return;

        switch (leaveType) {
            case 1: // 病假
                balance.setSickUsed(balance.getSickUsed().subtract(days));
                break;
            case 2: // 年假
                balance.setAnnualUsed(balance.getAnnualUsed().subtract(days));
                break;
            case 6: // 调休
                balance.setCompUsed(balance.getCompUsed().subtract(days));
                break;
            default:
                return;
        }
        this.updateById(balance);
    }
}
