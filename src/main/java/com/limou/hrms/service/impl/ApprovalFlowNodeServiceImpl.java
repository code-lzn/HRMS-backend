package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.ApprovalFlowNodeMapper;
import com.limou.hrms.model.entity.ApprovalFlowNode;
import com.limou.hrms.service.ApprovalFlowNodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApprovalFlowNodeServiceImpl extends ServiceImpl<ApprovalFlowNodeMapper, ApprovalFlowNode>
        implements ApprovalFlowNodeService {
}
