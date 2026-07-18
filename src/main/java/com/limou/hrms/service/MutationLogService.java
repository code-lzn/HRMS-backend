package com.limou.hrms.service;

import com.limou.hrms.model.vo.MutationLogVO;

import java.util.List;

public interface MutationLogService {

    /** 获取当前登录员工的所有人事异动记录（含审批详情） */
    List<MutationLogVO> getMyMutationLogs(Long userId);
}
