package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.OvertimeRecord;
import com.limou.hrms.model.vo.OvertimeProgressVO;
import com.limou.hrms.model.vo.OvertimeVO;

import java.util.List;

public interface OvertimeService extends IService<OvertimeRecord> {

    OvertimeVO apply(Long userId, String overtimeDate, String startTime, String endTime,
                     Double overtimeHours, Integer overtimeType, String reason);

    List<OvertimeVO> getMyOvertimes(Long userId);

    void cancel(Long requestId, Long userId);

    OvertimeProgressVO getApprovalProgress(Long requestId, Long userId);
}
