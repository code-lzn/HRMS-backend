package com.limou.hrms.service.salary.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.EmployeeSalaryMapper;
import com.limou.hrms.mapper.SalaryChangeHistoryMapper;
import com.limou.hrms.mapper.SalaryAccountMapper;
import com.limou.hrms.model.dto.salary.EmployeeSalaryUpdateRequest;
import com.limou.hrms.model.entity.EmployeeSalary;
import com.limou.hrms.model.entity.SalaryAccount;
import com.limou.hrms.model.entity.SalaryChangeHistory;
import com.limou.hrms.model.enums.ChangeTypeEnum;
import com.limou.hrms.model.vo.salary.EmployeeSalaryVO;
import com.limou.hrms.model.vo.salary.SalaryChangeHistoryVO;
import com.limou.hrms.service.salary.EmployeeSalaryService;
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

    @Override
    public EmployeeSalaryVO getEmployeeSalary(Long employeeId) {
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
        newSalary.setEmployee_id(employeeId);
        newSalary.setAccount_id(request.getAccount_id());
        newSalary.setBase_salary(request.getBase_salary());
        newSalary.setAllowance_base(request.getAllowance_base());
        newSalary.setSocial_security_base(request.getSocial_security_base());
        newSalary.setHousing_fund_base(request.getHousing_fund_base());
        newSalary.setPerformance_base(request.getPerformance_base());
        newSalary.setEffective_date(request.getEffective_date());
        this.save(newSalary);

        // 记录调薪历史
        SalaryChangeHistory history = new SalaryChangeHistory();
        history.setEmployee_id(employeeId);
        history.setChange_type(ChangeTypeEnum.SALARY_ADJUST.getValue());
        if (oldSalary != null) {
            history.setOld_value(JSONUtil.toJsonStr(oldSalary));
        }
        history.setNew_value(JSONUtil.toJsonStr(newSalary));
        history.setEffective_date(request.getEffective_date());
        history.setOperator_id(operatorId);
        history.setRemark(request.getRemark());
        salaryChangeHistoryMapper.insert(history);

        log.info("员工 {} 薪资档案已更新，操作人：{}", employeeId, operatorId);
    }

    @Override
    public List<SalaryChangeHistoryVO> getSalaryHistory(Long employeeId) {
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

    // ==================== VO 转换 ====================

    private EmployeeSalaryVO toVO(EmployeeSalary salary) {
        EmployeeSalaryVO vo = new EmployeeSalaryVO();
        BeanUtils.copyProperties(salary, vo);
        // 获取账套名称
        if (salary.getAccount_id() != null) {
            SalaryAccount account = salaryAccountMapper.selectById(salary.getAccount_id());
            vo.setAccount_name(account != null ? account.getName() : "");
        }
        return vo;
    }

    private SalaryChangeHistoryVO toHistoryVO(SalaryChangeHistory history) {
        SalaryChangeHistoryVO vo = new SalaryChangeHistoryVO();
        BeanUtils.copyProperties(history, vo);
        ChangeTypeEnum changeType = ChangeTypeEnum.fromValue(history.getChange_type());
        vo.setChange_type_label(changeType != null ? changeType.getLabel() : "");
        return vo;
    }
}
