package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调岗历史表 — 追溯员工岗位变动轨迹
 */
@TableName("transfer_history")
@Data
public class TransferHistory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long employeeId;
    private Long transferApplicationId;
    private Long fromDepartmentId;
    private Long toDepartmentId;
    private Long fromPositionId;
    private Long toPositionId;
    private String fromJobLevel;
    private String toJobLevel;
    private LocalDate transferDate;
    private String reason;
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
