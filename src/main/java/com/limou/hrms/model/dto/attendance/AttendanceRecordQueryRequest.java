package com.limou.hrms.model.dto.attendance;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 打卡记录查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceRecordQueryRequest extends PageRequest implements Serializable {

    /**
     * 员工ID（HR/部门主管查询他人时传入）
     */
    private Long employeeId;

    /**
     * 部门ID（HR可筛选指定部门；部门主管仅可传本部门及下属部门）
     */
    private Long departmentId;

    /**
     * 考勤日期起始
     */
    private LocalDate startDate;

    /**
     * 考勤日期截止
     */
    private LocalDate endDate;

    /**
     * 上班状态筛选：1=正常 2=迟到 3=旷工半天 4=缺卡
     */
    private Integer startStatus;

    /**
     * 下班状态筛选：1=正常 2=早退 3=旷工半天 4=缺卡
     */
    private Integer endStatus;

    private static final long serialVersionUID = 1L;
}
