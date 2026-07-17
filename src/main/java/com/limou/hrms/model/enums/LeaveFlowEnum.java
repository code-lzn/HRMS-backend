package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 请假审批流枚举
 */
@Getter
public enum LeaveFlowEnum {

    SIMPLE("请假-单节点", "直接上级"),
    DEPT("请假-双节点-部门", "直接上级→部门负责人"),
    HR("请假-HR备案", "直接上级→HR备案");

    private final String flowName;
    private final String description;

    LeaveFlowEnum(String flowName, String description) {
        this.flowName = flowName;
        this.description = description;
    }

}
