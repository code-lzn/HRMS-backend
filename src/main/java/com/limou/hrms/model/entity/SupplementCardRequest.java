package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("supplement_card_request")
@Data
public class SupplementCardRequest implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private LocalDate attendanceDate;
    /** 卡类型：1=上班卡 2=下班卡 */
    private Integer cardType;
    private String reason;
    /** 状态：1=草稿 2=审批中 3=已通过 4=已拒绝 */
    private Integer status;
    private Long approvalInstanceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
