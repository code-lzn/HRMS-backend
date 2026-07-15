package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 字段权限响应
 * <p>
 * 按当前用户角色返回三个维度的字段列表，前端据此控制页面展示与交互。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldPermissionsVO {

    /**
     * 当前角色能看到哪些字段（详情页不在列表中的字段不展示）
     */
    private List<String> viewableFields;

    /**
     * 当前角色能直接修改哪些字段（编辑页仅在列表中的字段渲染输入框）
     */
    private List<String> editableFields;

    /**
     * 改了要走审批流程的字段（不在 editableFields 中但用户想改时，引导发起对应审批）
     */
    private List<String> flowRequiredFields;
}
