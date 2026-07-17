package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LeaveTypeDistributionVO implements Serializable {

    private List<String> leaveTypes;

    private List<Integer> counts;

    private List<Double> percentages;

    private static final long serialVersionUID = 1L;
}
