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

    private String workStartTime;

    private String workEndTime;

    private String lunchStartTime;

    private String lunchEndTime;

    private String flexibleStart;

    private String flexibleEnd;

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
