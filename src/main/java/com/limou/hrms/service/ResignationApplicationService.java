package com.limou.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.resignation.ResignationApplicationAddRequest;
import com.limou.hrms.model.dto.resignation.ResignationApplicationQueryRequest;
import com.limou.hrms.model.entity.ResignationApplication;
import com.limou.hrms.model.vo.ResignationApplicationVO;

import java.util.List;

/**
 * 离职申请服务
 */
public interface ResignationApplicationService extends IService<ResignationApplication> {

    /**
     * 创建离职申请
     *
     * @param addRequest 创建请求
     * @param userId     当前用户ID
     * @return 申请ID
     */
    Long createApplication(ResignationApplicationAddRequest addRequest, Long userId);

    /**
     * 处理审批结果
     *
     * @param id           申请ID
     * @param approved     是否通过
     * @param rejectReason 拒绝原因
     */
    void processApprovalResult(Long id, boolean approved, String rejectReason);

    /**
     * 定时扫描待离职到期员工并执行离职生效
     */
    void processPendingResignations();

    /**
     * 获取查询条件
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<ResignationApplication> getQueryWrapper(ResignationApplicationQueryRequest queryRequest);

    /**
     * 获取VO
     *
     * @param entity 实体
     * @return VO
     */
    ResignationApplicationVO getVO(ResignationApplication entity);

    /**
     * 批量获取VO
     *
     * @param entityList 实体列表
     * @return VO列表
     */
    List<ResignationApplicationVO> getVOList(List<ResignationApplication> entityList);
}
