package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.ApprovalFlowMapper;
import com.limou.hrms.model.entity.ApprovalFlow;
import com.limou.hrms.service.ApprovalFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApprovalFlowServiceImpl extends ServiceImpl<ApprovalFlowMapper, ApprovalFlow>
        implements ApprovalFlowService {
}
