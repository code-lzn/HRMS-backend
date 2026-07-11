package generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.model.entity.Employee;
import generator.service.EmployeeService;
import generator.mapper.EmployeeMapper;
import org.springframework.stereotype.Service;

/**
* @author henan
* @description 针对表【employee(员工)】的数据库操作Service实现
* @createDate 2026-07-11 11:09:42
*/
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
    implements EmployeeService{

}




