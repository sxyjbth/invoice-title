package com.saibao.invoice.security;

import com.saibao.invoice.integration.dingtalk.DingDepartmentSnapshot;
import com.saibao.invoice.integration.dingtalk.DingEmployeeSnapshot;
import com.saibao.invoice.integration.dingtalk.DingTalkClient;
import com.saibao.invoice.integration.dingtalk.DingTalkIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(EmployeeDingTalkAuthenticationWebTest.FakeDingTalkConfiguration.class)
class EmployeeDingTalkAuthenticationWebTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void employeeApisMustUseDingTalkSessionInsteadOfCallerSuppliedUserId() throws Exception {
        HttpClient anonymous = sessionClient();
        assertThat(get(anonymous, "/api/employee/invoice-titles?pageNum=1&pageSize=20&dingUserId=ding-employee-001").statusCode())
                .isEqualTo(401);

        HttpClient employee = sessionClient();
        HttpResponse<String> login = post(employee, "/api/employee/auth/dingtalk",
                "{\"corpCode\":\"default\",\"authCode\":\"valid-code\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("ding-employee-001").contains("示例员工");

        HttpResponse<String> titles = get(employee, "/api/employee/invoice-titles?pageNum=1&pageSize=20");
        assertThat(titles.statusCode()).isEqualTo(200);
        assertThat(titles.body()).contains("杭州赛宝卓越技术有限公司");

        HttpResponse<String> qr = post(employee, "/api/employee/invoice-titles/1/qr-token", "{}");
        assertThat(qr.statusCode()).isEqualTo(200);
        assertThat(qr.body()).contains("expiresAt");
    }

    @Test
    void unknownOrInactiveDingTalkEmployeeMustNotReceiveSession() throws Exception {
        HttpResponse<String> response = post(sessionClient(), "/api/employee/auth/dingtalk",
                "{\"corpCode\":\"default\",\"authCode\":\"unknown-code\"}");
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("未同步");
    }

    @Test
    void expiredQrTokenShouldReturnGoneWithFriendlyMessage() throws Exception {
        String token = "expired-" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                INSERT INTO invoice_qr_token
                (token, title_id, version_id, employee_id, expires_at, created_at)
                VALUES (?, 1, 3, 1, ?, ?)
                """, token, LocalDateTime.now().minusSeconds(1), LocalDateTime.now().minusMinutes(11));
        try {
            HttpResponse<String> response = get(sessionClient(),
                    "/api/employee/invoice-titles/qr/" + token);

            assertThat(response.statusCode()).isEqualTo(410);
            assertThat(response.body()).contains("二维码已过期，请重新获取二维码");
        } finally {
            jdbcTemplate.update("DELETE FROM invoice_qr_token WHERE token = ?", token);
        }
    }

    private HttpClient sessionClient() {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder().uri(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder().uri(uri(path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    @TestConfiguration
    static class FakeDingTalkConfiguration {
        @Bean
        @Primary
        DingTalkClient fakeDingTalkClient() {
            return new DingTalkClient() {
                @Override
                public DingTalkIdentity resolveIdentity(String authCode) {
                    return "valid-code".equals(authCode)
                            ? new DingTalkIdentity("ding-employee-001", "union-001")
                            : new DingTalkIdentity("not-synced-user", "not-synced-union");
                }

                @Override
                public List<DingDepartmentSnapshot> listDepartments() {
                    return List.of();
                }

                @Override
                public List<DingEmployeeSnapshot> listEmployees() {
                    return List.of();
                }
            };
        }
    }
}
