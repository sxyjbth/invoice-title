package com.saibao.invoice.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectPermissionMapperSqlContractTest {

    private static final Path MAPPER = Path.of(
            "src", "main", "resources", "mapper", "SubjectPermissionMapper.xml");
    private static final Path DIRECTORY_MAPPER = Path.of(
            "src", "main", "resources", "mapper", "DingDirectoryMapper.xml");
    private static final Path EMPLOYEE_TITLE_MAPPER = Path.of(
            "src", "main", "resources", "mapper", "EmployeeInvoiceTitleMapper.xml");

    @Test
    void effectiveEmployeeCountKeepsOuterEmployeeCorrelationOutOfJoinOnClause() throws IOException {
        String mapperXml = Files.readString(MAPPER, StandardCharsets.UTF_8);

        assertThat(mapperXml)
                .doesNotContain("ON employee_department.employee_id = e.id")
                .contains("WHERE employee_department.employee_id = e.id");
    }

    @Test
    void multiDepartmentPermissionQueriesKeepOuterEmployeeCorrelationOutOfJoinOnClause() throws IOException {
        String directoryMapperXml = Files.readString(DIRECTORY_MAPPER, StandardCharsets.UTF_8);
        String employeeTitleMapperXml = Files.readString(EMPLOYEE_TITLE_MAPPER, StandardCharsets.UTF_8);

        assertThat(directoryMapperXml)
                .doesNotContain("ON employee_department.employee_id = e.id")
                .contains("WHERE employee_department.employee_id = e.id");
        assertThat(employeeTitleMapperXml)
                .doesNotContain("ON employee_department.employee_id = current_employee.id")
                .contains("WHERE employee_department.employee_id = current_employee.id");
    }

    @Test
    void everyDepartmentPermissionArmExcludesOnlyTheMatchingDepartmentEmployeeEdge() throws IOException {
        String subjectMapperXml = Files.readString(MAPPER, StandardCharsets.UTF_8);
        String directoryMapperXml = Files.readString(DIRECTORY_MAPPER, StandardCharsets.UTF_8);
        String employeeTitleMapperXml = Files.readString(EMPLOYEE_TITLE_MAPPER, StandardCharsets.UTF_8);

        assertThat(subjectMapperXml)
                .contains("FROM subject_department_employee_exclusion department_exclusion")
                .contains("department_exclusion.subject_id = s.id")
                .contains("department_exclusion.department_id = employee_department.department_id")
                .contains("department_exclusion.employee_id = e.id");
        assertThat(directoryMapperXml)
                .contains("FROM subject_department_employee_exclusion department_exclusion")
                .contains("department_exclusion.subject_id = #{subjectId}")
                .contains("department_exclusion.department_id = employee_department.department_id")
                .contains("department_exclusion.employee_id = e.id");
        assertThat(employeeTitleMapperXml)
                .contains("FROM subject_department_employee_exclusion department_exclusion")
                .contains("department_exclusion.subject_id = s.id")
                .contains("department_exclusion.department_id = employee_department.department_id")
                .contains("department_exclusion.employee_id = current_employee.id");
    }
}
