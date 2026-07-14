package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.EmployeeChangeLogMapper;
import com.limou.hrms.model.entity.EmployeeChangeLog;
import com.limou.hrms.service.EmployeeChangeLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 员工变更日志服务实现
 */
@Slf4j
@Service
public class EmployeeChangeLogServiceImpl extends ServiceImpl<EmployeeChangeLogMapper, EmployeeChangeLog>
        implements EmployeeChangeLogService {

}
