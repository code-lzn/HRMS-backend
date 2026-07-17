package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.builder.ApproverResolver;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.transfer.TransferCreateDTO;
import com.limou.hrms.model.dto.transfer.TransferUpdateDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.TransferStatus;
import com.limou.hrms.model.query.TransferQuery;
import com.limou.hrms.common.PageRequest;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调岗管理服务实现 — 含调岗 CRUD + 审批回调 + 调岗生效处理
 */
@Service
@Slf4j
public class TransferServiceImpl
        extends ServiceImpl<TransferApplicationMapper, TransferApplication>
        implements TransferService, ApprovalCallback {

    @Resource
    private TransferApplicationMapper transferMapper;
    @Resource
    private TransferHistoryMapper transferHistoryMapper;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeeWorkInfoMapper workInfoMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private DataScopeContext dataScopeContext;
    @Resource
    private ApproverResolver approverResolver;

    // ==================== 调岗 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApplication(TransferCreateDTO dto) {
        // 校验员工在职状态
        Employee employee = employeeMapper.selectById(dto.getEmployeeId());
        if (employee == null) {
            log.warn("调岗申请创建失败: id为{}的员工不存在", dto.getEmployeeId());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "员工不存在");
        }
        Integer empStatus = employee.getStatus();
        if (empStatus == null
                || (empStatus != EmployeeStatus.PROBATION.getValue()
                    && empStatus != EmployeeStatus.REGULAR.getValue())) {
            log.warn("调岗申请创建失败: 员工id为{}, 状态为{}, 仅试用期或正式员工可调岗", dto.getEmployeeId(), EmployeeStatus.getByValue(empStatus).getDesc());
            throw new BusinessException(ErrorCode.TRANSFER_EMPLOYEE_NOT_ACTIVE);
        }

        // 从工作信息自动带入原岗位
        EmployeeWorkInfo wi = getWorkInfoOrThrow(dto.getEmployeeId());

        // 新部门必须与当前不同
        if (dto.getToDepartmentId().equals(wi.getDepartmentId())) {
            log.warn("调岗申请创建失败: 员工id为{},调岗前部门id为{}, 新部门id为{}, 前后部门id不能相同", dto.getEmployeeId(), wi.getDepartmentId(), dto.getToDepartmentId());
            throw new BusinessException(ErrorCode.TRANSFER_DEPT_SAME);
        }

        TransferApplication app = new TransferApplication();
        app.setEmployeeId(dto.getEmployeeId());
        app.setFromDepartmentId(wi.getDepartmentId());
        app.setFromPositionId(wi.getPositionId());
        app.setFromJobLevel(wi.getJobLevel());
        app.setFromDirectReportId(wi.getDirectReportId());
        app.setToDepartmentId(dto.getToDepartmentId());
        app.setToPositionId(dto.getToPositionId());
        app.setToJobLevel(dto.getToJobLevel());
        app.setToDirectReportId(dto.getToDirectReportId());
        app.setSalaryAdjustment(dto.getSalaryAdjustment());
        app.setReason(dto.getReason());
        app.setApplicantId(dataScopeContext.getCurrentEmployeeId());
        app.setStatus(TransferStatus.DRAFT.getCode());
        transferMapper.insert(app);

        // 直接提交
        if (Boolean.TRUE.equals(dto.getSubmitDirectly())) {
            submitToApproval(app.getId());
        }

        log.info("调岗申请创建成功: id={}, employeeId={}", app.getId(), dto.getEmployeeId());
        return app.getId();
    }

    @Override
    public void updateDraft(Long id, TransferUpdateDTO dto) {
        TransferApplication app = getAppOrThrow(id);
        if (app.getStatus() != TransferStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.TRANSFER_DRAFT_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.TRANSFER_DRAFT_ONLY, "仅申请人可编辑草稿");
        }

        if (dto.getToDepartmentId() != null) app.setToDepartmentId(dto.getToDepartmentId());
        if (dto.getToPositionId() != null) app.setToPositionId(dto.getToPositionId());
        if (dto.getToJobLevel() != null) app.setToJobLevel(dto.getToJobLevel());
        if (dto.getToDirectReportId() != null) app.setToDirectReportId(dto.getToDirectReportId());
        if (dto.getSalaryAdjustment() != null) app.setSalaryAdjustment(dto.getSalaryAdjustment());
        if (dto.getReason() != null) app.setReason(dto.getReason());

        transferMapper.updateById(app);
        log.info("调岗草稿更新成功: id={}", id);
    }

    @Override
    public void deleteDraft(Long id) {
        TransferApplication app = getAppOrThrow(id);
        if (app.getStatus() != TransferStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.TRANSFER_DRAFT_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            throw new BusinessException(ErrorCode.TRANSFER_DRAFT_ONLY, "仅申请人可删除草稿");
        }
        transferMapper.deleteById(id);
        log.info("调岗草稿删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitToApproval(Long id) {
        TransferApplication app = getAppOrThrow(id);
        if (app.getStatus() != TransferStatus.DRAFT.getCode()) {
            throw new BusinessException(ErrorCode.TRANSFER_SUBMIT_DRAFT_ONLY);
        }
        if (StringUtils.isBlank(app.getReason())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "调岗原因不能为空");
        }

        ApprovalInstance instance = approvalFlowService.createInstance(
                ApprovalBizType.TRANSFER, app.getId(), app.getApplicantId());

        app.setStatus(TransferStatus.PENDING.getCode());
        app.setApprovalInstanceId(instance.getId());
        transferMapper.updateById(app);

        log.info("调岗申请已提交审批: id={}, instanceId={}", id, instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        TransferApplication app = getAppOrThrow(id);
        if (app.getStatus() != TransferStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.TRANSFER_CANCEL_FIRST_NODE_ONLY);
        }
        Long currentEmployeeId = dataScopeContext.getCurrentEmployeeId();
        if (!app.getApplicantId().equals(currentEmployeeId)) {
            log.warn("调岗申请{}不是申请人{}，无法撤回", id, currentEmployeeId);
            throw new BusinessException(ErrorCode.TRANSFER_CANCEL_FIRST_NODE_ONLY, "仅申请人可撤回");
        }

        approvalFlowService.cancel(app.getApprovalInstanceId());

        app.setStatus(TransferStatus.DRAFT.getCode());
        app.setApprovalInstanceId(null);
        transferMapper.updateById(app);
        log.info("调岗申请已撤回: 表单id={}，状态->{}", id, TransferStatus.fromCode(app.getStatus()).getDesc());
    }

    @Override
    public Page<TransferListVO> list(TransferQuery query) {
        DataScopeEnum scope = dataScopeContext.getApprovalScope();
        switch (scope) {
            case ALL: return queryAllList(query);
            case DEPT: {
                Long deptId = dataScopeContext.getCurrentDepartmentId();
                if (deptId == null) return new Page<>(query.getCurrent(), query.getPageSize());
                return queryDeptList(deptId, query);
            }
            case SELF: {
                Long employeeId = dataScopeContext.getCurrentEmployeeId();
                if (employeeId == null) return new Page<>(query.getCurrent(), query.getPageSize());
                return queryPersonalList(employeeId, query);
            }
            default: return new Page<>(query.getCurrent(), query.getPageSize());
        }
    }

    @Override
    public TransferDetailVO getDetail(Long id) {
        TransferApplication app = getAppOrThrow(id);
        TransferDetailVO vo = buildDetailVO(app);
        if (app.getApprovalInstanceId() != null) {
            vo.setApprovalProgress(approvalFlowService.getDetail(app.getApprovalInstanceId()));
        }
        return vo;
    }

    @Override
    public Page<TransferHistoryVO> getHistory(Long employeeId, PageRequest page) {
        QueryWrapper<TransferHistory> qw = new QueryWrapper<>();
        qw.eq("employee_id", employeeId).orderByDesc("transfer_date");
        Page<TransferHistory> historyPage = transferHistoryMapper.selectPage(
                new Page<>(page.getCurrent(), page.getPageSize()), qw);

        List<TransferHistoryVO> records = historyPage.getRecords().stream().map(h -> {
            TransferHistoryVO vo = new TransferHistoryVO();
            vo.setId(h.getId());
            vo.setEmployeeId(h.getEmployeeId());
            vo.setFromDepartmentName(getDeptName(h.getFromDepartmentId()));
            vo.setToDepartmentName(getDeptName(h.getToDepartmentId()));
            vo.setFromPositionName(getPositionName(h.getFromPositionId()));
            vo.setToPositionName(getPositionName(h.getToPositionId()));
            vo.setFromJobLevel(h.getFromJobLevel());
            vo.setToJobLevel(h.getToJobLevel());
            vo.setTransferDate(h.getTransferDate());
            vo.setReason(h.getReason());
            return vo;
        }).collect(Collectors.toList());

        Page<TransferHistoryVO> result = new Page<>(historyPage.getCurrent(), historyPage.getSize(), historyPage.getTotal());
        result.setRecords(records);
        return result;
    }

    // ==================== 审批回调 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.TRANSFER) return;
        TransferApplication app = getAppOrThrow(bizId);

        // 1. 更新员工工作信息
        EmployeeWorkInfo wi = getWorkInfoOrThrow(app.getEmployeeId());
        wi.setDepartmentId(app.getToDepartmentId());
        if (app.getToPositionId() != null) wi.setPositionId(app.getToPositionId());
        if (app.getToJobLevel() != null) wi.setJobLevel(app.getToJobLevel());
        if (app.getToDirectReportId() != null) wi.setDirectReportId(app.getToDirectReportId());
        workInfoMapper.updateById(wi);

        // 2. 写入调岗历史
        TransferHistory history = new TransferHistory();
        history.setEmployeeId(app.getEmployeeId());
        history.setTransferApplicationId(app.getId());
        history.setFromDepartmentId(app.getFromDepartmentId());
        history.setToDepartmentId(app.getToDepartmentId());
        history.setFromPositionId(app.getFromPositionId());
        history.setToPositionId(app.getToPositionId());
        history.setFromJobLevel(app.getFromJobLevel());
        history.setToJobLevel(app.getToJobLevel());
        history.setTransferDate(LocalDate.now());
        history.setReason(app.getReason());
        transferHistoryMapper.insert(history);

        // 3. 更新申请状态
        app.setStatus(TransferStatus.EFFECTIVE.getCode());
        transferMapper.updateById(app);

        log.info("调岗生效: id={}, employeeId={}, 从部门{}→到部门{}", bizId, app.getEmployeeId(),
                app.getFromDepartmentId(), app.getToDepartmentId());
    }

    @Override
    public void onRejected(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.TRANSFER) return;
        TransferApplication app = getAppOrThrow(bizId);
        app.setStatus(TransferStatus.REJECTED.getCode());
        transferMapper.updateById(app);
        log.info("调岗审批已拒绝: 表单id={}，状态->{}", bizId, TransferStatus.fromCode(app.getStatus()).getDesc());
    }

    // ==================== 私有方法 ====================

    private TransferApplication getAppOrThrow(Long id) {
        TransferApplication app = transferMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.TRANSFER_NOT_FOUND);
        }
        return app;
    }

    private EmployeeWorkInfo getWorkInfoOrThrow(Long employeeId) {
        EmployeeWorkInfo wi = workInfoMapper.selectOne(
                new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", employeeId));
        if (wi == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "员工工作信息不存在");
        }
        return wi;
    }

    // ==================== 角色路由 ====================

    private Page<TransferListVO> queryAllList(TransferQuery query) {
        QueryWrapper<TransferApplication> qw = buildQueryWrapper(query);
        qw.orderByDesc("create_time");
        Page<TransferApplication> page = transferMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private Page<TransferListVO> queryDeptList(Long deptId, TransferQuery query) {
        QueryWrapper<TransferApplication> qw = buildQueryWrapper(query);
        qw.and(w -> w.eq("from_department_id", deptId).or().eq("to_department_id", deptId));
        qw.orderByDesc("create_time");
        Page<TransferApplication> page = transferMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private Page<TransferListVO> queryPersonalList(Long employeeId, TransferQuery query) {
        QueryWrapper<TransferApplication> qw = buildQueryWrapper(query);
        qw.eq("applicant_id", employeeId);
        qw.orderByDesc("create_time");
        Page<TransferApplication> page = transferMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), qw);
        return toListVOPage(page);
    }

    private QueryWrapper<TransferApplication> buildQueryWrapper(TransferQuery query) {
        QueryWrapper<TransferApplication> qw = new QueryWrapper<>();
        if (query.getStatus() != null) qw.eq("status", query.getStatus());
        if (query.getEmployeeId() != null) qw.eq("employee_id", query.getEmployeeId());
        return qw;
    }

    private Page<TransferListVO> toListVOPage(Page<TransferApplication> page) {
        List<TransferListVO> records = page.getRecords().stream().map(app -> {
            TransferListVO vo = new TransferListVO();
            vo.setId(app.getId());
            vo.setEmployeeId(app.getEmployeeId());
            vo.setEmployeeName(approverResolver.getEmployeeName(app.getEmployeeId()));
            vo.setEmployeeNo(getEmployeeNo(app.getEmployeeId()));
            vo.setFromDepartmentName(getDeptName(app.getFromDepartmentId()));
            vo.setToDepartmentName(getDeptName(app.getToDepartmentId()));
            vo.setFromPositionName(getPositionName(app.getFromPositionId()));
            vo.setToPositionName(getPositionName(app.getToPositionId()));
            vo.setStatus(app.getStatus());
            vo.setStatusDesc(getStatusDesc(app.getStatus()));
            vo.setApplicantName(approverResolver.getEmployeeName(app.getApplicantId()));
            vo.setCreateTime(app.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<TransferListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    private TransferDetailVO buildDetailVO(TransferApplication app) {
        TransferDetailVO vo = new TransferDetailVO();
        vo.setId(app.getId());
        vo.setEmployeeId(app.getEmployeeId());
        vo.setEmployeeName(approverResolver.getEmployeeName(app.getEmployeeId()));
        vo.setEmployeeNo(getEmployeeNo(app.getEmployeeId()));
        // 原岗位
        vo.setFromDepartmentId(app.getFromDepartmentId());
        vo.setFromDepartmentName(getDeptName(app.getFromDepartmentId()));
        vo.setFromPositionId(app.getFromPositionId());
        vo.setFromPositionName(getPositionName(app.getFromPositionId()));
        vo.setFromJobLevel(app.getFromJobLevel());
        vo.setFromDirectReportId(app.getFromDirectReportId());
        vo.setFromDirectReportName(approverResolver.getEmployeeName(app.getFromDirectReportId()));
        // 新岗位
        vo.setToDepartmentId(app.getToDepartmentId());
        vo.setToDepartmentName(getDeptName(app.getToDepartmentId()));
        vo.setToPositionId(app.getToPositionId());
        vo.setToPositionName(getPositionName(app.getToPositionId()));
        vo.setToJobLevel(app.getToJobLevel());
        vo.setToDirectReportId(app.getToDirectReportId());
        vo.setToDirectReportName(approverResolver.getEmployeeName(app.getToDirectReportId()));
        vo.setSalaryAdjustment(app.getSalaryAdjustment());
        vo.setReason(app.getReason());
        vo.setStatus(app.getStatus());
        vo.setStatusDesc(getStatusDesc(app.getStatus()));
        vo.setApprovalInstanceId(app.getApprovalInstanceId());
        vo.setApplicantId(app.getApplicantId());
        vo.setApplicantName(approverResolver.getEmployeeName(app.getApplicantId()));
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());
        return vo;
    }

    private String getStatusDesc(Integer status) {
        TransferStatus ts = TransferStatus.fromCode(status);
        return ts != null ? ts.getDesc() : "";
    }

    private String getEmployeeNo(Long employeeId) {
        if (employeeId == null) return null;
        Employee emp = employeeMapper.selectById(employeeId);
        return emp != null ? emp.getEmployeeNo() : null;
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) return null;
        Department dept = departmentMapper.selectById(deptId);
        return dept != null ? dept.getName() : null;
    }

    private String getPositionName(Long positionId) {
        if (positionId == null) return null;
        Position pos = positionMapper.selectById(positionId);
        return pos != null ? pos.getName() : null;
    }
}
