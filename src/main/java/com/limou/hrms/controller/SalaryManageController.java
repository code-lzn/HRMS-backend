package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.salary.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * 薪资管理接口（HR / 财务 / 管理端）
 *
 * 注意：本 Controller 为新增的管理端薪资模块，
 * 不修改已有的 SalaryController（员工端工资条查看）。
 */
@RestController
@RequestMapping("/salary-manage")
@Slf4j
public class SalaryManageController {

    @Resource private SalAccountService salAccountService;
    @Resource private SalItemService salItemService;
    @Resource private SalaryBizService salaryBizService;
    @Resource private UserService userService;

    // ==================== 1. 薪资账套管理 ====================

    @GetMapping("/accounts")
    public BaseResponse<List<SalaryAccountVO>> listAccounts() {
        return ResultUtils.success(salAccountService.listAccounts());
    }

    @GetMapping("/accounts/{id}")
    public BaseResponse<SalaryAccountVO> getAccountDetail(@PathVariable Long id) {
        return ResultUtils.success(salAccountService.getAccountDetail(id));
    }

    @PostMapping("/accounts")
    public BaseResponse<Long> createAccount(@RequestBody SalaryAccountRequest req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        SalAccount account = new SalAccount();
        account.setName(req.getName());
        account.setScopeType(req.getScopeType());
        account.setScopeIds(req.getScopeIds());
        account.setEffectiveDate(req.getEffectiveDate());
        Long id = salAccountService.createAccount(account);
        // 同时创建工资项目
        if (req.getItems() != null) {
            for (SalaryItemRequest itemReq : req.getItems()) {
                SalItem item = new SalItem();
                item.setAccountId(id);
                item.setName(itemReq.getName());
                item.setItemType(itemReq.getItemType());
                item.setFormula(itemReq.getFormula());
                item.setSortOrder(itemReq.getSortOrder());
                item.setIsTaxable(itemReq.getIsTaxable());
                salItemService.addItem(item);
            }
        }
        return ResultUtils.success(id);
    }

    @PutMapping("/accounts/{id}")
    public BaseResponse<String> updateAccount(@PathVariable Long id, @RequestBody SalaryAccountRequest req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        SalAccount account = new SalAccount();
        account.setId(id);
        account.setName(req.getName());
        account.setScopeType(req.getScopeType());
        account.setScopeIds(req.getScopeIds());
        account.setEffectiveDate(req.getEffectiveDate());
        salAccountService.updateAccount(account);
        return ResultUtils.success("ok");
    }

    @DeleteMapping("/accounts/{id}")
    public BaseResponse<String> deleteAccount(@PathVariable Long id) {
        salAccountService.deleteAccount(id);
        return ResultUtils.success("ok");
    }

    @PostMapping("/accounts/{id}/copy")
    public BaseResponse<Long> copyAccount(@PathVariable Long id) {
        return ResultUtils.success(salAccountService.copyAccount(id));
    }

    // ==================== 2. 工资项目管理 ====================

    @PostMapping("/accounts/{id}/items")
    public BaseResponse<String> addItem(@PathVariable Long id, @RequestBody SalaryItemRequest req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        SalItem item = new SalItem();
        item.setAccountId(id);
        item.setName(req.getName());
        item.setItemType(req.getItemType());
        item.setFormula(req.getFormula());
        item.setSortOrder(req.getSortOrder());
        item.setIsTaxable(req.getIsTaxable());
        salItemService.addItem(item);
        return ResultUtils.success("ok");
    }

    @PutMapping("/items/{itemId}")
    public BaseResponse<String> updateItem(@PathVariable Long itemId, @RequestBody SalaryItemRequest req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        SalItem item = new SalItem();
        item.setId(itemId);
        item.setName(req.getName());
        item.setItemType(req.getItemType());
        item.setFormula(req.getFormula());
        item.setSortOrder(req.getSortOrder());
        item.setIsTaxable(req.getIsTaxable());
        salItemService.updateItem(item);
        return ResultUtils.success("ok");
    }

    @DeleteMapping("/items/{itemId}")
    public BaseResponse<String> deleteItem(@PathVariable Long itemId) {
        salItemService.deleteItem(itemId);
        return ResultUtils.success("ok");
    }

    @PutMapping("/accounts/{id}/items/sort")
    public BaseResponse<String> sortItems(@PathVariable Long id, @RequestBody SalaryItemSortRequest req) {
        ThrowUtils.throwIf(req == null || req.getItemIds() == null, ErrorCode.PARAMS_ERROR);
        salItemService.sortItems(id, req.getItemIds());
        return ResultUtils.success("ok");
    }

