package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.SalChangeLogMapper;
import com.limou.hrms.model.entity.SalChangeLog;
import com.limou.hrms.model.enums.ChangeTypeEnum;
import com.limou.hrms.model.vo.SalaryChangeLogVO;
import com.limou.hrms.service.SalChangeLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调薪历史服务实现
 */
@Service
@Slf4j
public class SalChangeLogServiceImpl extends ServiceImpl<SalChangeLogMapper, SalChangeLog>
        implements SalChangeLogService {

    @Override
    public List<SalaryChangeLogVO> getHistoryByEmployeeId(Long employeeId) {
        List<SalChangeLog> logs = this.lambdaQuery()
                .eq(SalChangeLog::getEmployeeId, employeeId)
                .orderByDesc(SalChangeLog::getCreateTime)
                .list();
        if (logs == null) return new ArrayList<>();
        return logs.stream().map(log -> {
            SalaryChangeLogVO vo = new SalaryChangeLogVO();
            vo.setId(log.getId());
            vo.setEmployeeId(log.getEmployeeId());
            vo.setChangeType(log.getChangeType());
            ChangeTypeEnum typeEnum = ChangeTypeEnum.getByValue(log.getChangeType());
            vo.setChangeTypeText(typeEnum != null ? typeEnum.getText() : "");
            vo.setOldValue(log.getOldValue());
            vo.setNewValue(log.getNewValue());
            vo.setEffectiveDate(log.getEffectiveDate());
            vo.setOperatorId(log.getOperatorId());
            vo.setRemark(log.getRemark());
            vo.setCreateTime(log.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void recordChange(Long employeeId, Integer changeType, String oldValue,
                             String newValue, Long operatorId, String remark) {
        SalChangeLog log = new SalChangeLog();
        log.setEmployeeId(employeeId);
        log.setChangeType(changeType);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setEffectiveDate(new Date());
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        log.setCreateTime(new Date());
        this.save(log);
    }
}
