package com.limou.hrms.model.dto.approval;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 委托审批设置请求参数
 */
@Data
public class DelegateSettingDTO {

    /** 被委托人ID（employee.id） */
    private Long delegateId;

    /** 委托开始时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 委托结束时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
