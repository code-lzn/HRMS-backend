package com.limou.hrms.model.query;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 转正申请分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProbationQuery extends PageRequest {

    /** 状态筛选 */
    private Integer status;

    /** 员工ID筛选 */
    private Long employeeId;

    /** 部门ID筛选 */
    private Long departmentId;
    /** 关键词搜索（员工姓名/工号） */
    private String keyword;
}
