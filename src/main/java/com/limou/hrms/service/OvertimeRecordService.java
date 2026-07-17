package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.overtime.OvertimeRecordCreateDTO;
import com.limou.hrms.model.dto.overtime.OvertimeRecordUpdateDTO;
import com.limou.hrms.model.vo.OvertimeRecordListVO;
import com.limou.hrms.model.vo.OvertimeRecordVO;

import java.time.LocalDate;

/**
 * 加班记录服务
 */
public interface OvertimeRecordService {

    /**
     * 创建加班记录（自动转入调休余额）
     */
    OvertimeRecordVO createOvertimeRecord(OvertimeRecordCreateDTO dto);

    /**
     * 查询加班记录列表（分页，仅HR）
     */
    Page<OvertimeRecordListVO> queryRecords(Long employeeId, LocalDate startDate, LocalDate endDate,
                                             Integer isUsed, int page, int size);

    /**
     * 编辑加班记录（同步调整调休余额）
     */
    OvertimeRecordVO updateOvertimeRecord(Long id, OvertimeRecordUpdateDTO dto);

    /**
     * 删除加班记录（同步扣减调休余额）
     */
    void deleteOvertimeRecord(Long id);
}