package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 提交补卡申请 DTO
 */
@Data
public class SupplementCardSubmitDTO implements Serializable {

    /**
     * 补卡日期
     */
    @NotNull(message = "补卡日期不能为空")
    private LocalDate attendanceDate;

    /**
     * 补卡类型：1=上班卡 2=下班卡
     */
    @NotNull(message = "补卡类型不能为空")
    private Integer cardType;

    /**
     * 补卡原因
     */
    @NotBlank(message = "补卡原因不能为空")
    private String reason;

    private static final long serialVersionUID = 1L;
}
