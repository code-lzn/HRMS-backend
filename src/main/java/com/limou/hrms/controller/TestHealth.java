package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.utils.DingTalkUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestHealth {

    @RequestMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("OK");
    }

    @GetMapping("/test/dingtalk")
    public BaseResponse<String> testDingTalk() {
        boolean ok = DingTalkUtil.sendText("钉钉告警测试消息，如果你收到这条消息说明配置成功！");
        return ResultUtils.success(ok ? "发送成功，请查看钉钉群" : "发送失败，请检查后端日志");
    }
}
