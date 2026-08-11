package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.SubjectPermission;
import com.saibao.invoice.dto.SubjectPermissionPageQueryDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import com.saibao.invoice.vo.DingDepartmentVO;
import com.saibao.invoice.vo.EmployeePermissionRuleVO;

/** 主体查看权限持久化接口。 */
public interface SubjectPermissionMapper {
    long count(SubjectPermissionPageQueryDTO query);
    List<SubjectPermission> selectPage(SubjectPermissionPageQueryDTO query);
    int insert(SubjectPermission permission);
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedBy") String updatedBy);
    int deleteBySubjectId(@Param("subjectId") Long subjectId);
    List<DingDepartmentVO> selectProfileDepartments(@Param("subjectId") Long subjectId);
    List<EmployeePermissionRuleVO> selectProfileEmployeeRules(@Param("subjectId") Long subjectId);
    long countEffectiveEmployees(@Param("subjectId") Long subjectId);
}
