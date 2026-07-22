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

    /** 所属部门ID */
    private Long departmentId;

    /** 所属部门名称 */
    private String departmentName;

    /** 职位ID */
    private Long positionId;

    /** 职位名称 */
    private String positionName;

    /** 职级（如 P5、M3） */
    private String jobLevel;

    /** 直接汇报人员工ID */
    private Long directReportId;

    /** 直接汇报人姓名 */
    private String directReportName;

    /** 工作地点 */
    private String workLocation;
}