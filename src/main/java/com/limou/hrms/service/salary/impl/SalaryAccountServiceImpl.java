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
import org.apache.commons.lang3.StringUtils;
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
        if (request == null || StringUtils.isBlank(request.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账套名称不能为空");
        }
        SalaryAccount account = new SalaryAccount();
        account.setName(request.getName());
        account.setScopeType(request.getScopeType());
        if (request.getScopeIds() != null && !request.getScopeIds().isEmpty()) {
            account.setScopeIds(JSONUtil.toJsonStr(request.getScopeIds()));
        }
        account.setEffectiveDate(request.getEffectiveDate());
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
        if (StringUtils.isNotBlank(request.getName())) {
            account.setName(request.getName());
        }
        if (request.getScopeType() != null) {
            account.setScopeType(request.getScopeType());
        }
        if (request.getScopeIds() != null) {
            account.setScopeIds(JSONUtil.toJsonStr(request.getScopeIds()));
        }
        if (request.getEffectiveDate() != null) {
            account.setEffectiveDate(request.getEffectiveDate());
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
        if (StringUtils.isNotBlank(request.getName())) {
            wrapper.like("name", request.getName());
        }
        if (request.getScopeType() != null) {
            wrapper.eq("scope_type", request.getScopeType());
        }
        wrapper.orderByDesc("create_time");
        List<SalaryAccount> accounts = this.list(wrapper);
        return accounts.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Long addSalaryItem(Long accountId, SalaryItemAddRequest request) {
        if (request == null || StringUtils.isBlank(request.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "项目名称不能为空");
        }
        SalaryItem item = new SalaryItem();
        item.setAccountId(accountId);
        item.setName(request.getName());
        item.setItemType(request.getItemType());
        item.setFormula(request.getFormula());
        item.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        item.setIsTaxable(request.getIsTaxable() != null ? request.getIsTaxable() : 1);
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
        if (StringUtils.isNotBlank(request.getName())) {
            item.setName(request.getName());
        }
        if (request.getItemType() != null) {
            item.setItemType(request.getItemType());
        }
        if (request.getFormula() != null) {
            item.setFormula(request.getFormula());
        }
        if (request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
        }
        if (request.getIsTaxable() != null) {
            item.setIsTaxable(request.getIsTaxable());
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
        if (request == null || request.getItemIds() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        for (int i = 0; i < request.getItemIds().size(); i++) {
            SalaryItem item = salaryItemMapper.selectById(request.getItemIds().get(i));
            if (item != null && item.getAccountId().equals(accountId)) {
                item.setSortOrder(i);
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
        // 清除 BeanUtils 的 String 拷贝，手动处理 scopeIds
        vo.setScopeIds(null);
        if (StringUtils.isNotBlank(account.getScopeIds())) {
            String raw = account.getScopeIds().trim();
            if (raw.startsWith("[")) {
                // JSON 格式: "[1,2,3]"
                vo.setScopeIds(JSONUtil.toList(raw, Long.class));
            } else {
                // 逗号分隔格式: "1,2,3"
                vo.setScopeIds(java.util.Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(java.util.stream.Collectors.toList()));
            }
        }
        ScopeTypeEnum scopeType = ScopeTypeEnum.fromValue(account.getScopeType());
        vo.setScopeTypeLabel(scopeType != null ? scopeType.getLabel() : "");
        vo.setItems(getSalaryItems(account.getId()));
        return vo;
    }

    private SalaryItemVO toItemVO(SalaryItem item) {
        SalaryItemVO vo = new SalaryItemVO();
        BeanUtils.copyProperties(item, vo);
        SalaryItemTypeEnum typeEnum = SalaryItemTypeEnum.fromValue(item.getItemType());
        vo.setItemTypeLabel(typeEnum != null ? typeEnum.getLabel() : "");
        return vo;
    }
}
