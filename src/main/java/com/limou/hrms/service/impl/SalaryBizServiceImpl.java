package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ChangeTypeEnum;
import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.SalChangeLogService;
import com.limou.hrms.service.SalTaxCumulativeService;
import com.limou.hrms.service.SalaryBizService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 薪资核心业务编排服务实现
 */
@Service
@Slf4j
public class SalaryBizServiceImpl implements SalaryBizService {

    /**
     * 月计薪天数
     */
    private static final BigDecimal WORK_DAYS = BigDecimal.valueOf(21.75);
    /**
     * 日工作小时
     */
    private static final BigDecimal WORK_HOURS = BigDecimal.valueOf(8);
    /**
     * 迟到单次扣款
     */
    private static final BigDecimal LATE_FINE = BigDecimal.valueOf(50);

    @Resource
    private EmpSalaryProfileMapper empSalaryProfileMapper;
    @Resource
    private SalaryBatchMapper salaryBatchMapper;
    @Resource
    private SalarySlipMapper salarySlipMapper;
    @Resource
    private SalAccountMapper salAccountMapper;
    @Resource
    private SalItemMapper salItemMapper;
    @Resource
    private SalChangeLogMapper salChangeLogMapper;
    @Resource
    private SalChangeLogService salChangeLogService;
    @Resource
    private SalTaxCumulativeService salTaxCumulativeService;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private AttendanceMapper attendanceMapper;
    @Resource
    private LeaveMapper leaveMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 员工薪资档案 ====================

