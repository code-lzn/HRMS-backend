package com.limou.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.entity.DeptEmployeeCount;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.vo.EmployeeListVO;
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

    /**
     * 分页查询员工列表（多表 JOIN）
     */
    Page<EmployeeListVO> selectEmployeePage(Page<?> page,
                                            @Param("keyword") String keyword,
                                            @Param("departmentIds") List<Long> departmentIds,
                                            @Param("positionIds") List<Long> positionIds,
                                            @Param("statuses") List<Integer> statuses,
                                            @Param("jobLevels") List<String> jobLevels,
                                            @Param("hireDateStart") String hireDateStart,
                                            @Param("hireDateEnd") String hireDateEnd,
                                            @Param("deptIds") List<Long> deptIds,
                                            @Param("selfEmployeeId") Long selfEmployeeId);
}