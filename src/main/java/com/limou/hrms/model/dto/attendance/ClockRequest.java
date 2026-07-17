package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 打卡请求
 */
@Data
public class ClockRequest implements Serializable {

    /**
     * 打卡类型：1=上班打卡 2=下班打卡
     */
    @NotNull(message = "打卡类型不能为空")
    private Integer clockType;

    private static final long serialVersionUID = 1L;
}
