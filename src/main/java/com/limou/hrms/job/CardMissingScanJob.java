package com.limou.hrms.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.AttendanceRecordMapper;
import com.limou.hrms.model.entity.AttendanceRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * 缺卡扫描定时任务 — 每天凌晨 1 点执行，将前一天未打卡的记录标记为缺卡
 */
@Component
@Slf4j
public class CardMissingScanJob {

    @Resource
    private AttendanceRecordMapper attendanceRecordMapper;

    /**
     * 每天凌晨 1:00 执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scanCardMissing() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始扫描 {} 缺卡记录...", yesterday);

        // 查前一天所有 startStatus 或 endStatus 为空的记录
        List<AttendanceRecord> records = attendanceRecordMapper.selectList(
                new QueryWrapper<AttendanceRecord>()
                        .eq("attendance_date", yesterday)
                        .and(w -> w.isNull("start_status").or().isNull("end_status"))
        );

        if (records.isEmpty()) {
            log.info("{} 未发现缺卡记录", yesterday);
            return;
        }

        int count = 0;
        for (AttendanceRecord r : records) {
            boolean updated = false;
            if (r.getStartStatus() == null) {
                r.setStartStatus(4);
                updated = true;
            }
            if (r.getEndStatus() == null) {
                r.setEndStatus(4);
                updated = true;
            }
            if (updated) {
                attendanceRecordMapper.updateById(r);
                count++;
            }
        }
        log.info("缺卡扫描完成：{} 共 {} 条记录标记为缺卡", yesterday, count);
    }
}