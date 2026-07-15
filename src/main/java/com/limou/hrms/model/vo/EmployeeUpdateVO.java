package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新员工档案响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUpdateVO {

    /**
     * 已成功更新的字段名列表
     */
    private List<String> updatedFields;

    /**
     * 需走审批流程的字段名列表（无直接编辑权限）
     */
    private List<String> flowRequiredFields;
}