    // ==================== 3. 员工薪资档案管理 ====================

    @GetMapping("/employee-salaries/{employeeId}")
    public BaseResponse<EmployeeSalaryVO> getEmployeeSalary(@PathVariable Long employeeId) {
        return ResultUtils.success(salaryBizService.getEmployeeSalary(employeeId));
    }

    @PutMapping("/employee-salaries/{employeeId}")
    public BaseResponse<String> updateEmployeeSalary(@PathVariable Long employeeId,
                                                      @RequestBody EmployeeSalaryUpdateRequest req,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);

        EmpSalaryProfile profile = new EmpSalaryProfile();
        profile.setAccountSetId(req.getAccountSetId());
        profile.setBaseSalary(req.getBaseSalary());
        profile.setAllowanceBase(req.getAllowanceBase());
        profile.setSocialInsuranceBase(req.getSocialInsuranceBase());
        profile.setHousingFundBase(req.getHousingFundBase());
        profile.setPerformanceBase(req.getPerformanceBase());
        profile.setProbationSalaryRatio(req.getProbationSalaryRatio());
        profile.setEffectiveDate(req.getEffectiveDate());

        salaryBizService.updateEmployeeSalary(employeeId, profile, loginUser.getId());
        return ResultUtils.success("ok");
    }

    @GetMapping("/employee-salaries/{employeeId}/history")
    public BaseResponse<List<SalaryChangeLogVO>> getEmployeeSalaryHistory(@PathVariable Long employeeId) {
        return ResultUtils.success(salaryBizService.getEmployeeSalaryHistory(employeeId));
    }

    // ==================== 4. 月度薪资核算 ====================

    @PostMapping("/batches")
    public BaseResponse<SalaryBatchVO> createBatch(@RequestBody SalaryBatchCreateRequest req,
                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(salaryBizService.createBatch(req.getSalaryMonth(), loginUser.getId()));
    }

    @GetMapping("/batches")
    public BaseResponse<List<SalaryBatchVO>> listBatches() {
        return ResultUtils.success(salaryBizService.listBatches());
    }

    @GetMapping("/batches/{id}")
    public BaseResponse<SalaryBatchVO> getBatchDetail(@PathVariable Long id) {
        return ResultUtils.success(salaryBizService.getBatchDetail(id));
    }

    @PostMapping("/batches/{id}/calculate")
    public BaseResponse<String> calculateBatch(@PathVariable Long id) {
        salaryBizService.calculateBatch(id);
        return ResultUtils.success("计算完成");
    }

    @GetMapping("/batches/{id}/preview")
    public BaseResponse<SalaryBatchPreviewVO> previewBatch(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "1") long current,
                                                            @RequestParam(defaultValue = "20") long size) {
        return ResultUtils.success(salaryBizService.previewBatch(id, current, size));
    }

    @GetMapping("/batches/{id}/anomalies")
    public BaseResponse<List<SalaryDetailVO>> getAnomalies(@PathVariable Long id) {
        return ResultUtils.success(salaryBizService.getAnomalies(id));
    }

    @PutMapping("/batches/{id}/adjust")
    public BaseResponse<String> adjustDetail(@PathVariable Long id,
                                              @RequestBody SalaryBatchAdjustRequest req,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        salaryBizService.adjustDetail(id, req.getEmployeeId(), req.getManualAdjust(),
                req.getAdjustReason(), loginUser.getId());
        return ResultUtils.success("ok");
    }

    @PostMapping("/batches/{id}/submit")
    public BaseResponse<String> submitForApproval(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        salaryBizService.submitForApproval(id, loginUser.getId());
        return ResultUtils.success("ok");
    }

    // ==================== 5. 审批操作 ====================

    @PostMapping("/batches/{id}/approve")
    public BaseResponse<String> approveBatch(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        salaryBizService.approveBatch(id, loginUser.getId());
        return ResultUtils.success("ok");
    }

    @PostMapping("/batches/{id}/reject")
    public BaseResponse<String> rejectBatch(@PathVariable Long id,
                                             @RequestBody SalaryBatchRejectRequest req,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        salaryBizService.rejectBatch(id, req.getReason(), loginUser.getId());
        return ResultUtils.success("ok");
    }

    @PostMapping("/batches/{id}/mark-paid")
    public BaseResponse<String> markPaid(@PathVariable Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        salaryBizService.markPaid(id, loginUser.getId());
        return ResultUtils.success("ok");
    }
}
