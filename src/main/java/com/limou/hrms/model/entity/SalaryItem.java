package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 工资项目
 */
@TableName(value = "salary_item")
@Data
public class SalaryItem implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属账套 ID → salary_account.id
     */
    private Long accountId;

    /**
     * 项目名称，如"基本工资""绩效奖金"
     */
    private String name;

    /**
     * 项目类型：1=固定收入, 2=变动收入, 3=考勤扣款, 4=社保扣除, 5=公积金扣除, 6=个税
     */
    private Integer itemType;

    /**
     * 计算公式，如"base*0.08"
     */
    private String formula;

    /**
     * 排序号（工资条展示顺序）
     */
    private Integer sortOrder;

    /**
     * 是否计入应纳税所得额：1=是, 0=否
     */
    private Integer isTaxable;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
