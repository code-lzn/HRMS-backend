package com.limou.hrms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.DeleteRequest;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.model.dto.employee.EmpProfileUpdateRequest;
import com.limou.hrms.model.dto.employee.EmployeeAddRequest;
import com.limou.hrms.model.dto.employee.EmployeeQueryRequest;
import com.limou.hrms.model.dto.employee.EmployeeUpdateRequest;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.model.vo.EmpProfileVO;
import com.limou.hrms.model.vo.EmployeeChangeLogVO;
import com.limou.hrms.model.vo.EmployeeDetailVO;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.vo.EmployeeExcelVO;
import com.limou.hrms.model.dto.onboarding.OnboardingAddRequest;
import com.limou.hrms.model.entity.HrOnboarding;
import com.limou.hrms.model.vo.EmployeeVO;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.OnboardingService;
import com.limou.hrms.service.UserService;
import com.limou.hrms.utils.ExcelUtils;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工个人中心接口
 */
@RestController
@RequestMapping("/employee")
@Slf4j
public class EmployeeController {


    @Resource
    private EmployeeService employeeService;

    @Resource
    private UserService userService;

    @Resource
    private OnboardingService onboardingService;

    /**
     * 查看我的档案（含敏感字段脱敏、锁定字段标记）
     */
    @GetMapping("/profile")
    public BaseResponse<EmpProfileVO> getMyProfile(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        EmpProfileVO vo = employeeService.getProfile(loginUser.getId());
        return ResultUtils.success(vo);
    }

    /**
     * 编辑我的档案（仅可编辑邮箱/现居住地址/紧急联系人）
     */
    @PostMapping("/profileUpdate")
    public BaseResponse<Boolean> updateMyProfile(@RequestBody EmpProfileUpdateRequest updateRequest,
                                                  HttpServletRequest request) {
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        employeeService.updateProfile(loginUser.getId(), updateRequest);
        return ResultUtils.success(true);
    }

    /**
     * 员工列表（分页 + 高级搜索）
     */
    @GetMapping("/list")
    public BaseResponse<Page<EmployeeVO>> listEmployees(EmployeeQueryRequest request, HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Page<EmployeeVO> result = employeeService.listEmployees(request, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 获取可选为部门负责人的员工列表（角色：系统管理员/HR专员/部门主管）
     */
    @GetMapping("/manager-candidates")
    public BaseResponse<List<Employee>> listManagerCandidates() {
        return ResultUtils.success(employeeService.listManagerCandidates());
    }

    /**
     * 导出员工列表为 Excel
     */
    @GetMapping("/export")
    public void exportEmployees(EmployeeQueryRequest request,
                                 HttpServletRequest httpRequest,
                                 HttpServletResponse response) {
        User loginUser = userService.getLoginUser(httpRequest);
        List<EmployeeExcelVO> data = employeeService.exportEmployees(request, loginUser.getId());
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        ExcelUtils.export(response, "员工列表_" + timestamp, "员工信息", data, EmployeeExcelVO.class);
    }

    /**
     * 员工详情
     */
    @GetMapping("/detail")
    public BaseResponse<EmployeeDetailVO> getDetail(Long id, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        EmployeeDetailVO detail = employeeService.getDetail(id, loginUser.getId());
        return ResultUtils.success(detail);
    }
    /**
     * 新增员工 → 提交入职申请，审批通过后自动创建员工
     */
    @PostMapping("/add")
    public BaseResponse<Map<String, Object>> addEmployee(@RequestBody EmployeeAddRequest request,
                                                          HttpServletRequest httpRequest) {
        if (request == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);

        OnboardingAddRequest onboardingReq = new OnboardingAddRequest();
        onboardingReq.setCandidateName(request.getEmployeeName());
        onboardingReq.setGender(request.getGender() != null && request.getGender() == 1 ? "MALE" : "FEMALE");
        onboardingReq.setPhone(request.getPhone());
        onboardingReq.setEmail(request.getEmail());
        onboardingReq.setIdCard(request.getIdCard());
        onboardingReq.setBirthday(request.getBirthday());
        onboardingReq.setRegisteredAddress(request.getRegisteredAddress());
        onboardingReq.setCurrentAddress(request.getCurrentAddress());
        onboardingReq.setDeptId(request.getDepartmentId());
        onboardingReq.setPositionId(request.getPositionId());
        onboardingReq.setWorkLocation(request.getWorkLocation());
        onboardingReq.setDirectReportId(request.getDirectReportId());
        onboardingReq.setHireDate(request.getHireDate());
        onboardingReq.setEmploymentType(request.getEmploymentType());
        onboardingReq.setContractType(request.getContractType());
        onboardingReq.setContractExpireDate(request.getContractExpireDate());
        onboardingReq.setProbationMonth(3);
        onboardingReq.setBaseSalary(request.getBaseSalary());
        onboardingReq.setBankAccount(request.getBankAccount());
        onboardingReq.setBankName(request.getBankName());
        onboardingReq.setEmergencyContactName(request.getEmergencyContactName());
        onboardingReq.setEmergencyContactPhone(request.getEmergencyContactPhone());

        HrOnboarding entity = onboardingService.addOnboarding(onboardingReq, loginUser.getId(), true);
        Map<String, Object> data = new HashMap<>();
        data.put("id", entity.getId());
        data.put("recordId", entity.getRecordId());
        data.put("status", "APPROVING");
        return ResultUtils.success(data);
    }
    /**
     * 更新员工
     */
    @PutMapping("/update")
    public BaseResponse<Boolean> updateEmployee(@RequestBody EmployeeUpdateRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getId() == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        employeeService.updateEmployee(request, loginUser.getId());
        return ResultUtils.success(true);
    }
    /**
     * 删除员工
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteEmployee(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        employeeService.deleteEmployee(request.getId());
        return ResultUtils.success(true);
    }
    /**
     * 员工变更历史
     */
    @GetMapping("/change-logs")
    public BaseResponse<Page<EmployeeChangeLogVO>> getChangeLogs(Long employeeId, Integer page, Integer size) {
        ThrowUtils.throwIf(employeeId == null || employeeId <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(employeeService.getChangeLogs(employeeId, page, size));
    }
}
