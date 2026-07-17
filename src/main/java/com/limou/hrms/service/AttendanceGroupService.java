package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.attendance.AttendanceGroupDTO;
import com.limou.hrms.model.entity.AttendanceGroup;
import com.limou.hrms.model.vo.AttendanceGroupVO;

import java.util.List;

public interface AttendanceGroupService extends IService<AttendanceGroup> {

    AttendanceGroupVO createGroup(AttendanceGroupDTO dto);

    AttendanceGroupVO updateGroup(AttendanceGroupDTO dto);

    void deleteGroup(Long id);

    AttendanceGroupVO getGroupDetail(Long id);

    List<AttendanceGroupVO> getAllGroups();

    AttendanceGroup getGroupByEmployeeId(Long employeeId);

    void assignEmployees(Long groupId, List<Long> employeeIds);

    void removeEmployees(Long groupId, List<Long> employeeIds);
}
