package com.limou.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.transfer.TransferApplicationAddRequest;
import com.limou.hrms.model.dto.transfer.TransferApplicationQueryRequest;
import com.limou.hrms.model.entity.TransferApplication;
import com.limou.hrms.model.vo.TransferApplicationVO;

import java.util.List;

/**
 * 调岗申请服务
 */
public interface TransferApplicationService extends IService<TransferApplication> {

    /**
     * 创建调岗申请
     *
     * @param addRequest 创建请求
     * @param userId     当前用户ID
     * @return 申请ID
     */
    Long createApplication(TransferApplicationAddRequest addRequest, Long userId);

    /**
     * 推进审批节点（状态机流转）
     *
     * @param id       申请ID
     * @param approved 是否通过当前节点
     * @param rejectReason 拒绝原因
     */
    void processApprovalNode(Long id, boolean approved, String rejectReason);

    /**
     * 调岗生效
     *
     * @param id 申请ID
     */
    void effectTransfer(Long id);

    /**
     * 获取查询条件
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<TransferApplication> getQueryWrapper(TransferApplicationQueryRequest queryRequest);

    /**
     * 获取VO
     *
     * @param entity 实体
     * @return VO
     */
    TransferApplicationVO getVO(TransferApplication entity);

    /**
     * 批量获取VO
     *
     * @param entityList 实体列表
     * @return VO列表
     */
    List<TransferApplicationVO> getVOList(List<TransferApplication> entityList);
}
