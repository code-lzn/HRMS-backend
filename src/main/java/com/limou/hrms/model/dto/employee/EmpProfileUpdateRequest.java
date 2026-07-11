package com.limou.hrms.model.dto.employee;

import java.io.Serializable;
import lombok.Data;

/**
 * 个人档案可编辑字段更新请求
 */
@Data
public class EmpProfileUpdateRequest implements Serializable {

    private String email;
    private String currentAddress;
    private String emergencyContactName;
    private String emergencyContactPhone;

    private static final long serialVersionUID = 1L;
}
