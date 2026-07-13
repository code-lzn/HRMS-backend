package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.EmployeeDetailMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.model.dto.employee.EmpProfileUpdateRequest;
import com.limou.hrms.model.entity.EmpSalaryProfile;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.EmployeeDetail;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.vo.EmpProfileVO;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.service.EmpSalaryProfileService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.service.PositionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Set;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
        implements EmployeeService {
    @Resource
    private DepartmentService departmentService;
    @Resource
    private PositionService positionService;
    @Resource
    private EmpSalaryProfileService salaryProfileService;
    @Resource
    private EmployeeDetailMapper employeeDetailMapper;

    /**
     * 可编辑的字段名集合
     */
    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "email", "currentAddress", "emergencyContactName", "emergencyContactPhone");

    private static final Set<String> SENSITIVE_FIELDS = Set.of("idCard", "phone");

    @Override
    public EmpProfileVO getProfile(Long userId) {
        Employee emp = getByUserId(userId);
        EmployeeDetail detail = getDetailByEmployeeId(emp.getId());

        Department department = departmentService.getById(emp.getDepartmentId());
        Position position = positionService.getById(emp.getPositionId());
        EmpSalaryProfile salary = salaryProfileService.getById(emp.getSalaryId());

        EmpProfileVO vo = new EmpProfileVO();
        BeanUtils.copyProperties(emp, vo);
        if (detail != null) {
            BeanUtils.copyProperties(detail, vo);
        }
        if (department != null) {
            vo.setDepartmentName(department.getDeptName());
        }
        if (position != null) {
            vo.setPositionName(position.getName());
        }
        if (salary != null) {
            vo.setBaseSalary(salary.getBaseSalary());
        }
        vo.setIdCard(maskIdCard(detail != null ? detail.getIdCard() : null));
        vo.setPhone(maskPhone(emp.getPhone()));
        vo.setEditableFields(EDITABLE_FIELDS);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, EmpProfileUpdateRequest request) {
        Employee emp = getByUserId(userId);
        EmployeeDetail detail = getOrCreateDetail(emp.getId());

        if (request.getEmail() != null) {
            emp.setEmail(request.getEmail());
            updateById(emp);
        }
        if (request.getCurrentAddress() != null) {
            detail.setCurrentAddress(request.getCurrentAddress());
        }
        if (request.getEmergencyContactName() != null) {
            detail.setEmergencyContactName(request.getEmergencyContactName());
        }
        if (request.getEmergencyContactPhone() != null) {
            detail.setEmergencyContactPhone(request.getEmergencyContactPhone());
        }

        boolean detailUpdated = employeeDetailMapper.updateById(detail) > 0;
        if (!detailUpdated && hasDetailChanges(request)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    @Override
    public Employee getByUserId(Long userId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("userId", userId);
        Employee emp = this.getOne(wrapper);
        if (emp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
        }
        return emp;
    }

    // ==================== 私有方法 ====================

    private EmployeeDetail getDetailByEmployeeId(Long employeeId) {
        QueryWrapper<EmployeeDetail> wrapper = new QueryWrapper<>();
        wrapper.eq("employeeId", employeeId);
        return employeeDetailMapper.selectOne(wrapper);
    }

    private EmployeeDetail getOrCreateDetail(Long employeeId) {
        EmployeeDetail detail = getDetailByEmployeeId(employeeId);
        if (detail == null) {
            detail = new EmployeeDetail();
            detail.setEmployeeId(employeeId);
            employeeDetailMapper.insert(detail);
        }
        return detail;
    }

    private boolean hasDetailChanges(EmpProfileUpdateRequest request) {
        return request.getCurrentAddress() != null
                || request.getEmergencyContactName() != null
                || request.getEmergencyContactPhone() != null;
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        int len = idCard.length();
        return idCard.substring(0, 4) + idCard.substring(4, len - 4).replaceAll(".", "*") + idCard.substring(len - 4);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
