package com.limou.hrms.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.ApprovalNodeMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.enums.NodeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批超时扫描定时任务 — 每小时执行，扫描超过48小时未审批的节点
 */
@Component
@Slf4j
public class ApprovalTimeoutJob {

    @Resource
    private ApprovalNodeMapper approvalNodeMapper;

    /** 超时阈值：48小时 */
    private static final int TIMEOUT_HOURS = 48;

    /**
     * 每小时整点执行，扫描超时节点
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scanTimeoutNodes() {
        log.info("开始扫描审批超时节点...");
        LocalDateTime deadline = LocalDateTime.now().minusHours(TIMEOUT_HOURS);

        // 查询所有待审批且创建时间早于48小时前的节点
        List<ApprovalNode> timeoutNodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .eq("status", NodeStatus.PENDING.getCode())
                        .lt("create_time", deadline)
        );

        if (timeoutNodes.isEmpty()) {
            log.info("未发现超时节点");
            return;
        }

        int count = 0;
        for (ApprovalNode node : timeoutNodes) {
            node.setStatus(NodeStatus.TIMEOUT.getCode());
            node.setOperateTime(LocalDateTime.now());
            approvalNodeMapper.updateById(node);
            count++;
            log.info("审批超时: nodeId={}, instanceId={}, approverId={}, createTime={}",
                    node.getId(), node.getInstanceId(), node.getApproverId(), node.getCreateTime());
        }
        log.info("审批超时扫描完成，共处理 {} 个超时节点", count);
    }
}
