package com.saibao.invoice.service;

import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.domain.EmployeeDepartmentMembership;
import com.saibao.invoice.dto.EmployeeSelectionResolveDTO;
import com.saibao.invoice.dto.EmployeeDirectoryPageQueryDTO;
import com.saibao.invoice.mapper.DingDirectoryMapper;
import com.saibao.invoice.service.impl.DingDirectoryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

class DingDirectoryServiceImplTest {

    @Test
    void resolvesOrganizationDepartmentAndEmployeeSelectionsAsDistinctActiveEmployeesGroupedByOrganization() {
        DingDirectoryMapper mapper = mock(DingDirectoryMapper.class);
        DingEmployee seboOne = employee(1L, "sebo", "赛宝", "R001", "赛宝甲");
        DingEmployee seboTwo = employee(2L, "sebo", "赛宝", "R002", "赛宝乙");
        DingEmployee waldenOne = employee(3L, "walden", "瓦尔登", "W001", "瓦尔登甲");
        when(mapper.countEmployees(any(EmployeeDirectoryPageQueryDTO.class))).thenReturn(2L);
        when(mapper.selectEmployeePage(any(EmployeeDirectoryPageQueryDTO.class)))
                .thenReturn(List.of(seboOne, seboTwo));
        when(mapper.selectActiveEmployeeIdsByDepartmentIds(List.of(10L))).thenReturn(List.of(2L, 3L));
        when(mapper.selectEmployeesByIds(anyList())).thenReturn(List.of(seboTwo, waldenOne));
        when(mapper.selectEmployeeDepartmentMembershipsByEmployeeIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of());

        EmployeeSelectionResolveDTO request = new EmployeeSelectionResolveDTO();
        request.setCorpCodes(List.of("sebo"));
        request.setDepartmentIds(List.of(10L));
        request.setEmployeeIds(List.of(1L));

        var result = new DingDirectoryServiceImpl(mapper).resolveEmployeeSelections(request);

        assertThat(result.getSelectedEmployeeIds()).containsExactly(1L, 2L, 3L);
        assertThat(result.getSelectedEmployees()).extracting("employeeName")
                .containsExactly("赛宝甲", "赛宝乙", "瓦尔登甲");
        assertThat(result.getEmployeeGroups()).hasSize(2);
        assertThat(result.getEmployeeGroups().get(0).getCorpCode()).isEqualTo("sebo");
        assertThat(result.getEmployeeGroups().get(0).getEmployeeCount()).isEqualTo(2L);
        assertThat(result.getEmployeeGroups().get(1).getCorpCode()).isEqualTo("walden");
        assertThat(result.getSelectedEmployeeCount()).isEqualTo(3L);
    }

    @Test
    void emptySelectionResolveRequestReturnsEmptyResponseWithoutQueryingDirectory() {
        DingDirectoryMapper mapper = mock(DingDirectoryMapper.class);

        var result = new DingDirectoryServiceImpl(mapper)
                .resolveEmployeeSelections(new EmployeeSelectionResolveDTO());

        assertThat(result.getSelectedEmployeeIds()).isEmpty();
        assertThat(result.getSelectedEmployees()).isEmpty();
        assertThat(result.getEmployeeGroups()).isEmpty();
        assertThat(result.getSelectedEmployeeCount()).isZero();
        verify(mapper, never()).selectEmployeesByIds(anyList());
    }

    @Test
    void resolvedSelectionRejectsMoreThanFiveThousandDistinctEmployees() {
        DingDirectoryMapper mapper = mock(DingDirectoryMapper.class);
        List<Long> employeeIds = LongStream.rangeClosed(1, 5_001).boxed().toList();
        List<DingEmployee> employees = employeeIds.stream()
                .map(id -> employee(id, "sebo", "赛宝", "R" + id, "员工" + id))
                .toList();
        when(mapper.selectActiveEmployeeIdsByDepartmentIds(List.of(10L))).thenReturn(employeeIds);
        when(mapper.selectEmployeesByIds(employeeIds)).thenReturn(employees);

        EmployeeSelectionResolveDTO request = new EmployeeSelectionResolveDTO();
        request.setDepartmentIds(List.of(10L));

        assertThatThrownBy(() -> new DingDirectoryServiceImpl(mapper).resolveEmployeeSelections(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("单次最多选择 5000 名员工");
        verify(mapper, never()).selectEmployeeDepartmentMembershipsByEmployeeIds(anyList());
    }

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

    private DingEmployee employee(Long id, String corpCode, String corpName,
                                  String employeeNo, String employeeName) {
        DingEmployee employee = new DingEmployee();
        employee.setId(id);
        employee.setCorpCode(corpCode);
        employee.setCorpName(corpName);
        employee.setEmployeeNo(employeeNo);
        employee.setEmployeeName(employeeName);
        employee.setStatus("ACTIVE");
        return employee;
    }
}
