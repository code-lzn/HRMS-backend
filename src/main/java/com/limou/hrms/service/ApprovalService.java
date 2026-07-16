package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.ApprovalDetail;
import com.limou.hrms.model.entity.ApprovalRecord;
import com.limou.hrms.model.vo.ApprovalDetailVO;
import com.limou.hrms.model.vo.ApprovalPendingVO;

import java.util.List;

public interface ApprovalService extends IService<ApprovalRecord> {

    List<ApprovalPendingVO> getPendingList(Long employeeId);

    ApprovalDetailVO getApprovalDetail(Long recordId);

    void approve(Long detailId, Long employeeId, String comment);

    void reject(Long detailId, Long employeeId, String comment);

    void transfer(Long detailId, Long employeeId, Long targetEmployeeId, String comment);

    /** 启动审批（默认用申请人所在部门解析 DEPT_MANAGER） */
    ApprovalRecord startApproval(String businessType, Long businessId, Long applicantEmployeeId, String applicantName);

    /** 启动审批（按目标部门解析 DEPT_MANAGER，入职审批专用） */
    ApprovalRecord startApproval(String businessType, Long businessId, Long applicantEmployeeId,
                                  String applicantName, Long targetDepartmentId);

    /** 启动审批（支持按节点序号覆盖审批人，调岗审批专用） */
    ApprovalRecord startApproval(String businessType, Long businessId, Long applicantEmployeeId,
                                  String applicantName, Long targetDepartmentId,
                                  java.util.Map<Integer, Long> nodeApproverOverrides);

    /** 获取审批记录的所有审批明细 */
    List<ApprovalDetail> getApprovalDetails(Long recordId);
}
