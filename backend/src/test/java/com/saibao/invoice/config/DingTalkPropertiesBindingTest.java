package com.saibao.invoice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DingTalkPropertiesBindingTest {

    @Test
    void shouldBindTwoOrganizationsFromProductionStyleListConfiguration() {
        var source = new MapConfigurationPropertySource(Map.ofEntries(
                Map.entry("sebo.dingtalk.enabled", "true"),
                Map.entry("sebo.dingtalk.organizations[0].corp-code", "sebo"),
                Map.entry("sebo.dingtalk.organizations[0].corp-name", "赛宝绿创能源技术（上海）有限公司"),
                Map.entry("sebo.dingtalk.organizations[0].corp-id", "ding-sebo"),
                Map.entry("sebo.dingtalk.organizations[0].client-id", "sebo-client"),
                Map.entry("sebo.dingtalk.organizations[0].client-secret", "sebo-secret"),
                Map.entry("sebo.dingtalk.organizations[1].corp-code", "walden"),
                Map.entry("sebo.dingtalk.organizations[1].corp-name", "瓦尔登环境科学研究院（北京）有限公司"),
                Map.entry("sebo.dingtalk.organizations[1].corp-id", "ding-walden"),
                Map.entry("sebo.dingtalk.organizations[1].client-id", "walden-client"),
                Map.entry("sebo.dingtalk.organizations[1].client-secret", "walden-secret")));

        DingTalkProperties properties = new Binder(source)
                .bind("sebo.dingtalk", Bindable.of(DingTalkProperties.class))
                .orElseThrow(() -> new AssertionError("钉钉配置绑定失败"));

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.activeOrganizations()).extracting(DingTalkProperties.Organization::getCorpCode)
                .containsExactly("sebo", "walden");
        assertThat(properties.activeOrganizations().get(1).getCorpName())
                .isEqualTo("瓦尔登环境科学研究院（北京）有限公司");
    }

    @Test
    void shouldBindExactProductionEnvironmentVariableNames() {
        Map<String, Object> rawEnvironment = Map.ofEntries(
                Map.entry("SEBO_DINGTALK_ENABLED", "true"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_0_CORP_CODE", "sebo"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_0_CORP_NAME", "赛宝"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_0_CORP_ID", "corp-sebo"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_0_CLIENT_ID", "client-sebo"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_0_CLIENT_SECRET", "secret-sebo"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_1_CORP_CODE", "walden"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_1_CORP_NAME", "瓦尔登"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_1_CORP_ID", "corp-walden"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_1_CLIENT_ID", "client-walden"),
                Map.entry("SEBO_DINGTALK_ORGANIZATIONS_1_CLIENT_SECRET", "secret-walden"));
        assertThat(DingTalkOrganizationEnvironmentPostProcessor.translate(rawEnvironment))
                .containsEntry("sebo.dingtalk.organizations[0].corp-code", "sebo");
        var environment = new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, rawEnvironment);

        StandardEnvironment standardEnvironment = new StandardEnvironment();
        standardEnvironment.getPropertySources().replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environment);
        new DingTalkOrganizationEnvironmentPostProcessor()
                .postProcessEnvironment(standardEnvironment, null);

        DingTalkProperties properties = new Binder(ConfigurationPropertySources.from(standardEnvironment.getPropertySources()))
                .bind("sebo.dingtalk", Bindable.of(DingTalkProperties.class))
                .orElseThrow(() -> new AssertionError("生产环境变量绑定失败"));

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.activeOrganizations())
                .extracting(DingTalkProperties.Organization::getCorpCode)
                .containsExactly("sebo", "walden");
    }
}
