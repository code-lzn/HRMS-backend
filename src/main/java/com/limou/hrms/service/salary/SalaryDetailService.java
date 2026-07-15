package com.limou.hrms.service.salary;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.salary.PayslipVerifyRequest;
import com.limou.hrms.model.entity.SalaryDetail;
import com.limou.hrms.model.vo.salary.PayslipVO;
import java.util.List;

/**
 * 薪资明细（工资条）服务接口
 */
public interface SalaryDetailService extends IService<SalaryDetail> {

    /**
     * 获取我的工资条列表（仅已通过/已发放的批次）
     */
    List<PayslipVO> getMyPayslips(Long employeeId);

    /**
     * 查看工资条详情（需二次验证后才能查看）
     */
    PayslipVO getPayslipDetail(Long employeeId, Long detailId);

    /**
     * 二次验证
     */
    boolean verifyPayslip(Long employeeId, Long detailId, PayslipVerifyRequest request);
}
