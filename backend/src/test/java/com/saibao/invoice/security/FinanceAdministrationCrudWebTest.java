package com.saibao.invoice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void titleEndpointsCreateUpdateAndReturnDatabasePageTotal() throws Exception {
        String taxpayerId = "91330100ONLINE0001";
        HttpResponse<String> created = send(administrator, "POST", "/api/admin/invoice-titles", """
                {"companyName":"上线新增测试有限公司","taxpayerId":"91330100ONLINE0001",
                 "registeredAddress":"杭州市钱塘区测试路1号","phone":"0571-88888888",
                 "bankName":"宁波银行杭州分行","bankAccount":"001234567890",
                 "subjectIds":[1],"status":"DRAFT"}
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
                 "subjectIds":[1,2],"status":"PUBLISHED"}
                """);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(get(administrator, "/api/admin/invoice-titles/" + titleId).body())
                .contains("上线编辑测试有限公司", "\"status\":\"PUBLISHED\"", "\"subjectIds\":[1,2]");
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
    void permissionProfileUsesDirectorySelectionsAndReturnsEffectiveEmployeeOverrides() throws Exception {
        HttpResponse<String> saved = send(administrator, "PUT", "/api/admin/subjects/1/permission-profile", """
                {"allEmployeeVisible":false,"departmentIds":[1],
                 "employeeRules":[{"employeeId":1,"effect":"DENY"},{"employeeId":3,"effect":"ALLOW"}]}
                """);
        assertThat(saved.statusCode()).isEqualTo(200);

        HttpResponse<String> profile = get(administrator, "/api/admin/subjects/1/permission-profile");
        assertThat(profile.statusCode()).isEqualTo(200);
        assertThat(profile.body())
                .contains("技术中心", "ding-dept-tech", "示例员工", "\"effect\":\"DENY\"")
                .contains("采购员工", "\"effect\":\"ALLOW\"", "\"visibleCount\":2");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM subject_permission
                WHERE subject_id = 1 AND target_id IN ('ding-dept-tech', 'ding-employee-001', 'ding-employee-003')
                """, Long.class)).isEqualTo(3L);
    }

    @Test
    void employeeDirectoryFiltersByEffectiveSubjectPermissionAndReturnsSwitchState() throws Exception {
        HttpResponse<String> saved = send(administrator, "PUT", "/api/admin/subjects/1/permission-profile", """
                {"allEmployeeVisible":false,"departmentIds":[1],
                 "employeeRules":[{"employeeId":1,"effect":"DENY"},{"employeeId":3,"effect":"ALLOW"}]}
                """);
        assertThat(saved.statusCode()).isEqualTo(200);

        HttpResponse<String> enabled = get(administrator,
                "/api/admin/directory/employees?pageNum=1&pageSize=10&subjectId=1&permissionStatus=ENABLED");
        assertThat(enabled.statusCode()).isEqualTo(200);
        assertThat(enabled.body())
                .contains("采购员工", "研发员工", "\"permissionEnabled\":true", "\"total\":2")
                .doesNotContain("示例员工", "财务员工");

        HttpResponse<String> disabled = get(administrator,
                "/api/admin/directory/employees?pageNum=1&pageSize=10&subjectId=1&permissionStatus=DISABLED");
        assertThat(disabled.statusCode()).isEqualTo(200);
        assertThat(disabled.body())
                .contains("示例员工", "财务员工", "\"permissionEnabled\":false", "\"total\":2")
                .doesNotContain("采购员工", "研发员工");
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
