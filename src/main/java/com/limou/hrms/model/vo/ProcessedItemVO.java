package com.limou.hrms.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 已办列表项 VO
 */
@Data
public class ProcessedItemVO {
    private Long instanceId;
    private Long nodeId;
    private String bizType;
    private String bizTypeDesc;
    private String title;
    private String applicantName;
    private String nodeName;
    private Integer nodeStatus;
    private String nodeStatusDesc;
    private String comment;
    private LocalDateTime operateTime;
}
