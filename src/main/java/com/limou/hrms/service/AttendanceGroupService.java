package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest;
import com.limou.hrms.model.entity.AttendanceGroup;
import com.limou.hrms.model.vo.AttendanceGroupVO;

/**
 * 考勤组服务
 */
public interface AttendanceGroupService extends IService<AttendanceGroup> {

    /**
     * 创建考勤组
     */
    AttendanceGroupVO createAttendanceGroup(AttendanceGroupCreateRequest dto);
}
