package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.SalAccount;
import com.limou.hrms.model.vo.SalaryAccountVO;

import java.util.List;

/**
 * 薪资账套服务
 */
public interface SalAccountService extends IService<SalAccount> {

    /**
     * 查询账套列表（含工资项目）
     */
    List<SalaryAccountVO> listAccounts();

    /**
     * 查询账套详情（含工资项目）
     */
    SalaryAccountVO getAccountDetail(Long id);

    /**
     * 新建账套（含工资项目）
     */
    Long createAccount(SalAccount account);

    /**
     * 编辑账套基本信息
     */
    void updateAccount(SalAccount account);

    /**
     * 删除账套（软删除，校验是否被引用）
     */
    void deleteAccount(Long id);

    /**
     * 复制账套
     */
    Long copyAccount(Long sourceId);
}