    @Override
    public EmployeeSalaryVO getEmployeeSalary(Long employeeId) {
        EmpSalaryProfile profile = empSalaryProfileMapper.selectOne(
                new LambdaQueryWrapper<EmpSalaryProfile>()
                        .eq(EmpSalaryProfile::getEmployeeId, employeeId)
                        .eq(EmpSalaryProfile::getIsDeleted, 0)
        );
        ThrowUtils.throwIf(profile == null, ErrorCode.NOT_FOUND_ERROR, "该员工未设置薪资档案");

        Employee employee = employeeMapper.selectById(employeeId);
        Department dept = employee != null && employee.getDepartmentId() != null
                ? departmentMapper.selectById(employee.getDepartmentId()) : null;

        EmployeeSalaryVO vo = new EmployeeSalaryVO();
        vo.setId(profile.getId());
        vo.setEmployeeId(profile.getEmployeeId());
        if (employee != null) {
            vo.setEmployeeName(employee.getEmployeeName());
            vo.setEmployeeNo(employee.getEmployeeNo());
        }
        vo.setDepartmentName(dept != null ? dept.getDeptName() : null);
        vo.setAccountSetId(profile.getAccountSetId());
        if (profile.getAccountSetId() != null) {
            SalAccount account = salAccountMapper.selectById(profile.getAccountSetId());
            vo.setAccountName(account != null ? account.getName() : null);
        }
        vo.setBaseSalary(profile.getBaseSalary());
        vo.setAllowanceBase(profile.getAllowanceBase());
        vo.setPerformanceBase(profile.getPerformanceBase());
        vo.setSocialInsuranceBase(profile.getSocialInsuranceBase());
        vo.setHousingFundBase(profile.getHousingFundBase());
        vo.setProbationSalaryRatio(profile.getProbationSalaryRatio());
        vo.setBankAccount(profile.getBankAccount());
        vo.setBankName(profile.getBankName());
        vo.setEffectiveDate(profile.getEffectiveDate());
        vo.setCreatedTIme(profile.getCreatedTIme());
        vo.setUpdatedTime(profile.getUpdatedTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployeeSalary(Long employeeId, EmpSalaryProfile newProfile, Long operatorId) {
        // 查旧档案
        EmpSalaryProfile oldProfile = empSalaryProfileMapper.selectOne(
                new LambdaQueryWrapper<EmpSalaryProfile>()
                        .eq(EmpSalaryProfile::getEmployeeId, employeeId)
                        .eq(EmpSalaryProfile::getIsDeleted, 0)
        );

        String oldJson = null;
        if (oldProfile != null) {
            try {
                oldJson = objectMapper.writeValueAsString(oldProfile);
            } catch (Exception ignored) {
            }
            newProfile.setId(oldProfile.getId());
            newProfile.setUpdatedTime(new Date());
            empSalaryProfileMapper.updateById(newProfile);
        } else {
            newProfile.setEmployeeId(employeeId);
            newProfile.setIsDeleted(0);
            newProfile.setCreatedTIme(new Date());
            newProfile.setUpdatedTime(new Date());
            empSalaryProfileMapper.insert(newProfile);
        }

        String newJson;
        try {
            newJson = objectMapper.writeValueAsString(newProfile);
        } catch (Exception e) {
            newJson = null;
        }

        // 自动判定变更类型
        int changeType = ChangeTypeEnum.SALARY_ADJUST.getValue();
        if (oldProfile != null && oldProfile.getAccountSetId() != null
                && !oldProfile.getAccountSetId().equals(newProfile.getAccountSetId())) {
            changeType = ChangeTypeEnum.ACCOUNT_CHANGE.getValue();
        }

        salChangeLogService.recordChange(employeeId, changeType, oldJson, newJson,
                operatorId, "更新薪资档案");
        log.info("更新员工薪资档案: employeeId={}, operatorId={}", employeeId, operatorId);
    }

    @Override
    public List<SalaryChangeLogVO> getEmployeeSalaryHistory(Long employeeId) {
        return salChangeLogService.getHistoryByEmployeeId(employeeId);
    }

    // ==================== 核算批次管理 ====================

    @Override
    public SalaryBatchVO createBatch(String salaryMonth, Long operatorId) {
        // 检查月份是否已存在
        SalaryBatch exist = salaryBatchMapper.selectOne(
                new LambdaQueryWrapper<SalaryBatch>().eq(SalaryBatch::getSalaryMonth, salaryMonth)
        );
        ThrowUtils.throwIf(exist != null, ErrorCode.OPERATION_ERROR, "该月份已存在核算批次");

        SalaryBatch batch = new SalaryBatch();
        batch.setBatchNo("SAL" + salaryMonth.replace("-", "") + "001");
        batch.setSalaryMonth(salaryMonth);
        batch.setStatus("DRAFT");
        batch.setTotalEmployeeCount(0);
        batch.setTotalGross(BigDecimal.ZERO);
        batch.setTotalDeduction(BigDecimal.ZERO);
        batch.setTotalNet(BigDecimal.ZERO);
        batch.setCreatedBy(operatorId);
        batch.setCreatedAt(new Date());
        batch.setUpdatedAt(new Date());
        salaryBatchMapper.insert(batch);

        log.info("创建核算批次: batchNo={}, salaryMonth={}", batch.getBatchNo(), salaryMonth);
        return toBatchVO(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateBatch(Long batchId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!"DRAFT".equals(batch.getStatus()) && !"CALCULATING".equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前状态不允许计算");

        // 更新状态为计算中
        batch.setStatus("CALCULATING");
        batch.setUpdatedAt(new Date());
        salaryBatchMapper.updateById(batch);

        // 获取所有在职员工
        List<Employee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getIsDeleted, 0)
                        .in(Employee::getStatus, Arrays.asList(1, 2)) // 试用期+正式
        );

        // 解析核算月份
        String[] parts = batch.getSalaryMonth().split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeduction = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        int successCount = 0;

        for (Employee emp : employees) {
            try {
                SalarySlip slip = calculateEmployeeSalary(emp, batchId, year, month);
                salarySlipMapper.insert(slip);
                totalGross = totalGross.add(slip.getGrossSalary());
                totalDeduction = totalDeduction.add(slip.getTotalDeduction());
                totalNet = totalNet.add(slip.getNetSalary());
                successCount++;
            } catch (Exception e) {
                log.error("计算员工薪资失败: employeeId={}, error={}", emp.getId(), e.getMessage());
                // 写入阻断记录
                SalarySlip slip = new SalarySlip();
                slip.setBatchId(batchId);
                slip.setEmployeeId(emp.getId());
                slip.setHasAnomaly(2); // 红色阻断
                slip.setAnomalyReason("计算失败: " + e.getMessage());
                slip.setGrossSalary(BigDecimal.ZERO);
                slip.setTotalDeduction(BigDecimal.ZERO);
                slip.setNetSalary(BigDecimal.ZERO);
                slip.setCreatedAt(new Date());
                slip.setUpdatedAt(new Date());
                salarySlipMapper.insert(slip);
            }
        }

        // 更新批次汇总
        batch.setStatus("PENDING_CONFIRM");
        batch.setTotalEmployeeCount(successCount);
        batch.setTotalGross(totalGross.setScale(2, RoundingMode.HALF_UP));
        batch.setTotalDeduction(totalDeduction.setScale(2, RoundingMode.HALF_UP));
        batch.setTotalNet(totalNet.setScale(2, RoundingMode.HALF_UP));
        batch.setUpdatedAt(new Date());
        salaryBatchMapper.updateById(batch);

        log.info("薪资计算完成: batchId={}, 核算人数={}, 应发={}, 实发={}",
                batchId, successCount, totalGross, totalNet);
    }

    /**
     * 计算单个员工的薪资
     */
    private SalarySlip calculateEmployeeSalary(Employee emp, Long batchId, int year, int month) {
        // 1. 查薪资档案
        EmpSalaryProfile profile = empSalaryProfileMapper.selectOne(
                new LambdaQueryWrapper<EmpSalaryProfile>()
                        .eq(EmpSalaryProfile::getEmployeeId, emp.getId())
                        .eq(EmpSalaryProfile::getIsDeleted, 0)
        );
        if (profile == null) {
            SalarySlip slip = new SalarySlip();
            slip.setBatchId(batchId);
            slip.setEmployeeId(emp.getId());
            slip.setHasAnomaly(2);
            slip.setAnomalyReason("新员工未设置薪资档案，无法计算");
            slip.setGrossSalary(BigDecimal.ZERO);
            slip.setTotalDeduction(BigDecimal.ZERO);
            slip.setNetSalary(BigDecimal.ZERO);
            slip.setCreatedAt(new Date());
            slip.setUpdatedAt(new Date());
            return slip;
        }

        // 2. 试用期比例
        boolean isProbation = emp.getStatus() != null && emp.getStatus() == 1;
        BigDecimal probationRatio = profile.getProbationSalaryRatio() != null
                ? profile.getProbationSalaryRatio() : BigDecimal.ONE;
        BigDecimal effectiveRatio = isProbation ? probationRatio : BigDecimal.ONE;

        // 3. 基本工资（试用期调整）
        BigDecimal baseSalary = profile.getBaseSalary() != null
                ? profile.getBaseSalary().multiply(effectiveRatio).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 4. 岗位津贴（试用期调整）
        BigDecimal allowance = profile.getAllowanceBase() != null
                ? profile.getAllowanceBase().multiply(effectiveRatio).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 5. 绩效奖金（变动收入，默认按基数×1.0计算，后续可对接绩效系统）
        BigDecimal performanceBonus = profile.getPerformanceBase() != null
                ? profile.getPerformanceBase().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 6. 加班费（暂时默认为0，后续对接加班系统）
        BigDecimal overtimePay = BigDecimal.ZERO;
        // todo: 对接加班审批记录，计算 小时工资 × 倍数 × 时长

        // 7. 考勤扣款：迟到
        int lateCount = countAttendanceStatus(emp.getId(), year, month, 1); // status=1=迟到
        BigDecimal lateDeduction = LATE_FINE.multiply(BigDecimal.valueOf(lateCount))
                .setScale(2, RoundingMode.HALF_UP);

        // 8. 考勤扣款：请假
        BigDecimal leaveDays = sumLeaveDays(emp.getId(), year, month);
        BigDecimal dailySalary = baseSalary.divide(WORK_DAYS, 2, RoundingMode.HALF_UP);
        BigDecimal leaveDeduction = dailySalary.multiply(leaveDays)
                .setScale(2, RoundingMode.HALF_UP);

        // 9. 社保三险（基数×比例）
        BigDecimal ssBase = profile.getSocialInsuranceBase() != null
                ? profile.getSocialInsuranceBase() : BigDecimal.ZERO;
        BigDecimal socialPension = ssBase.multiply(BigDecimal.valueOf(0.08))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal socialMedical = ssBase.multiply(BigDecimal.valueOf(0.02))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal socialUnemployment = ssBase.multiply(BigDecimal.valueOf(0.005))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSS = socialPension.add(socialMedical).add(socialUnemployment);

        // 10. 公积金（基数×12%）
        BigDecimal hfBase = profile.getHousingFundBase() != null
                ? profile.getHousingFundBase() : BigDecimal.ZERO;
        BigDecimal housingFund = hfBase.multiply(BigDecimal.valueOf(0.12))
                .setScale(2, RoundingMode.HALF_UP);

        // 11. 应发工资
        BigDecimal grossSalary = baseSalary.add(allowance).add(performanceBonus)
                .add(overtimePay).subtract(lateDeduction).subtract(leaveDeduction)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // 12. 个税（累计预扣法）
        BigDecimal incomeTax = salTaxCumulativeService.calculateMonthlyTax(
                emp.getId(), year, month, grossSalary, totalSS, housingFund);

        // 13. 应扣合计
        BigDecimal totalDeduction = totalSS.add(housingFund).add(incomeTax)
                .add(lateDeduction).add(leaveDeduction)
                .setScale(2, RoundingMode.HALF_UP);

        // 14. 实发
        BigDecimal netSalary = grossSalary.subtract(totalSS).subtract(housingFund)
                .subtract(incomeTax).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 15. 异常检测
        int hasAnomaly = 0;
        StringBuilder anomalyReason = new StringBuilder();

        // 请假>15天 → 黄色预警
        if (leaveDays.compareTo(BigDecimal.valueOf(15)) > 0) {
            hasAnomaly = 1;
            anomalyReason.append("当月请假超过15天; ");
        }
        // todo: 加班>50h → 黄色预警
        // 变动>30% → 红色预警（需有上月对比）
        SalarySlip lastMonthSlip = findLastMonthSlip(emp.getId(), year, month);
        if (lastMonthSlip != null && lastMonthSlip.getNetSalary().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal change = netSalary.subtract(lastMonthSlip.getNetSalary())
                    .abs().divide(lastMonthSlip.getNetSalary(), 2, RoundingMode.HALF_UP);
            if (change.compareTo(BigDecimal.valueOf(0.3)) > 0) {
                hasAnomaly = 2;
                anomalyReason.append("薪资较上月变动超过30%; ");
            }
        }

        // 16. 组装明细
        SalarySlip slip = new SalarySlip();
        slip.setBatchId(batchId);
        slip.setEmployeeId(emp.getId());
        slip.setBaseSalary(baseSalary);
        slip.setAllowance(allowance);
        slip.setPerformanceBonus(performanceBonus);
        slip.setOvertimePay(overtimePay);
        slip.setLateDeduction(lateDeduction);
        slip.setLeaveDeduction(leaveDeduction);
        slip.setSocialPension(socialPension);
        slip.setSocialMedical(socialMedical);
        slip.setSocialUnemployment(socialUnemployment);
        slip.setHousingFund(housingFund);
        slip.setIncomeTax(incomeTax);
        slip.setGrossSalary(grossSalary);
        slip.setTotalDeduction(totalDeduction);
        slip.setNetSalary(netSalary);
        slip.setHasAnomaly(hasAnomaly);
        slip.setAnomalyReason(anomalyReason.length() > 0 ? anomalyReason.toString() : null);
        slip.setManualAdjust(BigDecimal.ZERO);
        slip.setAdjustReason(null);
        slip.setCreatedAt(new Date());
        slip.setUpdatedAt(new Date());

        return slip;
    }

    @Override
    public SalaryBatchPreviewVO previewBatch(Long batchId, long current, long size) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");

        Page<SalarySlip> page = new Page<>(current, size);
        Page<SalarySlip> result = salarySlipMapper.selectPage(page,
                new LambdaQueryWrapper<SalarySlip>()
                        .eq(SalarySlip::getBatchId, batchId)
                        .orderByAsc(SalarySlip::getEmployeeId)
        );

        List<SalaryDetailVO> records = result.getRecords().stream()
                .map(slip -> toDetailVO(slip, null))
                .collect(Collectors.toList());

        SalaryBatchPreviewVO preview = new SalaryBatchPreviewVO();
        preview.setBatch(toBatchVO(batch));
        preview.setRecords(records);
        preview.setTotal(result.getTotal());
        preview.setCurrent(current);
        preview.setSize(size);
        return preview;
    }

    @Override
    public List<SalaryDetailVO> getAnomalies(Long batchId) {
        List<SalarySlip> slips = salarySlipMapper.selectList(
                new LambdaQueryWrapper<SalarySlip>()
                        .eq(SalarySlip::getBatchId, batchId)
                        .gt(SalarySlip::getHasAnomaly, 0)
        );
        return slips.stream().map(slip -> toDetailVO(slip, null)).collect(Collectors.toList());
    }

    @Override
    public void adjustDetail(Long batchId, Long employeeId, BigDecimal manualAdjust,
                             String adjustReason, Long operatorId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!"PENDING_CONFIRM".equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "仅待确认状态可手动调整");

        SalarySlip slip = salarySlipMapper.selectOne(
                new LambdaQueryWrapper<SalarySlip>()
                        .eq(SalarySlip::getBatchId, batchId)
                        .eq(SalarySlip::getEmployeeId, employeeId)
        );
        ThrowUtils.throwIf(slip == null, ErrorCode.NOT_FOUND_ERROR, "该员工不在本批次中");

        BigDecimal oldNetSalary = slip.getNetSalary();
        BigDecimal newNetSalary = oldNetSalary.add(manualAdjust).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal newGross = slip.getGrossSalary().add(manualAdjust)
                .setScale(2, RoundingMode.HALF_UP);

        slip.setManualAdjust(manualAdjust);
        slip.setAdjustReason(adjustReason);
        slip.setGrossSalary(newGross);
        slip.setNetSalary(newNetSalary);
        slip.setUpdatedAt(new Date());
        salarySlipMapper.updateById(slip);

        // 更新批次汇总
        BigDecimal diff = manualAdjust;
        batch.setTotalGross(batch.getTotalGross().add(diff).setScale(2, RoundingMode.HALF_UP));
        batch.setTotalNet(batch.getTotalNet().add(diff).setScale(2, RoundingMode.HALF_UP));
        batch.setUpdatedAt(new Date());
        salaryBatchMapper.updateById(batch);

        log.info("手动调整薪资: batchId={}, employeeId={}, adjust={}, operatorId={}",
                batchId, employeeId, manualAdjust, operatorId);
    }

