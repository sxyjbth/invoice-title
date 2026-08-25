package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.DingDepartment;
import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.domain.DingDirectorySyncLog;
import com.saibao.invoice.domain.EmployeeDepartmentMembership;
import com.saibao.invoice.dto.DepartmentDirectoryPageQueryDTO;
import com.saibao.invoice.dto.EmployeeDirectoryPageQueryDTO;
import com.saibao.invoice.vo.DingOrganizationVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 钉钉通讯录部门与员工查询持久化接口。 */
public interface DingDirectoryMapper {
    List<DingOrganizationVO> selectOrganizations();
    long countEmployees(EmployeeDirectoryPageQueryDTO query);
    List<DingEmployee> selectEmployeePage(EmployeeDirectoryPageQueryDTO query);
    List<EmployeeDepartmentMembership> selectEmployeeDepartmentMembershipsByEmployeeIds(
            @Param("employeeIds") List<Long> employeeIds);
    /** 返回指定在职员工与最终已选有效部门之间的全部授权边。 */
    List<EmployeeDepartmentMembership> selectActiveEmployeeDepartmentMemberships(
            @Param("departmentIds") List<Long> departmentIds,
            @Param("employeeIds") List<Long> employeeIds);
    DingEmployee selectEmployeeByIdentity(@Param("corpCode") String corpCode,
                                          @Param("dingUserId") String dingUserId);
    DingEmployee selectActiveEmployeeById(@Param("id") Long id);
    List<DingEmployee> selectEmployeesByIds(@Param("ids") List<Long> ids);
    /** 查询指定部门中的所有在职员工目录主键，用于撤销部门授权时清理残留个人授权。 */
    List<Long> selectActiveEmployeeIdsByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);
    long countDepartments(DepartmentDirectoryPageQueryDTO query);
    List<DingDepartment> selectDepartmentPage(DepartmentDirectoryPageQueryDTO query);
    List<DingDepartment> selectDepartmentsByIds(@Param("ids") List<Long> ids);

    DingDepartment selectDepartmentByDingId(@Param("corpCode") String corpCode,
                                            @Param("dingDepartmentId") String dingDepartmentId);
    int insertDepartment(DingDepartment department);
    int updateDepartment(DingDepartment department);
    int disableDepartmentsNotIn(@Param("corpCode") String corpCode,
                                @Param("dingDepartmentIds") List<String> dingDepartmentIds);

    DingEmployee selectAnyEmployeeByIdentity(@Param("corpCode") String corpCode,
                                             @Param("dingUserId") String dingUserId);
    int insertEmployee(DingEmployee employee);
    int updateEmployee(DingEmployee employee);
    int inactivateEmployeesNotIn(@Param("corpCode") String corpCode,
                                 @Param("dingUserIds") List<String> dingUserIds);
    int deleteEmployeeDepartments(@Param("employeeId") Long employeeId);
    int insertEmployeeDepartment(@Param("employeeId") Long employeeId,
                                 @Param("departmentId") Long departmentId,
                                 @Param("primary") boolean primary);

    int insertSyncLog(DingDirectorySyncLog syncLog);
    int finishSyncLog(DingDirectorySyncLog syncLog);
}
