package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.attendance.HRAttendanceDTO;
import com.limou.hrms.model.dto.attendance.HRAttendanceQueryRequest;
import com.limou.hrms.model.vo.HRAttendanceVO;
import com.limou.hrms.model.vo.PageResult;
import com.limou.hrms.service.HRAttendanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import javax.annotation.Resource;

@RestController
@RequestMapping("/hr/attendance")
@Slf4j
public class HRAttendanceController {

    @Resource
    private HRAttendanceService hrAttendanceService;

    @GetMapping("/list")
    public BaseResponse<PageResult<HRAttendanceVO>> queryAttendance(HRAttendanceQueryRequest request,
                                                                    HttpServletRequest httpRequest) {
        PageResult<HRAttendanceVO> result = hrAttendanceService.queryAttendance(request, httpRequest);
        return ResultUtils.success(result);
    }

    @GetMapping("/detail/{id}")
    public BaseResponse<HRAttendanceVO> getDetail(@PathVariable Long id) {
        HRAttendanceVO vo = hrAttendanceService.getDetail(id);
        return ResultUtils.success(vo);
    }

    @PostMapping("/create")
    public BaseResponse<HRAttendanceVO> createAttendance(@RequestBody HRAttendanceDTO dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        HRAttendanceVO vo = hrAttendanceService.createAttendance(dto);
        return ResultUtils.success(vo);
    }

    @PostMapping("/update")
    public BaseResponse<HRAttendanceVO> updateAttendance(@RequestBody HRAttendanceDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        HRAttendanceVO vo = hrAttendanceService.updateAttendance(dto);
        return ResultUtils.success(vo);
    }

    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteAttendance(@PathVariable Long id) {
        hrAttendanceService.deleteAttendance(id);
        return ResultUtils.success(true);
    }

    @DeleteMapping("/batch-delete")
    public BaseResponse<Boolean> batchDeleteAttendance(@RequestBody Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        hrAttendanceService.batchDeleteAttendance(ids);
        return ResultUtils.success(true);
    }
}