package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.ApprovalDelegateMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.EmployeePersonalInfoMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.model.dto.approval.DelegateSettingDTO;
import com.limou.hrms.model.entity.ApprovalDelegate;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.EmployeePersonalInfo;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.vo.MyDelegatesVO;
import com.limou.hrms.service.ApprovalDelegateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 委托审批服务实现。
 * 操作人信息由 Service 内部解析。
 */
@Service
@Slf4j
public class ApprovalDelegateServiceImpl extends ServiceImpl<ApprovalDelegateMapper, ApprovalDelegate>
        implements ApprovalDelegateService {

    @Resource
    private ApprovalDelegateMapper approvalDelegateMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private EmployeePersonalInfoMapper personalInfoMapper;
    @Resource
    private EmployeeWorkInfoMapper workInfoMapper;
    @Resource
    private DataScopeContext dataScopeContext;
    @Resource
    private CacheManager cacheManager;

    @Override
    public ApprovalDelegate createDelegate(DelegateSettingDTO dto) {
        Long delegatorId = resolveCurrentEmployeeId();
        Long delegateId = dto.getDelegateId();

        if (delegatorId.equals(delegateId)) {
            log.warn("不能委托给自己 delegatorId={}, delegateId={}", delegatorId, delegateId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能委托给自己");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            log.warn("结束时间必须晚于开始时间 startTime={}, endTime={}", dto.getStartTime(), dto.getEndTime());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间必须晚于开始时间");
        }
        Long overlap = approvalDelegateMapper.selectCount(
                new QueryWrapper<ApprovalDelegate>()
                        .eq("delegator_id", delegatorId)
                        .eq("enabled", 1)
                        .lt("start_time", dto.getEndTime())
                        .gt("end_time", dto.getStartTime()));
        if (overlap > 0) {
            log.warn("委托时间与已有委托冲突 delegatorId={}, delegateId={}, startTime={}, endTime={}",
                    delegatorId, delegateId, dto.getStartTime(), dto.getEndTime());
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "委托时间与已有委托冲突");
        }

        ApprovalDelegate delegate = new ApprovalDelegate();
        delegate.setDelegatorId(delegatorId);
        delegate.setDelegateId(delegateId);
        delegate.setStartTime(dto.getStartTime());
        delegate.setEndTime(dto.getEndTime());
        delegate.setEnabled(1);
        approvalDelegateMapper.insert(delegate);

        // 创建委托后清除相关缓存
        evictPendingCountAndDelegateCache();

        log.info("委托创建成功: delegatorId={}, delegateId={}", delegatorId, delegateId);
        return delegate;
    }

    @Override
    public void cancelDelegate(Long delegateId) {
        Long delegatorId = resolveCurrentEmployeeId();
        ApprovalDelegate delegate = approvalDelegateMapper.selectById(delegateId);
        if (delegate == null) {
            log.warn("委托不存在 delegateId={}", delegateId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "委托不存在");
        }
        if (!delegate.getDelegatorId().equals(delegatorId)) {
            log.warn("仅委托人可取消委托 delegateId={}, delegatorId={}", delegateId, delegatorId);
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "仅委托人可取消委托");
        }
        delegate.setEnabled(0);
        approvalDelegateMapper.updateById(delegate);

        // 取消委托后清除相关缓存
        evictPendingCountAndDelegateCache();

        log.info("委托已取消: delegateId={}, delegatorId={}", delegateId, delegatorId);
    }

    @Override
    public MyDelegatesVO getMyDelegates() {
        Long empId = resolveCurrentEmployeeId();
        MyDelegatesVO result = new MyDelegatesVO();
        // asDelegator: 我委托别人
        List<ApprovalDelegate> asDelegator = approvalDelegateMapper.selectList(
                new QueryWrapper<ApprovalDelegate>().eq("delegator_id", empId).eq("enabled", 1));
        enrichDelegateNames(asDelegator);
        result.setAsDelegator(asDelegator);
        // asDelegate: 别人委托我
        List<ApprovalDelegate> asDelegate = approvalDelegateMapper.selectList(
                new QueryWrapper<ApprovalDelegate>().eq("delegate_id", empId).eq("enabled", 1));
        enrichDelegateNames(asDelegate);
        result.setAsDelegate(asDelegate);
        return result;
    }

    /** 根据 delegateId/delegatorId 填充姓名和职位 */
    private void enrichDelegateNames(List<ApprovalDelegate> list) {
        for (ApprovalDelegate d : list) {
            // 被委托人
            Employee emp = employeeMapper.selectById(d.getDelegateId());
            if (emp != null) {
                EmployeePersonalInfo info = personalInfoMapper.selectOne(
                        new QueryWrapper<EmployeePersonalInfo>().eq("employee_id", emp.getId()).last("LIMIT 1"));
                if (info != null) d.setDelegateName(info.getName());
                EmployeeWorkInfo wi = workInfoMapper.selectOne(
                        new QueryWrapper<EmployeeWorkInfo>().eq("employee_id", emp.getId()).last("LIMIT 1"));
                if (wi != null) d.setDelegatePosition(wi.getJobLevel());
            }
            // 委托人
            Employee delegatorEmp = employeeMapper.selectById(d.getDelegatorId());
            if (delegatorEmp != null) {
                EmployeePersonalInfo info = personalInfoMapper.selectOne(
                        new QueryWrapper<EmployeePersonalInfo>().eq("employee_id", delegatorEmp.getId()).last("LIMIT 1"));
                if (info != null) d.setDelegatorName(info.getName());
            }
        }
    }

    @Override
    @Cacheable(value = "delegateRouting", key = "#originalApproverId")
    public Long resolveApprover(Long originalApproverId) {
        List<ApprovalDelegate> delegates = approvalDelegateMapper.selectList(
                new QueryWrapper<ApprovalDelegate>()
                        .eq("delegator_id", originalApproverId)
                        .eq("enabled", 1)
                        .apply("NOW() BETWEEN start_time AND end_time"));
        if (!delegates.isEmpty()) {
            return delegates.get(0).getDelegateId();
        }
        return originalApproverId;
    }

    // ==================== 私有方法 ====================

    /** 测试用：直接注入当前用户 */
    private Long testEmployeeId;

    /** 测试用 */
    public void setCurrentUserForTest(Long employeeId) {
        this.testEmployeeId = employeeId;
    }

    private Long resolveCurrentEmployeeId() {
        if (testEmployeeId != null) return testEmployeeId;
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        if (employeeId == null) {
            log.warn("未登录或未关联员工档案, employeeId={}", (Object) null);
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录或未关联员工档案");
        }
        return employeeId;
    }

    private void evictPendingCountAndDelegateCache() {
        org.springframework.cache.Cache pendingCache = cacheManager.getCache("pendingCount");
        if (pendingCache != null) pendingCache.clear();
        org.springframework.cache.Cache delegateCache = cacheManager.getCache("delegateRouting");
        if (delegateCache != null) delegateCache.clear();
    }
}
