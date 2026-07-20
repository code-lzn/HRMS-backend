package com.limou.hrms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 个人中心可编辑字段白名单配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "profile.editable")
public class ProfileConfig {

    /**
     * 员工可编辑字段白名单（API 对外字段名）
     */
    private List<String> employeeFields = Arrays.asList(
            "email", "address", "emergencyContact", "emergencyPhone"
    );

    /**
     * 内部 DB 字段名 → API 字段名映射
     */
    public static final Map<String, String> API_TO_DB_FIELD = new HashMap<>();

    static {
        API_TO_DB_FIELD.put("email", "email");
        API_TO_DB_FIELD.put("address", "currentAddress");
        API_TO_DB_FIELD.put("emergencyContact", "emergencyContactName");
        API_TO_DB_FIELD.put("emergencyPhone", "emergencyContactPhone");
    }

    /**
     * 锁定字段（展示但不可编辑）
     */
    private List<String> lockedFields = Arrays.asList(
            "name", "phone", "idCard", "departmentName", "positionName", "jobLevel"
    );
}
