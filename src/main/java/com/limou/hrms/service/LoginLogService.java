package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.LoginLog;
import com.limou.hrms.model.vo.LoginLogVO;

import java.util.List;

public interface LoginLogService extends IService<LoginLog> {

    /**
     * 获取我的登录日志（最近30条）
     */
    List<LoginLogVO> getMyLoginLogs(Long userId);

    /**
     * 记录登录日志
     */
    void recordLoginLog(Long userId, String ip, String device, Integer loginType,
                        boolean success, String failReason);
}
