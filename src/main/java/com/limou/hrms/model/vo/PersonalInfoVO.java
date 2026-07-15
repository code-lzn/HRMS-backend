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

    private String name;
    private Integer gender;
    private String genderDesc;
    private String phone;
    private String email;
    private String idCard;
    private String birthday;
    private String registeredAddress;
    private String currentAddress;
    private String emergencyContactName;
    private String emergencyContactPhone;
}
