package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.ApprovalDelegateMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.entity.ApprovalDelegate;
import com.limou.hrms.service.ApprovalDelegateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 委托审批服务实现
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
    private UserMapper userMapper;

    @Override
    @CacheEvict(value = {"pendingCount", "delegateRouting"}, allEntries = true)// 创建委托后，需要刷新待办红点和委托路由缓存
    public ApprovalDelegate createDelegate(Long delegatorId, Long delegateId, LocalDateTime startTime, LocalDateTime endTime) {
        // 校验：不能委托给自己
        if (delegatorId.equals(delegateId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能委托给自己");
        }
        // 校验：endTime > startTime
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "结束时间必须晚于开始时间");
        }
        // 校验：时间不重叠
        Long overlap = approvalDelegateMapper.selectCount(
                new QueryWrapper<ApprovalDelegate>()
                        .eq("delegator_id", delegatorId)
                        .eq("enabled", 1)
                        .lt("start_time", endTime)
                        .gt("end_time", startTime));
        if (overlap > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "委托时间与已有委托冲突");
        }

        ApprovalDelegate delegate = new ApprovalDelegate();
        delegate.setDelegatorId(delegatorId);
        delegate.setDelegateId(delegateId);
        delegate.setStartTime(startTime);
        delegate.setEndTime(endTime);
        delegate.setEnabled(1);
        approvalDelegateMapper.insert(delegate);
        log.info("委托创建成功: delegatorId={}, delegateId={}, startTime={}, endTime={}",
                delegatorId, delegateId, startTime, endTime);
        return delegate;
    }

    @Override
    @CacheEvict(value = {"pendingCount", "delegateRouting"}, allEntries = true)// 取消委托后，需要刷新待办红点和委托路由缓存
    public void cancelDelegate(Long delegateId, Long delegatorId) {
        ApprovalDelegate delegate = approvalDelegateMapper.selectById(delegateId);
        if (delegate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "委托不存在");
        }
        if (!delegate.getDelegatorId().equals(delegatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "仅委托人可取消委托");
        }
        delegate.setEnabled(0);
        approvalDelegateMapper.updateById(delegate);
        log.info("委托已取消: delegateId={}, delegatorId={}", delegateId, delegatorId);
    }

    @Override
    public Map<String, List<ApprovalDelegate>> getMyDelegates(Long userId) {
        Map<String, List<ApprovalDelegate>> result = new HashMap<>();
        // asDelegator: 我委托别人
        List<ApprovalDelegate> asDelegator = approvalDelegateMapper.selectList(
                new QueryWrapper<ApprovalDelegate>().eq("delegator_id", userId).eq("enabled", 1));
        // asDelegate: 别人委托我
        List<ApprovalDelegate> asDelegate = approvalDelegateMapper.selectList(
                new QueryWrapper<ApprovalDelegate>().eq("delegate_id", userId).eq("enabled", 1));
        result.put("asDelegator", asDelegator);
        result.put("asDelegate", asDelegate);
        return result;
    }

    @Override
    @Cacheable(value = "delegateRouting", key = "#originalApproverId")
    public Long resolveApprover(Long originalApproverId) {
        // 查当前时间有效委托
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
}
