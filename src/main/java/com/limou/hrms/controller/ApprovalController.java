package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.approval.ApprovalActionDTO;
import com.limou.hrms.model.dto.approval.DelegateSettingDTO;
import com.limou.hrms.model.entity.ApprovalDelegate;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.ApprovalInstanceVO;
import com.limou.hrms.model.vo.PendingItemVO;
import com.limou.hrms.model.vo.ProcessedItemVO;
import com.limou.hrms.service.ApprovalDelegateService;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批中心 — 通用审批接口
 *
 * <p>提供审批流程全生命周期管理：创建实例、节点操作（通过/拒绝/转交/撤回）、
 * 工作台查询（待办/已办/详情/红点数量）以及委托审批管理。
 * 审批实例的创建由各业务模块（入职、转正等）在提交申请时调用审批引擎完成，
 * 本 Controller 负责审批流转和查询能力。
 *
 * <p>当前用户通过 Session 解析，接口签名无需传入操作人 ID。
 *
 * @see ApprovalFlowService
 * @see ApprovalDelegateService
 */
@RestController
@RequestMapping("/approvals")
@Slf4j
public class ApprovalController {

    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ApprovalDelegateService approvalDelegateService;
    @Resource
    private UserService userService;

    // ================================================================
    //  审批人工作台 — 查询接口
    // ================================================================

    /**
     * 查询当前用户的待办列表（含委托路由）。
     *
     * <p>返回本人待审批节点 + 他人委托给本人的节点，按创建时间倒序排列。
     * 每条记录含截止时间（节点创建后 48h）和委托人标识（委托场景）。
     *
     * @param query 分页及业务类型筛选参数，{@code bizType} 为空表示全部类型
     * @return 分页待办列表
     */
    @GetMapping("/pending")
    public BaseResponse<Page<PendingItemVO>> getPendingList(ApprovalQuery query) {
        return ResultUtils.success(approvalFlowService.getPendingList(getCurrentEmployeeId(), query));
    }

    /**
     * 获取当前用户的待办数量（含委托），用于顶部导航红点角标。
     *
     * @return {@code {"count": N}}
     */
    @GetMapping("/pending-count")
    public BaseResponse<Map<String, Long>> getPendingCount() {
        long count = approvalFlowService.getPendingCount(getCurrentEmployeeId());
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return ResultUtils.success(result);
    }

    /**
     * 查询当前用户的已办列表。
     *
     * <p>包含已通过、已拒绝、已转交的节点，按操作时间倒序排列。
     *
     * @param query 分页及业务类型筛选参数
     * @return 分页已办列表
     */
    @GetMapping("/processed")
    public BaseResponse<Page<ProcessedItemVO>> getProcessedList(ApprovalQuery query) {
        return ResultUtils.success(approvalFlowService.getProcessedList(getCurrentEmployeeId(), query));
    }

