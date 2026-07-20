package com.limou.hrms.service.impl;

import static com.limou.hrms.constant.UserConstant.USER_LOGIN_STATE;

import com.limou.hrms.constant.UserConstant;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.CommonConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.LoginLogMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.dto.user.UserQueryRequest;
import com.limou.hrms.model.entity.LoginLog;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.model.enums.UserRoleEnum;
import com.limou.hrms.model.vo.LoginUserVO;
import com.limou.hrms.model.vo.UserVO;
import com.limou.hrms.service.UserService;
import com.limou.hrms.utils.SqlUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private LoginLogMapper loginLogMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private com.limou.hrms.utils.JwtUtils jwtUtils;

    /** 密码版本号 Redis Key */
    public static final String PWD_VERSION_KEY = "pwd:version:%d";
    /** 密码版本号 session 属性名 */
    public static final String PWD_VERSION_ATTR = "PASSWORD_VERSION";

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
            queryWrapper.lambda().eq(User::getUserAccount, userAccount);
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
            user.setUserRole(UserConstant.DEFAULT_ROLE);
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
        queryWrapper.lambda().eq(User::getUserAccount, userAccount);
        queryWrapper.lambda().eq(User::getUserPassword, encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            // 记录登录失败日志（通过 userAccount 查找 userId）
            recordLoginLog(userAccount, request, 0);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        HttpSession session = request.getSession();
        session.setAttribute(USER_LOGIN_STATE, user);
        // 写入密码版本号（用于密码修改后强制下线）
        String versionKey = String.format(PWD_VERSION_KEY, user.getId());
        String version = stringRedisTemplate.opsForValue().get(versionKey);
        if (version == null) {
            version = "1";
            stringRedisTemplate.opsForValue().set(versionKey, version,
                    Duration.ofSeconds(session.getMaxInactiveInterval()));
        }
        session.setAttribute(PWD_VERSION_ATTR, version);
        // 记录登录成功日志
        recordLoginLog(user.getId(), request, 1);
        LoginUserVO loginUserVO = this.getLoginUserVO(user);
        // 生成 JWT Token，使前端可多标签页独立登录
        loginUserVO.setToken(jwtUtils.generateToken(user.getId(), user.getUserName(), user.getUserRole()));
        return loginUserVO;
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
        // 优先从 UserContext 获取（LoginInterceptor 已查过数据库）
        User contextUser = UserContext.getCurrentUser();
        if (contextUser != null) {
            return contextUser;
        }
        // 兜底：从 session + DB 查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
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
        // 优先从 UserContext 获取（LoginInterceptor 已查过数据库）
        User contextUser = UserContext.getCurrentUser();
        if (contextUser != null) {
            return contextUser;
        }
        // 兜底：从 session + DB 查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
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
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
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
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
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
        queryWrapper.lambda().eq(StringUtils.isNotBlank(unionId), User::getUnionId, unionId);
        queryWrapper.lambda().eq(StringUtils.isNotBlank(mpOpenId), User::getMpOpenId, mpOpenId);
        queryWrapper.lambda().eq(StringUtils.isNotBlank(userRole), User::getUserRole, userRole);
        queryWrapper.lambda().like(StringUtils.isNotBlank(userProfile), User::getUserProfile, userProfile);
        queryWrapper.lambda().like(StringUtils.isNotBlank(userName), User::getUserName, userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(Object userIdOrAccount, HttpServletRequest request, int result) {
        try {
            LoginLog loginLog = new LoginLog();
            if (userIdOrAccount instanceof Long) {
                loginLog.setUserId((Long) userIdOrAccount);
            }
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setIpAddress(getClientIp(request));
            loginLog.setDevice(request.getHeader("User-Agent"));
            loginLog.setResult(result);
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            // 日志记录失败不影响登录流程
            log.warn("记录登录日志失败: {}", e.getMessage());
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
