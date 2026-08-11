package com.saibao.invoice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 钉钉企业内部应用配置。生产密钥必须通过环境变量或配置中心注入，禁止写入仓库。
 */
@Data
@ConfigurationProperties(prefix = "sebo.dingtalk")
public class DingTalkProperties {
    /** 是否启用真实钉钉开放平台调用。 */
    private boolean enabled;
    /** 兼容旧单企业配置的业务编码。 */
    private String corpCode = "default";
    /** 兼容旧单企业配置的企业名称。 */
    private String corpName = "默认钉钉企业";
    /** 兼容旧单企业配置的企业 corpId。 */
    private String corpId;
    /** 兼容旧单企业配置的应用 ID。 */
    private String appId;
    /** 兼容旧单企业配置的应用 AgentId。 */
    private String agentId;
    /** 兼容旧单企业配置的 ClientId（钉钉旧接口中对应 AppKey）。 */
    private String clientId;
    /** 兼容旧单企业配置的 ClientSecret（钉钉旧接口中对应 AppSecret）。 */
    private String clientSecret;
    /** 兼容项目早期使用的 AppKey 配置名。 */
    private String appKey;
    /** 兼容项目早期使用的 AppSecret 配置名。 */
    private String appSecret;
    /** 多企业配置；非空时优先于所有旧单企业字段。 */
    private List<Organization> organizations = new ArrayList<>();
    /** 钉钉根部门 ID，通常为 1。 */
    private String rootDepartmentId = "1";
    /** 钉钉服务端 API 根地址。 */
    private String baseUrl = "https://oapi.dingtalk.com";
    /** 定时同步 cron。 */
    private String syncCron = "0 0 * * * *";
    /** 定时同步时区。 */
    private String syncZone = "Asia/Shanghai";
    /** 单个通讯录请求最多执行次数，包含第一次请求。 */
    private int requestMaxAttempts = 3;
    /** 每个正式通讯录请求前的限速等待毫秒数。 */
    private long requestIntervalMillis = 120L;
    /** 单请求触发临时限流后的重试等待毫秒数。 */
    private long requestRetryDelayMillis = 1200L;
    /** 整个双企业同步任务最多执行轮数，包含第一轮。 */
    private int syncMaxAttempts = 3;
    /** 整轮同步触发临时限流后的重试等待毫秒数。 */
    private long syncRetryDelayMillis = 2000L;

    /**
     * 返回本次实际使用的企业配置。多企业列表非空时不再使用旧单企业字段。
     */
    public List<Organization> activeOrganizations() {
        if (organizations != null && !organizations.isEmpty()) {
            return Collections.unmodifiableList(organizations);
        }
        Organization legacy = new Organization();
        legacy.setCorpCode(corpCode);
        legacy.setCorpName(corpName);
        legacy.setCorpId(corpId);
        legacy.setAppId(appId);
        legacy.setAgentId(agentId);
        legacy.setClientId(StringUtils.hasText(clientId) ? clientId : appKey);
        legacy.setClientSecret(StringUtils.hasText(clientSecret) ? clientSecret : appSecret);
        return List.of(legacy);
    }

    /** 校验同步所需的开关、业务编码和钉钉应用密钥。 */
    public void requireEnabled() {
        if (!enabled) {
            throw new IllegalArgumentException("钉钉通讯录同步未启用，请设置 SEBO_DINGTALK_ENABLED=true");
        }
        Set<String> corpCodes = new HashSet<>();
        for (Organization organization : activeOrganizations()) {
            if (!StringUtils.hasText(organization.getCorpCode())
                    || !StringUtils.hasText(organization.getCorpId())
                    || !StringUtils.hasText(organization.getClientId())
                    || !StringUtils.hasText(organization.getClientSecret())) {
                throw new IllegalArgumentException("钉钉企业配置缺少 corpCode、corpId、clientId 或 clientSecret");
            }
            if (!corpCodes.add(organization.getCorpCode())) {
                throw new IllegalArgumentException("钉钉企业业务编码重复：" + organization.getCorpCode());
            }
        }
    }

    /** 单个钉钉企业的开放平台应用配置。 */
    @Data
    public static class Organization {
        /** 业务编码，例如 sebo、walden；作为本地复合身份的一部分。 */
        private String corpCode;
        /** 企业展示名称。 */
        private String corpName;
        /** 钉钉企业 corpId。 */
        private String corpId;
        /** 钉钉应用 ID。 */
        private String appId;
        /** 钉钉应用 AgentId。 */
        private String agentId;
        /** 钉钉应用 ClientId（旧接口参数 appkey）。 */
        private String clientId;
        /** 钉钉应用 ClientSecret（旧接口参数 appsecret）。 */
        private String clientSecret;
    }
}
