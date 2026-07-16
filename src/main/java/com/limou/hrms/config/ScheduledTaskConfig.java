package com.limou.hrms.config;

import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.HrRegularization;
import com.limou.hrms.service.RegularizationService;
import com.limou.hrms.service.ResignationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {

    @Resource
    private RegularizationService regularizationService;
    @Autowired(required = false)
    private ResignationService resignationService;

    /** 每天9:00扫描试用期即将到期员工，提醒HR准备转正 */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkProbationEndingEmployees() {
        log.info("定时任务: 扫描试用期即将到期员工");
        try {
            List<Employee> employees = regularizationService.getProbationEndingEmployees();
            if (!employees.isEmpty()) {
                log.info("发现 {} 名试用期即将到期员工: {}", employees.size(),
                        employees.stream().map(Employee::getEmployeeName).toArray());
                for (Employee emp : employees) {
                    HrRegularization entity = new HrRegularization();
                    entity.setEmployeeId(emp.getId());
                    entity.setOriginHireDate(emp.getHireDate());
                    entity.setStatus("PENDING_ASSESSMENT");
                    entity.setBusinessNo("ZZ" + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date())
                            + "AUTO");
                    regularizationService.save(entity);
                }
            }
        } catch (Exception e) {
            log.error("扫描试用期到期员工失败", e);
        }
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
