package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.model.dto.employee.EmpProfileUpdateRequest;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.vo.EmpProfileVO;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Set;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
        implements EmployeeService {
    @Resource
    private  DepartmentService departmentService;
    @Resource
    private PositionService positionService;

    /**
    * 可编辑的字段名集合（不在这个集合里的字段均为锁定状态，前端提示"如需修改请联系 HR"）
    */
    private static final Set<String> EDITABLE_FIELDS = Set.of(
            "email", "currentAddress", "emergencyContactName", "emergencyContactPhone");

    private static final Set<String> SENSITIVE_FIELDS = Set.of("idCard", "phone");

    //拿到用户的个人用户的全部信息
    @Override
    public EmpProfileVO getProfile(Long userId) {
        Employee emp = getByUserId(userId);
        //需要查询到对应的部门和职位名称
        Department department = departmentService.getById(emp.getDepartmentId());
        Position position = positionService.getById(emp.getPositionId());
        EmpProfileVO vo = new EmpProfileVO();
        BeanUtils.copyProperties(emp, vo);
        if(department!=null){
            vo.setDepartmentName(department.getDeptName());
        }
        if(position!=null)
            vo.setPositionName(position.getName());
        vo.setIdCard(maskIdCard(emp.getIdCard()));
        vo.setPhone(maskPhone(emp.getPhone()));
        vo.setEditableFields(EDITABLE_FIELDS);
        return vo;
    }

    //更新用户的信息
    @Override
    public void updateProfile(Long userId, EmpProfileUpdateRequest request) {
        Employee emp = getByUserId(userId);
        if (request.getEmail() != null) {
            emp.setEmail(request.getEmail());
        }
        if (request.getCurrentAddress() != null) {
            emp.setCurrentAddress(request.getCurrentAddress());
        }
        if (request.getEmergencyContactName() != null) {
            emp.setEmergencyContactName(request.getEmergencyContactName());
        }
        if (request.getEmergencyContactPhone() != null) {
            emp.setEmergencyContactPhone(request.getEmergencyContactPhone());
        }
        boolean result = updateById(emp);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    private Employee getByUserId(Long userId) {
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.eq("userId", userId);
        Employee emp = this.getOne(wrapper);
        if (emp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
        }
        return emp;
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




