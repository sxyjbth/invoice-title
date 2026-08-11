package com.saibao.invoice.integration.dingtalk;

import com.saibao.invoice.config.DingTalkProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 钉钉企业内部应用服务端客户端。按配置列表顺序逐企业拉取，任何异常都不会输出应用密钥或令牌。
 */
@Component
public class DingTalkApiClient implements DingTalkClient {
    private final DingTalkProperties properties;
    private final DingTalkRetryExecutor retryExecutor;
    private final RestClient restClient;
    private final Map<String, AccessTokenEntry> tokenCache = new ConcurrentHashMap<>();

    public DingTalkApiClient(DingTalkProperties properties, DingTalkRetryExecutor retryExecutor) {
        this.properties = properties;
        this.retryExecutor = retryExecutor;
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public DingTalkIdentity resolveIdentity(String authCode) {
        List<DingTalkProperties.Organization> organizations = properties.activeOrganizations();
        if (organizations.size() != 1) {
            throw new IllegalArgumentException("多企业钉钉登录必须传入 corpCode");
        }
        return resolveIdentity(organizations.getFirst().getCorpCode(), authCode);
    }

    @Override
    public DingTalkIdentity resolveIdentity(String corpCode, String authCode) {
        properties.requireEnabled();
        if (authCode == null || authCode.isBlank()) {
            throw new IllegalArgumentException("钉钉免登码不能为空");
        }
        DingTalkProperties.Organization organization = requireOrganization(corpCode);
        JsonNode result = post(organization, "/topapi/v2/user/getuserinfo", Map.of("code", authCode));
        String userId = text(result, "userid");
        if (userId == null) {
            throw new SecurityException("钉钉免登码无效或已过期，请重新进入工作台");
        }
        return new DingTalkIdentity(userId, text(result, "unionid"));
    }

    /**
     * 顺序严格为：企业 access_token → 企业部门 → 企业员工；全部企业成功后才把快照交给同步服务。
     */
    @Override
    public List<DingOrganizationDirectorySnapshot> listDirectories() {
        properties.requireEnabled();
        List<DingOrganizationDirectorySnapshot> snapshots = new ArrayList<>();
        for (DingTalkProperties.Organization organization : properties.activeOrganizations()) {
            String accessToken = accessToken(organization);
            List<DingDepartmentSnapshot> departments = listDepartments(organization, accessToken);
            List<DingEmployeeSnapshot> employees = listEmployees(organization, accessToken, departments);
            snapshots.add(new DingOrganizationDirectorySnapshot(
                    organization.getCorpCode(),
                    organization.getCorpName(),
                    organization.getCorpId(),
                    departments,
                    employees));
        }
        return List.copyOf(snapshots);
    }

    @Override
    public List<DingDepartmentSnapshot> listDepartments() {
        return listDirectories().stream().flatMap(item -> item.departments().stream()).toList();
    }

    @Override
    public List<DingEmployeeSnapshot> listEmployees() {
        return listDirectories().stream().flatMap(item -> item.employees().stream()).toList();
    }

    @Override
    public String corpId() {
        List<DingTalkProperties.Organization> organizations = properties.activeOrganizations();
        return organizations.size() == 1 ? organizations.getFirst().getCorpId() : null;
    }

    private List<DingDepartmentSnapshot> listDepartments(
            DingTalkProperties.Organization organization, String accessToken) {
        String rootId = properties.getRootDepartmentId();
        Map<String, DingDepartmentSnapshot> departments = new LinkedHashMap<>();
        departments.put(rootId, new DingDepartmentSnapshot(rootId, "企业根部门", null, 0));

        ArrayDeque<String> pending = new ArrayDeque<>();
        pending.add(rootId);
        while (!pending.isEmpty()) {
            String parentId = pending.removeFirst();
            JsonNode result = post(organization, accessToken, "/topapi/v2/department/listsub",
                    Map.of("dept_id", numericOrText(parentId)));
            if (!result.isArray()) continue;
            for (JsonNode item : result) {
                String id = text(item, "dept_id");
                if (id == null || departments.containsKey(id)) continue;
                int sortNo = item.path("order").canConvertToInt() ? item.path("order").asInt() : 0;
                departments.put(id, new DingDepartmentSnapshot(
                        id, item.path("name").asText("未命名部门"), parentId, sortNo));
                pending.add(id);
            }
        }
        return List.copyOf(departments.values());
    }

    private List<DingEmployeeSnapshot> listEmployees(
            DingTalkProperties.Organization organization,
            String accessToken,
            List<DingDepartmentSnapshot> departments) {
        Map<String, MutableEmployee> employees = new LinkedHashMap<>();
        for (DingDepartmentSnapshot department : departments) {
            long cursor = 0L;
            boolean hasMore;
            do {
                JsonNode result = post(organization, accessToken, "/topapi/v2/user/list", Map.of(
                        "dept_id", numericOrText(department.dingDepartmentId()),
                        "cursor", cursor,
                        "size", 100));
                JsonNode list = result.path("list");
                if (list.isArray()) {
                    for (JsonNode user : list) {
                        mergeEmployee(employees, user, department.dingDepartmentId());
                    }
                }
                hasMore = result.path("has_more").asBoolean(false);
                cursor = result.path("next_cursor").asLong(0L);
            } while (hasMore);
        }
        return employees.values().stream().map(MutableEmployee::snapshot).toList();
    }

    private void mergeEmployee(Map<String, MutableEmployee> employees, JsonNode user, String fallbackDepartmentId) {
        String userId = text(user, "userid");
        if (userId == null) return;
        MutableEmployee employee = employees.computeIfAbsent(userId, ignored -> new MutableEmployee(
                userId,
                text(user, "unionid"),
                defaultText(text(user, "job_number"), userId),
                user.path("name").asText("未命名员工"),
                text(user, "mobile"),
                !user.has("active") || user.path("active").asBoolean()));
        JsonNode departmentIds = user.path("dept_id_list");
        if (departmentIds.isArray()) {
            departmentIds.forEach(node -> employee.departmentIds.add(node.asText()));
        }
        if (employee.departmentIds.isEmpty()) {
            employee.departmentIds.add(fallbackDepartmentId);
        }
    }

    private JsonNode post(DingTalkProperties.Organization organization, String path, Map<String, ?> body) {
        return post(organization, accessToken(organization), path, body);
    }

    private JsonNode post(
            DingTalkProperties.Organization organization,
            String accessToken,
            String path,
            Map<String, ?> body) {
        return retryExecutor.executeRequest(() -> postOnce(accessToken, path, body));
    }

    private JsonNode postOnce(String accessToken, String path, Map<String, ?> body) {
        try {
            JsonNode response = restClient.post()
                    .uri(builder -> builder.path(path).queryParam("access_token", accessToken).build())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return resultOf(response, path);
        } catch (DingTalkApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new IllegalStateException("钉钉接口网络请求失败：" + path, exception);
        }
    }

    /** access_token 获取不进入单请求重试；错误密钥不会重试，临时限流仅由整轮同步重试处理。 */
    private synchronized String accessToken(DingTalkProperties.Organization organization) {
        AccessTokenEntry cached = tokenCache.get(organization.getCorpCode());
        if (cached != null && cached.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return cached.token();
        }
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(builder -> builder.path("/gettoken")
                            .queryParam("appkey", organization.getClientId())
                            .queryParam("appsecret", organization.getClientSecret())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("钉钉 access_token 网络请求失败：" + organization.getCorpCode(), exception);
        }
        checkSuccess(response, "/gettoken");
        String token = text(response, "access_token");
        if (token == null) {
            throw new IllegalStateException("钉钉 access_token 响应缺少令牌：" + organization.getCorpCode());
        }
        AccessTokenEntry entry = new AccessTokenEntry(
                token, Instant.now().plusSeconds(response.path("expires_in").asLong(7200)));
        tokenCache.put(organization.getCorpCode(), entry);
        return token;
    }

    private DingTalkProperties.Organization requireOrganization(String corpCode) {
        if (corpCode == null || corpCode.isBlank()) {
            throw new IllegalArgumentException("企业业务编码 corpCode 不能为空");
        }
        return properties.activeOrganizations().stream()
                .filter(item -> corpCode.equals(item.getCorpCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未配置的钉钉企业业务编码：" + corpCode));
    }

    private JsonNode resultOf(JsonNode response, String operation) {
        checkSuccess(response, operation);
        return response.path("result");
    }

    private void checkSuccess(JsonNode response, String operation) {
        if (response == null) {
            throw new IllegalStateException("钉钉接口无响应：" + operation);
        }
        String errorCode = response.path("errcode").asText("0");
        if (!"0".equals(errorCode)) {
            String errorMessage = response.path("errmsg").asText("未知错误");
            throw new DingTalkApiException(errorCode,
                    "钉钉接口调用失败：" + operation + "，errcode=" + errorCode + "，errmsg=" + errorMessage);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Object numericOrText(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private record AccessTokenEntry(String token, Instant expiresAt) { }

    private static final class MutableEmployee {
        private final String userId;
        private final String unionId;
        private final String employeeNo;
        private final String employeeName;
        private final String mobile;
        private final boolean active;
        private final Set<String> departmentIds = new LinkedHashSet<>();

        private MutableEmployee(String userId, String unionId, String employeeNo,
                                String employeeName, String mobile, boolean active) {
            this.userId = userId;
            this.unionId = unionId;
            this.employeeNo = employeeNo;
            this.employeeName = employeeName;
            this.mobile = mobile;
            this.active = active;
        }

        private DingEmployeeSnapshot snapshot() {
            return new DingEmployeeSnapshot(userId, unionId, employeeNo, employeeName,
                    List.copyOf(departmentIds), mobile, active);
        }
    }
}
