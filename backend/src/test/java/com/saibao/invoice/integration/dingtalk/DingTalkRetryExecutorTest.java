package com.saibao.invoice.integration.dingtalk;

import com.saibao.invoice.config.DingTalkProperties;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DingTalkRetryExecutorTest {

    @Test
    void requestShouldRetryTemporaryRateLimitAtMostThreeAttempts() {
        DingTalkProperties properties = zeroDelayProperties();
        DingTalkRetryExecutor executor = new DingTalkRetryExecutor(properties, ignored -> { });
        AtomicInteger attempts = new AtomicInteger();

        String result = executor.executeRequest(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new DingTalkApiException("90018", "qps流控");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void requestShouldNotRetryConfigurationOrPermissionErrors() {
        DingTalkRetryExecutor executor = new DingTalkRetryExecutor(zeroDelayProperties(), ignored -> { });
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> executor.executeRequest(() -> {
            attempts.incrementAndGet();
            throw new DingTalkApiException("40014", "无效的access_token");
        })).isInstanceOf(DingTalkApiException.class);
        assertThat(attempts).hasValue(1);
    }

    @Test
    void wholeSyncShouldRetryFromBeginningForBothRateLimitCodes() {
        DingTalkRetryExecutor executor = new DingTalkRetryExecutor(zeroDelayProperties(), ignored -> { });
        AtomicInteger rounds = new AtomicInteger();

        int result = executor.executeSync(() -> {
            if (rounds.incrementAndGet() == 1) {
                throw new DingTalkApiException("90002", "请求被暂时限制");
            }
            return 2;
        });

        assertThat(result).isEqualTo(2);
        assertThat(rounds).hasValue(2);
    }

    private DingTalkProperties zeroDelayProperties() {
        DingTalkProperties properties = new DingTalkProperties();
        properties.setRequestMaxAttempts(3);
        properties.setRequestIntervalMillis(0);
        properties.setRequestRetryDelayMillis(0);
        properties.setSyncMaxAttempts(3);
        properties.setSyncRetryDelayMillis(0);
        return properties;
    }
}
