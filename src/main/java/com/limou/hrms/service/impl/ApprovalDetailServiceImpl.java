package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.ApprovalDetailMapper;
import com.limou.hrms.model.entity.ApprovalDetail;
import com.limou.hrms.service.ApprovalDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApprovalDetailServiceImpl extends ServiceImpl<ApprovalDetailMapper, ApprovalDetail>
        implements ApprovalDetailService {
}
