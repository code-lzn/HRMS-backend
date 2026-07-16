package com.limou.hrms.service;

import com.limou.hrms.model.dto.attendance.HRAttendanceDTO;
import com.limou.hrms.model.dto.attendance.HRAttendanceQueryRequest;
import com.limou.hrms.model.vo.HRAttendanceVO;
import com.limou.hrms.model.vo.PageResult;

import javax.servlet.http.HttpServletRequest;

public interface HRAttendanceService {

    PageResult<HRAttendanceVO> queryAttendance(HRAttendanceQueryRequest request, HttpServletRequest httpRequest);

    HRAttendanceVO getDetail(Long id);

    HRAttendanceVO createAttendance(HRAttendanceDTO dto);

    HRAttendanceVO updateAttendance(HRAttendanceDTO dto);

    void deleteAttendance(Long id);

    void batchDeleteAttendance(Long[] ids);
}