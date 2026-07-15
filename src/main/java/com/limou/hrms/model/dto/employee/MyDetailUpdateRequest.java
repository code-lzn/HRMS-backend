package com.limou.hrms.model.dto.employee;

import lombok.Data;

import java.io.Serializable;

/**
 * 个人档案可编辑字段更新请求
 * <p>
 * 用于 /api/employee/profile/updateMyDetail 接口
 * 仅开放三个字段：现居住地址、紧急联系人姓名、紧急联系人电话
 */
@Data
public class MyDetailUpdateRequest implements Serializable {

    /** 员工ID（必须等于当前登录人ID，越权拦截） */
    private Long employeeId;

    /** 现居住地址 */
    private String currentAddress;

    /** 紧急联系人姓名 */
    private String emergencyContactName;

    /** 紧急联系人电话（11位手机号） */
    private String emergencyContactPhone;

    private static final long serialVersionUID = 1L;
}
