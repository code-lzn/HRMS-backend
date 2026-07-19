package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.EmployeeLeaveBalanceMapper;
import com.limou.hrms.mapper.OvertimeRecordMapper;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.EmployeeLeaveBalance;
import com.limou.hrms.model.entity.OvertimeRecord;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.service.EmployeeLeaveBalanceService;
import com.limou.hrms.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class EmployeeLeaveBalanceServiceImpl extends ServiceImpl<EmployeeLeaveBalanceMapper, EmployeeLeaveBalance>
        implements EmployeeLeaveBalanceService {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private OvertimeRecordMapper overtimeRecordMapper;

    @Override
    public LeaveBalanceVO getCurrentYearBalance(Long employeeId) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        EmployeeLeaveBalance balance = getOrCreateBalance(employeeId, year);

        // 调休余额动态计算（上月1号至今的加班时长 - 已用调休）
        BigDecimal currentCompTotal = calculateCompensatoryLeaveDays(employeeId);
        if (currentCompTotal.compareTo(balance.getCompTotal()) != 0) {
            balance.setCompTotal(currentCompTotal);
            this.updateById(balance);
        }

        LeaveBalanceVO vo = new LeaveBalanceVO();
        vo.setAnnualRemaining(balance.getAnnualTotal().subtract(balance.getAnnualUsed()));
        vo.setSickRemaining(balance.getSickTotal().subtract(balance.getSickUsed()));
        vo.setCompRemaining(currentCompTotal.subtract(balance.getCompUsed()));

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
            balance.setAnnualTotal(calculateAnnualLeaveDays(employeeId, year));
            balance.setAnnualUsed(BigDecimal.ZERO);
            balance.setSickTotal(BigDecimal.ZERO);
            balance.setSickUsed(BigDecimal.ZERO);
            balance.setCompTotal(BigDecimal.ZERO);
            balance.setCompUsed(BigDecimal.ZERO);
            this.save(balance);
        } else if (year == Calendar.getInstance().get(Calendar.YEAR)) {
            // 当前年份每次都重新计算，确保规则变更后生效
            balance.setAnnualTotal(calculateAnnualLeaveDays(employeeId, year));
            this.updateById(balance);
        }
        return balance;
    }

//年假计算
    private BigDecimal calculateAnnualLeaveDays(Long employeeId, int year) {


//        - 入职不满1年：0天
//                - 满1年不满10年：5天/年
//                - 满10年不满20年：10天/年
//                - 满20年及以上：15天/年
        Employee employee = employeeService.getById(employeeId);
        if (employee == null || employee.getHireDate() == null) {
            return BigDecimal.ZERO;
        }

        Date hireDate = employee.getHireDate();
        Calendar hireCal = Calendar.getInstance();
        hireCal.setTime(hireDate);
        int hireYear = hireCal.get(Calendar.YEAR);

        Calendar targetCal = Calendar.getInstance();
        targetCal.set(year, 11, 31);
        Date targetDate = targetCal.getTime();

        Calendar hireForCompare = Calendar.getInstance();
        hireForCompare.setTime(hireDate);
        hireForCompare.set(Calendar.HOUR_OF_DAY, 0);
        hireForCompare.set(Calendar.MINUTE, 0);
        hireForCompare.set(Calendar.SECOND, 0);
        hireForCompare.set(Calendar.MILLISECOND, 0);

        Calendar targetForCompare = Calendar.getInstance();
        targetForCompare.setTime(targetDate);
        targetForCompare.set(Calendar.HOUR_OF_DAY, 0);
        targetForCompare.set(Calendar.MINUTE, 0);
        targetForCompare.set(Calendar.SECOND, 0);
        targetForCompare.set(Calendar.MILLISECOND, 0);

        long diffYears = targetForCompare.get(Calendar.YEAR) - hireForCompare.get(Calendar.YEAR);
        if (targetForCompare.get(Calendar.DAY_OF_YEAR) < hireForCompare.get(Calendar.DAY_OF_YEAR)) {
            diffYears--;
        }

        if (hireYear == year) {
            return BigDecimal.valueOf(3);
        }

        int baseDays;
        if (diffYears < 1) {
            baseDays = 3;
        } else if (diffYears < 10) {
            baseDays = 5;
        } else if (diffYears < 20) {
            baseDays = 10;
        } else {
            baseDays = 15;
        }

        return BigDecimal.valueOf(baseDays);
    }

    /**
     * 计算调休余额：从已审批的加班记录累加，有效期 = 加班当月及次月
     */
    private BigDecimal calculateCompensatoryLeaveDays(Long employeeId) {
        Calendar validStart = Calendar.getInstance();
        validStart.set(Calendar.DAY_OF_MONTH, 1);
        validStart.add(Calendar.MONTH, -1);
        validStart.set(Calendar.HOUR_OF_DAY, 0);
        validStart.set(Calendar.MINUTE, 0);
        validStart.set(Calendar.SECOND, 0);
        validStart.set(Calendar.MILLISECOND, 0);

        String startDate = DateUtil.formatDate(validStart.getTime());

        List<OvertimeRecord> approvedOvertimes = overtimeRecordMapper.selectList(
                new LambdaQueryWrapper<OvertimeRecord>()
                        .eq(OvertimeRecord::getEmployeeId, employeeId)
                        .eq(OvertimeRecord::getStatus, 1)
                        .ge(OvertimeRecord::getOvertimeDate, startDate)
        );

        double total = 0;
        for (OvertimeRecord r : approvedOvertimes) {
            if (r.getOvertimeHours() != null) {
                total += r.getOvertimeHours().doubleValue();
            }
        }

        return BigDecimal.valueOf(total);
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
