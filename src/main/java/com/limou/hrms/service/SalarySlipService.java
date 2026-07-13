package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.SalarySlip;
import com.limou.hrms.model.vo.SalarySlipDetailVO;
import com.limou.hrms.model.vo.SalarySlipVO;
import com.limou.hrms.model.vo.SalaryTrendVO;

import java.util.List;

/**
 * 薪资工资条服务
 */
public interface SalarySlipService extends IService<SalarySlip> {

    /**
     * 获取我的工资条列表（按月倒序）
     */
    List<SalarySlipVO> getMySalarySlips(Long userId);

    /**
     * 查看工资条详情（需二次密码验证）
     */
    SalarySlipDetailVO getSalarySlipDetail(Long detailId, Long userId, String password);

    /**
     * 获取近6个月薪资趋势
     */
    List<SalaryTrendVO> getMySalaryTrend(Long userId);
}
