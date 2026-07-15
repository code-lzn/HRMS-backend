package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.position.PositionCreateRequest;
import com.limou.hrms.model.dto.position.PositionQueryRequest;
import com.limou.hrms.model.dto.position.PositionUpdateRequest;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.vo.PositionVO;

import java.util.List;
import java.util.Map;

/**
 * 职位服务
 */
public interface PositionService extends IService<Position> {

    /**
     * 创建职位
     */
    Position createPosition(PositionCreateRequest dto);

    /**
     * 更新职位
     */
    Position updatePosition(Long id, PositionUpdateRequest dto);

    /**
     * 删除职位（逻辑删除）
     */
    void deletePosition(Long id);

    /**
     * 查询职位列表（分页）
     */
    Page<PositionVO> getPositionList(PositionQueryRequest query);

    /**
     * 查询职位详情
     */
    PositionVO getPositionDetail(Long id);

    /**
     * 查询职位序列枚举列表
     */
    List<Map<String, Object>> getSequences();
}
