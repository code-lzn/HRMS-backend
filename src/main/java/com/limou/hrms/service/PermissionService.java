package com.limou.hrms.service;

import com.limou.hrms.model.vo.UserPermissionVO;
import java.util.List;

public interface PermissionService {

    UserPermissionVO getUserPermissions(Long userId);

    boolean hasPermission(Long userId, String permissionCode);

    Integer getUserDataScope(Long userId);

    List<String> getUserPermissionCodes(Long userId);

    String getFieldPermissions(Long userId);

    /**
     * 获取系统所有可用权限编码列表
     */
    List<String> getAllPermissionCodes();
}