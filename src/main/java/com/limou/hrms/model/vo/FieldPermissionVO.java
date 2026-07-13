package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 字段权限 VO
 */
@Data
public class FieldPermissionVO implements Serializable {

    /** 可查看字段 */
    private List<String> viewableFields;

    /** 可编辑字段 */
    private List<String> editableFields;

    /** 锁定字段（置灰） */
    private List<String> lockedFields;

    private static final long serialVersionUID = 1L;
}
