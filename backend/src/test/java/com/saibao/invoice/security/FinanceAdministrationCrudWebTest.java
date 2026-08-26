package com.saibao.invoice.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FinanceAdministrationCrudWebTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private HttpClient administrator;

    @BeforeEach
    void loginAdministrator() throws Exception {
        jdbcTemplate.update("DELETE FROM finance_user");
        jdbcTemplate.update("""
                INSERT INTO finance_user
                (id, username, display_name, password_hash, role_type, status, created_by, created_at, updated_by, updated_at, deleted)
                VALUES (1, 'admin', '超级管理员', ?, 'SUPER_ADMIN', 'ENABLED', 0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0)
                """, passwordEncoder.encode("root"));
        administrator = sessionClient();
        assertThat(send(administrator, "POST", "/api/auth/login", "{\"username\":\"admin\",\"password\":\"root\"}").statusCode())
                .isEqualTo(200);
    }

    @AfterEach
    void clearDepartmentEmployeeExclusions() {
        jdbcTemplate.update("DELETE FROM subject_department_employee_exclusion");
    }

    @Test
    void titleEndpointsCreateUpdateAndReturnDatabasePageTotal() throws Exception {
        String taxpayerId = "91330100ONLINE0001";
        HttpResponse<String> created = send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"上线新增测试有限公司","taxpayerId":"91330100ONLINE0001",
                 "registeredAddress":"杭州市钱塘区测试路1号","phone":"0571-88888888",
                 "bankName":"宁波银行杭州分行","bankAccount":"001234567890",
                 "subjectIds":[],"status":"DRAFT"}
                """);
        assertThat(created.statusCode()).isEqualTo(200);
        long titleId = Long.parseLong(created.body());

        HttpResponse<String> page = get(administrator,
                "/api/admin/invoice-titles?pageNum=1&pageSize=10&keyword=" + taxpayerId);
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("\"total\":1", "上线新增测试有限公司");

        HttpResponse<String> updated = send(administrator, "PUT", "/api/admin/invoice-titles/" + titleId, """
                {"companyName":"上线编辑测试有限公司","taxpayerId":"91330100ONLINE0001",
                 "registeredAddress":"杭州市钱塘区修改路2号","phone":"0571-66666666",
                 "bankName":"宁波银行杭州分行","bankAccount":"009999999999",
                 "subjectIds":[],"status":"DRAFT"}
                """);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(get(administrator, "/api/admin/invoice-titles/" + titleId).body())
                .contains("上线编辑测试有限公司", "\"status\":\"DRAFT\"", "\"subjectIds\":[]");
    }

    @Test
    void titleEndpointRejectsMissingCompanyNameAndTaxpayerId() throws Exception {
        HttpResponse<String> missingCompanyName = send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"","taxpayerId":"91330100MA2B123456",
                 "subjectIds":[],"status":"DRAFT"}
                """);
        assertThat(missingCompanyName.statusCode()).isEqualTo(400);
        assertThat(missingCompanyName.body()).contains("公司名称不能为空");

        HttpResponse<String> missingTaxpayerId = send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"必填校验测试有限公司","taxpayerId":"",
                 "subjectIds":[],"status":"DRAFT"}
                """);
        assertThat(missingTaxpayerId.statusCode()).isEqualTo(400);
        assertThat(missingTaxpayerId.body()).contains("纳税人识别号不能为空");
    }

    @Test
    void titleEndpointRejectsInvalidTaxpayerIdFormat() throws Exception {
        HttpResponse<String> response = send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"纳税人识别号校验有限公司","taxpayerId":"taxpayer-123",
                 "phone":"13800138000","bankAccount":"6222021234567890",
                 "subjectIds":[],"status":"DRAFT"}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("纳税人识别号应为 15-20 位大写字母或数字");
    }

    @Test
    void titleEndpointRejectsInvalidPhoneFormat() throws Exception {
        HttpResponse<String> response = send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"联系电话校验有限公司","taxpayerId":"91330100MA2B123457",
                 "phone":"12345","bankAccount":"6222021234567890",
                 "subjectIds":[],"status":"DRAFT"}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("请输入正确的手机号、固定电话或 400/800 客服电话");
    }

    @Test
    void titleEndpointRejectsInvalidBankAccountFormat() throws Exception {
        HttpResponse<String> response = send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"银行账号校验有限公司","taxpayerId":"91330100MA2B123458",
                 "phone":"0571-88888888","bankAccount":"6222-ABC",
                 "subjectIds":[],"status":"DRAFT"}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("银行账号应为 8-32 位数字");
    }

    @Test
    void titleVersionHistoryIsPaginatedAndRestoreCreatesANewDraftVersion() throws Exception {
        HttpResponse<String> firstPage = get(administrator,
                "/api/admin/invoice-titles/1/versions?pageNum=1&pageSize=2");
        assertThat(firstPage.statusCode()).isEqualTo(200);
        assertThat(firstPage.body()).contains("\"total\":3", "\"versionNo\":3", "\"versionNo\":2");

        HttpResponse<String> restored = send(administrator, "POST",
                "/api/admin/invoice-titles/1/versions/1/restore", "{\"operatorUserId\":\"admin\"}");
        assertThat(restored.statusCode()).isEqualTo(200);

        HttpResponse<String> afterRestore = get(administrator,
                "/api/admin/invoice-titles/1/versions?pageNum=1&pageSize=10");
        assertThat(afterRestore.body()).contains("\"total\":4", "\"versionNo\":4", "\"status\":\"DRAFT\"");
    }

    @Test
    void subjectEndpointsCreateEditAndToggleStatus() throws Exception {
        HttpResponse<String> created = send(administrator, "POST", "/api/admin/subjects", """
                {"subjectName":"上线测试主体","status":"ENABLED","sortNo":99,"operatorUserId":"admin"}
                """);
        assertThat(created.statusCode()).isEqualTo(200);
        long subjectId = Long.parseLong(created.body());
        String generatedCode = jdbcTemplate.queryForObject(
                "SELECT subject_code FROM invoice_subject WHERE id = ?", String.class, subjectId);
        assertThat(generatedCode).startsWith("SUB-");

        HttpResponse<String> duplicateName = send(administrator, "POST", "/api/admin/subjects", """
                {"subjectName":"上线测试主体","status":"ENABLED","sortNo":100,"operatorUserId":"admin"}
                """);
        assertThat(duplicateName.statusCode()).isEqualTo(400);
        assertThat(duplicateName.body()).contains("主体名称已存在");

        HttpResponse<String> updated = send(administrator, "PUT", "/api/admin/subjects/" + subjectId, """
                {"subjectName":"上线编辑主体","status":"ENABLED","sortNo":88,"operatorUserId":"admin"}
                """);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(send(administrator, "PATCH",
                "/api/admin/subjects/" + subjectId + "/status?status=DISABLED&operatorUserId=admin", null).statusCode())
                .isEqualTo(200);
        assertThat(jdbcTemplate.queryForMap("SELECT subject_code, subject_name, status FROM invoice_subject WHERE id = ?", subjectId))
                .containsEntry("SUBJECT_CODE", generatedCode)
                .containsEntry("SUBJECT_NAME", "上线编辑主体")
                .containsEntry("STATUS", "DISABLED");
    }

    @Test
    void subjectCanBindAnInvoiceTitleAndExposeItsNameInTheSubjectList() throws Exception {
        long titleId = Long.parseLong(send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"待绑定抬头有限公司","taxpayerId":"91330100BINDTITLE001",
                 "subjectIds":[],"status":"DRAFT"}
                """).body());
        long subjectId = Long.parseLong(send(administrator, "POST", "/api/admin/subjects", """
                {"subjectName":"待绑定主体","status":"ENABLED","sortNo":98,"operatorUserId":"admin"}
                """).body());

        HttpResponse<String> bound = send(administrator, "PUT",
                "/api/admin/subjects/" + subjectId + "/title-binding",
                "{\"titleId\":" + titleId + ",\"operatorUserId\":\"admin\"}");

        assertThat(bound.statusCode()).isEqualTo(200);
        assertThat(get(administrator, "/api/admin/subjects?pageNum=1&pageSize=20&keyword="
                + java.net.URLEncoder.encode("待绑定主体", java.nio.charset.StandardCharsets.UTF_8)).body())
                .contains("\"boundTitleId\":" + titleId, "\"boundTitleName\":\"待绑定抬头有限公司\"");
        assertThat(get(administrator, "/api/admin/invoice-titles/" + titleId).body())
                .contains("\"subjectIds\":[" + subjectId + "]");
    }

    @Test
    void titleEndpointRejectsBindingMoreThanOneSubject() throws Exception {
        HttpResponse<String> response = send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"一对一数量校验有限公司","taxpayerId":"91330100ONETOONE01",
                 "subjectIds":[1,2],"status":"DRAFT"}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("一个发票抬头只能绑定一个主体");
    }

    @Test
    void relationTableEnforcesOneToOneUniquenessOnBothForeignKeys() {
        long titleId = 900001L;
        long subjectId = 900001L;
        jdbcTemplate.update("DELETE FROM invoice_title_subject WHERE title_id >= ? OR subject_id >= ?", titleId, subjectId);
        try {
            jdbcTemplate.update("INSERT INTO invoice_title_subject (title_id, subject_id) VALUES (?, ?)", titleId, subjectId);

            assertThatThrownBy(() -> jdbcTemplate.update(
                    "INSERT INTO invoice_title_subject (title_id, subject_id) VALUES (?, ?)", titleId, subjectId + 1))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcTemplate.update(
                    "INSERT INTO invoice_title_subject (title_id, subject_id) VALUES (?, ?)", titleId + 1, subjectId))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.update("DELETE FROM invoice_title_subject WHERE title_id >= ? OR subject_id >= ?", titleId, subjectId);
        }
    }

    @Test
    void rebindingShouldReplaceBothSidesOfTheOneToOneRelationship() throws Exception {
        long firstTitleId = Long.parseLong(send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"一对一旧抬头有限公司","taxpayerId":"91330100BINDOLD001",
                 "subjectIds":[],"status":"DRAFT"}
                """).body());
        long secondTitleId = Long.parseLong(send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"一对一新抬头有限公司","taxpayerId":"91330100BINDNEW001",
                 "subjectIds":[],"status":"DRAFT"}
                """).body());
        long firstSubjectId = Long.parseLong(send(administrator, "POST", "/api/admin/subjects", """
                {"subjectName":"一对一旧主体","status":"ENABLED","sortNo":96,"operatorUserId":"admin"}
                """).body());
        long secondSubjectId = Long.parseLong(send(administrator, "POST", "/api/admin/subjects", """
                {"subjectName":"一对一新主体","status":"ENABLED","sortNo":97,"operatorUserId":"admin"}
                """).body());
        send(administrator, "PUT", "/api/admin/subjects/" + firstSubjectId + "/title-binding",
                "{\"titleId\":" + firstTitleId + ",\"operatorUserId\":\"admin\"}");
        send(administrator, "PUT", "/api/admin/subjects/" + secondSubjectId + "/title-binding",
                "{\"titleId\":" + secondTitleId + ",\"operatorUserId\":\"admin\"}");

        HttpResponse<String> rebound = send(administrator, "PUT",
                "/api/admin/subjects/" + secondSubjectId + "/title-binding",
                "{\"titleId\":" + firstTitleId + ",\"operatorUserId\":\"admin\"}");

        assertThat(rebound.statusCode()).isEqualTo(200);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice_title_subject WHERE subject_id = ?", Long.class, secondSubjectId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice_title_subject WHERE title_id = ?", Long.class, firstTitleId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice_title_subject WHERE title_id = ? AND subject_id = ?", Long.class, firstTitleId, secondSubjectId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT subject_names FROM invoice_title WHERE id = ?", String.class, secondTitleId)).isEmpty();
    }

    @Test
    void directoryEndpointsUseServerPaginationAndFuzzySearchAcrossEmployeeFields() throws Exception {
        String[][] searches = {{"示例", "1"}, {"SB0001", "1"}, {"技术中心", "2"}, {"13800000001", "1"}};
        for (String[] search : searches) {
            String keyword = search[0];
            HttpResponse<String> response = get(administrator,
                    "/api/admin/directory/employees?pageNum=1&pageSize=10&keyword="
                            + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("ding-employee-001", "\"total\":" + search[1]);
        }
        HttpResponse<String> departments = get(administrator,
                "/api/admin/directory/departments?pageNum=1&pageSize=10&keyword="
                        + java.net.URLEncoder.encode("技术", java.nio.charset.StandardCharsets.UTF_8));
        assertThat(departments.statusCode()).isEqualTo(200);
        assertThat(departments.body()).contains("技术中心", "ding-dept-tech", "\"total\":1");
    }

    @Test
    void directoryEndpointsExposeOrganizationOptionsAndIsolateDepartmentMembersByOrganization() throws Exception {
        jdbcTemplate.update("UPDATE ding_department SET corp_code = 'sebo', corp_name = '赛宝企业' WHERE id IN (1, 2, 3)");
        jdbcTemplate.update("UPDATE ding_employee SET corp_code = 'sebo', corp_name = '赛宝企业' WHERE id IN (1, 2, 3, 4)");
        jdbcTemplate.update("DELETE FROM ding_employee_department WHERE employee_id = 99 OR department_id = 99");
        jdbcTemplate.update("DELETE FROM ding_employee WHERE id = 99");
        jdbcTemplate.update("DELETE FROM ding_department WHERE id = 99");
        jdbcTemplate.update("""
                INSERT INTO ding_department
                (id, corp_code, corp_name, ding_department_id, department_name, status, sort_no)
                VALUES (99, 'walden', '瓦尔登企业', 'ding-dept-walden-platform', '瓦尔登平台部', 'ENABLED', 99)
                """);
        jdbcTemplate.update("""
                INSERT INTO ding_employee
                (id, corp_code, corp_name, ding_user_id, employee_no, employee_name,
                 department_id, department_name, mobile, status)
                VALUES (99, 'walden', '瓦尔登企业', 'ding-user-walden', 'WD0001', '瓦尔登员工',
                        1, '企业根部门', '13900000099', 'ACTIVE')
                """);
        jdbcTemplate.update("""
                INSERT INTO ding_employee_department (employee_id, department_id, is_primary)
                VALUES (99, 99, 0)
                """);

        try {
            HttpResponse<String> organizations = get(administrator, "/api/admin/directory/organizations");
            assertThat(organizations.statusCode()).isEqualTo(200);
            assertThat(organizations.body()).contains("\"corpCode\":\"sebo\"", "\"corpCode\":\"walden\"",
                    "赛宝企业", "瓦尔登企业");

            HttpResponse<String> waldenDepartments = get(administrator,
                    "/api/admin/directory/departments?pageNum=1&pageSize=10&corpCode=walden");
            assertThat(waldenDepartments.statusCode()).isEqualTo(200);
            assertThat(waldenDepartments.body()).contains("瓦尔登平台部", "\"total\":1").doesNotContain("技术中心");

            HttpResponse<String> waldenMembers = get(administrator,
                    "/api/admin/directory/employees?pageNum=1&pageSize=10&corpCode=walden&departmentId=99");
            assertThat(waldenMembers.statusCode()).isEqualTo(200);
            assertThat(waldenMembers.body()).contains("瓦尔登员工", "WD0001", "\"total\":1").doesNotContain("示例员工");

            HttpResponse<String> crossOrganizationMembers = get(administrator,
                    "/api/admin/directory/employees?pageNum=1&pageSize=10&corpCode=sebo&departmentId=99");
            assertThat(crossOrganizationMembers.statusCode()).isEqualTo(200);
            assertThat(crossOrganizationMembers.body()).contains("\"total\":0").doesNotContain("瓦尔登员工");
        } finally {
            jdbcTemplate.update("DELETE FROM ding_employee_department WHERE employee_id = 99 OR department_id = 99");
            jdbcTemplate.update("DELETE FROM ding_employee WHERE id = 99");
            jdbcTemplate.update("DELETE FROM ding_department WHERE id = 99");
            jdbcTemplate.update("UPDATE ding_department SET corp_code = 'default', corp_name = '默认钉钉企业' WHERE id IN (1, 2, 3)");
            jdbcTemplate.update("UPDATE ding_employee SET corp_code = 'default', corp_name = '默认钉钉企业' WHERE id IN (1, 2, 3, 4)");
        }
    }

    @Test
    void allEmployeeVisiblePatchTakesEffectImmediatelyAndClearsExistingRules() throws Exception {
        HttpResponse<String> saved = send(administrator, "PUT", "/api/admin/subjects/1/permission-profile", """
                {"allEmployeeVisible":false,"selectedEmployeeIds":[1,3]}
                """);
        assertThat(saved.statusCode()).isEqualTo(200);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM subject_permission
                WHERE subject_id = 1 AND target_type = 'USER' AND permission_effect = 'ALLOW'
                """, Long.class)).isEqualTo(2L);
        jdbcTemplate.update("""
                INSERT INTO subject_department_employee_exclusion
                (subject_id, department_id, employee_id, created_by, updated_by)
                VALUES (1, 1, 1, 'legacy', 'legacy')
                """);

        HttpResponse<String> response = send(administrator, "PATCH",
                "/api/admin/subjects/1/permission-profile/all-employee-visible",
                "{\"allEmployeeVisible\":true}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(
                "\"allEmployeeVisible\":true",
                "\"visibleCount\":4",
                "\"selectedEmployeeIds\":[]");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT all_employee_visible FROM invoice_subject WHERE id = 1", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM subject_permission WHERE subject_id = 1
                """, Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM subject_department_employee_exclusion WHERE subject_id = 1
                """, Long.class)).isZero();
    }

    private HttpClient sessionClient() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder().uri(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(HttpClient client, String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Content-Type", "application/json")
                .method(method, publisher)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
