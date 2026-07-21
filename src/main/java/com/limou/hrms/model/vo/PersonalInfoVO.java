package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工个人信息 VO（脱敏后）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInfoVO {

    /** 姓名 */
    private String name;

    /** 性别：1=男 2=女 */
    private Integer gender;

    /** 性别描述 */
    private String genderDesc;

    /** 手机号（按角色脱敏，普通员工仅看前3后4） */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 身份证号（按角色脱敏，普通员工仅看前4后2） */
    private String idCard;

    /** 生日 */
    private String birthday;

    /** 户籍地址 */
    private String registeredAddress;

    /** 现居住地址 */
    private String currentAddress;

    /** 紧急联系人姓名（仅部分角色可见） */
    private String emergencyContactName;

    /** 紧急联系人电话（仅部分角色可见，按角色脱敏） */
    private String emergencyContactPhone;
}