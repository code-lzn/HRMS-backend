package com.limou.hrms.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.ResignationApplicationMapper;
import com.limou.hrms.model.dto.resignation.ResignationApplicationAddRequest;
import com.limou.hrms.model.dto.resignation.ResignationApplicationQueryRequest;
import com.limou.hrms.model.entity.ResignationApplication;
import com.limou.hrms.model.enums.ResignationStatusEnum;
import com.limou.hrms.model.vo.ResignationApplicationVO;
import com.limou.hrms.service.ResignationApplicationService;
import com.limou.hrms.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 离职申请服务实现
 */
@Service
@Slf4j
public class ResignationApplicationServiceImpl
        extends ServiceImpl<ResignationApplicationMapper, ResignationApplication>
        implements ResignationApplicationService {

    @Override
    public Long createApplication(ResignationApplicationAddRequest addRequest, Long userId) {
        ThrowUtils.throwIf(addRequest.getEmployeeId() == null, ErrorCode.PARAMS_ERROR, "员工ID不能为空");
        ThrowUtils.throwIf(addRequest.getLeaveDate() == null, ErrorCode.PARAMS_ERROR, "离职日期不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(addRequest.getLeaveReason()), ErrorCode.PARAMS_ERROR, "离职原因不能为空");
        ThrowUtils.throwIf(addRequest.getLeaveType() == null, ErrorCode.PARAMS_ERROR, "离职类型不能为空");
        ThrowUtils.throwIf(addRequest.getHandoverPersonId() == null, ErrorCode.PARAMS_ERROR, "工作交接人不能为空");

        // 离职日期必须 >= 今天
        Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        ThrowUtils.throwIf(addRequest.getLeaveDate().before(today),
                ErrorCode.PARAMS_ERROR, "离职日期不能早于今天");

        ResignationApplication application = new ResignationApplication();
        BeanUtils.copyProperties(addRequest, application);
        application.setCreateBy(userId);
        application.setStatus(ResignationStatusEnum.PENDING.getValue());

        boolean saved = this.save(application);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建离职申请失败");
        log.info("离职申请创建成功, id={}, employeeId={}, leaveDate={}",
                application.getId(), application.getEmployeeId(), application.getLeaveDate());
        return application.getId();
    }

    @Override
    public void processApprovalResult(Long id, boolean approved, String rejectReason) {
        ResignationApplication application = this.getById(id);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR, "离职申请不存在");
        ThrowUtils.throwIf(!application.getStatus().equals(ResignationStatusEnum.PENDING.getValue()),
                ErrorCode.OPERATION_ERROR, "仅审批中状态可处理");

        if (approved) {
            application.setStatus(ResignationStatusEnum.APPROVED_PENDING_LEAVE.getValue());
            // todo: 更新员工状态为待离职（status = 3）
            log.info("离职申请审批通过, id={}, employeeId={}", id, application.getEmployeeId());
        } else {
            application.setStatus(ResignationStatusEnum.REJECTED.getValue());
            log.info("离职申请被拒绝, id={}, reason={}", id, rejectReason);
        }

        boolean updated = this.updateById(application);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "处理审批结果失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processPendingResignations() {
        log.info("===== 开始处理到期待离职员工 =====");

        // 查询所有状态为"已通过待离职"且离职日期<=今天的申请
        QueryWrapper<ResignationApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", ResignationStatusEnum.APPROVED_PENDING_LEAVE.getValue());
        queryWrapper.le("leave_date", new Date());
        List<ResignationApplication> pendingList = this.list(queryWrapper);

        if (CollUtil.isEmpty(pendingList)) {
            log.info("无到期待离职员工");
            return;
        }

        for (ResignationApplication application : pendingList) {
            try {
                processResignation(application);
            } catch (Exception e) {
                log.error("处理离职生效失败, id={}, employeeId={}", application.getId(), application.getEmployeeId(), e);
            }
        }

        log.info("离职生效处理完成, 处理数量={}", pendingList.size());
    }

    /**
     * 执行离职生效处理
     */
    private void processResignation(ResignationApplication application) {
        Long employeeId = application.getEmployeeId();
        log.info("执行离职生效: employeeId={}, leaveDate={}", employeeId, application.getLeaveDate());

        // 更新申请状态为已离职
        application.setStatus(ResignationStatusEnum.RESIGNED.getValue());
        this.updateById(application);

        // todo: 1. 更新员工状态 status = 4 (已离职)
        // todo: 2. 禁用系统账号 (is_disabled = 1)
        // todo: 3. 标记工号序列可复用
        // todo: 4. 从考勤组移除
        // todo: 5. 计算薪资至离职日期（通知薪资模块）
        // todo: 6. 脱敏敏感字段（身份证、银行卡号）
        // todo: 7. 记录操作日志

        log.info("离职生效处理完成: employeeId={}", employeeId);
    }

    @Override
    public QueryWrapper<ResignationApplication> getQueryWrapper(ResignationApplicationQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = queryRequest.getId();
        Long employeeId = queryRequest.getEmployeeId();
        Integer status = queryRequest.getStatus();
        Integer leaveType = queryRequest.getLeaveType();

        QueryWrapper<ResignationApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(employeeId != null, "employee_id", employeeId);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.eq(leaveType != null, "leave_type", leaveType);
        queryWrapper.orderByDesc("create_time");
        return queryWrapper;
    }

    @Override
    public ResignationApplicationVO getVO(ResignationApplication entity) {
        if (entity == null) {
            return null;
        }
        ResignationApplicationVO vo = new ResignationApplicationVO();
        BeanUtils.copyProperties(entity, vo);
        // todo: 联表查询填充 employeeName, employeeNo, departmentName, handoverPersonName, createByName
        return vo;
    }

    @Override
    public List<ResignationApplicationVO> getVOList(List<ResignationApplication> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return new ArrayList<>();
        }
        return entityList.stream().map(this::getVO).collect(Collectors.toList());
    }
}
