package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 最近操作动态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentLogVO implements Serializable {

    /** 操作人姓名 */
    private String operatorName;
    /** 操作类型 */
    private String actionType;
    /** 操作描述 */
    private String description;
    /** 操作时间 */
    private Date operateTime;

    private static final long serialVersionUID = 1L;
}
