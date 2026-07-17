package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest;
import com.limou.hrms.model.dto.attendance.AttendanceGroupUpdateRequest;
import com.limou.hrms.model.entity.AttendanceGroup;
import com.limou.hrms.model.vo.AttendanceGroupListVO;
import com.limou.hrms.model.vo.AttendanceGroupVO;

/**
 * 考勤组服务
 */
public interface AttendanceGroupService extends IService<AttendanceGroup> {

    /**
     * 创建考勤组
     */
    AttendanceGroupVO createAttendanceGroup(AttendanceGroupCreateRequest dto);

    /**
     * 更新考勤组（部分更新，rules 传则全量替换）
     */
    AttendanceGroupVO updateAttendanceGroup(Long id, AttendanceGroupUpdateRequest dto);

    /**
     * 查询考勤组列表（分页 + 数据权限）
     */
    Page<AttendanceGroupListVO> queryAttendanceGroups(String keyword, Integer shiftType, int page, int size);

    /**
     * 删除考勤组（逻辑删除）
     */
    void deleteAttendanceGroup(Long id);
}
