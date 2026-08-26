package com.saibao.invoice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 部分可见新契约的验收测试。
 *
 * <p>这里刻意使用独立的主体和员工主键，避免与 data-test.sql 中的历史授权用例互相污染。</p>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubjectPermissionCanonicalProfileWebTest {

    private static final long SUBJECT_ID = 901L;
    private static final long SEBO_EMPLOYEE_ID = 901L;
    private static final long WALDEN_EMPLOYEE_ID = 902L;
    private static final String SHARED_DING_USER_ID = "same-user-across-corps";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient administrator;

    @BeforeEach
    void prepareCanonicalPermissionFixture() throws Exception {
        clearCanonicalPermissionFixture();
        jdbcTemplate.update("DELETE FROM finance_user");
        jdbcTemplate.update("""
                INSERT INTO finance_user
                (id, username, display_name, password_hash, role_type, status,
                 created_by, created_at, updated_by, updated_at, deleted)
                VALUES (1, 'admin', '超级管理员', ?, 'SUPER_ADMIN', 'ENABLED',
                        0, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0)
                """, passwordEncoder.encode("root"));
        jdbcTemplate.update("""
                INSERT INTO invoice_subject
                (id, subject_name, subject_code, status, all_employee_visible, sort_no,
                 created_by, updated_by, deleted)
                VALUES (?, '规范权限测试主体', 'PERMISSION-CANONICAL', 'ENABLED', 0, 901,
                        'test', 'test', 0)
                """, SUBJECT_ID);
        jdbcTemplate.update("""
                INSERT INTO ding_employee
                (id, corp_code, corp_name, ding_user_id, employee_no, employee_name,
                 department_id, department_name, mobile, status)
                VALUES (?, 'sebo', '赛宝绿创能源技术（上海）有限公司', ?, 'SEBO-901', '赛宝同号员工',
                        1, '技术中心', '13800000901', 'ACTIVE')
                """, SEBO_EMPLOYEE_ID, SHARED_DING_USER_ID);
        jdbcTemplate.update("""
                INSERT INTO ding_employee
                (id, corp_code, corp_name, ding_user_id, employee_no, employee_name,
                 department_id, department_name, mobile, status)
                VALUES (?, 'walden', '瓦尔登环境科学研究院（北京）有限公司', ?, 'WALDEN-902', '瓦尔登同号员工',
                        2, '财务部', '13800000902', 'ACTIVE')
                """, WALDEN_EMPLOYEE_ID, SHARED_DING_USER_ID);

        administrator = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        assertThat(send("POST", "/api/auth/login", "{\"username\":\"admin\",\"password\":\"root\"}")
                .statusCode()).isEqualTo(200);
    }

    @AfterEach
    void clearCanonicalPermissionFixture() {
        jdbcTemplate.update("DELETE FROM subject_department_employee_exclusion WHERE subject_id = ?", SUBJECT_ID);
        jdbcTemplate.update("DELETE FROM subject_permission WHERE subject_id = ?", SUBJECT_ID);
        jdbcTemplate.update("DELETE FROM invoice_title_subject WHERE subject_id = ?", SUBJECT_ID);
        jdbcTemplate.update("DELETE FROM invoice_subject WHERE id = ?", SUBJECT_ID);
        jdbcTemplate.update("DELETE FROM ding_employee_department WHERE employee_id IN (?, ?)",
                SEBO_EMPLOYEE_ID, WALDEN_EMPLOYEE_ID);
        jdbcTemplate.update("DELETE FROM ding_employee WHERE id IN (?, ?)",
                SEBO_EMPLOYEE_ID, WALDEN_EMPLOYEE_ID);
    }

    @Test
    void partialSavePersistsOnlyDistinctUserAllowsAndClearsLegacyRules() throws Exception {
        jdbcTemplate.update("UPDATE invoice_subject SET all_employee_visible = 1 WHERE id = ?", SUBJECT_ID);
        insertPermission("DEPARTMENT", "default", "ding-dept-tech", "技术中心", "ALLOW");
        insertPermission("USER", "default", "ding-employee-002", "财务员工", "DENY");
        jdbcTemplate.update("""
                INSERT INTO subject_department_employee_exclusion
                (subject_id, department_id, employee_id, created_by, updated_by)
                VALUES (?, 1, 1, 'legacy', 'legacy')
                """, SUBJECT_ID);

        HttpResponse<String> response = send("PUT", profilePath(), """
                {"allEmployeeVisible":false,
                 "selectedEmployeeIds":[1,4,901,902,1]}
                """);

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> rules = jdbcTemplate.queryForList("""
                SELECT CONCAT(target_type, '|', target_corp_code, '|', target_id, '|', permission_effect)
                FROM subject_permission
                WHERE subject_id = ? AND deleted = 0
                """, String.class, SUBJECT_ID);
        assertThat(rules).containsExactlyInAnyOrder(
                "USER|default|ding-employee-001|ALLOW",
                "USER|default|ding-employee-004|ALLOW",
                "USER|sebo|same-user-across-corps|ALLOW",
                "USER|walden|same-user-across-corps|ALLOW");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM subject_permission
                WHERE subject_id = ? AND (target_type <> 'USER' OR permission_effect <> 'ALLOW')
                """, Long.class, SUBJECT_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM subject_department_employee_exclusion WHERE subject_id = ?
                """, Long.class, SUBJECT_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT all_employee_visible FROM invoice_subject WHERE id = ?
                """, Boolean.class, SUBJECT_ID)).isFalse();
    }

    @Test
    void enablingAllEmployeesClearsEveryPartialVisibilityRule() throws Exception {
        insertPermission("USER", "default", "ding-employee-001", "示例员工", "ALLOW");
        insertPermission("DEPARTMENT", "default", "ding-dept-tech", "技术中心", "ALLOW");
        jdbcTemplate.update("""
                INSERT INTO subject_department_employee_exclusion
                (subject_id, department_id, employee_id, created_by, updated_by)
                VALUES (?, 1, 1, 'legacy', 'legacy')
                """, SUBJECT_ID);

        HttpResponse<String> response = send("PATCH", profilePath() + "/all-employee-visible",
                "{\"allEmployeeVisible\":true}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT all_employee_visible FROM invoice_subject WHERE id = ?
                """, Boolean.class, SUBJECT_ID)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM subject_permission WHERE subject_id = ?
                """, Long.class, SUBJECT_ID)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM subject_department_employee_exclusion WHERE subject_id = ?
                """, Long.class, SUBJECT_ID)).isZero();
        assertThat(response.body())
                .contains("\"allEmployeeVisible\":true", "\"selectedEmployeeIds\":[]", "\"employeeGroups\":[]");
    }

    @Test
    void profileGroupsFinalEmployeesByCorpAndCountMatchesTheGroupedSet() throws Exception {
        insertPermission("USER", "default", "ding-employee-001", "示例员工", "ALLOW");
        insertPermission("USER", "sebo", SHARED_DING_USER_ID, "赛宝同号员工", "ALLOW");
        insertPermission("USER", "walden", SHARED_DING_USER_ID, "瓦尔登同号员工", "ALLOW");

        HttpResponse<String> response = get(profilePath());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode profile = objectMapper.readTree(response.body());
        JsonNode selectedEmployeeIds = profile.path("selectedEmployeeIds");
        assertThat(selectedEmployeeIds.isArray()).isTrue();
        assertThat(longValues(selectedEmployeeIds)).containsExactlyInAnyOrder(1L, SEBO_EMPLOYEE_ID, WALDEN_EMPLOYEE_ID);

        JsonNode groups = profile.path("employeeGroups");
        assertThat(groups.isArray()).isTrue();
        assertThat(groups).hasSize(3);
        assertThat(textValues(groups, "corpCode")).containsExactlyInAnyOrder("default", "sebo", "walden");

        long groupedEmployeeCount = 0L;
        Set<String> identityKeys = new HashSet<>();
        for (JsonNode group : groups) {
            JsonNode employees = group.path("employees");
            assertThat(employees.isArray()).isTrue();
            groupedEmployeeCount += employees.size();
            for (JsonNode employee : employees) {
                identityKeys.add(group.path("corpCode").asText() + ":" + employee.path("dingUserId").asText());
            }
        }
        assertThat(groupedEmployeeCount).isEqualTo(3L);
        assertThat(profile.path("visibleCount").asLong()).isEqualTo(groupedEmployeeCount);
        assertThat(identityKeys).contains(
                "sebo:" + SHARED_DING_USER_ID,
                "walden:" + SHARED_DING_USER_ID);
    }

    private void insertPermission(String targetType,
                                  String corpCode,
                                  String targetId,
                                  String targetName,
                                  String effect) {
        jdbcTemplate.update("""
                INSERT INTO subject_permission
                (subject_id, target_type, target_corp_code, target_id, target_name,
                 permission_effect, status, source, created_by, updated_by, deleted)
                VALUES (?, ?, ?, ?, ?, ?, 'ENABLED', 'MANUAL', 'test', 'test', 0)
                """, SUBJECT_ID, targetType, corpCode, targetId, targetName, effect);
    }

    private List<Long> longValues(JsonNode array) {
        List<Long> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asLong()));
        return values;
    }

    private List<String> textValues(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.path(field).asText()));
        return values;
    }

    private String profilePath() {
        return "/api/admin/subjects/" + SUBJECT_ID + "/permission-profile";
    }

    private HttpResponse<String> get(String path) throws Exception {
        return administrator.send(HttpRequest.newBuilder().uri(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Content-Type", "application/json")
                .method(method, publisher)
                .build();
        return administrator.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
