package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.department.DepartmentAddRequest;
import com.limou.hrms.model.dto.department.DepartmentMergeRequest;
import com.limou.hrms.model.dto.department.DepartmentUpdateRequest;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.vo.DepartmentMergeResultVO;
import com.limou.hrms.model.vo.DepartmentTreeVO;

import java.util.List;

/**
 * 部门服务
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 获取部门树（含递归人数统计）
     */
    List<DepartmentTreeVO> getDepartmentTree(Long userId);

    /**
     * 查询单个部门详情
     */
    DepartmentTreeVO getDepartmentDetail(Long id, Long userId);

    /**
     * 根据用户 ID 获取可见的部门 ID 列表
     * @return 可见部门 ID 列表；null 表示全部可见；空列表表示无权限
     */
    List<Long> getVisibleDeptIds(Long userId);

    /**
     * 新增部门
     */
    Long addDepartment(DepartmentAddRequest request);

    /**
     * 更新部门
     */
    void updateDepartment(DepartmentUpdateRequest request);

    /**
     * 删除部门（校验无子部门 + 无在职员工）
     */
    void deleteDepartment(Long id);

    /**
     * 合并部门
     */
    DepartmentMergeResultVO mergeDepartments(DepartmentMergeRequest request, Long operatorId);

    /**
     * 计算部门及子部门在职员工数
     */
    long getEmployeeCount(Long deptId);
}
