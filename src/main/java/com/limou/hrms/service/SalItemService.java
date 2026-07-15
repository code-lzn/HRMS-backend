package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.SalItem;

import java.util.List;

/**
 * 工资项目服务
 */
public interface SalItemService extends IService<SalItem> {

    /**
     * 获取账套下的所有项目（按排序）
     */
    List<SalItem> getItemsByAccountId(Long accountId);

    /**
     * 添加项目
     */
    void addItem(SalItem item);

    /**
     * 编辑项目
     */
    void updateItem(SalItem item);

    /**
     * 删除项目
     */
    void deleteItem(Long itemId);

    /**
     * 调整项目排序
     */
    void sortItems(Long accountId, List<Long> itemIds);
}
