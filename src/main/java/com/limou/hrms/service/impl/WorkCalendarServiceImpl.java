package com.limou.hrms.service.impl;

import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.WorkCalendarMapper;
import com.limou.hrms.model.dto.attendance.WorkCalendarBatchRequest;
import com.limou.hrms.model.entity.WorkCalendar;
import com.limou.hrms.model.vo.WorkCalendarVO;
import com.limou.hrms.service.WorkCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作日历服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkCalendarServiceImpl implements WorkCalendarService {

    private final WorkCalendarMapper workCalendarMapper;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String HOLIDAY_API = "https://uapis.cn/api/v1/misc/holiday-calendar";

    @Override
    public WorkCalendarVO getCalendar(int year, int month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        List<WorkCalendar> list = workCalendarMapper.selectList(
                Wrappers.<WorkCalendar>lambdaQuery()
                        .between(WorkCalendar::getCalendarDate, firstDay, lastDay)
                        .orderByAsc(WorkCalendar::getCalendarDate));

        Map<LocalDate, WorkCalendar> map = list.stream()
                .collect(Collectors.toMap(WorkCalendar::getCalendarDate, c -> c, (a, b) -> a));

        List<WorkCalendarVO.DayItem> days = new ArrayList<>();
        for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
            WorkCalendar cal = map.get(d);
            WorkCalendarVO.DayItem item = WorkCalendarVO.DayItem.builder()
                    .date(d)
                    .dayType(cal != null ? cal.getDayType() : defaultDayType(d))
                    .dayTypeDesc(cal != null ? getDayTypeDesc(cal.getDayType()) : getDayTypeDesc(defaultDayType(d)))
                    .holidayName(cal != null ? cal.getHolidayName() : null)
                    .build();
            days.add(item);
        }

        return WorkCalendarVO.builder().year(year).month(month).days(days).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(WorkCalendarBatchRequest request) {
        for (WorkCalendarBatchRequest.DayItem item : request.getDays()) {
            WorkCalendar exist = workCalendarMapper.selectOne(
                    Wrappers.<WorkCalendar>lambdaQuery()
                            .eq(WorkCalendar::getCalendarDate, item.getDate()));
            if (exist != null) {
                exist.setDayType(item.getDayType());
                exist.setHolidayName(item.getHolidayName());
                workCalendarMapper.updateById(exist);
            } else {
                WorkCalendar cal = new WorkCalendar();
                cal.setCalendarDate(item.getDate());
                cal.setDayType(item.getDayType());
                cal.setHolidayName(item.getHolidayName());
                workCalendarMapper.insert(cal);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateYear(int year) {
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);

        // 查已存在的日期，不覆盖
        List<WorkCalendar> existing = workCalendarMapper.selectList(
                Wrappers.<WorkCalendar>lambdaQuery()
                        .between(WorkCalendar::getCalendarDate, firstDay, lastDay));
        Set<LocalDate> existingDates = existing.stream()
                .map(WorkCalendar::getCalendarDate).collect(Collectors.toSet());

        List<WorkCalendar> batch = new ArrayList<>();
        for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
            if (existingDates.contains(d)) continue;
            WorkCalendar cal = new WorkCalendar();
            cal.setCalendarDate(d);
            DayOfWeek dow = d.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                cal.setDayType(2); // 休息日
            } else {
                cal.setDayType(1); // 工作日
            }
            batch.add(cal);
        }

        if (!batch.isEmpty()) {
            for (WorkCalendar cal : batch) {
                workCalendarMapper.insert(cal);
            }
        }
        log.info("生成 {} 年标准日历完成，新增 {} 天", year, batch.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromExternal(int year) {
        String url = HOLIDAY_API + "?year=" + year + "&holiday_type=legal";
        String respBody;
        try {
            respBody = HttpUtil.get(url, 10000);
        } catch (Exception e) {
            log.error("调用节假日 API 失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "外部节假日服务不可用，请稍后重试");
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(respBody);
        } catch (Exception e) {
            log.error("解析节假日 API 响应失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "节假日数据解析失败，请稍后重试");
        }

        JsonNode daysNode = root.get("days");
        if (daysNode == null || !daysNode.isArray()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "节假日数据格式异常，请稍后重试");
        }

        int count = 0;
        List<WorkCalendar> batch = new ArrayList<>();

        for (JsonNode day : daysNode) {
            LocalDate date = LocalDate.parse(day.get("date").asText());
            boolean isWeekend = day.get("is_weekend").asBoolean();
            boolean isWorkday = day.get("is_workday").asBoolean();
            String legalType = day.has("legal_holiday_type") && !day.get("legal_holiday_type").isNull()
                    ? day.get("legal_holiday_type").asText() : null;
            String holidayName = day.has("legal_holiday_name") && !day.get("legal_holiday_name").isNull()
                    ? day.get("legal_holiday_name").asText() : null;

            Integer dayType = null;
            if ("rest".equals(legalType)) {
                dayType = 3; // 节假日
            } else if (isWeekend && isWorkday) {
                dayType = 1; // 调休上班
            }

            if (dayType == null) continue;

            // 查已有记录，不存在则新建
            WorkCalendar cal = workCalendarMapper.selectOne(
                    Wrappers.<WorkCalendar>lambdaQuery()
                            .eq(WorkCalendar::getCalendarDate, date));
            if (cal == null) {
                cal = new WorkCalendar();
                cal.setCalendarDate(date);
            }
            cal.setDayType(dayType);
            cal.setHolidayName(dayType == 3 ? holidayName : null);

            if (cal.getId() != null) {
                workCalendarMapper.updateById(cal);
            } else {
                workCalendarMapper.insert(cal);
            }
            count++;
        }

        log.info("同步 {} 年节假日完成，更新 {} 天", year, count);
        return count;
    }

    // ==================== 工具 ====================

    private int defaultDayType(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) ? 2 : 1;
    }

    private String getDayTypeDesc(Integer dayType) {
        if (dayType == null) return "";
        switch (dayType) {
            case 1: return "工作日";
            case 2: return "休息日";
            case 3: return "节假日";
            default: return "未知";
        }
    }
}
