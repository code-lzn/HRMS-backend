package com.limou.hrms.model.dto.resignation;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ResignationAddRequest implements Serializable {

    private Long employeeId;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date resignDate;

    private String resignReasonType;

    private String resignType;

    private String detailReason;

    private Long handoverPersonId;

    private Long flowId;

    private String remark;

    private static final long serialVersionUID = 1L;
}
