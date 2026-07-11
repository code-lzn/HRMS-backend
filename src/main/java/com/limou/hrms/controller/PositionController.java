package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.DeleteRequest;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.position.PositionAddRequest;
import com.limou.hrms.model.dto.position.PositionQueryRequest;
import com.limou.hrms.model.dto.position.PositionUpdateRequest;
import com.limou.hrms.model.vo.PositionVO;
import com.limou.hrms.model.vo.SequenceLevelVO;
import com.limou.hrms.service.PositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 职位管理接口
 */
@RestController
@RequestMapping("/positions")
@Slf4j
public class PositionController {

    @Resource
    private PositionService positionService;

    /**
     * 获取职位列表
     */
    @GetMapping("/list")
    public BaseResponse<List<PositionVO>> listPositions(
            @RequestParam(required = false) Integer sequence,
            @RequestParam(required = false) Long departmentId) {
        PositionQueryRequest request = new PositionQueryRequest();
        request.setSequence(sequence);
        request.setDepartmentId(departmentId);
        List<PositionVO> list = positionService.listPositions(request);
        return ResultUtils.success(list);
    }

    /**
     * 新增职位
     */
    @PostMapping("/add")
    public BaseResponse<Map<String, Long>> addPosition(@RequestBody PositionAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = positionService.addPosition(request);
        Map<String, Long> data = new HashMap<>();
        data.put("id", id);
        return ResultUtils.success(data);
    }

    /**
     * 更新职位（id 在请求体中）
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updatePosition(
            @RequestBody PositionUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        positionService.updatePosition(request);
        return ResultUtils.success(true);
    }

    /**
     * 删除职位（校验无员工引用）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePosition(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        positionService.deletePosition(request.getId());
        return ResultUtils.success(true);
    }

    /**
     * 获取序列职级对照
     */
    @GetMapping("/sequences")
    public BaseResponse<List<SequenceLevelVO>> getSequences() {
        List<SequenceLevelVO> sequences = positionService.getSequences();
        return ResultUtils.success(sequences);
    }
}
