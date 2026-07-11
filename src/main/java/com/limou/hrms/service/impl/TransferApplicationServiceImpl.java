package com.limou.hrms.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.TransferApplicationMapper;
import com.limou.hrms.mapper.TransferHistoryMapper;
import com.limou.hrms.model.dto.transfer.TransferApplicationAddRequest;
import com.limou.hrms.model.dto.transfer.TransferApplicationQueryRequest;
import com.limou.hrms.model.entity.TransferApplication;
import com.limou.hrms.model.entity.TransferHistory;
import com.limou.hrms.model.enums.TransferStatusEnum;
import com.limou.hrms.model.vo.TransferApplicationVO;
import com.limou.hrms.service.TransferApplicationService;
import com.limou.hrms.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调岗申请服务实现
 */
@Service
@Slf4j
public class TransferApplicationServiceImpl
        extends ServiceImpl<TransferApplicationMapper, TransferApplication>
        implements TransferApplicationService {

    @Resource
    private TransferHistoryMapper transferHistoryMapper;

    @Override
    public Long createApplication(TransferApplicationAddRequest addRequest, Long userId) {
        ThrowUtils.throwIf(addRequest.getEmployeeId() == null, ErrorCode.PARAMS_ERROR, "员工ID不能为空");
        ThrowUtils.throwIf(addRequest.getNewDepartmentId() == null, ErrorCode.PARAMS_ERROR, "新部门ID不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(addRequest.getReason()), ErrorCode.PARAMS_ERROR, "调岗原因不能为空");

        TransferApplication application = new TransferApplication();
        BeanUtils.copyProperties(addRequest, application);
        application.setCreateBy(userId);
        // 初始状态：原部门审批中
        application.setStatus(TransferStatusEnum.PENDING_OLD_DEPT.getValue());

        boolean saved = this.save(application);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建调岗申请失败");
        log.info("调岗申请创建成功, id={}, employeeId={}", application.getId(), application.getEmployeeId());
        return application.getId();
    }

    @Override
    public void processApprovalNode(Long id, boolean approved, String rejectReason) {
        TransferApplication application = this.getById(id);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR, "调岗申请不存在");

        int currentStatus = application.getStatus();

        if (!approved) {
            // 任一步拒绝，流程终止
            application.setStatus(TransferStatusEnum.REJECTED.getValue());
            this.updateById(application);
            log.info("调岗申请被拒绝, id={}, currentStatus={}", id, currentStatus);
            return;
        }

        // 状态机推进
        switch (currentStatus) {
            case 1: // 原部门审批中 → 新部门审批中
                application.setStatus(TransferStatusEnum.PENDING_NEW_DEPT.getValue());
                break;
            case 2: // 新部门审批中 → HR备案中
                application.setStatus(TransferStatusEnum.PENDING_HR.getValue());
                break;
            case 3: // HR备案中 → 已通过，执行调岗生效
                application.setStatus(TransferStatusEnum.APPROVED.getValue());
                this.updateById(application);
                this.effectTransfer(id);
                return;
            default:
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前状态不可审批");
        }

        this.updateById(application);
        log.info("调岗审批节点推进, id={}, newStatus={}", id, application.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void effectTransfer(Long id) {
        TransferApplication application = this.getById(id);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR, "调岗申请不存在");
        ThrowUtils.throwIf(!application.getStatus().equals(TransferStatusEnum.APPROVED.getValue()),
                ErrorCode.OPERATION_ERROR, "仅已通过的调岗申请可生效");

        // todo: 获取员工当前信息（原部门、原职位、原职级、原汇报人）
        Long oldDepartmentId = null;
        Long oldPositionId = null;
        String oldJobLevel = null;
        Long oldDirectReportId = null;

        // 1. 记录调岗历史
        TransferHistory history = new TransferHistory();
        history.setEmployeeId(application.getEmployeeId());
        history.setApplicationId(application.getId());
        history.setOldDepartmentId(oldDepartmentId);
        history.setNewDepartmentId(application.getNewDepartmentId());
        history.setOldPositionId(oldPositionId);
        history.setNewPositionId(application.getNewPositionId());
        history.setOldJobLevel(oldJobLevel);
        history.setNewJobLevel(application.getNewJobLevel());
        history.setOldDirectReportId(oldDirectReportId);
        history.setNewDirectReportId(application.getNewDirectReportId());
        history.setTransferDate(new Date());
        transferHistoryMapper.insert(history);

        // 2. 更新员工档案
        // todo: UPDATE employee SET department_id = ?, position_id = ?, job_level = ?, direct_report_id = ? WHERE id = ?
        // todo: 如有薪资调整，更新薪资信息

        log.info("调岗生效完成, id={}, employeeId={}, oldDept={}→newDept={}",
                id, application.getEmployeeId(), oldDepartmentId, application.getNewDepartmentId());
    }

    @Override
    public QueryWrapper<TransferApplication> getQueryWrapper(TransferApplicationQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = queryRequest.getId();
        Long employeeId = queryRequest.getEmployeeId();
        Integer status = queryRequest.getStatus();

        QueryWrapper<TransferApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(employeeId != null, "employee_id", employeeId);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.orderByDesc("create_time");
        return queryWrapper;
    }

    @Override
    public TransferApplicationVO getVO(TransferApplication entity) {
        if (entity == null) {
            return null;
        }
        TransferApplicationVO vo = new TransferApplicationVO();
        BeanUtils.copyProperties(entity, vo);
        // todo: 联表查询填充 employeeName, employeeNo, newDepartmentName, newPositionName, newDirectReportName, createByName
        return vo;
    }

    @Override
    public List<TransferApplicationVO> getVOList(List<TransferApplication> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return new ArrayList<>();
        }
        return entityList.stream().map(this::getVO).collect(Collectors.toList());
    }
}
