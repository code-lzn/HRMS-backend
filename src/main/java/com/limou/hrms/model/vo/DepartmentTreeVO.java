package com.limou.hrms.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点VO
 */
@Data
@ApiModel("部门树节点")
public class DepartmentTreeVO implements Serializable {

    @ApiModelProperty("部门ID")
    private Long id;

    @ApiModelProperty("部门名称")
    private String name;

    @ApiModelProperty("部门编码")
    private String code;

    @ApiModelProperty("上级部门ID")
    private Long parentId;

    @ApiModelProperty("部门负责人ID")
    private Long managerId;

    @ApiModelProperty("部门负责人姓名")
    private String managerName;

    @ApiModelProperty("排序序号")
    private Integer sortOrder;

    @ApiModelProperty("部门描述")
    private String description;

    @ApiModelProperty("部门人数（含子部门递归汇总）")
    private Integer employeeCount;

    @ApiModelProperty("子部门列表")
    private List<DepartmentTreeVO> children = new ArrayList<>();

    private static final long serialVersionUID = 1L;
}
