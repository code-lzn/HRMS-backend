package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.SalChangeLog;
import com.limou.hrms.model.vo.SalaryChangeLogVO;

import java.util.List;

/**
 * 调薪历史服务
 */
public interface SalChangeLogService extends IService<SalChangeLog> {

    /**
     * 查询员工调薪历史
     */
    List<SalaryChangeLogVO> getHistoryByEmployeeId(Long employeeId);

    /**
     * 记录调薪变更
     */
    void recordChange(Long employeeId, Integer changeType, String oldValue,
                      String newValue, Long operatorId, String remark);
}
