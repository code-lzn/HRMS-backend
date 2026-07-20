package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.resignation.ResignationCreateDTO;
import com.limou.hrms.model.dto.resignation.ResignationUpdateDTO;
import com.limou.hrms.model.query.ResignationQuery;
import com.limou.hrms.model.vo.ResignationDetailVO;
import com.limou.hrms.model.vo.ResignationListVO;

public interface ResignationService {
    Long createApplication(ResignationCreateDTO dto);
    void updateDraft(Long id, ResignationUpdateDTO dto);
    void deleteDraft(Long id);
    void submitToApproval(Long id);
    void cancel(Long id);
    void confirmResignation(Long id);
    Page<ResignationListVO> list(ResignationQuery query);
    ResignationDetailVO getDetail(Long id);
}
