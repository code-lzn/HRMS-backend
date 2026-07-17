package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工作日历实体
 */
@TableName("work_calendar")
@Data
public class WorkCalendar implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate calendarDate;

    /**
     * 日期类型：1=工作日 2=休息日 3=节假日
     */
    private Integer dayType;

    private String holidayName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
