package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("holiday_config")
@Data
public class HolidayConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Date holidayDate;

    private String holidayName;

    private Integer holidayType;

    private String description;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
