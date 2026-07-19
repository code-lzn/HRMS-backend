package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("approval_delegate")
@Data
public class ApprovalDelegate implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long delegatorId;
    private Long delegateId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 是否启用：0=已取消 1=生效中 */
    private Integer enabled;
    /** 原因（冗余字段用于查询，实际存业务表） */
    @TableField(exist = false)
    private String reason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 以下为展示字段，非数据库列 */
    @TableField(exist = false)
    private String delegateName;
    @TableField(exist = false)
    private String delegatePosition;
    @TableField(exist = false)
    private String delegatorName;

    private static final long serialVersionUID = 1L;
}
