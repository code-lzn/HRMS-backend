package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.model.dto.profile.LeaveQueryDTO;
import com.limou.hrms.model.dto.profile.PasswordChangeDTO;
import com.limou.hrms.model.dto.profile.PhoneChangeDTO;
import com.limou.hrms.model.dto.profile.PhoneUnbindDTO;
import com.limou.hrms.model.dto.profile.ProfileUpdateDTO;
import com.limou.hrms.model.dto.salary.PayslipVerifyRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.model.vo.salary.PayslipVO;

import java.util.List;

/**
 * 个人中心聚合服务
 */
public interface ProfileService {

    // ==================== 我的档案 ====================

    /**
     * 获取个人档案（敏感字段脱敏，锁定字段标记）
     */
    ProfileVO getProfile(User loginUser);

    /**
     * 编辑个人档案（白名单字段更新）
     */
    void updateProfile(User loginUser, ProfileUpdateDTO dto);

    // ==================== 我的考勤 ====================

    /**
     * 考勤日历视图
     */
    AttendanceCalendarVO getAttendanceCalendar(User loginUser, String yearMonth);

    /**
     * 网页打卡
     */
    ClockResultVO clock(User loginUser, Integer clockType);

    // ==================== 我的请假 ====================

    /**
     * 我的请假列表（分页）
     */
    Page<LeaveRequestVO> getMyLeaves(User loginUser, LeaveQueryDTO query);

    /**
     * 取消请假申请（仅审批中可取消）
     */
    void cancelLeave(User loginUser, Long leaveId);

    // ==================== 我的补卡 ====================

    /**
     * 我的补卡记录（强制只看当前用户）
     */
    Page<SupplementCardListVO> getMySupplementCards(User loginUser, int page, int size);

    // ==================== 我的薪资 ====================

    /**
     * 工资条列表
     */
    List<PayslipListVO> getMyPayslips(User loginUser);

    /**
     * 发送工资条验证码
     */
    void sendPayslipVerifyCode(User loginUser, Long salaryId);

    /**
     * 工资条详情（验证码校验）
     */
    PayslipVO getPayslipDetail(User loginUser, Long salaryId, String verifyCode);

    // ==================== 账号安全 ====================

    /**
     * 修改密码
     */
    void changePassword(User loginUser, PasswordChangeDTO dto);

    /**
     * 首次登录强制重置密码（无需旧密码），重置成功后清除 pwdReset 标记
     */
    void resetPassword(User loginUser, String newPassword, String confirmPassword);

    /**
     * 发送手机验证码（修改手机号前调用）
     */
    void sendPhoneVerifyCode(User loginUser, String phone);

    /**
     * 修改手机号（需先调用 sendPhoneVerifyCode 获取验证码）
     */
    void changePhone(User loginUser, PhoneChangeDTO dto);

    /**
     * 手机号解绑申请（验证新手机号后提交审批，审批通过后生效）
     */
    void submitPhoneUnbind(User loginUser, PhoneUnbindDTO dto);

    /**
     * 登录日志（最近20条）
     */
    List<LoginLogVO> getLoginLogs(User loginUser);

    // ==================== 聚合 ====================

    /**
     * 获取待办数量
     */
    PendingCountVO getPendingCount(User loginUser);

    /**
     * 薪资趋势（近6个月）
     */
    SalaryTrendVO getSalaryTrend(User loginUser);

    /**
     * 标记请假通知已读（查看请假列表后调用，避免红点重复计数）
     */
    void markLeaveNotificationsRead(User loginUser);
}
