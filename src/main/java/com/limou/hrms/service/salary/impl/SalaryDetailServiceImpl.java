package com.limou.hrms.service.salary.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.salary.PayslipVerifyRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.BatchStatusEnum;
import com.limou.hrms.model.vo.salary.PayslipVO;
import com.limou.hrms.model.vo.salary.SalaryItemDetailVO;
import com.limou.hrms.service.salary.SalaryDetailService;
import cn.hutool.json.JSONUtil;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * 薪资明细（工资条）服务实现
 */
@Service
@Slf4j
public class SalaryDetailServiceImpl extends ServiceImpl<SalaryDetailMapper, SalaryDetail>
        implements SalaryDetailService {

    /** 验证码 Redis Key：salary:verify:code:{employeeId}:{detailId} */
    private static final String VERIFY_CODE_KEY = "salary:verify:code:%d:%d";
    /** 发送频率限制 Key：salary:verify:limit:{employeeId}:{detailId} */
    private static final String VERIFY_LIMIT_KEY = "salary:verify:limit:%d:%d";
    /** 失败次数 Key：salary:verify:fail:{employeeId}:{detailId} */
    private static final String VERIFY_FAIL_KEY = "salary:verify:fail:%d:%d";
    /** 已验证标记 Key：salary:verify:viewed:{employeeId}:{detailId} */
    private static final String VIEWED_KEY = "salary:verify:viewed:%d:%d";

    /** 验证码有效期 */
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    /** 发送间隔限制 */
    private static final Duration LIMIT_TTL = Duration.ofSeconds(60);
    /** 失败锁定时间 */
    private static final Duration FAIL_LOCK_TTL = Duration.ofMinutes(15);
    /** 已验证标记有效期（= session 有效期，默认8小时） */
    private static final Duration VIEWED_TTL = Duration.ofHours(8);
    /** 最大失败次数 */
    private static final int MAX_FAILURES = 3;

    private static final String SALT = "limou";

    @Resource
    private SalaryBatchMapper salaryBatchMapper;

    @Resource
    private EmployeePersonalInfoMapper employeePersonalInfoMapper;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final SecureRandom random = new SecureRandom();

    // ==================== 发送验证码 ====================

    @Override
    public void sendPayslipVerifyCode(Long employeeId, Long detailId) {
        // ① 校验工资条存在且属于当前员工
        SalaryDetail detail = validatePayslipOwnership(employeeId, detailId);

        // ② 检查是否已通过验证（本次登录期间）
        if (isPayslipVerified(employeeId, detailId)) {
            log.info("工资条 {} 已通过验证，无需重复发送", detailId);
            return;
        }

        // ③ 频率限制：1 分钟内不可重复发送
        String limitKey = String.format(VERIFY_LIMIT_KEY, employeeId, detailId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_TOO_FREQUENT);
        }

        // ④ 检查是否被锁定（失败3次后锁定15分钟）
        String failKey = String.format(VERIFY_FAIL_KEY, employeeId, detailId);
        String failCount = stringRedisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= MAX_FAILURES) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_LIMIT_EXCEEDED);
        }

        // ⑤ 生成 6 位数字验证码
        String code = String.format("%06d", random.nextInt(1_000_000));

        // ⑥ 存入 Redis（5 分钟有效）
        String codeKey = String.format(VERIFY_CODE_KEY, employeeId, detailId);
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_TTL);

        // ⑦ 设置发送频率限制（60 秒）
        stringRedisTemplate.opsForValue().set(limitKey, "1", LIMIT_TTL);

        // ⑧ 获取员工手机号用于日志
        EmployeePersonalInfo personalInfo = employeePersonalInfoMapper.selectOne(
                new QueryWrapper<EmployeePersonalInfo>().eq("employee_id", employeeId));
        String maskedPhone = personalInfo != null ? personalInfo.getPhone() : "未知";
        if (maskedPhone != null && maskedPhone.length() > 4) {
            maskedPhone = "尾号" + maskedPhone.substring(maskedPhone.length() - 4);
        }

        log.info("【验证码】员工 {} 工资条 {} → {}，验证码: {}（5分钟有效，查看日志获取）",
                employeeId, detailId, maskedPhone, code);
    }

    // ==================== 验证码校验 ====================

    @Override
    public boolean verifyPayslip(Long employeeId, Long detailId, PayslipVerifyRequest request) {
        if (request == null || request.getVerifyType() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 降级方案：密码验证
        if (request.getVerifyType() == 2) {
            if (StringUtils.isBlank(request.getVerifyCode())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入登录密码");
            }
            // 查找员工关联的 user
            Employee employee = employeeMapper.selectById(employeeId);
            if (employee == null || employee.getUserId() == null) {
                throw new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND);
            }
            User user = userMapper.selectById(employee.getUserId());
            if (user == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            }
            // 比对密码
            String encrypted = DigestUtils.md5DigestAsHex((SALT + request.getVerifyCode()).getBytes());
            if (!user.getUserPassword().equals(encrypted)) {
                throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR, "登录密码错误");
            }
            log.info("员工 {} 使用密码验证工资条 {} 通过", employeeId, detailId);
            markAsVerified(employeeId, detailId);
            return true;
        }

        // 短信验证码校验
        if (request.getVerifyType() == 1) {
            if (StringUtils.isBlank(request.getVerifyCode())) {
                throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
            }

            String codeKey = String.format(VERIFY_CODE_KEY, employeeId, detailId);
            String storedCode = stringRedisTemplate.opsForValue().get(codeKey);

            if (storedCode == null) {
                throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
            }

            if (!storedCode.equals(request.getVerifyCode())) {
                // 记录失败次数
                String failKey = String.format(VERIFY_FAIL_KEY, employeeId, detailId);
                Long fails = stringRedisTemplate.opsForValue().increment(failKey);
                stringRedisTemplate.expire(failKey, FAIL_LOCK_TTL);
                int remaining = MAX_FAILURES - fails.intValue();
                if (remaining <= 0) {
                    throw new BusinessException(ErrorCode.VERIFY_CODE_LIMIT_EXCEEDED);
                }
                throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR,
                        "验证码错误，还剩" + remaining + "次机会");
            }

            // 验证通过：清除验证码和限制，标记已查看
            markAsVerified(employeeId, detailId);
            return true;
        }

        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的验证类型");
    }

    // ==================== 查看详情 ====================

    @Override
    public PayslipVO getPayslipDetail(Long employeeId, Long detailId) {
        // ① 校验归属
        SalaryDetail detail = validatePayslipOwnership(employeeId, detailId);

        // ② 检查批次状态
        SalaryBatch batch = salaryBatchMapper.selectById(detail.getBatchId());
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "批次不存在");
        }
        if (batch.getStatus() < BatchStatusEnum.APPROVED.getValue()) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "工资条尚未审批通过");
        }

        // ③ 二次验证：必须已验证过才能查看详情
        if (!isPayslipVerified(employeeId, detailId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请先完成二次验证");
        }

        // ④ 更新数据库查看状态
        if (detail.getPayslipViewed() == null || detail.getPayslipViewed() == 0) {
            detail.setPayslipViewed(2); // 已查看
            this.updateById(detail);
        }

        return toPayslipVO(detail, batch);
    }

    // ==================== 已验证标记 ====================

    @Override
    public boolean isPayslipVerified(Long employeeId, Long detailId) {
        String viewedKey = String.format(VIEWED_KEY, employeeId, detailId);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(viewedKey));
    }

    // ==================== 列表 ====================

    @Override
    public List<PayslipVO> getMyPayslips(Long employeeId) {
        QueryWrapper<SalaryBatch> batchWrapper = new QueryWrapper<>();
        batchWrapper.in("status", BatchStatusEnum.APPROVED.getValue(), BatchStatusEnum.PAID.getValue());
        List<SalaryBatch> batches = salaryBatchMapper.selectList(batchWrapper);
        if (batches.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> batchIds = batches.stream().map(SalaryBatch::getId).collect(Collectors.toList());

        QueryWrapper<SalaryDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId)
                .in("batch_id", batchIds)
                .orderByDesc("create_time");
        List<SalaryDetail> details = this.list(wrapper);

        // 查询员工信息（姓名、工号、部门）
        EmployeePersonalInfo personalInfo = employeePersonalInfoMapper.selectOne(
                new QueryWrapper<EmployeePersonalInfo>().eq("employee_id", employeeId));

        return details.stream().map(detail -> {
            PayslipVO vo = toPayslipVO(detail, findBatch(batches, detail.getBatchId()));
            if (personalInfo != null) {
                vo.setEmployeeName(personalInfo.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 校验工资条存在且属于当前员工
     */
    private SalaryDetail validatePayslipOwnership(Long employeeId, Long detailId) {
        SalaryDetail detail = this.getById(detailId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.PAYSLIP_NOT_FOUND);
        }
        if (!detail.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看他人工资条");
        }
        return detail;
    }

    /**
     * 验证通过后清除验证码和限制，标记已查看
     */
    private void markAsVerified(Long employeeId, Long detailId) {
        stringRedisTemplate.delete(String.format(VERIFY_CODE_KEY, employeeId, detailId));
        stringRedisTemplate.delete(String.format(VERIFY_LIMIT_KEY, employeeId, detailId));
        stringRedisTemplate.delete(String.format(VERIFY_FAIL_KEY, employeeId, detailId));
        stringRedisTemplate.opsForValue().set(
                String.format(VIEWED_KEY, employeeId, detailId), "true", VIEWED_TTL);
        log.info("工资条 {} 验证通过，标记已查看（{}小时有效）", detailId, VIEWED_TTL.toHours());
    }

    private PayslipVO toPayslipVO(SalaryDetail detail, SalaryBatch batch) {
        PayslipVO vo = new PayslipVO();
        vo.setId(detail.getId());
        vo.setBatchNo(batch != null ? batch.getBatchNo() : "");
        vo.setSalaryMonth(batch != null ? batch.getSalaryMonth() : "");
        vo.setGrossPay(detail.getGrossPay());
        vo.setTotalDeductions(detail.getTotalDeductions());
        vo.setNetPay(detail.getNetPay());
        vo.setPayslipViewed(detail.getPayslipViewed());
        vo.setCreateTime(detail.getCreateTime());

        // 解析工资项目明细
        if (StringUtils.isNotBlank(detail.getSalaryItems())) {
            List<SalaryItemDetailVO> allItems = JSONUtil.toList(detail.getSalaryItems(), SalaryItemDetailVO.class);
            vo.setIncomeItems(allItems.stream()
                    .filter(i -> i.getType() != null && i.getType() <= 2)
                    .collect(Collectors.toList()));
            vo.setDeductionItems(allItems.stream()
                    .filter(i -> i.getType() != null && i.getType() >= 3)
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private SalaryBatch findBatch(List<SalaryBatch> batches, Long batchId) {
        return batches.stream().filter(b -> b.getId().equals(batchId)).findFirst().orElse(null);
    }
}
