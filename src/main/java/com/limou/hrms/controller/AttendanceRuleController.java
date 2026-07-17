package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.dto.attendance.AttendanceGroupDTO;
import com.limou.hrms.model.entity.HolidayConfig;
import com.limou.hrms.model.vo.AttendanceGroupVO;
import com.limou.hrms.model.vo.HolidayConfigVO;
import com.limou.hrms.service.AttendanceGroupService;
import com.limou.hrms.service.HolidayConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/attendance/rule")
@Slf4j
public class AttendanceRuleController {

    @Resource
    private AttendanceGroupService attendanceGroupService;

    @Resource
    private HolidayConfigService holidayConfigService;

    @GetMapping("/groups")
    public BaseResponse<List<AttendanceGroupVO>> getAllGroups() {
        List<AttendanceGroupVO> list = attendanceGroupService.getAllGroups();
        return ResultUtils.success(list);
    }

    @GetMapping("/groups/{id}")
    public BaseResponse<AttendanceGroupVO> getGroupDetail(@PathVariable Long id) {
        AttendanceGroupVO vo = attendanceGroupService.getGroupDetail(id);
        return ResultUtils.success(vo);
    }

    @PostMapping("/groups")
    public BaseResponse<AttendanceGroupVO> createGroup(@RequestBody AttendanceGroupDTO dto) {
        AttendanceGroupVO vo = attendanceGroupService.createGroup(dto);
        return ResultUtils.success(vo);
    }

    @PutMapping("/groups")
    public BaseResponse<AttendanceGroupVO> updateGroup(@RequestBody AttendanceGroupDTO dto) {
        AttendanceGroupVO vo = attendanceGroupService.updateGroup(dto);
        return ResultUtils.success(vo);
    }

    @DeleteMapping("/groups/{id}")
    public BaseResponse<Boolean> deleteGroup(@PathVariable Long id) {
        attendanceGroupService.deleteGroup(id);
        return ResultUtils.success(true);
    }

    @PostMapping("/groups/{groupId}/employees")
    public BaseResponse<Boolean> assignEmployees(@PathVariable Long groupId,
                                                 @RequestBody List<Long> employeeIds) {
        attendanceGroupService.assignEmployees(groupId, employeeIds);
        return ResultUtils.success(true);
    }

    @DeleteMapping("/groups/{groupId}/employees")
    public BaseResponse<Boolean> removeEmployees(@PathVariable Long groupId,
                                                 @RequestBody List<Long> employeeIds) {
        attendanceGroupService.removeEmployees(groupId, employeeIds);
        return ResultUtils.success(true);
    }

    @GetMapping("/holidays")
    public BaseResponse<List<HolidayConfigVO>> getAllHolidays() {
        List<HolidayConfigVO> list = holidayConfigService.getAllHolidays();
        return ResultUtils.success(list);
    }

    @GetMapping("/holidays/year/{year}")
    public BaseResponse<List<HolidayConfigVO>> getHolidaysByYear(@PathVariable Integer year) {
        List<HolidayConfigVO> list = holidayConfigService.getHolidaysByYear(year);
        return ResultUtils.success(list);
    }

    @GetMapping("/holidays/{id}")
    public BaseResponse<HolidayConfigVO> getHolidayDetail(@PathVariable Long id) {
        HolidayConfigVO vo = holidayConfigService.getHolidayDetail(id);
        return ResultUtils.success(vo);
    }

    @PostMapping("/holidays")
    public BaseResponse<HolidayConfigVO> createHoliday(@RequestBody HolidayConfig config) {
        HolidayConfigVO vo = holidayConfigService.createHoliday(config);
        return ResultUtils.success(vo);
    }

    @PutMapping("/holidays")
    public BaseResponse<HolidayConfigVO> updateHoliday(@RequestBody HolidayConfig config) {
        HolidayConfigVO vo = holidayConfigService.updateHoliday(config);
        return ResultUtils.success(vo);
    }

    @DeleteMapping("/holidays/{id}")
    public BaseResponse<Boolean> deleteHoliday(@PathVariable Long id) {
        holidayConfigService.deleteHoliday(id);
        return ResultUtils.success(true);
    }
}
