package com.limou.hrms.model.dto.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class TransferAddRequest implements Serializable {

    private Long employeeId;

    private Long toDeptId;

    private Long toPositionId;

    private String toRankCode;

    private Long toReporterId;

    private BigDecimal salaryAdjustment;

    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date effectiveDate;

    private Long flowId;

    private String remark;

    private String workLocation;

    private String employmentType;

    private static final long serialVersionUID = 1L;
}
