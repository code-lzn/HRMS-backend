package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class HolidayConfigVO implements Serializable {

    private Long id;

    private Date holidayDate;

    private String holidayName;

    private Integer holidayType;

    private String holidayTypeText;

    private String description;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
