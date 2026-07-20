package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class OvertimeProgressVO implements Serializable {

    private OvertimeVO overtime;

    private List<ProgressNode> progressNodes;

    @Data
    public static class ProgressNode implements Serializable {
        private String nodeName;
        /** 0=已完成, 1=进行中, 2=未开始 */
        private Integer status;
        private String operatorName;
        private Date operateTime;
        private String comment;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
