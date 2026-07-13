package com.limou.hrms.model.dto.attendance;

import java.io.Serializable;
import lombok.Data;

/**
 * 打卡请求
 */
@Data
public class PunchRequest implements Serializable {

    /** 打卡类型：0=上班打卡 1=下班打卡 */
    private Integer punchType;

    /** 打卡位置 */
    private String location;

    private static final long serialVersionUID = 1L;
}
