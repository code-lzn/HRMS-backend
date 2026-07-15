package com.limou.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.DeptEmployeeCount;
import com.limou.hrms.model.entity.Employee;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工 Mapper
 */
public interface EmployeeMapper extends BaseMapper<Employee> {

    List<DeptEmployeeCount> countActiveByDept(@Param("activeStatuses") List<Integer> activeStatuses);

    @Select("SELECT * FROM employee WHERE user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    Employee selectByUserId(@Param("userId") Long userId);
}