package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 模块使用频率
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleUsageVO implements Serializable {

    /** 模块名称 */
    private String moduleName;
    /** 使用次数 */
    private Long usageCount;

    private static final long serialVersionUID = 1L;
}
