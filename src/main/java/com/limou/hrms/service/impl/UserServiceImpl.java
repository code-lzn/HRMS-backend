package com.limou.hrms.service.impl;

import static com.limou.hrms.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.CommonConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.RoleMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.dto.user.UserQueryRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.Role;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.enums.UserRoleEnum;
import com.limou.hrms.model.vo.LoginUserVO;
import com.limou.hrms.model.vo.UserVO;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.LoginLogService;
import com.limou.hrms.service.PasswordHistoryService;
import com.limou.hrms.service.PermissionService;
import com.limou.hrms.service.UserService;
import com.limou.hrms.utils.SqlUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * 用户服务实现

 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "limou";

    @Resource
    private PasswordHistoryService passwordHistoryService;

    @Resource
    private LoginLogService loginLogService;

    @Resource
    private EmployeeService employeeService;

    @Resource
    private RoleMapper roleMapper;

    @Lazy
    @Resource
    private PermissionService permissionService;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            // 尝试根据账号查询用户以记录登录失败日志
            User existUser = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
            if (existUser != null) {
                loginLogService.recordLoginLog(existUser.getId(),
                        request.getRemoteAddr(), request.getHeader("User-Agent"),
                        1, false, "密码错误");
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 记录登录成功日志
        loginLogService.recordLoginLog(user.getId(),
                request.getRemoteAddr(), request.getHeader("User-Agent"),
                1, true, null);
        return this.getLoginUserVO(user);
    }

//    @Override
//    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
//        String unionId = wxOAuth2UserInfo.getUnionId();
//        String mpOpenId = wxOAuth2UserInfo.getOpenid();
//        // 单机锁
//        synchronized (unionId.intern()) {
//            // 查询用户是否已存在
//            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//            queryWrapper.eq("unionId", unionId);
//            User user = this.getOne(queryWrapper);
//            // 被封号，禁止登录
//            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
//                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
//            }
//            // 用户不存在则创建
//            if (user == null) {
//                user = new User();
//                user.setUnionId(unionId);
//                user.setMpOpenId(mpOpenId);
//                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
//                user.setUserName(wxOAuth2UserInfo.getNickname());
//                boolean result = this.save(user);
//                if (!result) {
//                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
//                }
//            }
//            // 记录用户的登录态
//            request.getSession().setAttribute(USER_LOGIN_STATE, user);
//            return getLoginUserVO(user);
//        }
//    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        // 旧版：userRole 字段为 admin
        if (UserRoleEnum.ADMIN.getValue().equals(user.getUserRole())) {
            return true;
        }
        // 新版 RBAC：通过 roleId 关联的角色拥有管理员等效权限
        if (user.getRoleId() != null) {
            return permissionService.hasPermission(user.getId(), "*:*:*") ||
                   permissionService.hasPermission(user.getId(), "role:manage");
        }
        return false;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                userVO.setUserRoleName(role.getRoleName());
            }
        }
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        // 批量查询角色，避免 N+1
        Set<Long> roleIds = userList.stream()
                .map(User::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Role> roleMap;
        if (CollUtil.isNotEmpty(roleIds)) {
            roleMap = roleMapper.selectBatchIds(roleIds).stream()
                    .collect(Collectors.toMap(Role::getId, Function.identity()));
        } else {
            roleMap = new java.util.HashMap<>();
        }

        return userList.stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            if (user.getRoleId() != null) {
                Role role = roleMap.get(user.getRoleId());
                if (role != null) {
                    userVO.setUserRoleName(role.getRoleName());
                }
            }
            return userVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        if (StringUtils.isAnyBlank(oldPassword, newPassword, confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能少于8位");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 验证旧密码
        String oldHash = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes());
        if (!Objects.equals(user.getUserPassword(), oldHash)) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 新密码不能与旧密码相同
        String newHash = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes());
        if (Objects.equals(user.getUserPassword(), newHash)) {
            throw new BusinessException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }

        // 检查是否在最近3次密码历史中
        if (passwordHistoryService.isRecentlyUsed(userId, newHash)) {
            throw new BusinessException(ErrorCode.PASSWORD_RECENTLY_USED);
        }

        // 保存旧密码到历史
        passwordHistoryService.savePasswordHistory(userId, user.getUserPassword());

        // 更新密码
        user.setUserPassword(newHash);
        boolean updated = this.updateById(user);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "密码修改失败");
        }
    }

    @Override
    public void updatePhone(Long userId, String phone) {
        Employee emp = employeeService.lambdaQuery().eq(Employee::getUserId, userId).one();
        if (emp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
        }

        if (StringUtils.isBlank(phone)) {
            // 解绑手机
            emp.setPhone(null);
            employeeService.updateById(emp);
            return;
        }

        // 校验手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }

        // 检查手机号是否已被其他员工绑定
        Employee existing = employeeService.lambdaQuery()
                .eq(Employee::getPhone, phone)
                .ne(Employee::getId, emp.getId())
                .one();
        if (existing != null) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_BOUND);
        }

        emp.setPhone(phone);
        employeeService.updateById(emp);
    }
}
