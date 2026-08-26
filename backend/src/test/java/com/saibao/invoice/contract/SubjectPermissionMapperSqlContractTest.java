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
    private static final Path INVOICE_SUBJECT_MAPPER = Path.of(
            "src", "main", "resources", "mapper", "InvoiceSubjectMapper.xml");

    @Test
    void subjectListCountsDistinctEffectivelyVisibleEmployees() throws IOException {
        String statement = statement(INVOICE_SUBJECT_MAPPER, "select", "selectPage");

        assertThat(statement)
                .contains("COUNT(DISTINCT e.id)")
                .contains("e.status = 'ACTIVE'")
                .contains("s.status = 'ENABLED'")
                .contains("s.all_employee_visible = 1")
                .contains("target_type = 'USER'")
                .contains("target_corp_code = e.corp_code")
                .contains("target_id = e.ding_user_id")
                .contains("permission_effect = 'ALLOW'")
                .contains("status = 'ENABLED'")
                .contains("deleted = 0")
                .doesNotContain("COUNT(*) FROM subject_permission p");
    }

    @Test
    void departmentSelectionExpandsOnlyEnabledSameCorporationMemberships() throws IOException {
        String statement = statement(DIRECTORY_MAPPER, "select", "selectActiveEmployeeIdsByDepartmentIds");

        assertThat(statement)
                .contains("INNER JOIN ding_department d")
                .contains("d.id = ed.department_id")
                .contains("d.status = 'ENABLED'")
                .contains("d.corp_code = e.corp_code")
                .contains("e.status = 'ACTIVE'");
    }

    @Test
    void effectiveEmployeeCountUsesOnlyAllEmployeeOrExactUserAllow() throws IOException {
        String statement = statement(MAPPER, "select", "countEffectiveEmployees");

        assertCanonicalPermission(statement, "e");
    }

    @Test
    void directoryEffectivePermissionUsesOnlyAllEmployeeOrExactUserAllow() throws IOException {
        String statement = statement(DIRECTORY_MAPPER, "sql", "effectiveEmployeePermission");

        assertCanonicalPermission(statement, "e");
    }

    @Test
    void employeeTitleEffectivePermissionUsesOnlyAllEmployeeOrExactUserAllow() throws IOException {
        String statement = statement(EMPLOYEE_TITLE_MAPPER, "sql", "effectiveSubjectPermission");

        assertCanonicalPermission(statement, "current_employee");
    }

    private static void assertCanonicalPermission(String statement, String employeeAlias) {
        assertThat(statement)
                .contains("all_employee_visible = 1")
                .contains("target_type = 'USER'")
                .contains("target_corp_code = " + employeeAlias + ".corp_code")
                .contains("target_id = " + employeeAlias + ".ding_user_id")
                .contains("permission_effect = 'ALLOW'")
                .doesNotContain("target_type = 'DEPARTMENT'")
                .doesNotContain("subject_department_employee_exclusion")
                .doesNotContain("ding_employee_department");
    }

    private static String statement(Path mapper, String element, String id) throws IOException {
        String mapperXml = Files.readString(mapper, StandardCharsets.UTF_8);
        String openingTag = "<" + element + " id=\"" + id + "\"";
        int start = mapperXml.indexOf(openingTag);
        assertThat(start).as("%s statement %s exists", element, id).isGreaterThanOrEqualTo(0);
        int end = mapperXml.indexOf("</" + element + ">", start);
        assertThat(end).as("%s statement %s is closed", element, id).isGreaterThan(start);
        return mapperXml.substring(start, end);
    }
}
