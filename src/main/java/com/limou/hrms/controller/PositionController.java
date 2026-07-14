package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.position.PositionCreateRequest;
import com.limou.hrms.model.dto.position.PositionQueryRequest;
import com.limou.hrms.model.dto.position.PositionUpdateRequest;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.vo.PositionVO;
import com.limou.hrms.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 职位管理控制器
 */
@RestController
@RequestMapping("/api/v1/positions")
@Slf4j
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    /**
     * 查询职位序列枚举
     */
    @GetMapping("/sequences")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<List<Map<String, Object>>> getSequences() {
        List<Map<String, Object>> sequences = positionService.getSequences();
        return ResultUtils.success(sequences);
    }

    /**
     * 查询职位列表
     */
    @GetMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Page<PositionVO>> getPositionList(PositionQueryRequest queryReq) {
        Page<PositionVO> page = positionService.getPositionList(queryReq);
        return ResultUtils.success(page);
    }

    /**
     * 查询职位详情
     */
    @GetMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<PositionVO> getPositionDetail(@PathVariable Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PositionVO vo = positionService.getPositionDetail(id);
        return ResultUtils.success(vo);
    }

    /**
     * 创建职位
     */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Position> createPosition(@Valid @RequestBody PositionCreateRequest dto) {
        Position position = positionService.createPosition(dto);
        return ResultUtils.success(position);
    }

    /**
     * 更新职位
     */
    @PutMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Position> updatePosition(@PathVariable Long id,
                                                  @RequestBody PositionUpdateRequest dto) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Position position = positionService.updatePosition(id, dto);
        return ResultUtils.success(position);
    }

    /**
     * 删除职位（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Void> deletePosition(@PathVariable Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        positionService.deletePosition(id);
        return ResultUtils.success(null);
    }
}
