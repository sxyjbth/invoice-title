package com.saibao.invoice.integration.dingtalk;

import com.saibao.invoice.config.DingTalkProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 钉钉临时限流重试器。只重试 90002、90018 和钉钉明确返回的临时 QPS 限流。
 */
@Component
public class DingTalkRetryExecutor {
    private final DingTalkProperties properties;
    private final Sleeper sleeper;

    @Autowired
    public DingTalkRetryExecutor(DingTalkProperties properties) {
        this(properties, Thread::sleep);
    }

    public DingTalkRetryExecutor(DingTalkProperties properties, Sleeper sleeper) {
        this.properties = properties;
        this.sleeper = sleeper;
    }

    /** 执行单个正式通讯录请求，第一次请求前也执行请求间隔限速。 */
    public <T> T executeRequest(Supplier<T> operation) {
        int maxAttempts = positive(properties.getRequestMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            pause(properties.getRequestIntervalMillis());
            try {
                return operation.get();
            } catch (RuntimeException exception) {
                if (!isTemporaryRateLimit(exception) || attempt == maxAttempts) {
                    throw exception;
                }
                pause(properties.getRequestRetryDelayMillis());
            }
        }
        throw new IllegalStateException("钉钉单请求重试状态异常");
    }

    /** 执行整轮多企业同步；限流时从企业列表第一个企业重新开始。 */
    public <T> T executeSync(Supplier<T> operation) {
        int maxAttempts = positive(properties.getSyncMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException exception) {
                if (!isTemporaryRateLimit(exception) || attempt == maxAttempts) {
                    throw exception;
                }
                pause(properties.getSyncRetryDelayMillis());
            }
        }
        throw new IllegalStateException("钉钉整轮同步重试状态异常");
    }

    /** 递归检查异常链，避免外层包装后漏掉 90018。 */
    public boolean isTemporaryRateLimit(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DingTalkApiException apiException
                    && ("90002".equals(apiException.getErrorCode()) || "90018".equals(apiException.getErrorCode()))) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (message.contains("请求被暂时限制")
                    || message.toLowerCase().contains("qps流控"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private int positive(int value) {
        return Math.max(1, value);
    }

    private void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("钉钉通讯录同步重试被中断", exception);
        }
    }

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
