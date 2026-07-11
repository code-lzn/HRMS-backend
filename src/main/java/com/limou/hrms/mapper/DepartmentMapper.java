package com.limou.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.Department;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 部门 Mapper
 */
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 统计编码已用次数（含软删除记录，绕过@TableLogic）
     * 部门编码创建后不可回收复用
     */
    @Select("SELECT COUNT(*) FROM department WHERE deptCode = #{code}")
    long countByCodeIgnoreDelete(@Param("code") String code);
}
