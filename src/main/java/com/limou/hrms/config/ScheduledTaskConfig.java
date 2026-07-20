package com.limou.hrms.config;

import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmpMutationLogMapper;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalActionEnum;
import com.limou.hrms.model.enums.ApprovalRecordStatusEnum;
import com.limou.hrms.service.ApprovalDetailService;
import com.limou.hrms.service.ApprovalService;
import com.limou.hrms.service.RegularizationService;
import com.limou.hrms.service.ResignationService;
import com.limou.hrms.util.DingTalkUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {

    @Resource
    private RegularizationService regularizationService;
    @Autowired(required = false)
    private ResignationService resignationService;
    @Resource
    private EmpMutationLogMapper empMutationLogMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private ApprovalDetailService approvalDetailService;
    @Resource
    private ApprovalService approvalService;

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.profiles.active}")
    private String profile;

    /**
     * 每30分钟扫描一次：PENDING 超过48小时的审批明细，钉钉告警
     */
//    @Scheduled(cron = "0 */30 * * * ?")
    @Scheduled(cron = "0 */30 * * * ? ")
    public void alertOverdueApprovals() {
        log.info("定时任务: 扫描超时48小时未审批的记录");
        try {
            Date cutoff = new Date(System.currentTimeMillis() - 48 * 60 * 60 * 1000);

            List<ApprovalDetail> overdueDetails = approvalDetailService.lambdaQuery()
                    .eq(ApprovalDetail::getAction, ApprovalActionEnum.PENDING.getValue())
                    .lt(ApprovalDetail::getCreateTime, cutoff)
                    .list();

            if (overdueDetails.isEmpty()) return;

            String host = InetAddress.getLocalHost().getHostAddress();
            String envUrl = host + ":" + serverPort;

            for (ApprovalDetail detail : overdueDetails) {
                try {
                    ApprovalRecord record = approvalService.getById(detail.getRecordId());
                    if (record == null || !ApprovalRecordStatusEnum.APPROVING.getValue().equals(record.getStatus())) {
                        continue;
                    }

                    String msg = String.format(
                            "审批超时提醒\n> 环境: %s\n> 服务: %s\n> 申请人: %s\n> 业务类型: %s\n> 审批节点: %s\n> 已等待: 超过48小时\n> 请尽快处理！",
                            profile, envUrl, record.getApplicantName(),
                            record.getBusinessType(), detail.getNodeName());

                    DingTalkUtil.sendMarkdown("审批超时提醒", msg);
                } catch (Exception e) {
                    log.error("发送审批超时告警失败 detailId={}", detail.getId(), e);
                }
            }
            log.info("审批超时扫描完成，发现 {} 条超时记录", overdueDetails.size());
        } catch (Exception e) {
            log.error("扫描超时审批失败", e);
        }
    }

    /** 每天9:00扫描试用期即将到期员工，提醒HR准备转正 */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkProbationEndingEmployees() {
        log.info("定时任务: 扫描试用期即将到期员工");
        try {
            List<Employee> employees = regularizationService.getProbationEndingEmployees();
            if (!employees.isEmpty()) {
                log.info("发现 {} 名试用期即将到期员工: {}", employees.size(),
                        employees.stream().map(Employee::getEmployeeName).toArray());

                // 查找人力资源中心负责人
                Long hrManagerId = getHrManagerId();
                if (hrManagerId == null) {
                    log.warn("未找到人力资源中心负责人，跳过转正提醒");
                }

                for (Employee emp : employees) {
                    HrRegularization entity = new HrRegularization();
                    entity.setEmployeeId(emp.getId());
                    entity.setOriginHireDate(emp.getHireDate());
                    entity.setStatus("PENDING_ASSESSMENT");
                    entity.setBusinessNo("ZZ" + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date())
                            + "AUTO");
                    regularizationService.save(entity);

                    // 插入异动提醒到人力资源中心负责人的异动记录
                    if (hrManagerId != null) {
                        EmpMutationLog reminder = new EmpMutationLog();
                        reminder.setBusinessType("PROBATION_REMINDER");
                        reminder.setBusinessId(entity.getId());
                        reminder.setEmployeeId(hrManagerId);
                        reminder.setEmployeeName(emp.getEmployeeName());
                        reminder.setDeptId(emp.getDepartmentId());
                        reminder.setEffectDate(entity.getProbationEndDate());
                        reminder.setApprovalStatus("PENDING");
                        reminder.setOperatorId(0L);
                        reminder.setOperatorName("系统");
                        reminder.setCreateTime(new Date());
                        empMutationLogMapper.insert(reminder);
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描试用期到期员工失败", e);
        }
    }

    private Long getHrManagerId() {
        try {
            Department hrDept = departmentMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Department>()
                            .eq(Department::getDeptName, "人力资源中心"));
            if (hrDept != null && hrDept.getManagerId() != null) {
                return hrDept.getManagerId();
            }
        } catch (Exception e) {
            log.warn("查找人力资源中心部门失败", e);
        }
        return null;
    }

    /** 每天0:05处理到达离职日期的员工 */
    @Scheduled(cron = "0 5 0 * * ?")
    public void processDailyResignations() {
        log.info("定时任务: 处理到达离职日期的员工");
        if (resignationService == null) {
            log.warn("ResignationService未初始化，跳过离职处理");
            return;
        }
        try {
            resignationService.processDailyResignations();
        } catch (Exception e) {
            log.error("处理离职日期到达失败", e);
        }
    }
}
