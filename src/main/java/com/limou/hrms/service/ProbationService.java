package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.probation.ProbationCreateDTO;
import com.limou.hrms.model.dto.probation.ProbationHandleResultDTO;
import com.limou.hrms.model.dto.probation.ProbationUpdateDTO;
import com.limou.hrms.model.query.ProbationQuery;
import com.limou.hrms.model.vo.ProbationDetailVO;
import com.limou.hrms.model.vo.ProbationListVO;

/**
 * 转正管理服务接口
 */
public interface ProbationService {

    /** 创建转正申请（保存草稿或直接提交审批） */
    Long createApplication(ProbationCreateDTO dto);

    /** 更新草稿 */
    void updateDraft(Long id, ProbationUpdateDTO dto);

    /** 删除草稿 */
    void deleteDraft(Long id);

    /** 提交审批 */
    void submitToApproval(Long id);

    /** 撤回申请 */
    void cancel(Long id);

    /** 处理转正结果（拒绝后HR决定延期/辞退） */
    void handleResult(Long id, ProbationHandleResultDTO dto);

    /** 分页查询列表（角色路由） */
    Page<ProbationListVO> list(ProbationQuery query);

    /** 获取详情（含审批进度） */
    ProbationDetailVO getDetail(Long id);
}
