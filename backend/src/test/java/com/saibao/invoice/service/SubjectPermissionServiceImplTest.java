package com.saibao.invoice.service;

import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.domain.InvoiceSubject;
import com.saibao.invoice.domain.SubjectPermission;
import com.saibao.invoice.dto.SubjectPermissionProfileSaveDTO;
import com.saibao.invoice.dto.SubjectPermissionSaveDTO;
import com.saibao.invoice.mapper.DingDirectoryMapper;
import com.saibao.invoice.mapper.InvoiceSubjectMapper;
import com.saibao.invoice.mapper.SubjectPermissionMapper;
import com.saibao.invoice.service.impl.SubjectPermissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubjectPermissionServiceImplTest {

    @Test
    void repeatingPartialVisibilityPatchDoesNotClearSelectedEmployees() {
        Fixture fixture = fixture(false);

        fixture.service.updateAllEmployeeVisible(7L, false, "admin");

        verify(fixture.subjectMapper, never())
                .updateAllEmployeeVisible(anyLong(), anyBoolean(), anyString());
        verify(fixture.permissionMapper, never())
                .deleteDepartmentEmployeeExclusionsBySubjectId(anyLong());
        verify(fixture.permissionMapper, never()).deleteBySubjectId(anyLong());
    }

    @Test
    void repeatingAllEmployeeVisibilityPatchIsNoOp() {
        Fixture fixture = fixture(true);

        fixture.service.updateAllEmployeeVisible(7L, true, "admin");

        verify(fixture.subjectMapper, never())
                .updateAllEmployeeVisible(anyLong(), anyBoolean(), anyString());
        verify(fixture.permissionMapper, never())
                .deleteDepartmentEmployeeExclusionsBySubjectId(anyLong());
        verify(fixture.permissionMapper, never()).deleteBySubjectId(anyLong());
    }

    @Test
    void changingVisibilityModeClearsThePreviousPreciseEmployeeSet() {
        Fixture fixture = fixture(false);
        when(fixture.subjectMapper.updateAllEmployeeVisible(7L, true, "admin")).thenReturn(1);

        fixture.service.updateAllEmployeeVisible(7L, true, "admin");

        verify(fixture.permissionMapper).deleteDepartmentEmployeeExclusionsBySubjectId(7L);
        verify(fixture.permissionMapper).deleteBySubjectId(7L);
    }

    @Test
    void legacyCreateRequiresAnOrganizationCode() {
        Fixture fixture = fixture(false);
        SubjectPermissionSaveDTO request = legacyRequest(" ", "shared-user-id");

        assertThatThrownBy(() -> fixture.service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("员工所属企业编码不能为空");

        verify(fixture.directoryMapper, never())
                .selectEmployeeByIdentity(anyString(), anyString());
        verify(fixture.permissionMapper, never()).insert(any(SubjectPermission.class));
    }

    @Test
    void legacyCreateLooksUpAnActiveEmployeeByOrganizationAndDingUserId() {
        Fixture fixture = fixture(false);
        DingEmployee employee = employee(31L, "sebo", "shared-user-id", "赛宝员工");
        when(fixture.directoryMapper.selectEmployeeByIdentity("sebo", "shared-user-id"))
                .thenReturn(employee);
        doAnswer(invocation -> {
            SubjectPermission permission = invocation.getArgument(0);
            permission.setId(99L);
            return 1;
        }).when(fixture.permissionMapper).insert(any(SubjectPermission.class));
        SubjectPermissionSaveDTO request = legacyRequest(" sebo ", " shared-user-id ");
        request.setTargetName("请求中的旧姓名");

        Long permissionId = fixture.service.create(request);

        assertThat(permissionId).isEqualTo(99L);
        verify(fixture.directoryMapper).selectEmployeeByIdentity("sebo", "shared-user-id");
        ArgumentCaptor<SubjectPermission> permissionCaptor =
                ArgumentCaptor.forClass(SubjectPermission.class);
        verify(fixture.permissionMapper).insert(permissionCaptor.capture());
        assertThat(permissionCaptor.getValue()).satisfies(permission -> {
            assertThat(permission.getTargetCorpCode()).isEqualTo("sebo");
            assertThat(permission.getTargetId()).isEqualTo("shared-user-id");
            assertThat(permission.getTargetName()).isEqualTo("赛宝员工");
        });
    }

    @Test
    void legacyCreateRejectsAnEmployeeMissingFromTheRequestedOrganization() {
        Fixture fixture = fixture(false);
        SubjectPermissionSaveDTO request = legacyRequest("walden", "shared-user-id");
        when(fixture.directoryMapper.selectEmployeeByIdentity("walden", "shared-user-id"))
                .thenReturn(null);

        assertThatThrownBy(() -> fixture.service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("所选员工不存在、已离职或尚未同步");

        verify(fixture.permissionMapper, never()).insert(any(SubjectPermission.class));
    }

    @Test
    void legacyCreateRejectsAnInactiveEmployeeReturnedByTheDirectory() {
        Fixture fixture = fixture(false);
        SubjectPermissionSaveDTO request = legacyRequest("sebo", "shared-user-id");
        DingEmployee inactiveEmployee = employee(31L, "sebo", "shared-user-id", "离职员工");
        inactiveEmployee.setStatus("INACTIVE");
        when(fixture.directoryMapper.selectEmployeeByIdentity("sebo", "shared-user-id"))
                .thenReturn(inactiveEmployee);

        assertThatThrownBy(() -> fixture.service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("所选员工不存在、已离职或尚未同步");

        verify(fixture.permissionMapper, never()).insert(any(SubjectPermission.class));
    }

    @Test
    void allEmployeeVisibilityCannotBeCombinedWithSelectedEmployeeIds() {
        Fixture fixture = fixture(false);
        SubjectPermissionProfileSaveDTO request = new SubjectPermissionProfileSaveDTO();
        request.setAllEmployeeVisible(true);
        request.setSelectedEmployeeIds(List.of(31L));

        assertThatThrownBy(() -> fixture.service.saveProfile(7L, request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("全员可见时不能同时提交已选员工");

        verify(fixture.subjectMapper, never())
                .updateAllEmployeeVisible(anyLong(), anyBoolean(), anyString());
        verify(fixture.permissionMapper, never()).deleteBySubjectId(anyLong());
    }

    @Test
    void legacyDepartmentExpansionCannotExceedFiveThousandEmployees() {
        Fixture fixture = fixture(false);
        SubjectPermissionProfileSaveDTO request = new SubjectPermissionProfileSaveDTO();
        request.setAllEmployeeVisible(false);
        request.setSelectedEmployeeIds(null);
        request.setDepartmentIds(List.of(10L));
        List<Long> expandedEmployeeIds = LongStream.rangeClosed(1, 5_001)
                .boxed()
                .toList();
        when(fixture.directoryMapper.selectActiveEmployeeIdsByDepartmentIds(List.of(10L)))
                .thenReturn(expandedEmployeeIds);

        assertThatThrownBy(() -> fixture.service.saveProfile(7L, request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("员工选择不能超过 5000 个");

        verify(fixture.directoryMapper, never()).selectEmployeesByIds(any());
        verify(fixture.subjectMapper, never())
                .updateAllEmployeeVisible(anyLong(), anyBoolean(), anyString());
        verify(fixture.permissionMapper, never()).deleteBySubjectId(anyLong());
    }

    private Fixture fixture(boolean allEmployeeVisible) {
        SubjectPermissionMapper permissionMapper = mock(SubjectPermissionMapper.class);
        InvoiceSubjectMapper subjectMapper = mock(InvoiceSubjectMapper.class);
        DingDirectoryMapper directoryMapper = mock(DingDirectoryMapper.class);
        InvoiceSubject subject = new InvoiceSubject();
        subject.setId(7L);
        subject.setSubjectName("测试主体");
        subject.setAllEmployeeVisible(allEmployeeVisible);
        when(subjectMapper.selectById(7L)).thenReturn(subject);
        when(permissionMapper.selectProfileEmployeeRules(7L)).thenReturn(List.of());
        SubjectPermissionServiceImpl service =
                new SubjectPermissionServiceImpl(permissionMapper, subjectMapper, directoryMapper);
        return new Fixture(service, permissionMapper, subjectMapper, directoryMapper);
    }

    private SubjectPermissionSaveDTO legacyRequest(String corpCode, String dingUserId) {
        SubjectPermissionSaveDTO request = new SubjectPermissionSaveDTO();
        request.setSubjectId(7L);
        request.setTargetType("USER");
        request.setTargetCorpCode(corpCode);
        request.setTargetId(dingUserId);
        request.setTargetName("员工");
        request.setOperatorUserId("admin");
        return request;
    }

    private DingEmployee employee(Long id, String corpCode, String dingUserId, String name) {
        DingEmployee employee = new DingEmployee();
        employee.setId(id);
        employee.setCorpCode(corpCode);
        employee.setDingUserId(dingUserId);
        employee.setEmployeeName(name);
        employee.setStatus("ACTIVE");
        return employee;
    }

    private record Fixture(SubjectPermissionServiceImpl service,
                           SubjectPermissionMapper permissionMapper,
                           InvoiceSubjectMapper subjectMapper,
                           DingDirectoryMapper directoryMapper) {
    }
}
