package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.dto.salary.SalaryAccountAddRequest;
import com.limou.hrms.model.dto.salary.SalaryAccountQueryRequest;
import com.limou.hrms.model.dto.salary.SalaryAccountUpdateRequest;
import com.limou.hrms.model.dto.salary.SalaryItemAddRequest;
import com.limou.hrms.model.dto.salary.SalaryItemSortRequest;
import com.limou.hrms.model.dto.salary.SalaryItemUpdateRequest;
import com.limou.hrms.model.vo.salary.SalaryAccountVO;
import com.limou.hrms.model.vo.salary.SalaryItemVO;
import com.limou.hrms.service.salary.SalaryAccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪资账套管理 Controller
 */
@Api(tags = "薪资账套管理")
@RestController
@RequestMapping("/v1/salary-accounts")
@Slf4j
public class SalaryAccountController {

    @Resource
    private SalaryAccountService salaryAccountService;

    @ApiOperation("查询账套列表")
    @GetMapping
    public BaseResponse<List<SalaryAccountVO>> listAccounts(SalaryAccountQueryRequest request) {
        if (request == null) {
            request = new SalaryAccountQueryRequest();
        }
        List<SalaryAccountVO> list = salaryAccountService.listAccounts(request);
        return ResultUtils.success(list);
    }

    @ApiOperation("查询账套详情（含工资项目列表）")
    @GetMapping("/{id}")
    public BaseResponse<SalaryAccountVO> getAccount(@ApiParam("账套ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SalaryAccountVO vo = salaryAccountService.getAccountDetail(id);
        return ResultUtils.success(vo);
    }

    @ApiOperation("新建账套")
    @PostMapping
    public BaseResponse<Long> createAccount(@RequestBody SalaryAccountAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = salaryAccountService.createAccount(request);
        return ResultUtils.success(id);
    }

    @ApiOperation("编辑账套")
    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateAccount(
            @ApiParam("账套ID") @PathVariable Long id,
            @RequestBody SalaryAccountUpdateRequest request) {
        if (request == null || id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        request.setId(id);
        salaryAccountService.updateAccount(request);
        return ResultUtils.success(true);
    }

    @ApiOperation("删除账套")
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteAccount(@ApiParam("账套ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryAccountService.deleteAccount(id);
        return ResultUtils.success(true);
    }

    // ==================== 工资项目管理 ====================

    @ApiOperation("获取账套下的工资项目列表")
    @GetMapping("/{id}/items")
    public BaseResponse<List<SalaryItemVO>> listItems(@ApiParam("账套ID") @PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<SalaryItemVO> items = salaryAccountService.getSalaryItems(id);
        return ResultUtils.success(items);
    }

    @ApiOperation("添加工资项目")
    @PostMapping("/{id}/items")
    public BaseResponse<Long> addItem(
            @ApiParam("账套ID") @PathVariable Long id,
            @RequestBody SalaryItemAddRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long itemId = salaryAccountService.addSalaryItem(id, request);
        return ResultUtils.success(itemId);
    }

    @ApiOperation("编辑工资项目")
    @PutMapping("/items/{itemId}")
    public BaseResponse<Boolean> updateItem(
            @ApiParam("工资项目ID") @PathVariable Long itemId,
            @RequestBody SalaryItemUpdateRequest request) {
        if (request == null || itemId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        request.setId(itemId);
        salaryAccountService.updateSalaryItem(request);
        return ResultUtils.success(true);
    }

    @ApiOperation("删除工资项目")
    @DeleteMapping("/items/{itemId}")
    public BaseResponse<Boolean> deleteItem(@ApiParam("工资项目ID") @PathVariable Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryAccountService.deleteSalaryItem(itemId);
        return ResultUtils.success(true);
    }

    @ApiOperation("调整工资项目排序")
    @PutMapping("/{id}/items/sort")
    public BaseResponse<Boolean> sortItems(
            @ApiParam("账套ID") @PathVariable Long id,
            @RequestBody SalaryItemSortRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        salaryAccountService.sortSalaryItems(id, request);
        return ResultUtils.success(true);
    }
}
