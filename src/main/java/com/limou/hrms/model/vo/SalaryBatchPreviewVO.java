package com.limou.hrms.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 批次预览汇总视图
 */
@Data
public class SalaryBatchPreviewVO {

    /** 批次汇总信息 */
    private SalaryBatchVO batch;

    /** 分页明细 */
    private List<SalaryDetailVO> records;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Long current;

    /** 每页大小 */
    private Long size;
}
