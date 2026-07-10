package com.limou.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.Employee;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 员工 Mapper（组织架构模块依赖的最小结构）
 */
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 统计指定部门及子部门中的在职员工数（试用期 + 正式）
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM employee WHERE is_deleted = 0 AND status IN (1, 2) "
            + "AND department_id IN "
            + "<foreach collection='deptIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    long countActiveByDeptIds(@Param("deptIds") java.util.Collection<Long> deptIds);

    /**
     * 统计指定职位被引用的在职员工数
     */
    @Select("SELECT COUNT(*) FROM employee WHERE is_deleted = 0 AND status IN (1, 2) AND position_id = #{positionId}")
    long countActiveByPositionId(@Param("positionId") Long positionId);

    /**
     * 批量更新员工的部门（用于部门合并转移）
     */
    @org.apache.ibatis.annotations.Update("<script>"
            + "UPDATE employee SET department_id = #{targetDeptId} "
            + "WHERE is_deleted = 0 AND department_id IN "
            + "<foreach collection='deptIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    int batchUpdateDeptId(@Param("deptIds") java.util.Collection<Long> deptIds, @Param("targetDeptId") Long targetDeptId);
}
