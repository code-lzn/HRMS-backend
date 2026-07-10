package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.position.PositionAddRequest;
import com.limou.hrms.model.dto.position.PositionQueryRequest;
import com.limou.hrms.model.dto.position.PositionUpdateRequest;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.vo.PositionVO;
import com.limou.hrms.model.vo.SequenceLevelVO;

import java.util.List;

/**
 * 职位服务
 */
public interface PositionService extends IService<Position> {

    /**
     * 职位列表查询
     */
    List<PositionVO> listPositions(PositionQueryRequest request);

    /**
     * 新增职位
     */
    Long addPosition(PositionAddRequest request);

    /**
     * 更新职位
     */
    void updatePosition(PositionUpdateRequest request);

    /**
     * 删除职位（校验无员工引用）
     */
    void deletePosition(Long id);

    /**
     * 获取序列职级对照
     */
    List<SequenceLevelVO> getSequences();
}
