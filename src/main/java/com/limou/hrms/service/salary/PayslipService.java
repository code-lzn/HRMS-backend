package com.limou.hrms.service.salary;

import com.limou.hrms.model.dto.salary.PayslipVerifyRequest;
import com.limou.hrms.model.vo.salary.PayslipListVO;
import com.limou.hrms.model.vo.salary.PayslipVO;
import java.util.List;

/**
 * 工资条服务接口
 */
public interface PayslipService {

    /**
     * 获取员工工资条列表（按月倒序，仅已通过/已发放的批次）
     */
    List<PayslipListVO> getMyPayslips(Long employeeId);

    /**
     * 二次验证
     */
    boolean verify(Long employeeId, Long batchId, PayslipVerifyRequest request);

    /**
     * 获取工资条详情（已验证通过后才能查看）
     */
    PayslipVO getPayslipDetail(Long employeeId, Long batchId);

    /**
     * 获取个人薪资趋势（近N个月）
     */
    List<PayslipListVO> getSalaryTrend(Long employeeId, int months);
}
