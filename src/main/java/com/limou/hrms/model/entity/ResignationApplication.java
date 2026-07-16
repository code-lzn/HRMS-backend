package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("resignation_application")
@Data
public class ResignationApplication implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private LocalDate resignationDate;
    /** 离职类型：1=辞职 2=辞退 3=合同到期 4=其他 */
    private Integer resignationType;
    private String reason;
    private Long handoverToId;
    /** 状态：1=草稿 2=审批中 3=审批通过待离职 4=已离职 5=已拒绝 */
    private Integer status;
    private Long approvalInstanceId;
    private LocalDate actualResignationDate;
    private Long applicantId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
