package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AttendanceTrendVO implements Serializable {

    private List<String> months;

    private List<Double> rates;

    private static final long serialVersionUID = 1L;
}
