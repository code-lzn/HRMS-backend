package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.employee.MyDetailUpdateRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.EmployeeStatus;
import com.limou.hrms.model.vo.EmployeeChangeLogVO;
import com.limou.hrms.model.vo.MyProfileVO;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 个人中心 - 我的档案 接口
 * <p>
 * 对应需求文档：个人中心 → 我的档案
 * - 用工基础信息（不可编辑）→ employee 表
 * - 个人详细档案（部分可编辑）→ employee_detail 表
 * - 修改记录 → employee_change_log 表
 */
@RestController
@RequestMapping("/employee/profile")
@Slf4j
public class ProfileController {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private UserService userService;

    // ==================== 接口1：获取我的档案详情 ====================

    /**
     * 获取我的档案详情（页面初始化核心接口）
     * <p>
     * 联表查询 employee + employee_detail，根据角色控制：
     * - 普通员工：强制只查自己，脱敏手机/身份证/紧急联系人电话，隐藏薪资银行卡
     * - HR/管理员：可查任意员工，返回完整明文
     *
     * @param employeeId 目标员工ID（非必填，不传默认当前登录员工；HR可传他人ID查看）
     * @param request    HTTP请求（获取登录用户）
     * @return 完整档案信息
     */
    @GetMapping("/getMyProfile")
    public BaseResponse<MyProfileVO> getMyProfile(@RequestParam(required = false) Long employeeId,
                                                   HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean isAdminOrHR = userService.isAdmin(loginUser);

        // 普通员工：强制只能查自己，拦截越权查看
        if (!isAdminOrHR && employeeId != null) {
            Employee loginEmp = employeeService.getByUserId(loginUser.getId());
            if (!loginEmp.getId().equals(employeeId)) {
                throw new BusinessException(ErrorCode.PROFILE_PERMISSION_DENIED);
            }
        }

        MyProfileVO vo = employeeService.getMyFullProfile(loginUser.getId(), employeeId, isAdminOrHR);
        return ResultUtils.success(vo);
    }

    // ==================== 接口2：修改个人可编辑档案信息 ====================

    /**
     * 修改个人可编辑档案信息（仅三个字段）
     * <p>
     * 开放编辑字段：
     * - currentAddress 现居住地址
     * - emergencyContactName 紧急联系人
     * - emergencyContactPhone 紧急联系电话
     * <p>
     * 后端逻辑：
     * 1. 身份校验：employeeId 必须等于当前登录人ID
     * 2. 已离职员工拦截
     * 3. 手机号格式校验
     * 4. 对比新旧值，更新 employee_detail
     * 5. 自动循环写入 employee_change_log
     *
     * @param updateRequest 更新请求体
     * @param request       HTTP请求（获取登录用户）
     * @return 更新时间与员工ID
     */
    @PutMapping("/updateMyDetail")
    public BaseResponse<Map<String, Object>> updateMyDetail(@RequestBody MyDetailUpdateRequest updateRequest,
                                                             HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(updateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(updateRequest.getEmployeeId() == null || updateRequest.getEmployeeId() <= 0,
                ErrorCode.PARAMS_ERROR, "员工ID不能为空");

        User loginUser = userService.getLoginUser(request);

        // 普通员工越权拦截（Controller层二次校验）
        if (!userService.isAdmin(loginUser)) {
            Employee loginEmp = employeeService.getByUserId(loginUser.getId());
            if (!loginEmp.getId().equals(updateRequest.getEmployeeId())) {
                throw new BusinessException(ErrorCode.PROFILE_UPDATE_DENIED);
            }
            // 已离职拦截
            if (EmployeeStatus.RESIGNED.getCode() == loginEmp.getStatus()) {
                throw new BusinessException(ErrorCode.EMPLOYEE_RESIGNED);
            }
        }

        Date updateTime = employeeService.updateMyDetail(loginUser.getId(), updateRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("employeeId", updateRequest.getEmployeeId());
        data.put("updateTime", updateTime);
        return ResultUtils.success(data);
    }

    // ==================== 接口3：查询个人档案修改日志 ====================

    /**
     * 查询个人档案修改日志（分页）
     * <p>
     * 基于 employee_change_log 表，按创建时间倒序
     * - 普通员工：不传 employeeId，默认查自己
     * - HR/管理员：可传入任意 employeeId 查看他人变更历史
     *
     * @param employeeId 目标员工ID（非必填，不传查本人）
     * @param pageNum    页码（必填，从1开始）
     * @param pageSize   每页条数（必填）
     * @param request    HTTP请求（获取登录用户）
     * @return 分页变更日志
     */
    @GetMapping("/log/list")
    public BaseResponse<Page<EmployeeChangeLogVO>> getChangeLogs(@RequestParam(required = false) Long employeeId,
                                                                   @RequestParam Integer pageNum,
                                                                   @RequestParam Integer pageSize,
                                                                   HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(pageNum == null || pageNum <= 0, ErrorCode.PARAMS_ERROR, "页码不能为空且必须大于0");
        ThrowUtils.throwIf(pageSize == null || pageSize <= 0, ErrorCode.PARAMS_ERROR, "每页条数不能为空且必须大于0");

        User loginUser = userService.getLoginUser(request);
        boolean isAdminOrHR = userService.isAdmin(loginUser);

        // 普通员工：强制只查自己
        if (!isAdminOrHR && employeeId != null) {
            Employee loginEmp = employeeService.getByUserId(loginUser.getId());
            if (!loginEmp.getId().equals(employeeId)) {
                throw new BusinessException(ErrorCode.PROFILE_PERMISSION_DENIED);
            }
        }

        Page<EmployeeChangeLogVO> result = employeeService.getMyChangeLogs(
                loginUser.getId(), employeeId, pageNum, pageSize);
        return ResultUtils.success(result);
    }
}
