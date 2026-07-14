package com.limou.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.limou.hrms.model.entity.Role;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT * FROM role WHERE status = 1 AND isDelete = 0 ORDER BY id")
    List<Role> selectAllEnabled();

    @Select("SELECT * FROM role WHERE roleCode = #{roleCode} AND isDelete = 0")
    Role selectByCode(String roleCode);

    @Select("SELECT * FROM role WHERE isDelete = 0 ORDER BY id")
    List<Role> selectAll();
}