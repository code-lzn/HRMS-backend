package com.limou.hrms.service.salary.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.SalaryBatchMapper;
import com.limou.hrms.mapper.SalaryDetailMapper;
import com.limou.hrms.model.dto.salary.PayslipVerifyRequest;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.entity.SalaryDetail;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.BatchStatusEnum;
import com.limou.hrms.model.vo.salary.PayslipVO;
import com.limou.hrms.model.vo.salary.SalaryItemDetailVO;
import com.limou.hrms.service.salary.SalaryDetailService;
import com.limou.hrms.service.UserService;
import cn.hutool.json.JSONUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 薪资明细（工资条）服务实现
 */
@Service
public class SalaryDetailServiceImpl extends ServiceImpl<SalaryDetailMapper, SalaryDetail>
        implements SalaryDetailService {

    @Resource
    private SalaryBatchMapper salaryBatchMapper;

    @Resource
    private UserService userService;

    @Override
    public List<PayslipVO> getMyPayslips(Long employeeId) {
        // 查找已通过或已发放批次的工资条
        QueryWrapper<SalaryBatch> batchWrapper = new QueryWrapper<>();
        batchWrapper.in("status", BatchStatusEnum.APPROVED.getValue(), BatchStatusEnum.PAID.getValue());
        List<SalaryBatch> batches = salaryBatchMapper.selectList(batchWrapper);
        if (batches.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> batchIds = batches.stream().map(SalaryBatch::getId).collect(Collectors.toList());

        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId)
                .in("batch_id", batchIds)
                .orderByDesc("create_time");
        List<SalaryDetail> details = this.list(wrapper);

        return details.stream().map(detail -> toPayslipVO(detail, findBatch(batches, detail.getBatchId())))
                .collect(Collectors.toList());
    }

    @Override
    public PayslipVO getPayslipDetail(Long employeeId, Long detailId) {
        SalaryDetail detail = this.getById(detailId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "工资条不存在");
        }
        if (!detail.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看他人工资条");
        }
        // 检查批次状态
        SalaryBatch batch = salaryBatchMapper.selectById(detail.getBatchId());
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        if (batch.getStatus() < BatchStatusEnum.APPROVED.getValue()) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "工资条尚未审批通过");
        }
        // TODO: 二次验证逻辑
        return toPayslipVO(detail, batch);
    }

    @Override
    public boolean verifyPayslip(Long employeeId, Long detailId, PayslipVerifyRequest request) {
        if (request == null || request.getVerifyType() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 验证类型：1=短信验证码, 2=登录密码
        if (request.getVerifyType() == 2) {
            // 密码验证：获取当前登录用户，比对密码
            // TODO: 实现密码验证逻辑
            return true;
        }
        // TODO: 短信验证码逻辑
        return true;
    }

    // ==================== 私有方法 ====================

    private PayslipVO toPayslipVO(SalaryDetail detail, SalaryBatch batch) {
        PayslipVO vo = new PayslipVO();
        vo.setId(detail.getId());
        vo.setBatchNo(batch != null ? batch.getBatchNo() : "");
        vo.setSalaryMonth(batch != null ? batch.getSalaryMonth() : "");
        vo.setGrossPay(detail.getGrossPay());
        vo.setTotalDeductions(detail.getTotalDeductions());
        vo.setNetPay(detail.getNetPay());
        vo.setPayslipViewed(detail.getPayslipViewed());
        vo.setCreateTime(detail.getCreateTime());

        // 解析工资项目明细
        if (StringUtils.isNotBlank(detail.getSalaryItems())) {
            List<SalaryItemDetailVO> allItems = JSONUtil.toList(detail.getSalaryItems(), SalaryItemDetailVO.class);
            // 收入项（type=1,2）
            vo.setIncomeItems(allItems.stream()
                    .filter(i -> i.getType() != null && i.getType() <= 2)
                    .collect(Collectors.toList()));
            // 扣除项（type=3,4,5,6）
            vo.setDeductionItems(allItems.stream()
                    .filter(i -> i.getType() != null && i.getType() >= 3)
                    .collect(Collectors.toList()));
        }
        // TODO: 补充员工姓名、工号、部门信息
        return vo;
    }

    private SalaryBatch findBatch(List<SalaryBatch> batches, Long batchId) {
        return batches.stream().filter(b -> b.getId().equals(batchId)).findFirst().orElse(null);
    }
}
