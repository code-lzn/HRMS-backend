package com.limou.hrms.model.dto.profile;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑个人档案请求（仅允许编辑白名单字段）
 */
@Data
public class ProfileUpdateDTO implements Serializable {

    /**
     * 邮箱
     */
    private String email;

    /**
     * 现居住地址
     */
    private String address;

    /**
     * 紧急联系人姓名
     */
    private String emergencyContact;

    /**
     * 紧急联系人电话
     */
    private String emergencyPhone;

    private static final long serialVersionUID = 1L;
}
