package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 批量设置工作日历请求
 */
@Data
public class WorkCalendarBatchRequest implements Serializable {

    @NotEmpty(message = "日期列表不能为空")
    @Valid
    private List<DayItem> days;

    @Data
    public static class DayItem implements Serializable {

        @NotNull(message = "日期不能为空")
        private LocalDate date;

        /**
         * 1=工作日 2=休息日 3=节假日
         */
        @NotNull(message = "日期类型不能为空")
        private Integer dayType;

        /**
         * 节假日名称（dayType=3 时可填）
         */
        private String holidayName;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
