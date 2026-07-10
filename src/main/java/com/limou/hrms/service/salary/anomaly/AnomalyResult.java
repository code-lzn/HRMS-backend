package com.limou.hrms.service.salary.anomaly;

import com.limou.hrms.model.enums.AbnormalLevelEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 异常检测结果
 */
@Data
@AllArgsConstructor
public class AnomalyResult {

    private AbnormalLevelEnum level;

    private String reason;
}
