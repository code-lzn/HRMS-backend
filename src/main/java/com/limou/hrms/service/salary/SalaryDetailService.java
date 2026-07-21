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
     * 发送工资条二次验证码（短信 / 降级密码）
     */
    /** @return 验证码 */
    String sendPayslipVerifyCode(Long employeeId, Long detailId);

    /**
     * 校验工资条验证码（或密码）
     */
    boolean verifyPayslip(Long employeeId, Long detailId, PayslipVerifyRequest request);

    /**
     * 查看工资条详情（需先通过验证码校验，已验证后可直接查看）
     */
    PayslipVO getPayslipDetail(Long employeeId, Long detailId);

    /**
     * 检查工资条是否已通过验证（本次登录期间）
     */
    boolean isPayslipVerified(Long employeeId, Long detailId);
}