    /**
     * 获取审批实例详情（含完整节点时间线）。
     *
     * <p>返回审批实例基本信息及所有审批节点的状态、审批人、审批意见、操作时间。
     * 前端可据此绘制审批进度条/时间线。
     *
     * @param instanceId 审批实例 ID
     * @return 审批实例 VO（含节点列表）
     * @throws BusinessException 40001 审批实例不存在
     */
    @GetMapping("/{instanceId}")
    public BaseResponse<ApprovalInstanceVO> getDetail(@PathVariable Long instanceId) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(approvalFlowService.getDetail(instanceId));
    }

    // ================================================================
    //  审批流转 — 操作接口
    // ================================================================

    /**
     * 审批通过当前节点。
     *
     * <p>前置条件：当前用户 = 该节点审批人（含委托路由）、节点状态 = 待审批、节点未超时。
     * 若为最后一个节点，示例状态变更为"已通过"并触发 {@code onApproved} 业务回调；
     * 否则推进到下一节点。
     *
     * <p>若当前用户是被委托人，系统自动记录 {@code originalApproverId = 委托人ID} 用于审计。
     *
     * @param nodeId 审批节点 ID
     * @param dto    审批参数，{@code comment} 为审批意见（可选）
     * @throws BusinessException 40002 审批节点不存在
     * @throws BusinessException 40003 该节点不属于当前用户
     * @throws BusinessException 40004 该节点已被处理
     * @throws BusinessException 40011 审批已超时
     */
    @PostMapping("/{nodeId}/approve")
    public BaseResponse<?> approve(@PathVariable Long nodeId, @RequestBody ApprovalActionDTO dto) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        approvalFlowService.approve(nodeId, getCurrentEmployeeId(), dto.getComment());
        return ResultUtils.success("ok");
    }

    /**
     * 审批拒绝当前节点。
     *
     * <p>前置条件：同审批通过。该节点置为"已拒绝"，示例置为"已拒绝"，
     * 触发 {@code onRejected} 业务回调。拒绝理由必填。
     *
     * @param nodeId 审批节点 ID
     * @param dto    审批参数，{@code comment} 为拒绝理由（必填）
     * @throws BusinessException 40000 拒绝理由为空
     * @throws BusinessException 40002 审批节点不存在
     * @throws BusinessException 40003 该节点不属于当前用户
     * @throws BusinessException 40004 该节点已被处理
     * @throws BusinessException 40011 审批已超时
     */
    @PostMapping("/{nodeId}/reject")
    public BaseResponse<?> reject(@PathVariable Long nodeId, @RequestBody ApprovalActionDTO dto) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        if (StringUtils.isBlank(dto.getComment())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拒绝时必须填写审批意见");
        }
        approvalFlowService.reject(nodeId, getCurrentEmployeeId(), dto.getComment());
        return ResultUtils.success("ok");
    }

    /**
     * 将当前审批节点转交给他人。
     *
     * <p>保存 {@code originalApproverId = 原审批人}，更新 {@code approverId = 新审批人}，
     * 节点状态重置为"待审批"。新审批人会收到待办任务。
     *
     * @param nodeId 审批节点 ID
     * @param dto    转交参数，{@code toApproverId} 为新审批人 employee.id，{@code comment} 为转交说明（可选）
     * @throws BusinessException 40000 目标审批人为空
     * @throws BusinessException 40002 审批节点不存在
     * @throws BusinessException 40003 该节点不属于当前用户
     * @throws BusinessException 40004 该节点已被处理
     */
    @PostMapping("/{nodeId}/transfer")
    public BaseResponse<?> transfer(@PathVariable Long nodeId, @RequestBody ApprovalActionDTO dto) {
        ThrowUtils.throwIf(nodeId == null || nodeId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(dto.getToApproverId() == null || dto.getToApproverId() <= 0,
                ErrorCode.PARAMS_ERROR, "目标审批人不能为空");
        approvalFlowService.transfer(nodeId, getCurrentEmployeeId(), dto.getToApproverId());
        return ResultUtils.success("ok");
    }

    /**
     * 撤回审批申请。
     *
     * <p>前置条件：申请人为当前用户、当前审批节点为第一节点（尚未被处理）、
     * 实例状态 = 审批中。实例状态变更为"已撤回"，申请人可重新编辑后再次提交。
     *
     * @param instanceId 审批实例 ID
     * @throws BusinessException 40001 审批实例不存在
     * @throws BusinessException 40004 当前状态不允许撤回
     * @throws BusinessException 40005 仅第一节点可撤回
     */
    @PostMapping("/{instanceId}/cancel")
    public BaseResponse<?> cancel(@PathVariable Long instanceId) {
        ThrowUtils.throwIf(instanceId == null || instanceId <= 0, ErrorCode.PARAMS_ERROR);
        approvalFlowService.cancel(instanceId, getCurrentEmployeeId());
        return ResultUtils.success("ok");
    }

    // ================================================================
    //  委托审批管理
    // ================================================================

    /**
     * 设置委托审批。
     *
     * <p>审批人可将指定时间段的审批任务自动路由给被委托人。
     * 被委托人审批时系统自动记录"代审批"标识（{@code originalApproverId}）。
     *
     * <p>校验规则：不能委托给自己、结束时间必须晚于开始时间、
     * 同一时段不能有重叠的有效委托。
     *
     * @param dto 委托设置参数（被委托人ID、起止时间）
     * @return 创建的委托记录
     * @throws BusinessException 40000 参数校验失败（委托自己/时间冲突/时间无效）
     */
    @PostMapping("/delegates")
    public BaseResponse<ApprovalDelegate> createDelegate(@RequestBody DelegateSettingDTO dto) {
        ApprovalDelegate result = approvalDelegateService.createDelegate(
                getCurrentEmployeeId(), dto.getDelegateId(), dto.getStartTime(), dto.getEndTime());
        return ResultUtils.success(result);
    }

    /**
     * 取消委托审批（软删除，设 enabled=0 保留审计记录）。
     *
     * <p>前置条件：仅委托人本人可取消自己的委托。
     *
     * @param id 委托记录 ID
     * @throws BusinessException 40400 委托不存在
     * @throws BusinessException 40300 仅委托人可取消委托
     */
    @DeleteMapping("/delegates/{id}")
    public BaseResponse<?> cancelDelegate(@PathVariable Long id) {
        approvalDelegateService.cancelDelegate(id, getCurrentEmployeeId());
        return ResultUtils.success(null);
    }

    /**
     * 查询当前用户的委托关系。
     *
     * <p>返回两部分：
     * <ul>
     *   <li>{@code asDelegator} — 我委托给别人</li>
     *   <li>{@code asDelegate}  — 别人委托给我</li>
     * </ul>
     *
     * @return 委托关系双列表
     */
    @GetMapping("/delegates/my")
    public BaseResponse<Map<String, List<ApprovalDelegate>>> getMyDelegates() {
        return ResultUtils.success(approvalDelegateService.getMyDelegates(getCurrentEmployeeId()));
    }

    // ================================================================
    //  私有方法
    // ================================================================

    /**
     * 从请求属性获取当前操作人的 employee.id。
     *
     * <p>由 {@link com.limou.hrms.interceptor.EmployeeResolveInterceptor} 在请求进入时
     * 解析并写入 {@code currentEmployeeId} 属性，无需每次查 DB。
     */
    private Long getCurrentEmployeeId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attrs.getRequest();
        Long employeeId = (Long) request.getAttribute(
                com.limou.hrms.interceptor.EmployeeResolveInterceptor.CURRENT_EMPLOYEE_ID);
        if (employeeId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录或未关联员工档案");
        }
        return employeeId;
    }
}
