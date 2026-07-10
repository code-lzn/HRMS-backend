package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.DepartmentMergeLogMapper;
import com.limou.hrms.model.entity.DepartmentMergeLog;
import com.limou.hrms.service.DepartmentMergeLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 部门合并日志服务实现
 */
@Slf4j
@Service
public class DepartmentMergeLogServiceImpl extends ServiceImpl<DepartmentMergeLogMapper, DepartmentMergeLog>
        implements DepartmentMergeLogService {

}
