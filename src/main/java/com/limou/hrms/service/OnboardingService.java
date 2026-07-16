package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.onboarding.OnboardingCreateDTO;
import com.limou.hrms.model.dto.onboarding.OnboardingUpdateDTO;
import com.limou.hrms.model.query.OnboardingQuery;
import com.limou.hrms.model.vo.OnboardingDetailVO;
import com.limou.hrms.model.vo.OnboardingListVO;

import java.time.LocalDate;

/**
 * 入职管理服务接口
 */
public interface OnboardingService {

    /**
     * 创建入职申请（保存草稿或直接提交审批）
     * @param dto 申请信息
     * @return 入职申请ID
     */
    Long createApplication(OnboardingCreateDTO dto);

    /**
     * 更新草稿（仅草稿状态可编辑）
     */
    void updateDraft(Long id, OnboardingUpdateDTO dto);

    /**
     * 删除草稿（仅草稿状态且本人操作）
     */
    void deleteDraft(Long id);

    /**
     * 提交审批（草稿→审批中，创建审批实例）
     */
    void submitToApproval(Long id);

    /**
     * 撤回申请（仅第一节点，回退为草稿）
     */
    void cancel(Long id);

    /**
     * 确认入职（已批准待入职→已入职）
     */
    void confirmJoin(Long id, LocalDate actualHireDate);

    /**
     * 标记放弃入职
     */
    void abandon(Long id);

    /**
     * 分页查询列表（角色路由）
     */
    Page<OnboardingListVO> list(OnboardingQuery query);

    /**
     * 获取详情（含审批进度，审批中/已通过/已拒绝时返回）
     */
    OnboardingDetailVO getDetail(Long id);

    /**
     * 预览工号（不消耗序号）
     */
    String previewEmployeeNo(Long departmentId);

    /**
     * 校验手机号是否可用
     * @param phone 手机号
     * @param excludeId 排除的入职申请ID（编辑时传）
     */
    boolean isPhoneAvailable(String phone, Long excludeId);
}
