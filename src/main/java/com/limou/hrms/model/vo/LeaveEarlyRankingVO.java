package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class LeaveEarlyRankingVO implements Serializable {

    private String departmentName;

    private int lateCount;

    private int earlyLeaveCount;

    private static final long serialVersionUID = 1L;
}