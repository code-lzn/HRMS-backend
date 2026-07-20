package com.limou.hrms.service;

import com.limou.hrms.model.dto.attendance.WorkCalendarBatchRequest;
import com.limou.hrms.model.vo.WorkCalendarVO;

/**
 * 工作日历服务
 */
public interface WorkCalendarService {

    /**
     * 查询某月日历
     */
    WorkCalendarVO getCalendar(int year, int month);

    /**
     * 批量设置日期属性
     */
    void batchUpdate(WorkCalendarBatchRequest request);

    /**
     * 生成整年标准日历（周一至五工作日，周六日休息日）
     */
    void generateYear(int year);

    /**
     * 从外部 API 同步法定节假日 + 调休
     */
    int syncFromExternal(int year);
}
