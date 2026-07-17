package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.attendance.AttendanceRecordQueryRequest;
import com.limou.hrms.model.dto.attendance.ClockRequest;
import com.limou.hrms.model.dto.attendance.SupplementCardSubmitDTO;
import com.limou.hrms.model.vo.AttendanceCalendarVO;
import com.limou.hrms.model.vo.AttendanceRecordVO;
import com.limou.hrms.model.vo.ClockResultVO;
import com.limou.hrms.model.vo.SupplementCardListVO;
import com.limou.hrms.model.vo.SupplementCardVO;

import java.time.LocalDate;

/**
 * 打卡服务
 */
public interface AttendanceService {

    /**
     * 打卡（上班/下班）
     */
    ClockResultVO clock(ClockRequest dto);

    /**
     * 查询打卡记录列表（分页 + 数据权限）
     */
    Page<AttendanceRecordVO> queryRecords(AttendanceRecordQueryRequest queryReq);

    /**
     * 考勤日历视图
     */
    AttendanceCalendarVO getCalendar(int year, int month, Long employeeId);

    /**
     * 提交补卡申请
     */
    SupplementCardVO submitSupplementCard(SupplementCardSubmitDTO dto);

    /**
     * 查询补卡申请列表（分页 + 数据权限）
     */
    Page<SupplementCardListVO> querySupplementCards(Long employeeId, Integer status,
                                                     LocalDate startDate, LocalDate endDate,
                                                     int page, int size);
}
