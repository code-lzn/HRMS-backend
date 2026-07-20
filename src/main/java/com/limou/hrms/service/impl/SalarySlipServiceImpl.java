package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.SalPayslipViewLogMapper;
import com.limou.hrms.mapper.SalarySlipMapper;
import com.limou.hrms.model.entity.*;
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
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 薪资工资条服务实现
 */
@Service
@Slf4j
public class SalarySlipServiceImpl extends ServiceImpl<SalarySlipMapper, SalarySlip>
        implements SalarySlipService {

    private static final String SALT = "hrms";

    @Resource
    private EmployeeService employeeService;

    @Resource
    private UserService userService;

    @Resource
    private SalaryBatchService salaryBatchService;

    @Resource
    private SalPayslipViewLogMapper salPayslipViewLogMapper;

    @Override
    public List<SalarySlipVO> getMySalarySlips(Long userId) {
        Employee emp = getEmployee(userId);

        List<SalarySlip> slips = this.lambdaQuery()
                .eq(SalarySlip::getEmployeeId, emp.getId())
                .orderByDesc(SalarySlip::getCreatedAt)
                .list();

        List<SalarySlipVO> voList = new ArrayList<>();
        for (SalarySlip slip : slips) {
            // 查询批次获取月份和状态
            SalaryBatch batch = salaryBatchService.getById(slip.getBatchId());
            // 仅审批通过或已发放的批次才展示工资条
            if (batch == null) continue;
            if (!"APPROVED".equals(batch.getStatus()) && !"PAID".equals(batch.getStatus())) {
                continue;
            }

            SalarySlipVO vo = new SalarySlipVO();
            vo.setId(slip.getId());
            vo.setGrossSalary(slip.getGrossSalary());
            vo.setTotalDeduction(slip.getTotalDeduction());
            vo.setNetSalary(slip.getNetSalary());
            vo.setHasAnomaly(slip.getHasAnomaly());
            vo.setSalaryMonth(batch.getSalaryMonth());
            vo.setBatchStatus(batch.getStatus());

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
        // 校验：仅审批通过或已发放的批次可查看工资条
        ThrowUtils.throwIf(batch == null
                        || (!"APPROVED".equals(batch.getStatus()) && !"PAID".equals(batch.getStatus())),
                ErrorCode.NO_AUTH_ERROR, "工资条尚未开放查看");

        // 记录查看日志（首次查看需密码验证，非首次可跳过 - 此接口统一要求密码）
        boolean isFirstView = salPayslipViewLogMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SalPayslipViewLog>()
                        .eq(SalPayslipViewLog::getBatchDetailId, detailId)
                        .eq(SalPayslipViewLog::getEmployeeId, emp.getId())
        ) == 0;
        SalPayslipViewLog viewLog = new SalPayslipViewLog();
        viewLog.setBatchDetailId(detailId);
        viewLog.setEmployeeId(emp.getId());
        viewLog.setVerifiedAt(new Date());
        viewLog.setViewedAt(new Date());
        salPayslipViewLogMapper.insert(viewLog);

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
