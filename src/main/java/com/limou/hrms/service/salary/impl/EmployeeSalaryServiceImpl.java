package com.limou.hrms.service.salary.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.EmployeeSalaryMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.mapper.PositionMapper;
import com.limou.hrms.mapper.SalaryAccountMapper;
import com.limou.hrms.mapper.SalaryChangeHistoryMapper;
import com.limou.hrms.model.dto.salary.EmployeeSalaryUpdateRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.EmployeeSalary;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.entity.SalaryAccount;
import com.limou.hrms.model.entity.SalaryChangeHistory;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.ChangeTypeEnum;
import com.limou.hrms.model.vo.salary.EmployeeSalaryVO;
import com.limou.hrms.model.vo.salary.SalaryChangeHistoryVO;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.salary.EmployeeSalaryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 员工薪资档案服务实现
 */
@Service
@Slf4j
public class EmployeeSalaryServiceImpl extends ServiceImpl<EmployeeSalaryMapper, EmployeeSalary>
        implements EmployeeSalaryService {

    @Resource
    private SalaryChangeHistoryMapper salaryChangeHistoryMapper;
    @Resource
    private SalaryAccountMapper salaryAccountMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private EmployeeWorkInfoMapper workInfoMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;

    @Resource
    private DataScopeContext dataScopeContext;

    @Resource
    private EmployeeMapper employeeMapper;

    @Override
    public EmployeeSalaryVO getEmployeeSalary(Long employeeId) {
        checkSalaryDataScope(employeeId);
        QueryWrapper<EmployeeSalary> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId)
                .orderByDesc("effective_date")
                .last("LIMIT 1");
        EmployeeSalary salary = this.getOne(wrapper);
        if (salary == null) {
            return null;
        }
        return toVO(salary);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployeeSalary(Long employeeId, EmployeeSalaryUpdateRequest request, Long operatorId) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取旧档案
        QueryWrapper<EmployeeSalary> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId)
                .orderByDesc("effective_date")
                .last("LIMIT 1");
        EmployeeSalary oldSalary = this.getOne(wrapper);

        // 创建新档案
        EmployeeSalary newSalary = new EmployeeSalary();
        newSalary.setEmployeeId(employeeId);
        newSalary.setAccountId(request.getAccountId());
        newSalary.setBaseSalary(request.getBaseSalary());
        newSalary.setAllowanceBase(request.getAllowanceBase());
        newSalary.setSocialSecurityBase(request.getSocialSecurityBase());
        newSalary.setHousingFundBase(request.getHousingFundBase());
        newSalary.setPerformanceBase(request.getPerformanceBase());
        newSalary.setEffectiveDate(request.getEffectiveDate());
        this.save(newSalary);

        // 判断变更类型
        Integer changeType = determineChangeType(oldSalary, newSalary);

        // 记录调薪历史
        SalaryChangeHistory history = new SalaryChangeHistory();
        history.setEmployeeId(employeeId);
        history.setChangeType(changeType);
        if (oldSalary != null) {
            history.setOldValue(JSONUtil.toJsonStr(oldSalary));
        }
        history.setNewValue(JSONUtil.toJsonStr(newSalary));
        history.setEffectiveDate(request.getEffectiveDate());
        history.setOperatorId(operatorId);
        history.setRemark(request.getRemark());
        salaryChangeHistoryMapper.insert(history);

        // 新员工首次设薪时检查薪资是否超出职级范围（对应PRD 5.1.4 二审触发条件#3）
        if (oldSalary == null) {
            checkSalaryRangeForNewEmployee(employeeId, newSalary.getBaseSalary());
        }

        log.info("员工 {} 薪资档案已更新，操作人：{}", employeeId, operatorId);
    }

    @Override
    public List<SalaryChangeHistoryVO> getSalaryHistory(Long employeeId) {
        checkSalaryDataScope(employeeId);
        QueryWrapper<SalaryChangeHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId)
                .orderByDesc("create_time");
        List<SalaryChangeHistory> histories = salaryChangeHistoryMapper.selectList(wrapper);
        return histories.stream().map(this::toHistoryVO).collect(Collectors.toList());
    }

    @Override
    public EmployeeSalary getActiveSalary(Long employeeId) {
        QueryWrapper<EmployeeSalary> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId)
                .orderByDesc("effective_date")
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 薪资数据范围校验：USER 角色只能查看自己的薪资
     */
    private void checkSalaryDataScope(Long employeeId) {
        DataScopeEnum scope = dataScopeContext.getSalaryScope();
        if (scope == DataScopeEnum.NONE) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看薪资数据");
        }
        if (scope == DataScopeEnum.SELF) {
            Long currentUserId = dataScopeContext.getCurrentUserId();
            if (currentUserId == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            Employee employee = employeeMapper.selectByUserId(currentUserId);
            if (employee == null || !employee.getId().equals(employeeId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看他人薪资数据");
            }
        }
        // ALL — 放行
    }

    /**
     * 判断变更类型
     */
    private Integer determineChangeType(EmployeeSalary oldSalary, EmployeeSalary newSalary) {
        if (oldSalary == null) {
            return ChangeTypeEnum.SALARY_ADJUST.getValue();
        }
        if (!oldSalary.getAccountId().equals(newSalary.getAccountId())) {
            return ChangeTypeEnum.ACCOUNT_CHANGE.getValue();
        }
        if (!oldSalary.getBaseSalary().equals(newSalary.getBaseSalary())) {
            return ChangeTypeEnum.SALARY_ADJUST.getValue();
        }
        return ChangeTypeEnum.BASE_ADJUST.getValue();
    }

    /**
     * 新员工首次设薪时检查是否超出职级范围。
     * 触发条件：标准职位（有职级范围）且薪资明显高于同职位平均水平 → 创建HR二审审批。
     * <p>
     * 对应 PRD 5.1.4 — 入职审批 HR 二审触发条件之"薪资超出职级范围"。
     */
    private void checkSalaryRangeForNewEmployee(Long employeeId, BigDecimal baseSalary) {
        if (baseSalary == null) return;
        try {
            EmployeeWorkInfo wi = workInfoMapper.selectOne(
                    new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", employeeId));
            if (wi == null || wi.getPositionId() == null) return;

            Position position = positionMapper.selectById(wi.getPositionId());
            if (position == null) return;
            // 非标准职位已在入职审批阶段强制开HR二审，此处仅检查标准职位
            if (position.getLevelMin() == null || position.getLevelMax() == null) return;

            // 计算同职位员工的平均基本工资
            QueryWrapper<EmployeeWorkInfo> wiQw = new QueryWrapper<>();
            wiQw.eq("position_id", position.getId());
            List<Long> samePositionEmpIds = workInfoMapper.selectList(wiQw).stream()
                    .map(EmployeeWorkInfo::getEmployeeId).collect(Collectors.toList());
            if (samePositionEmpIds.isEmpty()) return;

            QueryWrapper<EmployeeSalary> salaryQw = new QueryWrapper<>();
            salaryQw.in("employee_id", samePositionEmpIds)
                    .isNotNull("base_salary")
                    .orderByDesc("effective_date");
            List<EmployeeSalary> salaries = this.list(salaryQw);
            // 取每人最新的一条
            BigDecimal total = BigDecimal.ZERO;
            int count = 0;
            for (EmployeeSalary s : salaries) {
                if (!s.getEmployeeId().equals(employeeId)) {
                    total = total.add(s.getBaseSalary());
                    count++;
                }
            }
            if (count == 0) return;

            BigDecimal avgSalary = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            // 超过平均值 50% 视为异常高薪
            BigDecimal threshold = avgSalary.multiply(new BigDecimal("1.5"));

            if (baseSalary.compareTo(threshold) > 0) {
                log.warn("新员工薪资超出职级范围: employeeId={}, baseSalary={}, 同职位平均={}, 阈值={}",
                        employeeId, baseSalary, avgSalary, threshold);

                // 创建HR二审审批
                Employee employee = employeeMapper.selectById(employeeId);
                String title = (employee != null ? employee.getEmployeeNo() : employeeId.toString())
                        + " 薪资超范围HR确认";
                ApprovalFlowService approvalService = approvalFlowService;
                // 使用 SALARY_BATCH 类型创建额外审批
                approvalService.createInstance(ApprovalBizType.SALARY_BATCH, employeeId, employeeId);
                log.info("已创建薪资超范围HR审批: employeeId={}", employeeId);
            }
        } catch (Exception e) {
            log.error("薪资范围检查异常: employeeId={}", employeeId, e);
            // 检查失败不影响主流程
        }
    }

    // ==================== VO 转换 ====================

    private EmployeeSalaryVO toVO(EmployeeSalary salary) {
        EmployeeSalaryVO vo = new EmployeeSalaryVO();
        BeanUtils.copyProperties(salary, vo);
        if (salary.getAccountId() != null) {
            SalaryAccount account = salaryAccountMapper.selectById(salary.getAccountId());
            vo.setAccountName(account != null ? account.getName() : "");
        }
        return vo;
    }

    private SalaryChangeHistoryVO toHistoryVO(SalaryChangeHistory history) {
        SalaryChangeHistoryVO vo = new SalaryChangeHistoryVO();
        BeanUtils.copyProperties(history, vo);
        ChangeTypeEnum changeType = ChangeTypeEnum.fromValue(history.getChangeType());
        vo.setChangeTypeLabel(changeType != null ? changeType.getLabel() : "");
        return vo;
    }
}
