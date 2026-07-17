package com.limou.hrms.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.ResignationApplicationMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.ResignationApplication;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.enums.ResignationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * 离职生效定时任务 — 每天01:00扫描待离职员工并执行离职生效
 */
@Component
@Slf4j
public class ResignationExecutionJob {

    @Resource
    private ResignationApplicationMapper resignationMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private UserMapper userMapper;

    /**
     * 每天凌晨 1:00 执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void executeResignation() {
        log.info("===== 离职生效处理开始 =====");
        LocalDate today = LocalDate.now();
        List<ResignationApplication> list = resignationMapper.selectList(
                new QueryWrapper<ResignationApplication>()
                        .eq("status", ResignationStatus.APPROVED.getCode()) // 审批通过待离职
                        .eq("resignation_date", today));

        for (ResignationApplication app : list) {
            try {
                // 员工状态 → 已离职
                Employee employee = employeeMapper.selectById(app.getEmployeeId());
                if (employee != null) {
                    employee.setStatus(EmployeeStatus.RESIGNED.getValue());
                    employeeMapper.updateById(employee);

                    // 禁用系统账号
                    if (employee.getUserId() != null) {
                        User user = userMapper.selectById(employee.getUserId());
                        if (user != null) {
                            user.setIsDelete(1);
                            userMapper.updateById(user);
                            log.info("【离职生效】账号已禁用: userId={}", user.getId());
                        }
                    }
                }

                // 更新离职申请
                app.setStatus(ResignationStatus.RESIGNED.getCode());
                app.setActualResignationDate(today);
                resignationMapper.updateById(app);

                log.info("【离职生效】员工ID={} 已离职", app.getEmployeeId());
            } catch (Exception e) {
                log.error("离职生效处理失败: resignationId={}, employeeId={}",
                        app.getId(), app.getEmployeeId(), e);
            }
        }
        log.info("===== 离职生效处理结束，本次处理 {} 人 =====", list.size());
    }
}
