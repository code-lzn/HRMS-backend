package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
@RequestMapping("/v1/positions")
@Slf4j
@Api(tags = "职位管理")
public class PositionController {

    @Resource
    private PositionService positionService;

    /**
     * 获取职位列表
     */
    @GetMapping
    @ApiOperation("获取职位列表")
    public BaseResponse<List<PositionVO>> listPositions(
            @ApiParam("职位序列") @RequestParam(required = false) Integer sequence,
            @ApiParam("部门ID") @RequestParam(required = false) Long departmentId) {
        PositionQueryRequest request = new PositionQueryRequest();
        request.setSequence(sequence);
        request.setDepartmentId(departmentId);
        List<PositionVO> list = positionService.listPositions(request);
        return ResultUtils.success(list);
    }

    /**
     * 新增职位
     */
    @PostMapping
    @ApiOperation("新增职位")
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
     * 更新职位
     */
    @PutMapping("/{id}")
    @ApiOperation("更新职位")
    public BaseResponse<Boolean> updatePosition(
            @ApiParam("职位ID") @PathVariable Long id,
            @RequestBody PositionUpdateRequest request) {
        if (request == null || id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        request.setId(id);
        positionService.updatePosition(request);
        return ResultUtils.success(true);
    }

    /**
     * 删除职位
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除职位（校验无员工引用）")
    public BaseResponse<Boolean> deletePosition(@ApiParam("职位ID") @PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        positionService.deletePosition(id);
        return ResultUtils.success(true);
    }

    /**
     * 获取序列职级对照
     */
    @GetMapping("/sequences")
    @ApiOperation("获取序列职级对照")
    public BaseResponse<List<SequenceLevelVO>> getSequences() {
        List<SequenceLevelVO> sequences = positionService.getSequences();
        return ResultUtils.success(sequences);
    }
}
