package com.limou.hrms.builder;

import com.limou.hrms.model.enums.ApprovalBizType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批节点构建器工厂 — 按业务类型路由到对应 Builder
 */
@Component
public class ApprovalNodeBuilderFactory {

    @Resource
    private List<ApprovalNodeBuilder> builders;

    private final Map<ApprovalBizType, ApprovalNodeBuilder> builderMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ApprovalNodeBuilder builder : builders) {
            builderMap.put(builder.supportedBizType(), builder);
        }
    }

    public ApprovalNodeBuilder getBuilder(ApprovalBizType bizType) {
        ApprovalNodeBuilder builder = builderMap.get(bizType);
        if (builder == null) {
            throw new IllegalArgumentException("未找到业务类型对应的构建器: " + bizType.getCode());
        }
        return builder;
    }
}
