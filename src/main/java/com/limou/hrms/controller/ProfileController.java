package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.model.dto.profile.LeaveQueryDTO;
import com.limou.hrms.model.dto.profile.PasswordChangeDTO;
import com.limou.hrms.model.dto.profile.PhoneChangeDTO;
import com.limou.hrms.model.dto.profile.PhoneUnbindDTO;
import com.limou.hrms.model.dto.profile.ProfileUpdateDTO;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.model.vo.salary.PayslipVO;
import com.limou.hrms.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 个人中心 Controller
 */
@RestController
@RequestMapping("/profile")
@Slf4j
public class ProfileController {

    @Resource
    private ProfileService profileService;

    // ==================== 我的档案 ====================

    /**
     * 获取个人档案（敏感字段脱敏，锁定字段标记）
     */
    @GetMapping
    @AuthCheck
    public BaseResponse<ProfileVO> getProfile() {
        User loginUser = UserContext.getCurrentUser();
        ProfileVO vo = profileService.getProfile(loginUser);
        return ResultUtils.success(vo);
    }

    /**
     * 编辑个人档案（仅可编辑白名单字段）
     */
    @PutMapping
    @AuthCheck
    public BaseResponse<Boolean> updateProfile(@RequestBody ProfileUpdateDTO dto) {
        User loginUser = UserContext.getCurrentUser();
        profileService.updateProfile(loginUser, dto);
        return ResultUtils.success(true);
    }

    // ==================== 我的考勤 ====================

    /**
     * 考勤日历视图
     */
    @GetMapping("/attendance")
    @AuthCheck
    public BaseResponse<AttendanceCalendarVO> getAttendanceCalendar(@RequestParam String yearMonth) {
        User loginUser = UserContext.getCurrentUser();
        AttendanceCalendarVO vo = profileService.getAttendanceCalendar(loginUser, yearMonth);
        return ResultUtils.success(vo);
    }

    /**
     * 网页打卡（上班/下班）
     */
    @PostMapping("/attendance/clock")
    @AuthCheck
    public BaseResponse<ClockResultVO> clock(@RequestParam Integer clockType) {
        User loginUser = UserContext.getCurrentUser();
        ClockResultVO vo = profileService.clock(loginUser, clockType);
        return ResultUtils.success(vo);
    }

    // ==================== 我的请假 ====================

    /**
     * 我的请假列表（分页，按状态筛选）
     */
    @GetMapping("/leaves")
    @AuthCheck
    public BaseResponse<Page<LeaveRequestVO>> getMyLeaves(LeaveQueryDTO query) {
        User loginUser = UserContext.getCurrentUser();
        Page<LeaveRequestVO> page = profileService.getMyLeaves(loginUser, query);
        // 查看请假列表后标记通知已读
        profileService.markLeaveNotificationsRead(loginUser);
        return ResultUtils.success(page);
    }

    /**
     * 取消请假申请（仅审批中可取消）
     */
    @PostMapping("/leaves/{id}/cancel")
    @AuthCheck
    public BaseResponse<Boolean> cancelLeave(@PathVariable Long id) {
        User loginUser = UserContext.getCurrentUser();
        profileService.cancelLeave(loginUser, id);
        return ResultUtils.success(true);
    }

    // ==================== 我的薪资 ====================

    /**
     * 工资条列表（按月）
     */
    @GetMapping("/salaries")
    @AuthCheck
    public BaseResponse<List<PayslipListVO>> getMyPayslips() {
        User loginUser = UserContext.getCurrentUser();
        List<PayslipListVO> list = profileService.getMyPayslips(loginUser);
        return ResultUtils.success(list);
    }

    /**
     * 发送工资条验证码（首次查看需二次验证）
     */
    @PostMapping("/salaries/{id}/verify")
    @AuthCheck
    public BaseResponse<Boolean> sendPayslipVerifyCode(@PathVariable Long id) {
        User loginUser = UserContext.getCurrentUser();
        profileService.sendPayslipVerifyCode(loginUser, id);
        return ResultUtils.success(true);
    }

    /**
     * 工资条详情（验证码校验通过后返回完整明细）
     */
    @GetMapping("/salaries/{id}")
    @AuthCheck
    public BaseResponse<PayslipVO> getPayslipDetail(@PathVariable Long id,
                                                     @RequestParam(required = false) String verifyCode) {
        User loginUser = UserContext.getCurrentUser();
        PayslipVO vo = profileService.getPayslipDetail(loginUser, id, verifyCode);
        return ResultUtils.success(vo);
    }

    // ==================== 账号安全 ====================

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @AuthCheck
    public BaseResponse<Boolean> changePassword(@RequestBody PasswordChangeDTO dto) {
        User loginUser = UserContext.getCurrentUser();
        profileService.changePassword(loginUser, dto);
        return ResultUtils.success(true);
    }

    /**
     * 发送手机验证码（修改手机号前调用）
     */
    @PostMapping("/phone/send-code")
    @AuthCheck
    public BaseResponse<Boolean> sendPhoneVerifyCode(@RequestParam String phone) {
        User loginUser = UserContext.getCurrentUser();
        profileService.sendPhoneVerifyCode(loginUser, phone);
        return ResultUtils.success(true);
    }

    /**
     * 修改手机号（绑定/修改，直接生效）
     */
    @PutMapping("/phone")
    @AuthCheck
    public BaseResponse<Boolean> changePhone(@RequestBody PhoneChangeDTO dto) {
        User loginUser = UserContext.getCurrentUser();
        profileService.changePhone(loginUser, dto);
        return ResultUtils.success(true);
    }

    /**
     * 手机号解绑申请（需HR审批后生效）
     */
    @PostMapping("/phone/unbind")
    @AuthCheck
    public BaseResponse<Boolean> submitPhoneUnbind(@RequestBody PhoneUnbindDTO dto) {
        User loginUser = UserContext.getCurrentUser();
        profileService.submitPhoneUnbind(loginUser, dto);
        return ResultUtils.success(true);
    }

    /**
     * 登录日志（最近20条）
     */
    @GetMapping("/login-logs")
    @AuthCheck
    public BaseResponse<List<LoginLogVO>> getLoginLogs() {
        User loginUser = UserContext.getCurrentUser();
        List<LoginLogVO> logs = profileService.getLoginLogs(loginUser);
        return ResultUtils.success(logs);
    }

    // ==================== 聚合 ====================

    /**
     * 获取待办数量（红点角标）
     */
    @GetMapping("/pending-count")
    @AuthCheck
    public BaseResponse<PendingCountVO> getPendingCount() {
        User loginUser = UserContext.getCurrentUser();
        PendingCountVO vo = profileService.getPendingCount(loginUser);
        return ResultUtils.success(vo);
    }

    /**
     * 薪资趋势（近6个月实发工资）
     */
    @GetMapping("/salary-trend")
    @AuthCheck
    public BaseResponse<SalaryTrendVO> getSalaryTrend() {
        User loginUser = UserContext.getCurrentUser();
        SalaryTrendVO vo = profileService.getSalaryTrend(loginUser);
        return ResultUtils.success(vo);
    }
}
