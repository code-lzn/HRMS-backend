package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.EmpSalaryProfileMapper;
import com.limou.hrms.mapper.SalAccountMapper;
import com.limou.hrms.mapper.SalItemMapper;
import com.limou.hrms.model.entity.EmpSalaryProfile;
import com.limou.hrms.model.entity.SalAccount;
import com.limou.hrms.model.entity.SalItem;
import com.limou.hrms.model.enums.ScopeTypeEnum;
import com.limou.hrms.model.vo.SalaryAccountVO;
import com.limou.hrms.model.vo.SalaryItemVO;
import com.limou.hrms.service.SalAccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 薪资账套服务实现
 */
@Service
@Slf4j
public class SalAccountServiceImpl extends ServiceImpl<SalAccountMapper, SalAccount>
        implements SalAccountService {

    @Resource
    private SalItemMapper salItemMapper;

    @Resource
    private EmpSalaryProfileMapper empSalaryProfileMapper;

    @Override
    public List<SalaryAccountVO> listAccounts() {
        List<SalAccount> accounts = this.lambdaQuery()
                .eq(SalAccount::getIsDeleted, 0)
                .orderByDesc(SalAccount::getCreateTime)
                .list();
        return accounts.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SalaryAccountVO getAccountDetail(Long id) {
        SalAccount account = this.getById(id);
        ThrowUtils.throwIf(account == null || account.getIsDeleted() == 1,
                ErrorCode.NOT_FOUND_ERROR, "账套不存在");
        return toVO(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(SalAccount account) {
        account.setIsDeleted(0);
        account.setCreateTime(new Date());
        account.setUpdateTime(new Date());
        this.save(account);
        log.info("新建账套成功: id={}, name={}", account.getId(), account.getName());
        return account.getId();
    }

    @Override
    public void updateAccount(SalAccount account) {
        SalAccount exist = this.getById(account.getId());
        ThrowUtils.throwIf(exist == null || exist.getIsDeleted() == 1,
                ErrorCode.NOT_FOUND_ERROR, "账套不存在");
        account.setUpdateTime(new Date());
        this.updateById(account);
        log.info("编辑账套成功: id={}", account.getId());
    }

    @Override
    public void deleteAccount(Long id) {
        SalAccount account = this.getById(id);
        ThrowUtils.throwIf(account == null || account.getIsDeleted() == 1,
                ErrorCode.NOT_FOUND_ERROR, "账套不存在");
        // 校验是否被员工引用
        Long count = empSalaryProfileMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmpSalaryProfile>()
                        .eq(EmpSalaryProfile::getAccountSetId, id)
                        .eq(EmpSalaryProfile::getIsDeleted, 0)
        );
        ThrowUtils.throwIf(count > 0, ErrorCode.OPERATION_ERROR, "该账套已被员工引用，无法删除");
        // 软删除
        account.setIsDeleted(1);
        account.setUpdateTime(new Date());
        this.updateById(account);
        log.info("删除账套成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyAccount(Long sourceId) {
        SalAccount source = this.getById(sourceId);
        ThrowUtils.throwIf(source == null || source.getIsDeleted() == 1,
                ErrorCode.NOT_FOUND_ERROR, "源账套不存在");
        // 复制账套
        SalAccount copy = new SalAccount();
        copy.setName(source.getName() + "(副本)");
        copy.setScopeType(source.getScopeType());
        copy.setScopeIds(source.getScopeIds());
        copy.setEffectiveDate(source.getEffectiveDate());
        copy.setIsDeleted(0);
        copy.setCreateTime(new Date());
        copy.setUpdateTime(new Date());
        this.save(copy);
        // 复制工资项目
        List<SalItem> items = salItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SalItem>()
                        .eq(SalItem::getAccountId, sourceId)
                        .orderByAsc(SalItem::getSortOrder)
        );
        for (SalItem item : items) {
            SalItem copyItem = new SalItem();
            copyItem.setAccountId(copy.getId());
            copyItem.setName(item.getName());
            copyItem.setItemType(item.getItemType());
            copyItem.setFormula(item.getFormula());
            copyItem.setSortOrder(item.getSortOrder());
            copyItem.setIsTaxable(item.getIsTaxable());
            copyItem.setCreateTime(new Date());
            copyItem.setUpdateTime(new Date());
            salItemMapper.insert(copyItem);
        }
        log.info("复制账套成功: sourceId={}, newId={}", sourceId, copy.getId());
        return copy.getId();
    }

    private SalaryAccountVO toVO(SalAccount account) {
        SalaryAccountVO vo = new SalaryAccountVO();
        vo.setId(account.getId());
        vo.setName(account.getName());
        vo.setScopeType(account.getScopeType());
        ScopeTypeEnum scopeEnum = ScopeTypeEnum.getByValue(account.getScopeType());
        vo.setScopeTypeText(scopeEnum != null ? scopeEnum.getText() : "");
        vo.setScopeIds(account.getScopeIds());
        vo.setEffectiveDate(account.getEffectiveDate());
        vo.setCreateTime(account.getCreateTime());
        vo.setUpdateTime(account.getUpdateTime());
        // 查询工资项目
        List<SalItem> items = salItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SalItem>()
                        .eq(SalItem::getAccountId, account.getId())
                        .orderByAsc(SalItem::getSortOrder)
        );
        List<SalaryItemVO> itemVOs = new ArrayList<>();
        if (items != null) {
            for (SalItem item : items) {
                SalaryItemVO itemVO = new SalaryItemVO();
                itemVO.setId(item.getId());
                itemVO.setAccountId(item.getAccountId());
                itemVO.setName(item.getName());
                itemVO.setItemType(item.getItemType());
                itemVO.setItemTypeText(
                        com.limou.hrms.model.enums.SalaryItemTypeEnum.getByValue(item.getItemType()) != null
                                ? com.limou.hrms.model.enums.SalaryItemTypeEnum.getByValue(item.getItemType()).getText()
                                : ""
                );
                itemVO.setFormula(item.getFormula());
                itemVO.setSortOrder(item.getSortOrder());
                itemVO.setIsTaxable(item.getIsTaxable());
                itemVOs.add(itemVO);
            }
        }
        vo.setItems(itemVOs);
        return vo;
    }
}
