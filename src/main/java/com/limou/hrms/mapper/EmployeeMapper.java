package com.limou.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.Employee;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 员工 Mapper（组织架构模块依赖的最小结构）
 */
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 统计指定部门及子部门中的在职员工数（在职 + 试用期）
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM employee WHERE isDeleted = 0 AND status IN (1, 2) "
            + "AND departmentId IN "
            + "<foreach collection='deptIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    long countActiveByDeptIds(@Param("deptIds") java.util.Collection<Long> deptIds);

    /**
     * 统计指定职位被引用的在职员工数
     */
    @Select("SELECT COUNT(*) FROM employee WHERE isDeleted = 0 AND status IN (1, 2) AND positionId = #{positionId}")
    long countActiveByPositionId(@Param("positionId") Long positionId);

    /**
     * 批量更新员工的部门（用于部门合并转移）
     */
    @Update("<script>"
            + "UPDATE employee SET departmentId = #{targetDeptId} "
            + "WHERE isDeleted = 0 AND departmentId IN "
            + "<foreach collection='deptIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    int batchUpdateDeptId(@Param("deptIds") java.util.Collection<Long> deptIds, @Param("targetDeptId") Long targetDeptId);
}
