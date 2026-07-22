package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
import com.limou.hrms.context.UserContext;
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
 * 职位管理控制器 — CRUD + 序列枚举
 */
@RestController
@RequestMapping("/positions")
@Slf4j
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    /**
     * GET /api/positions/sequences — 获取职位序列 + 职级配置枚举
     */
    @GetMapping("/sequences")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<List<Map<String, Object>>> getSequences() {
        log.info("{} 获取职位序列枚举", UserContext.getCurrentUser());
        List<Map<String, Object>> sequences = positionService.getSequences();
        return ResultUtils.success(sequences);
    }

    /**
     * GET /api/positions — 查询职位列表（分页 + 模糊搜索）
     */
    @GetMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<Page<PositionVO>> getPositionList(PositionQueryRequest queryReq) {
        log.info("{} 查询职位列表, keyword={}", UserContext.getCurrentUser(), queryReq.getKeyword());
        Page<PositionVO> page = positionService.getPositionList(queryReq);
        return ResultUtils.success(page);
    }

    /**
     * GET /api/positions/{id} — 查询职位详情
     */
    @GetMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE,UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<PositionVO> getPositionDetail(@PathVariable Long id) {
        log.info("{} 查询职位详情, id={}", UserContext.getCurrentUser(), id);
        PositionVO vo = positionService.getPositionDetail(id);
        return ResultUtils.success(vo);
    }

    /**
     * POST /api/positions — 创建职位
     */
    @PostMapping
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Position> createPosition(@Valid @RequestBody PositionCreateRequest dto) {
        log.info("{} 创建职位, name={}", UserContext.getCurrentUser(), dto.getName());
        Position position = positionService.createPosition(dto);
        return ResultUtils.success(position);
    }

    /**
     * PUT /api/positions/{id} — 更新职位（部分更新）
     */
    @PutMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Position> updatePosition(@PathVariable Long id,
                                                  @RequestBody PositionUpdateRequest dto) {
        log.info("{} 更新职位, id={}", UserContext.getCurrentUser(), id);
        Position position = positionService.updatePosition(id, dto);
        return ResultUtils.success(position);
    }

    /**
     * DELETE /api/positions/{id} — 删除职位（逻辑删除，需先清空员工关联）
     */
    @DeleteMapping("/{id}")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<Void> deletePosition(@PathVariable Long id) {
        log.info("{} 删除职位, id={}", UserContext.getCurrentUser(), id);
        positionService.deletePosition(id);
        return ResultUtils.success(null);
    }
}