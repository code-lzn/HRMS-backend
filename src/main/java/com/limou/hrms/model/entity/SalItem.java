package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工资项目表
 * @TableName sal_item
 */
@TableName(value = "sal_item")
@Data
public class SalItem implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属账套ID */
    @TableField("account_id")
    private Long accountId;

    /** 项目名称，如基本工资、绩效奖金 */
    private String name;

    /** 项目类型：1=固定收入 2=变动收入 3=考勤扣款 4=社保扣除 5=公积金扣除 6=个税 */
    @TableField("item_type")
    private Integer itemType;

    /** 计算公式/规则描述 */
    private String formula;

    /** 排序序号 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 是否计入个税：0=否 1=是 */
    @TableField("is_taxable")
    private Integer isTaxable;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

    /** 更新时间 */
    @TableField("update_time")
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
