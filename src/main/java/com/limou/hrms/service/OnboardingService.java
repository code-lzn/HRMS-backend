package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.dto.onboarding.OnboardingAddRequest;
import com.limou.hrms.model.entity.HrOnboarding;
import com.limou.hrms.model.vo.ApprovalPendingVO;
import com.limou.hrms.model.vo.OnboardingVO;

import java.util.Date;
import java.util.List;

/**
 * 入职管理服务
 */
public interface OnboardingService extends IService<HrOnboarding> {

    /**
     * 新增入职申请（保存草稿 或 保存并提交审批）
     */
    HrOnboarding addOnboarding(OnboardingAddRequest request, Long hrEmployeeId, boolean submitNow);

    /**
     * 草稿提交审批
     */
    void submitForApproval(Long onboardingId, Long hrEmployeeId);

    /**
     * 编辑草稿
     */
    void updateOnboarding(Long id, OnboardingAddRequest request, Long hrEmployeeId);

    /**
     * 删除草稿
     */
    void deleteOnboarding(Long id, Long hrEmployeeId);

    /**
     * HR确认到岗 → 自动创建 employee + employee_detail + user
     */
    void confirmOnboarding(Long id, Date actualEntryDate, Long hrEmployeeId);

    /**
     * 标记放弃入职
     */
    void abandonOnboarding(Long id, Long hrEmployeeId);

    /**
     * 入职列表（分页 + 状态筛选）
     */
    Page<OnboardingVO> listOnboarding(String keyword, java.util.List<String> statuses, int page, int size);

    /**
     * 入职详情
     */
    OnboardingVO getOnboardingDetail(Long id);

    /**
     * 审批通过回调（由 ApprovalService 调用）
     */
    void onApprovalPassed(Long businessId);

    /**
     * 审批拒绝回调（由 ApprovalService 调用）
     */
    void onApprovalRejected(Long businessId);

    /**
     * 员工确认入职（候选人同意入职）
     */
    void employeeConfirm(Long id, Long employeeId);

    /**
     * 获取员工的人事异动记录（入职申请），包含审批流程详情
     */
    java.util.List<com.limou.hrms.model.vo.MutationLogVO> getEmployeeMutationLogs(Long userId);

    /** 获取部门负责人管辖部门的入职待审批任务列表 */
    List<ApprovalPendingVO> getDeptManagerOnboardingPendingList(Long employeeId);

    /** 部门负责人审批入职申请通过 */
    void deptManagerApprove(Long detailId, Long employeeId, String comment);

    /** 部门负责人审批入职申请拒绝 */
    void deptManagerReject(Long detailId, Long employeeId, String comment);

    /** 获取可转交审批的人员列表（角色ID=1和3） */
    List<com.limou.hrms.model.vo.UserVO> getTransferableUsers();
}
