package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.SalaryBatchMapper;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.service.SalaryBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SalaryBatchServiceImpl extends ServiceImpl<SalaryBatchMapper, SalaryBatch>
        implements SalaryBatchService {
}
