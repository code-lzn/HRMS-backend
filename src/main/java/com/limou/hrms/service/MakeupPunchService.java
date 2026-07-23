package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.MakeupPunch;
import com.limou.hrms.model.vo.MakeupPunchProgressVO;
import com.limou.hrms.model.vo.MakeupPunchVO;

import java.util.List;

/**
 * 补卡服务
 */
public interface MakeupPunchService extends IService<MakeupPunch> {

    /**
     * 申请补卡
     */
    MakeupPunchVO apply(Long userId, String punchDate, Integer punchType, String punchTime, String reason);

    /**
     * 审批补卡
     */
//    MakeupPunchVO approve(Long requestId, Integer result, String comment, Long approverId);

    /**
     * 获取我的补卡记录
     */
    List<MakeupPunchVO> getMyMakeupPunches(Long userId);

    /**
     * 获取补卡审批进度
     */
    MakeupPunchProgressVO getApprovalProgress(Long requestId, Long userId);

    /**
     * 撤回补卡申请
     */
    void cancel(Long requestId, Long userId);
    /**
     * 删除补卡申请（仅已撤回/已拒绝）
     */
    void delete(Long requestId, Long userId);
}
