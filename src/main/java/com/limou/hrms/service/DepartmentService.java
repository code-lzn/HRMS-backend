package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.department.DepartmentCreateRequest;
import com.limou.hrms.model.dto.department.DepartmentQueryRequest;
import com.limou.hrms.model.dto.department.DepartmentUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.DepartmentTreeNode;
import com.limou.hrms.model.vo.DepartmentVO;

import java.util.List;

/**
 * 部门服务
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 创建部门
     */
    Department createDepartment(DepartmentCreateRequest dto, User loginUser);

    /**
     * 更新部门
     */
    Department updateDepartment(Long id, DepartmentUpdateRequest dto, User loginUser);

    /**
     * 删除部门（逻辑删除）
     */
    void deleteDepartment(Long id, User loginUser);

    /**
     * 查询部门树（含人数统计 + 数据权限）
     */
    List<DepartmentTreeNode> getDepartmentTree(User loginUser);

    /**
     * 查询部门列表（平铺/分页）
     */
    Page<DepartmentVO> getDepartmentList(DepartmentQueryRequest query, User loginUser);

    /**
     * 查询部门详情
     */
    DepartmentVO getDepartmentDetail(Long id, User loginUser);
}
