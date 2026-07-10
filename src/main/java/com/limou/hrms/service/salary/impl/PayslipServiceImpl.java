package com.limou.hrms.service.salary.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.SalaryBatchMapper;
import com.limou.hrms.mapper.SalaryDetailMapper;
import com.limou.hrms.model.dto.salary.PayslipVerifyRequest;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.entity.SalaryDetail;
import com.limou.hrms.model.enums.BatchStatusEnum;
import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import com.limou.hrms.model.vo.salary.PayslipListVO;
import com.limou.hrms.model.vo.salary.PayslipVO;
import com.limou.hrms.model.vo.salary.SalaryItemAmountVO;
import com.limou.hrms.service.salary.PayslipService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 工资条服务实现
 */
@Service
@Slf4j
public class PayslipServiceImpl implements PayslipService {

    @Resource
    private SalaryBatchMapper salaryBatchMapper;

    @Resource
    private SalaryDetailMapper salaryDetailMapper;

    // 10分钟内免验证（简化实现，实际应存Redis）
    private static final long VERIFY_WINDOW_MS = 10 * 60 * 1000;

    @Override
    public List<PayslipListVO> getMyPayslips(Long employeeId) {
        // 查询已通过/已发放批次的该员工薪资明细
        QueryWrapper<SalaryBatch> batchWrapper = new QueryWrapper<>();
        batchWrapper.in("status", BatchStatusEnum.APPROVED.getValue(), BatchStatusEnum.PAID.getValue())
                .orderByDesc("salary_month");
        List<SalaryBatch> batches = salaryBatchMapper.selectList(batchWrapper);

        List<PayslipListVO> result = new ArrayList<>();
        for (SalaryBatch batch : batches) {
            QueryWrapper<SalaryDetail> detailWrapper = new QueryWrapper<>();
            detailWrapper.eq("batch_id", batch.getId())
                    .eq("employee_id", employeeId);
            SalaryDetail detail = salaryDetailMapper.selectOne(detailWrapper);
            if (detail != null) {
                PayslipListVO vo = new PayslipListVO();
                vo.setBatch_id(batch.getId());
                vo.setSalary_month(batch.getSalary_month());
                vo.setGross_pay(detail.getGross_pay());
                vo.setNet_pay(detail.getNet_pay());
                vo.setStatus(batch.getStatus());
                BatchStatusEnum statusEnum = BatchStatusEnum.fromValue(batch.getStatus());
                vo.setStatus_label(statusEnum != null ? statusEnum.getLabel() : "");
                vo.setPayslip_viewed(detail.getPayslip_viewed());
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public boolean verify(Long employeeId, Long batchId, PayslipVerifyRequest request) {
        if (request == null || request.getVerify_code() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        // 简化实现：验证码为"123456"或密码验证
        // 实际应接入短信服务或密码校验
        if ("123456".equals(request.getVerify_code())) {
            log.info("员工 {} 工资条验证通过（批次 {}）", employeeId, batchId);
            return true;
        }
        // 密码验证（备用）
        if (request.getVerify_type() == 2) {
            // TODO: 实际应调用 passwordEncoder.matches(verify_code, employee.getPassword())
            log.info("员工 {} 使用密码验证", employeeId);
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证失败，验证码错误");
    }

    @Override
    public PayslipVO getPayslipDetail(Long employeeId, Long batchId) {
        SalaryBatch batch = salaryBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }

        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_id", batchId)
                .eq("employee_id", employeeId);
        SalaryDetail detail = salaryDetailMapper.selectOne(wrapper);
        if (detail == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到薪资记录");
        }

        // 标记已查看
        detail.setPayslip_viewed(1);
        salaryDetailMapper.updateById(detail);

        // 解析工资项
        List<SalaryItemAmountVO> allItems = new ArrayList<>();
        if (detail.getSalary_items() != null) {
            allItems = JSONUtil.toList(
                    JSONUtil.parseArray(detail.getSalary_items()), SalaryItemAmountVO.class);
        }

        // 分离收入项和扣除项
        List<SalaryItemAmountVO> incomeItems = allItems.stream()
                .filter(item -> item.getType() == SalaryItemTypeEnum.FIXED_INCOME.getValue()
                        || item.getType() == SalaryItemTypeEnum.VARIABLE_INCOME.getValue())
                .collect(Collectors.toList());

        List<SalaryItemAmountVO> deductionItems = allItems.stream()
                .filter(item -> item.getType() == SalaryItemTypeEnum.ATTENDANCE_DEDUCT.getValue()
                        || item.getType() == SalaryItemTypeEnum.SOCIAL_SECURITY.getValue()
                        || item.getType() == SalaryItemTypeEnum.HOUSING_FUND.getValue()
                        || item.getType() == SalaryItemTypeEnum.INCOME_TAX.getValue())
                .collect(Collectors.toList());

        PayslipVO vo = new PayslipVO();
        vo.setSalary_month(batch.getSalary_month());
        vo.setGross_pay(detail.getGross_pay());
        vo.setTotal_deductions(detail.getTotal_deductions());
        vo.setNet_pay(detail.getNet_pay());
        vo.setIncome_items(incomeItems);
        vo.setDeduction_items(deductionItems);

        return vo;
    }

    @Override
    public List<PayslipListVO> getSalaryTrend(Long employeeId, int months) {
        // 获取最近N个月的实发工资
        QueryWrapper<SalaryBatch> batchWrapper = new QueryWrapper<>();
        batchWrapper.in("status", BatchStatusEnum.APPROVED.getValue(), BatchStatusEnum.PAID.getValue())
                .orderByDesc("salary_month")
                .last("LIMIT " + months);
        List<SalaryBatch> batches = salaryBatchMapper.selectList(batchWrapper);

        List<PayslipListVO> result = new ArrayList<>();
        for (SalaryBatch batch : batches) {
            QueryWrapper<SalaryDetail> detailWrapper = new QueryWrapper<>();
            detailWrapper.eq("batch_id", batch.getId())
                    .eq("employee_id", employeeId);
            SalaryDetail detail = salaryDetailMapper.selectOne(detailWrapper);
            if (detail != null) {
                PayslipListVO vo = new PayslipListVO();
                vo.setSalary_month(batch.getSalary_month());
                vo.setNet_pay(detail.getNet_pay());
                result.add(vo);
            }
        }
        return result;
    }
}
