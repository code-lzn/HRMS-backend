package com.limou.hrms.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.RegularizationApplicationMapper;
import com.limou.hrms.model.dto.regularization.RegularizationApplicationAddRequest;
import com.limou.hrms.model.dto.regularization.RegularizationApplicationQueryRequest;
import com.limou.hrms.model.entity.RegularizationApplication;
import com.limou.hrms.model.enums.ApprovalStatusEnum;
import com.limou.hrms.model.enums.RegularizationResultEnum;
import com.limou.hrms.model.vo.RegularizationApplicationVO;
import com.limou.hrms.service.RegularizationApplicationService;
import com.limou.hrms.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 转正申请服务实现
 */
@Service
@Slf4j
public class RegularizationApplicationServiceImpl
        extends ServiceImpl<RegularizationApplicationMapper, RegularizationApplication>
        implements RegularizationApplicationService {

    @Override
    public Long createApplication(RegularizationApplicationAddRequest addRequest, Long userId) {
        ThrowUtils.throwIf(addRequest.getEmployeeId() == null, ErrorCode.PARAMS_ERROR, "员工ID不能为空");
        ThrowUtils.throwIf(addRequest.getProbationStartDate() == null, ErrorCode.PARAMS_ERROR, "试用期开始日期不能为空");
        ThrowUtils.throwIf(addRequest.getProbationEndDate() == null, ErrorCode.PARAMS_ERROR, "试用期结束日期不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(addRequest.getPerformanceReview()), ErrorCode.PARAMS_ERROR, "表现评价不能为空");

        RegularizationApplication application = new RegularizationApplication();
        BeanUtils.copyProperties(addRequest, application);
        application.setCreateBy(userId);
        application.setStatus(ApprovalStatusEnum.PENDING.getValue());

        boolean saved = this.save(application);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建转正申请失败");
        log.info("转正申请创建成功, id={}, employeeId={}", application.getId(), application.getEmployeeId());
        return application.getId();
    }

    @Override
    public List<RegularizationApplicationVO> getPendingList() {
        // todo: 查询试用期即将到期（7天内）或已到期但尚未发起转正的员工
        // SELECT * FROM employee e
        // WHERE e.status = 1 (试用期)
        // AND e.hire_date + 试用期月数 - 7天 <= 今天
        // AND NOT EXISTS (
        //   SELECT 1 FROM regularization_reminder r
        //   WHERE r.employee_id = e.id AND r.is_processed = 1
        // )
        log.info("查询待转正员工列表");
        return new ArrayList<>();
    }

    @Override
    public void processApprovalResult(Long id, Integer result, String rejectReason) {
        RegularizationApplication application = this.getById(id);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR, "转正申请不存在");
        ThrowUtils.throwIf(!application.getStatus().equals(ApprovalStatusEnum.PENDING.getValue()),
                ErrorCode.OPERATION_ERROR, "仅审批中状态可处理");

        RegularizationResultEnum resultEnum = RegularizationResultEnum.getByValue(result);
        ThrowUtils.throwIf(resultEnum == null, ErrorCode.PARAMS_ERROR, "审批结果无效");

        switch (resultEnum) {
            case PASS:
                application.setStatus(ApprovalStatusEnum.APPROVED.getValue());
                application.setResult(RegularizationResultEnum.PASS.getValue());
                // todo: 更新员工状态为正式（status = 2），更新员工档案
                // todo: 如有薪资调整，更新薪资信息
                log.info("转正通过, id={}, employeeId={}", id, application.getEmployeeId());
                break;
            case EXTEND:
                application.setStatus(ApprovalStatusEnum.APPROVED.getValue());
                application.setResult(RegularizationResultEnum.EXTEND.getValue());
                // todo: 更新员工试用期延长至 extendedProbationDate
                log.info("延长试用期, id={}, extendedTo={}", id, application.getExtendedProbationDate());
                break;
            case FAIL:
                application.setStatus(ApprovalStatusEnum.APPROVED.getValue());
                application.setResult(RegularizationResultEnum.FAIL.getValue());
                // todo: 更新员工状态为辞退
                log.info("转正不通过, id={}, employeeId={}", id, application.getEmployeeId());
                break;
        }

        boolean updated = this.updateById(application);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "处理审批结果失败");
    }

    @Override
    public QueryWrapper<RegularizationApplication> getQueryWrapper(RegularizationApplicationQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = queryRequest.getId();
        Long employeeId = queryRequest.getEmployeeId();
        Integer status = queryRequest.getStatus();
        Integer result = queryRequest.getResult();

        QueryWrapper<RegularizationApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(employeeId != null, "employee_id", employeeId);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.eq(result != null, "result", result);
        queryWrapper.orderByDesc("create_time");
        return queryWrapper;
    }

    @Override
    public RegularizationApplicationVO getVO(RegularizationApplication entity) {
        if (entity == null) {
            return null;
        }
        RegularizationApplicationVO vo = new RegularizationApplicationVO();
        BeanUtils.copyProperties(entity, vo);
        // todo: 联表查询填充 employeeName, employeeNo, departmentName, createByName
        return vo;
    }

    @Override
    public List<RegularizationApplicationVO> getVOList(List<RegularizationApplication> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return new ArrayList<>();
        }
        return entityList.stream().map(this::getVO).collect(Collectors.toList());
    }
}
