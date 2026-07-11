package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

/**
* @author henan
* @description 针对表【department(部门表)】的数据库操作Service实现
* @createDate 2026-07-11 14:06:52
*/
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
    implements DepartmentService{

}




