package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class AttendanceGroupVO implements Serializable {

    private Long id;

    private String groupName;

    private Integer shiftType;

    private String shiftTypeText;

    private Date workStartTime;

    private Date workEndTime;

    private Date lunchStartTime;

    private Date lunchEndTime;

    private Date flexibleStart;

    private Date flexibleEnd;

    private Integer lateThreshold;

    private Integer earlyThreshold;

    private String description;

    private Integer status;

    private String statusText;

    private Integer employeeCount;

    private List<Long> employeeIds;

    private List<Long> departmentIds;

    private List<String> departmentNames;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
