package com.limou.hrms.service.salary.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.SalaryAccountMapper;
import com.limou.hrms.mapper.SalaryItemMapper;
import com.limou.hrms.model.dto.salary.SalaryAccountAddRequest;
import com.limou.hrms.model.dto.salary.SalaryAccountQueryRequest;
import com.limou.hrms.model.dto.salary.SalaryAccountUpdateRequest;
import com.limou.hrms.model.dto.salary.SalaryItemAddRequest;
import com.limou.hrms.model.dto.salary.SalaryItemSortRequest;
import com.limou.hrms.model.dto.salary.SalaryItemUpdateRequest;
import com.limou.hrms.model.entity.SalaryAccount;
import com.limou.hrms.model.entity.SalaryItem;
import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import com.limou.hrms.model.enums.ScopeTypeEnum;
import com.limou.hrms.model.vo.salary.SalaryAccountVO;
import com.limou.hrms.model.vo.salary.SalaryItemVO;
import com.limou.hrms.service.salary.SalaryAccountService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 薪资账套服务实现
 */
@Service
@Slf4j
public class SalaryAccountServiceImpl extends ServiceImpl<SalaryAccountMapper, SalaryAccount>
        implements SalaryAccountService {

    @Resource
    private SalaryItemMapper salaryItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(SalaryAccountAddRequest request) {
        if (request == null || request.getName() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账套名称不能为空");
        }
        SalaryAccount account = new SalaryAccount();
        account.setName(request.getName());
        account.setScope_type(request.getScope_type());
        if (request.getScope_ids() != null) {
            account.setScope_ids(JSONUtil.toJsonStr(request.getScope_ids()));
        }
        account.setEffective_date(request.getEffective_date());
        boolean saved = this.save(account);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建账套失败");
        }
        // 同步创建工资项目
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (SalaryItemAddRequest itemReq : request.getItems()) {
                addSalaryItem(account.getId(), itemReq);
            }
        }
        return account.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(SalaryAccountUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SalaryAccount account = this.getById(request.getId());
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "账套不存在");
        }
        if (request.getName() != null) {
            account.setName(request.getName());
        }
        if (request.getScope_type() != null) {
            account.setScope_type(request.getScope_type());
        }
        if (request.getScope_ids() != null) {
            account.setScope_ids(JSONUtil.toJsonStr(request.getScope_ids()));
        }
        if (request.getEffective_date() != null) {
            account.setEffective_date(request.getEffective_date());
        }
        this.updateById(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long id) {
        SalaryAccount account = this.getById(id);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "账套不存在");
        }
        // 删除关联的工资项目
        QueryWrapper<SalaryItem> wrapper = new QueryWrapper<>();
        wrapper.eq("account_id", id);
        salaryItemMapper.delete(wrapper);
        // 逻辑删除账套
        this.removeById(id);
    }

    @Override
    public SalaryAccountVO getAccountDetail(Long id) {
        SalaryAccount account = this.getById(id);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "账套不存在");
        }
        return toVO(account);
    }

    @Override
    public List<SalaryAccountVO> listAccounts(SalaryAccountQueryRequest request) {
        QueryWrapper<SalaryAccount> wrapper = new QueryWrapper<>();
        if (request.getName() != null) {
            wrapper.like("name", request.getName());
        }
        if (request.getScope_type() != null) {
            wrapper.eq("scope_type", request.getScope_type());
        }
        wrapper.orderByDesc("create_time");
        List<SalaryAccount> accounts = this.list(wrapper);
        return accounts.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Long addSalaryItem(Long accountId, SalaryItemAddRequest request) {
        if (request == null || request.getName() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "项目名称不能为空");
        }
        SalaryItem item = new SalaryItem();
        item.setAccount_id(accountId);
        item.setName(request.getName());
        item.setItem_type(request.getItem_type());
        item.setFormula(request.getFormula());
        item.setSort_order(request.getSort_order() != null ? request.getSort_order() : 0);
        item.setIs_taxable(request.getIs_taxable() != null ? request.getIs_taxable() : 1);
        salaryItemMapper.insert(item);
        return item.getId();
    }

    @Override
    public void updateSalaryItem(SalaryItemUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SalaryItem item = salaryItemMapper.selectById(request.getId());
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "工资项目不存在");
        }
        if (request.getName() != null) {
            item.setName(request.getName());
        }
        if (request.getItem_type() != null) {
            item.setItem_type(request.getItem_type());
        }
        if (request.getFormula() != null) {
            item.setFormula(request.getFormula());
        }
        if (request.getSort_order() != null) {
            item.setSort_order(request.getSort_order());
        }
        if (request.getIs_taxable() != null) {
            item.setIs_taxable(request.getIs_taxable());
        }
        salaryItemMapper.updateById(item);
    }

    @Override
    public void deleteSalaryItem(Long itemId) {
        if (itemId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryItemMapper.deleteById(itemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortSalaryItems(Long accountId, SalaryItemSortRequest request) {
        if (request == null || request.getItem_ids() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        for (int i = 0; i < request.getItem_ids().size(); i++) {
            SalaryItem item = salaryItemMapper.selectById(request.getItem_ids().get(i));
            if (item != null && item.getAccount_id().equals(accountId)) {
                item.setSort_order(i);
                salaryItemMapper.updateById(item);
            }
        }
    }

    @Override
    public List<SalaryItemVO> getSalaryItems(Long accountId) {
        QueryWrapper<SalaryItem> wrapper = new QueryWrapper<>();
        wrapper.eq("account_id", accountId).orderByAsc("sort_order");
        List<SalaryItem> items = salaryItemMapper.selectList(wrapper);
        return items.stream().map(this::toItemVO).collect(Collectors.toList());
    }

    // ==================== VO 转换 ====================

    private SalaryAccountVO toVO(SalaryAccount account) {
        SalaryAccountVO vo = new SalaryAccountVO();
        BeanUtils.copyProperties(account, vo);
        ScopeTypeEnum scopeType = ScopeTypeEnum.fromValue(account.getScope_type());
        vo.setScope_type_label(scopeType != null ? scopeType.getLabel() : "");
        vo.setItems(getSalaryItems(account.getId()));
        return vo;
    }

    private SalaryItemVO toItemVO(SalaryItem item) {
        SalaryItemVO vo = new SalaryItemVO();
        BeanUtils.copyProperties(item, vo);
        SalaryItemTypeEnum typeEnum = SalaryItemTypeEnum.fromValue(item.getItem_type());
        vo.setItem_type_label(typeEnum != null ? typeEnum.getLabel() : "");
        return vo;
    }
}
