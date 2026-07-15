package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.SalItemMapper;
import com.limou.hrms.model.entity.SalItem;
import com.limou.hrms.service.SalItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 工资项目服务实现
 */
@Service
@Slf4j
public class SalItemServiceImpl extends ServiceImpl<SalItemMapper, SalItem>
        implements SalItemService {

    @Override
    public List<SalItem> getItemsByAccountId(Long accountId) {
        return this.lambdaQuery()
                .eq(SalItem::getAccountId, accountId)
                .orderByAsc(SalItem::getSortOrder)
                .list();
    }

    @Override
    public void addItem(SalItem item) {
        item.setCreateTime(new Date());
        item.setUpdateTime(new Date());
        if (item.getSortOrder() == null) {
            item.setSortOrder(0);
        }
        if (item.getIsTaxable() == null) {
            item.setIsTaxable(1);
        }
        this.save(item);
        log.info("添加工资项目成功: accountId={}, name={}", item.getAccountId(), item.getName());
    }

    @Override
    public void updateItem(SalItem item) {
        SalItem exist = this.getById(item.getId());
        ThrowUtils.throwIf(exist == null, ErrorCode.NOT_FOUND_ERROR, "工资项目不存在");
        item.setUpdateTime(new Date());
        this.updateById(item);
        log.info("编辑工资项目成功: id={}", item.getId());
    }

    @Override
    public void deleteItem(Long itemId) {
        SalItem item = this.getById(itemId);
        ThrowUtils.throwIf(item == null, ErrorCode.NOT_FOUND_ERROR, "工资项目不存在");
        this.removeById(itemId);
        log.info("删除工资项目成功: id={}", itemId);
    }

    @Override
    public void sortItems(Long accountId, List<Long> itemIds) {
        for (int i = 0; i < itemIds.size(); i++) {
            SalItem item = new SalItem();
            item.setId(itemIds.get(i));
            item.setSortOrder(i + 1);
            item.setUpdateTime(new Date());
            this.updateById(item);
        }
        log.info("调整工资项目排序成功: accountId={}, count={}", accountId, itemIds.size());
    }
}
