package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工工作信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkInfoVO {

    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private String jobLevel;
    private Long directReportId;
    private String directReportName;
    private String workLocation;
}
