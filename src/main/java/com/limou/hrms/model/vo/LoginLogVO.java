package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogVO implements Serializable {

    private Long id;

    private String loginTime;

    private String ipAddress;

    private String device;

    /**
     * 1=成功 0=失败
     */
    private Integer result;

    private String resultDesc;

    private static final long serialVersionUID = 1L;
}
