package com.limou.hrms.service.salary;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.salary.SalaryAccountAddRequest;
import com.limou.hrms.model.dto.salary.SalaryAccountQueryRequest;
import com.limou.hrms.model.dto.salary.SalaryAccountUpdateRequest;
import com.limou.hrms.model.dto.salary.SalaryItemAddRequest;
import com.limou.hrms.model.dto.salary.SalaryItemSortRequest;
import com.limou.hrms.model.dto.salary.SalaryItemUpdateRequest;
import com.limou.hrms.model.entity.SalaryAccount;
import com.limou.hrms.model.vo.salary.SalaryAccountVO;
import com.limou.hrms.model.vo.salary.SalaryItemVO;
import java.util.List;

/**
 * 薪资账套服务接口
 */
public interface SalaryAccountService extends IService<SalaryAccount> {

    /**
     * 新建账套（可选同步创建工资项目）
     */
    Long createAccount(SalaryAccountAddRequest request);

    /**
     * 编辑账套
     */
    void updateAccount(SalaryAccountUpdateRequest request);

    /**
     * 删除账套（逻辑删除，同时删除关联的工资项目）
     */
    void deleteAccount(Long id);

    /**
     * 获取账套详情（含工资项目列表）
     */
    SalaryAccountVO getAccountDetail(Long id);

    /**
     * 查询账套列表
     */
    List<SalaryAccountVO> listAccounts(SalaryAccountQueryRequest request);

    /**
     * 添加工资项目
     */
    Long addSalaryItem(Long accountId, SalaryItemAddRequest request);

    /**
     * 编辑工资项目
     */
    void updateSalaryItem(SalaryItemUpdateRequest request);

    /**
     * 删除工资项目
     */
    void deleteSalaryItem(Long itemId);

    /**
     * 调整工资项目排序
     */
    void sortSalaryItems(Long accountId, SalaryItemSortRequest request);

    /**
     * 获取账套下的工资项目列表
     */
    List<SalaryItemVO> getSalaryItems(Long accountId);
}
