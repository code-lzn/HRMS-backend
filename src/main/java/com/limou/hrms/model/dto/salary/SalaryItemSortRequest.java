package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 工资项目排序请求
 */
@ApiModel("工资项目排序请求")
@Data
public class SalaryItemSortRequest implements Serializable {

    @ApiModelProperty("按顺序排列的项目 ID 列表")
    private List<Long> itemIds;

    private static final long serialVersionUID = 1L;
}
