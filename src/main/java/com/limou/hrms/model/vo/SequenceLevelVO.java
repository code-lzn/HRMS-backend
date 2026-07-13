package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 序列职级对照
 */
@Data
public class SequenceLevelVO implements Serializable {

    /** 序列值：1=M 2=P 3=S */
    private Integer sequence;

    /** 序列名称 */
    private String sequenceName;

    /** 序列编码：M/P/S */
    private String sequenceCode;

    /** 职级列表 */
    private List<String> levels;

    private static final long serialVersionUID = 1L;
}
