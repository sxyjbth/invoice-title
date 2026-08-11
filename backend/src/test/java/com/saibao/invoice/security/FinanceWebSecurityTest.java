package com.saibao.invoice.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
class FinanceWebSecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedAccounts() {
        jdbcTemplate.update("DELETE FROM finance_user");
        insertAccount(1L, "superadmin", "超级管理员", "Admin@123456", "SUPER_ADMIN");
        insertAccount(2L, "finance.user", "财务人员", "Finance@123", "FINANCE");
    }

    @Test
    void unauthenticatedBrowserMustNotAccessFinanceApis() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/admin/invoice-titles?pageNum=1&pageSize=20"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void financeShouldLoginUseFinanceApisAndChangeOwnPassword() throws Exception {
        HttpClient client = sessionClient();
        assertThat(post(client, "/api/auth/login", "{\"username\":\"finance.user\",\"password\":\"Finance@123\"}").statusCode())
                .isEqualTo(200);
        assertThat(get(client, "/api/admin/invoice-titles?pageNum=1&pageSize=20").statusCode()).isEqualTo(200);
        assertThat(post(client, "/api/admin/finance-users", "{}").statusCode()).isEqualTo(403);

        assertThat(post(client, "/api/auth/change-password", "{\"currentPassword\":\"Finance@123\",\"newPassword\":\"Changed@456\"}").statusCode())
                .isEqualTo(200);
        assertThat(post(sessionClient(), "/api/auth/login", "{\"username\":\"finance.user\",\"password\":\"Finance@123\"}").statusCode())
                .isEqualTo(400);
        assertThat(post(sessionClient(), "/api/auth/login", "{\"username\":\"finance.user\",\"password\":\"Changed@456\"}").statusCode())
                .isEqualTo(200);
    }

    @Test
    void superAdministratorShouldCreateAndResetFinanceAccount() throws Exception {
        HttpClient administrator = sessionClient();
        assertThat(post(administrator, "/api/auth/login", "{\"username\":\"superadmin\",\"password\":\"Admin@123456\"}").statusCode())
                .isEqualTo(200);
        assertThat(post(administrator, "/api/admin/finance-users", "{\"username\":\"finance.new\",\"displayName\":\"新财务\",\"initialPassword\":\"Initial@123\"}").statusCode())
                .isEqualTo(200);

        Long accountId = jdbcTemplate.queryForObject("SELECT id FROM finance_user WHERE username = 'finance.new'", Long.class);
        assertThat(post(administrator, "/api/admin/finance-users/" + accountId + "/reset-password", "{\"newPassword\":\"Reset@789\"}").statusCode())
                .isEqualTo(200);
        assertThat(post(sessionClient(), "/api/auth/login", "{\"username\":\"finance.new\",\"password\":\"Initial@123\"}").statusCode())
                .isEqualTo(400);
        assertThat(post(sessionClient(), "/api/auth/login", "{\"username\":\"finance.new\",\"password\":\"Reset@789\"}").statusCode())
                .isEqualTo(200);
    }

    private HttpClient sessionClient() {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder().uri(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private void insertAccount(Long id, String username, String displayName, String password, String role) {
        jdbcTemplate.update("""
                INSERT INTO finance_user
                (id, username, display_name, password_hash, role_type, status, created_by, created_at, updated_by, updated_at, deleted)
                VALUES (?, ?, ?, ?, ?, 'ENABLED', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 0)
                """, id, username, displayName, passwordEncoder.encode(password), role);
    }
}
