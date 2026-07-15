package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 页面访问日志
 * @TableName page_view_log
 */
@TableName(value = "page_view_log")
@Data
public class PageViewLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 访问日期 */
    private Date viewDate;

    /** 页面访问量 */
    private Long viewCount;

    private static final long serialVersionUID = 1L;
}
