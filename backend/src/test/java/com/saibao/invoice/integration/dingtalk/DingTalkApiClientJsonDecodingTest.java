package com.saibao.invoice.integration.dingtalk;

import com.saibao.invoice.config.DingTalkProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DingTalkApiClientJsonDecodingTest {
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::respond);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void decodesRealDingTalkJsonResponsesThroughRestClient() {
        DingTalkProperties properties = new DingTalkProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setRequestIntervalMillis(0);

        DingTalkProperties.Organization organization = new DingTalkProperties.Organization();
        organization.setCorpCode("sebo");
        organization.setCorpName("Sebo");
        organization.setCorpId("ding-corp");
        organization.setClientId("app-key");
        organization.setClientSecret("app-secret");
        properties.setOrganizations(List.of(organization));

        DingTalkApiClient client = new DingTalkApiClient(
                properties, new DingTalkRetryExecutor(properties, ignored -> { }));

        List<DingOrganizationDirectorySnapshot> directories = client.listDirectories();

        assertThat(directories).singleElement().satisfies(directory -> {
            assertThat(directory.corpCode()).isEqualTo("sebo");
            assertThat(directory.departments()).hasSize(1);
            assertThat(directory.employees()).singleElement().satisfies(employee -> {
                assertThat(employee.dingUserId()).isEqualTo("user-001");
                assertThat(employee.employeeName()).isEqualTo("Test User");
            });
        });
    }

    private void respond(HttpExchange exchange) throws IOException {
        String response = switch (exchange.getRequestURI().getPath()) {
            case "/gettoken" -> """
                    {"errcode":0,"errmsg":"ok","access_token":"token","expires_in":7200}
                    """;
            case "/topapi/v2/department/listsub" -> """
                    {"errcode":0,"errmsg":"ok","result":[]}
                    """;
            case "/topapi/v2/user/list" -> """
                    {"errcode":0,"errmsg":"ok","result":{"has_more":false,"next_cursor":0,"list":[
                      {"userid":"user-001","unionid":"union-001","job_number":"E001","name":"Test User",
                       "mobile":"13800000000","active":true,"dept_id_list":[1]}
                    ]}}
                    """;
            default -> null;
        };
        if (response == null) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
