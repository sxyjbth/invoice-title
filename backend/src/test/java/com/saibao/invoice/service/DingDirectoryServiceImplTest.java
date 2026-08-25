package com.saibao.invoice.service;

import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.domain.EmployeeDepartmentMembership;
import com.saibao.invoice.dto.EmployeeDirectoryPageQueryDTO;
import com.saibao.invoice.mapper.DingDirectoryMapper;
import com.saibao.invoice.service.impl.DingDirectoryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DingDirectoryServiceImplTest {

    @Test
    void employeePageReturnsEveryDepartmentMembershipForPermissionLinkage() {
        DingDirectoryMapper mapper = mock(DingDirectoryMapper.class);
        EmployeeDirectoryPageQueryDTO query = new EmployeeDirectoryPageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        DingEmployee employee = new DingEmployee();
        employee.setId(21L);
        employee.setDepartmentId(101L);
        employee.setEmployeeName("跨部门员工");
        when(mapper.countEmployees(query)).thenReturn(1L);
        when(mapper.selectEmployeePage(query)).thenReturn(List.of(employee));
        when(mapper.selectEmployeeDepartmentMembershipsByEmployeeIds(List.of(21L))).thenReturn(List.of(
                membership(21L, 101L),
                membership(21L, 202L)
        ));

        var result = new DingDirectoryServiceImpl(mapper).pageEmployees(query);

        assertThat(result.getRecords()).singleElement().satisfies(record ->
                assertThat(record.getDepartmentIds()).containsExactly(101L, 202L));
    }

    private EmployeeDepartmentMembership membership(Long employeeId, Long departmentId) {
        EmployeeDepartmentMembership membership = new EmployeeDepartmentMembership();
        membership.setEmployeeId(employeeId);
        membership.setDepartmentId(departmentId);
        return membership;
    }
}
