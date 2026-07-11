package com.limou.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.onboarding.OnboardingApplicationAddRequest;
import com.limou.hrms.model.dto.onboarding.OnboardingApplicationConfirmHireRequest;
import com.limou.hrms.model.dto.onboarding.OnboardingApplicationQueryRequest;
import com.limou.hrms.model.entity.OnboardingApplication;
import com.limou.hrms.model.vo.OnboardingApplicationVO;

import java.util.List;

/**
 * 入职申请服务
 */
public interface OnboardingApplicationService extends IService<OnboardingApplication> {

    /**
     * 创建入职申请（保存草稿或提交审批）
     *
     * @param addRequest 创建请求
     * @param userId     当前用户ID
     * @return 申请ID
     */
    Long createApplication(OnboardingApplicationAddRequest addRequest, Long userId);

    /**
     * 提交审批
     *
     * @param id 申请ID
     */
    void submitForApproval(Long id);

    /**
     * 确认入职（审批通过后HR确认实际到岗）
     *
     * @param id      申请ID
     * @param request 确认请求
     */
    void confirmHire(Long id, OnboardingApplicationConfirmHireRequest request);

    /**
     * 放弃入职
     *
     * @param id 申请ID
     */
    void abandon(Long id);

    /**
     * 获取查询条件
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<OnboardingApplication> getQueryWrapper(OnboardingApplicationQueryRequest queryRequest);

    /**
     * 获取VO
     *
     * @param entity 实体
     * @return VO
     */
    OnboardingApplicationVO getVO(OnboardingApplication entity);

    /**
     * 批量获取VO
     *
     * @param entityList 实体列表
     * @return VO列表
     */
    List<OnboardingApplicationVO> getVOList(List<OnboardingApplication> entityList);
}
