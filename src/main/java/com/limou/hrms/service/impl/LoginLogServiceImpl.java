package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.LoginLogMapper;
import com.limou.hrms.model.entity.LoginLog;
import com.limou.hrms.model.vo.LoginLogVO;
import com.limou.hrms.service.LoginLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog>
        implements LoginLogService {

    @Override
    public List<LoginLogVO> getMyLoginLogs(Long userId) {
        List<LoginLog> logs = this.lambdaQuery()
                .eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getLoginTime)
                .last("LIMIT 30")
                .list();

        List<LoginLogVO> voList = new ArrayList<>();
        for (LoginLog log : logs) {
            LoginLogVO vo = new LoginLogVO();
            vo.setId(log.getId());
            vo.setLoginTime(log.getLoginTime());
            vo.setIp(log.getIp());
            vo.setDevice(log.getDevice());
            vo.setLoginType(log.getLoginType());
            vo.setLoginTypeText(log.getLoginType() != null && log.getLoginType() == 1 ? "密码登录" : "短信验证码登录");
            vo.setIsSuccess(log.getIsSuccess());
            vo.setFailReason(log.getFailReason());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public void recordLoginLog(Long userId, String ip, String device, Integer loginType,
                               boolean success, String failReason) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setLoginTime(new Date());
        log.setIp(ip);
        log.setDevice(device);
        log.setLoginType(loginType);
        log.setIsSuccess(success ? 1 : 0);
        log.setFailReason(failReason);
        this.save(log);
    }
}
