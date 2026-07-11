package com.limou.hrms.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.CommonConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.OnboardingApplicationMapper;
import com.limou.hrms.model.dto.onboarding.OnboardingApplicationAddRequest;
import com.limou.hrms.model.dto.onboarding.OnboardingApplicationConfirmHireRequest;
import com.limou.hrms.model.dto.onboarding.OnboardingApplicationQueryRequest;
import com.limou.hrms.model.entity.OnboardingApplication;
import com.limou.hrms.model.enums.OnboardingStatusEnum;
import com.limou.hrms.model.vo.OnboardingApplicationVO;
import com.limou.hrms.service.OnboardingApplicationService;
import com.limou.hrms.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 入职申请服务实现
 */
@Service
@Slf4j
public class OnboardingApplicationServiceImpl extends ServiceImpl<OnboardingApplicationMapper, OnboardingApplication>
        implements OnboardingApplicationService {

    @Override
    public Long createApplication(OnboardingApplicationAddRequest addRequest, Long userId) {
        OnboardingApplication application = new OnboardingApplication();
        BeanUtils.copyProperties(addRequest, application);
        application.setCreateBy(userId);

        // 判断是保存草稿还是提交审批
        boolean submit = addRequest.getSubmit() != null && addRequest.getSubmit();
        if (submit) {
            validateRequired(addRequest);
            application.setStatus(OnboardingStatusEnum.PENDING.getValue());
        } else {
            application.setStatus(OnboardingStatusEnum.DRAFT.getValue());
        }

        boolean saved = this.save(application);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建入职申请失败");
        log.info("入职申请创建成功, id={}, status={}", application.getId(), application.getStatus());
        return application.getId();
    }

    @Override
    public void submitForApproval(Long id) {
        OnboardingApplication application = this.getById(id);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR, "入职申请不存在");
        ThrowUtils.throwIf(!application.getStatus().equals(OnboardingStatusEnum.DRAFT.getValue()),
                ErrorCode.OPERATION_ERROR, "仅草稿状态可提交审批");

        // todo: 创建审批流（对接审批中心模块），审批节点：部门负责人 → [HR负责人]（可选二审）
        application.setStatus(OnboardingStatusEnum.PENDING.getValue());
        boolean updated = this.updateById(application);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "提交审批失败");
        log.info("入职申请提交审批成功, id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmHire(Long id, OnboardingApplicationConfirmHireRequest request) {
        OnboardingApplication application = this.getById(id);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR, "入职申请不存在");
        ThrowUtils.throwIf(!application.getStatus().equals(OnboardingStatusEnum.APPROVED.getValue()),
                ErrorCode.OPERATION_ERROR, "仅已批准状态的申请可确认入职");

        Date actualHireDate = request.getActualHireDate();
        ThrowUtils.throwIf(actualHireDate == null, ErrorCode.PARAMS_ERROR, "实际入职日期不能为空");

        // ===== 入职自动处理流程（事务内） =====
        // 1. 生成工号（year + deptCode + seq）
        String employeeNo = generateEmployeeNo(application.getDepartmentId());
        log.info("生成工号: {}", employeeNo);

        // 2. 随机生成账号密码
        String account = application.getPhone();
        String password = RandomUtil.randomString(8);
        log.info("创建系统账号: account={}", account);
        // todo: 密码加密存储 + 写入 employee 表

        // 3. 写入员工档案
        // todo: INSERT INTO employee (employeeNo, account, password, status, hireDate, hireType, ...)
        // todo: INSERT INTO employee_personal_info (...)
        // todo: INSERT INTO employee_work_info (...)
        // todo: INSERT INTO employee_salary_info (...)
        Long employeeId = null; // todo: 从 employee 表获取插入后的ID

        // 4. 更新入职申请
        application.setStatus(OnboardingStatusEnum.ONBOARDED.getValue());
        application.setActualHireDate(actualHireDate);
        application.setEmployeeId(employeeId);
        boolean updated = this.updateById(application);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "确认入职失败");

        // 5. 事务提交后异步发送通知
        // todo: 发送欢迎邮件给候选人
        // todo: 发送确认通知给 HR 和部门负责人
        log.info("确认入职完成, id={}, employeeNo={}", id, employeeNo);
    }

    @Override
    public void abandon(Long id) {
        OnboardingApplication application = this.getById(id);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR, "入职申请不存在");
        ThrowUtils.throwIf(application.getStatus().equals(OnboardingStatusEnum.ONBOARDED.getValue()),
                ErrorCode.OPERATION_ERROR, "已入职的申请不可放弃");

        application.setStatus(OnboardingStatusEnum.ABANDONED.getValue());
        boolean updated = this.updateById(application);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "放弃入职失败");
        log.info("放弃入职, id={}", id);
    }

    @Override
    public QueryWrapper<OnboardingApplication> getQueryWrapper(OnboardingApplicationQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = queryRequest.getId();
        Integer status = queryRequest.getStatus();
        String keyword = queryRequest.getKeyword();
        Long departmentId = queryRequest.getDepartmentId();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        QueryWrapper<OnboardingApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(status != null, "status", status);
        queryWrapper.eq(departmentId != null, "department_id", departmentId);
        // 模糊搜索姓名/手机号
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(w -> w.like("name", keyword).or().like("phone", keyword));
        }
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public OnboardingApplicationVO getVO(OnboardingApplication entity) {
        if (entity == null) {
            return null;
        }
        OnboardingApplicationVO vo = new OnboardingApplicationVO();
        BeanUtils.copyProperties(entity, vo);
        // todo: 联表查询填充 departmentName, positionName, directReportName, createByName
        return vo;
    }

    @Override
    public List<OnboardingApplicationVO> getVOList(List<OnboardingApplication> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return new ArrayList<>();
        }
        return entityList.stream().map(this::getVO).collect(Collectors.toList());
    }

    /**
     * 校验必填字段
     */
    private void validateRequired(OnboardingApplicationAddRequest request) {
        ThrowUtils.throwIf(StringUtils.isBlank(request.getName()), ErrorCode.PARAMS_ERROR, "姓名不能为空");
        ThrowUtils.throwIf(request.getGender() == null, ErrorCode.PARAMS_ERROR, "性别不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(request.getPhone()), ErrorCode.PARAMS_ERROR, "手机号不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(request.getEmail()), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(request.getIdCard()), ErrorCode.PARAMS_ERROR, "身份证号不能为空");
        ThrowUtils.throwIf(request.getExpectedHireDate() == null, ErrorCode.PARAMS_ERROR, "预计入职日期不能为空");
        ThrowUtils.throwIf(request.getDepartmentId() == null, ErrorCode.PARAMS_ERROR, "所属部门不能为空");
        ThrowUtils.throwIf(request.getPositionId() == null, ErrorCode.PARAMS_ERROR, "职位不能为空");
        ThrowUtils.throwIf(request.getHireType() == null, ErrorCode.PARAMS_ERROR, "录用类型不能为空");
    }

    /**
     * 生成员工工号（年 + 部门编码 + 3位序号）
     * todo: 对接员工档案模块的工号生成服务，使用 SELECT ... FOR UPDATE 保证并发安全
     */
    private String generateEmployeeNo(Long departmentId) {
        // 临时实现：年份 + 部门ID后3位 + 随机3位序号
        int year = java.time.LocalDate.now().getYear() % 100;
        String deptCode = String.format("%03d", departmentId % 1000);
        String seq = String.format("%03d", (int) (System.currentTimeMillis() % 1000));
        return String.format("%02d%s%s", year, deptCode, seq);
    }
}
