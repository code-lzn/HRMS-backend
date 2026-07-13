package com.limou.hrms.model.dto.attendance;

import java.io.Serializable;
import lombok.Data;

/**
 * 审批请求（请假/补卡通用）
 */
@Data
public class ApprovalRequest implements Serializable {

    /** 申请记录ID */
    private Long id;

    /** 审批结果：1=通过 2=拒绝 */
    private Integer result;

    /** 审批意见 */
    private String comment;

    private static final long serialVersionUID = 1L;
}
