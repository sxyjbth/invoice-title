package com.saibao.invoice.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把约定的 SEBO_DINGTALK_ORGANIZATIONS_0_* 环境变量转换为 Spring 可绑定的列表键。
 * Spring 默认环境变量映射无法可靠表达 List 下标，因此必须在配置绑定前显式转换。
 */
public class DingTalkOrganizationEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_SOURCE_NAME = "seboDingTalkOrganizationEnvironment";
    private static final Pattern ORGANIZATION_VARIABLE = Pattern.compile(
            "^SEBO_DINGTALK_ORGANIZATIONS_(\\d+)_(CORP_CODE|CORP_NAME|CORP_ID|APP_ID|AGENT_ID|CLIENT_ID|CLIENT_SECRET)$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        PropertySource<?> source = environment.getPropertySources()
                .get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
            return;
        }
        Map<String, Object> rawEnvironment = new LinkedHashMap<>();
        for (String propertyName : enumerable.getPropertyNames()) {
            rawEnvironment.put(propertyName, enumerable.getProperty(propertyName));
        }
        Map<String, Object> translated = translate(rawEnvironment);
        if (!translated.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, translated));
        }
    }

    /** 将原始环境变量映射为 sebo.dingtalk.organizations[index].field。 */
    static Map<String, Object> translate(Map<String, ?> environment) {
        Map<String, Object> translated = new LinkedHashMap<>();
        environment.forEach((name, value) -> {
            Matcher matcher = ORGANIZATION_VARIABLE.matcher(name.toUpperCase(Locale.ROOT));
            if (!matcher.matches()) return;
            String index = matcher.group(1);
            String field = toKebabCase(matcher.group(2));
            translated.put("sebo.dingtalk.organizations[" + index + "]." + field, value);
        });
        return translated;
    }

    private static String toKebabCase(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
