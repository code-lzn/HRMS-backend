package com.limou.hrms.mapper;

import com.limou.hrms.model.entity.Attendance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @description 针对表【attendance_record(考勤打卡记录表)】的数据库操作Mapper
* @Entity com.limou.hrms.model.entity.Attendance
*/
public interface AttendanceMapper extends BaseMapper<Attendance> {

    List<Map<String, Object>> getDepartmentAttendanceStats(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

}
