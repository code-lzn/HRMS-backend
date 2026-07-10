package com.limou.hrms.model.vo.salary;

import java.util.List;
import lombok.Data;

/**
 * 批次预览VO（含批次汇总 + 明细列表）
 */
@Data
public class SalaryBatchPreviewVO {

    /**
     * 批次汇总
     */
    private SalaryBatchVO batch;

    /**
     * 薪资明细列表
     */
    private List<SalaryDetailVO> records;

    /**
     * 总记录数
     */
    private Long total;
}