    @Override
    public void submitForApproval(Long batchId, Long operatorId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!"PENDING_CONFIRM".equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前状态不允许提交审批");

        batch.setStatus("APPROVING");
        batch.setUpdatedAt(new Date());
        salaryBatchMapper.updateById(batch);

        log.info("提交审批: batchId={}, operatorId={}", batchId, operatorId);
    }

    // ==================== 审批操作 ====================

    @Override
    public void approveBatch(Long batchId, Long operatorId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!"APPROVING".equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前状态不允许审批");

        batch.setStatus("APPROVED");
        batch.setApprovedBy(operatorId);
        batch.setUpdatedAt(new Date());
        salaryBatchMapper.updateById(batch);

        log.info("审批通过: batchId={}, operatorId={}", batchId, operatorId);
    }

    @Override
    public void rejectBatch(Long batchId, String reason, Long operatorId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!"APPROVING".equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "当前状态不允许驳回");

        batch.setStatus("REJECTED");
        batch.setUpdatedAt(new Date());
        salaryBatchMapper.updateById(batch);

        log.info("审批驳回: batchId={}, reason={}, operatorId={}", batchId, reason, operatorId);
    }

    @Override
    public void markPaid(Long batchId, Long operatorId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        ThrowUtils.throwIf(!"APPROVED".equals(batch.getStatus()),
                ErrorCode.OPERATION_ERROR, "仅审批通过的批次可标记已发放");

        batch.setStatus("PAID");
        batch.setPaidAt(new Date());
        batch.setUpdatedAt(new Date());
        salaryBatchMapper.updateById(batch);

        log.info("标记已发放: batchId={}, operatorId={}", batchId, operatorId);
    }

    // ==================== 查询 ====================

    @Override
    public List<SalaryBatchVO> listBatches() {
        List<SalaryBatch> batches = salaryBatchMapper.selectList(
                new LambdaQueryWrapper<SalaryBatch>().orderByDesc(SalaryBatch::getCreatedAt)
        );
        return batches.stream().map(this::toBatchVO).collect(Collectors.toList());
    }

    @Override
    public SalaryBatchVO getBatchDetail(Long batchId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        ThrowUtils.throwIf(batch == null, ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        return toBatchVO(batch);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 统计某月考勤状态次数
     */
    private int countAttendanceStatus(Long employeeId, int year, int month, int status) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1, 0, 0, 0);
        Date startDate = cal.getTime();
        cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        Date endDate = cal.getTime();

        Long count = attendanceMapper.selectCount(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getEmployeeId, employeeId)
                        .eq(Attendance::getStatus, status)
                        .eq(Attendance::getIsDeleted, 0)
                        .ge(Attendance::getAttendanceDate, startDate)
                        .le(Attendance::getAttendanceDate, endDate)
        );
        return count != null ? count.intValue() : 0;
    }

    /**
     * 汇总某月请假天数
     */
    private BigDecimal sumLeaveDays(Long employeeId, int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1, 0, 0, 0);
        Date startDate = cal.getTime();
        cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        Date endDate = cal.getTime();

        List<Leave> leaves = leaveMapper.selectList(
                new LambdaQueryWrapper<Leave>()
                        .eq(Leave::getEmployeeId, employeeId)
                        .eq(Leave::getStatus, 1) // 已通过
                        .eq(Leave::getIsDeleted, 0)
                        .ge(Leave::getStartDate, startDate)
                        .le(Leave::getEndDate, endDate)
        );

        BigDecimal total = BigDecimal.ZERO;
        if (leaves != null) {
            for (Leave leave : leaves) {
                if (leave.getTotalDays() != null) {
                    total = total.add(leave.getTotalDays());
                }
            }
        }
        return total;
    }

    /**
     * 找上月工资条（用于异常检测）
     */
    private SalarySlip findLastMonthSlip(Long employeeId, int year, int month) {
        int lastMonth = month - 1;
        int lastYear = year;
        if (lastMonth == 0) {
            lastMonth = 12;
            lastYear--;
        }
        String lastSalaryMonth = String.format("%04d-%02d", lastYear, lastMonth);

        // 找到上月批次
        SalaryBatch lastBatch = salaryBatchMapper.selectOne(
                new LambdaQueryWrapper<SalaryBatch>().eq(SalaryBatch::getSalaryMonth, lastSalaryMonth)
        );
        if (lastBatch == null) return null;

        return salarySlipMapper.selectOne(
                new LambdaQueryWrapper<SalarySlip>()
                        .eq(SalarySlip::getBatchId, lastBatch.getId())
                        .eq(SalarySlip::getEmployeeId, employeeId)
        );
    }

    private SalaryBatchVO toBatchVO(SalaryBatch batch) {
        SalaryBatchVO vo = new SalaryBatchVO();
        vo.setId(batch.getId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setSalaryMonth(batch.getSalaryMonth());
        vo.setStatus(batch.getStatus());
        vo.setStatusText(getStatusText(batch.getStatus()));
        vo.setTotalEmployeeCount(batch.getTotalEmployeeCount());
        vo.setTotalGross(batch.getTotalGross());
        vo.setTotalDeduction(batch.getTotalDeduction());
        vo.setTotalNet(batch.getTotalNet());
        vo.setPaidAt(batch.getPaidAt());
        vo.setCreatedAt(batch.getCreatedAt());
        return vo;
    }

    private SalaryDetailVO toDetailVO(SalarySlip slip, Employee emp) {
        SalaryDetailVO vo = new SalaryDetailVO();
        vo.setId(slip.getId());
        vo.setBatchId(slip.getBatchId());
        vo.setEmployeeId(slip.getEmployeeId());
        vo.setBaseSalary(slip.getBaseSalary());
        vo.setAllowance(slip.getAllowance());
        vo.setPerformanceBonus(slip.getPerformanceBonus());
        vo.setOvertimePay(slip.getOvertimePay());
        vo.setLateDeduction(slip.getLateDeduction());
        vo.setLeaveDeduction(slip.getLeaveDeduction());
        vo.setSocialPension(slip.getSocialPension());
        vo.setSocialMedical(slip.getSocialMedical());
        vo.setSocialUnemployment(slip.getSocialUnemployment());
        vo.setHousingFund(slip.getHousingFund());
        vo.setIncomeTax(slip.getIncomeTax());
        vo.setGrossSalary(slip.getGrossSalary());
        vo.setTotalDeduction(slip.getTotalDeduction());
        vo.setNetSalary(slip.getNetSalary());
        vo.setManualAdjust(slip.getManualAdjust());
        vo.setAdjustReason(slip.getAdjustReason());
        vo.setHasAnomaly(slip.getHasAnomaly());
        vo.setAnomalyReason(slip.getAnomalyReason());
        vo.setCreatedAt(slip.getCreatedAt());
        // 填充员工信息
        if (emp != null) {
            vo.setEmployeeNo(emp.getEmployeeNo());
            vo.setEmployeeName(emp.getEmployeeName());
        }
        return vo;
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "DRAFT":
                return "草稿";
            case "CALCULATING":
                return "计算中";
            case "PENDING_CONFIRM":
                return "待确认";
            case "APPROVING":
                return "审批中";
            case "APPROVED":
                return "已通过";
            case "PAID":
                return "已发放";
            case "REJECTED":
                return "已驳回";
            default:
                return status;
        }
    }
}
