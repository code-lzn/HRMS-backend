package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 请假审批进度视图
 */
@Data
public class LeaveProgressVO implements Serializable {

    /** 请假记录 */
    private LeaveVO leave;

    /** 审批进度节点列表 */
    private List<ProgressNode> progressNodes;

    @Data
    public static class ProgressNode implements Serializable {
        /** 节点名称 */
        private String nodeName;
        /** 节点状态: 0=已完成, 1=进行中, 2=未开始 */
        private Integer status;
        /** 操作人姓名 */
        private String operatorName;
        /** 操作时间 */
        private Date operateTime;
        /** 审批意见 */
        private String comment;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
