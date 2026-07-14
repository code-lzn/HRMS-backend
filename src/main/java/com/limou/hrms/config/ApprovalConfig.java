package com.limou.hrms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 审批中心配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "approval")
public class ApprovalConfig {

    /** HR负责人审批节点开关，默认开启 */
    private HrNode hrNode = new HrNode();

    @Data
    public static class HrNode {
        /** 是否启用 HR 负责人审批节点 */
        private boolean enabled = true;
    }
}
