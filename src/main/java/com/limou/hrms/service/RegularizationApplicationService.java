package com.limou.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.regularization.RegularizationApplicationAddRequest;
import com.limou.hrms.model.dto.regularization.RegularizationApplicationQueryRequest;
import com.limou.hrms.model.entity.RegularizationApplication;
import com.limou.hrms.model.vo.RegularizationApplicationVO;

import java.util.List;

/**
 * 转正申请服务
 */
public interface RegularizationApplicationService extends IService<RegularizationApplication> {

    /**
     * 创建转正申请
     *
     * @param addRequest 创建请求
     * @param userId     当前用户ID
     * @return 申请ID
     */
    Long createApplication(RegularizationApplicationAddRequest addRequest, Long userId);

    /**
     * 获取待转正员工列表（试用期即将到期或已到期）
     *
     * @return 待转正员工信息列表
     */
    List<RegularizationApplicationVO> getPendingList();

    /**
     * 处理审批结果
     *
     * @param id     申请ID
     * @param result 审批结果
     * @param rejectReason 拒绝原因
     */
    void processApprovalResult(Long id, Integer result, String rejectReason);

    /**
     * 获取查询条件
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<RegularizationApplication> getQueryWrapper(RegularizationApplicationQueryRequest queryRequest);

    /**
     * 获取VO
     *
     * @param entity 实体
     * @return VO
     */
    RegularizationApplicationVO getVO(RegularizationApplication entity);

    /**
     * 批量获取VO
     *
     * @param entityList 实体列表
     * @return VO列表
     */
    List<RegularizationApplicationVO> getVOList(List<RegularizationApplication> entityList);
}
