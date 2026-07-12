package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.SalarySlipMapper;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.entity.SalarySlip;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.SalarySlipDetailVO;
import com.limou.hrms.model.vo.SalarySlipVO;
import com.limou.hrms.model.vo.SalaryTrendVO;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.SalaryBatchService;
import com.limou.hrms.service.SalarySlipService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 薪资工资条服务实现
 */
@Service
@Slf4j
public class SalarySlipServiceImpl extends ServiceImpl<SalarySlipMapper, SalarySlip>
        implements SalarySlipService {

    private static final String SALT = "limou";

    @Resource
    private EmployeeService employeeService;

    @Resource
    private UserService userService;

    @Resource
    private SalaryBatchService salaryBatchService;

    @Override
    public List<SalarySlipVO> getMySalarySlips(Long userId) {
        Employee emp = getEmployee(userId);

        List<SalarySlip> slips = this.lambdaQuery()
                .eq(SalarySlip::getEmployeeId, emp.getId())
                .orderByDesc(SalarySlip::getCreatedAt)
                .list();

        List<SalarySlipVO> voList = new ArrayList<>();
        for (SalarySlip slip : slips) {
            SalarySlipVO vo = new SalarySlipVO();
            vo.setId(slip.getId());
            vo.setGrossSalary(slip.getGrossSalary());
            vo.setTotalDeduction(slip.getTotalDeduction());
            vo.setNetSalary(slip.getNetSalary());
            vo.setHasAnomaly(slip.getHasAnomaly());

            // 查询批次获取月份和状态
            SalaryBatch batch = salaryBatchService.getById(slip.getBatchId());
            if (batch != null) {
                vo.setSalaryMonth(batch.getSalaryMonth());
                vo.setBatchStatus(batch.getStatus());
            }

            voList.add(vo);
        }
        return voList;
    }

    @Override
    public SalarySlipDetailVO getSalarySlipDetail(Long detailId, Long userId, String password) {
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        // 二次验证：密码校验
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        ThrowUtils.throwIf(!Objects.equals(user.getUserPassword(), encryptPassword),
                ErrorCode.SALARY_VERIFY_FAILED);

        SalarySlip slip = this.getById(detailId);
        ThrowUtils.throwIf(slip == null, ErrorCode.SALARY_NOT_FOUND);

        Employee emp = getEmployee(userId);
        ThrowUtils.throwIf(!Objects.equals(slip.getEmployeeId(), emp.getId()),
                ErrorCode.NO_AUTH_ERROR);

        SalaryBatch batch = salaryBatchService.getById(slip.getBatchId());

        SalarySlipDetailVO vo = new SalarySlipDetailVO();
        vo.setId(slip.getId());
        vo.setSalaryMonth(batch != null ? batch.getSalaryMonth() : null);
        vo.setEmployeeName(emp.getEmployeeName());
        vo.setEmployeeNo(emp.getEmployeeNo());

        // 收入项
        vo.setBaseSalary(slip.getBaseSalary());
        vo.setAllowance(slip.getAllowance());
        vo.setPerformanceBonus(slip.getPerformanceBonus());
        vo.setOvertimePay(slip.getOvertimePay());
        vo.setManualAdjust(slip.getManualAdjust());
        vo.setAdjustReason(slip.getAdjustReason());
        vo.setGrossSalary(slip.getGrossSalary());

        // 扣除项
        vo.setLateDeduction(slip.getLateDeduction());
        vo.setLeaveDeduction(slip.getLeaveDeduction());
        vo.setSocialPension(slip.getSocialPension());
        vo.setSocialMedical(slip.getSocialMedical());
        vo.setSocialUnemployment(slip.getSocialUnemployment());
        vo.setHousingFund(slip.getHousingFund());
        vo.setIncomeTax(slip.getIncomeTax());
        vo.setTotalDeduction(slip.getTotalDeduction());

        // 实发
        vo.setNetSalary(slip.getNetSalary());

        return vo;
    }

    @Override
    public List<SalaryTrendVO> getMySalaryTrend(Long userId) {
        Employee emp = getEmployee(userId);

        // 查询最近6条工资条记录（按月倒序），再在内存中反转
        List<SalarySlip> slips = this.lambdaQuery()
                .eq(SalarySlip::getEmployeeId, emp.getId())
                .orderByDesc(SalarySlip::getCreatedAt)
                .last("LIMIT 6")
                .list();

        List<SalaryTrendVO> trendList = new ArrayList<>();
        for (int i = slips.size() - 1; i >= 0; i--) {
            SalarySlip slip = slips.get(i);
            SalaryBatch batch = salaryBatchService.getById(slip.getBatchId());

            SalaryTrendVO vo = new SalaryTrendVO();
            vo.setMonth(batch != null ? batch.getSalaryMonth() : null);
            vo.setNetSalary(slip.getNetSalary());
            vo.setGrossSalary(slip.getGrossSalary());
            trendList.add(vo);
        }
        return trendList;
    }

    private Employee getEmployee(Long userId) {
        Employee emp = employeeService.lambdaQuery().eq(Employee::getUserId, userId).one();
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
        return emp;
    }
}
