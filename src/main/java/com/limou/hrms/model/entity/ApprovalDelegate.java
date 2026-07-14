package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
