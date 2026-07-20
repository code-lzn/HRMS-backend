package com.limou.hrms.model.query;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 请假列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveQuery extends PageRequest implements Serializable {

    /** 强制只看该员工（个人中心用，绕过数据权限） */
    private Long employeeId;

    /** 模糊搜索（员工姓名） */
    private String keyword;

    /** 请假类型 */
    private Integer leaveType;

    /** 状态 */
    private Integer status;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    private static final long serialVersionUID = 1L;
}
