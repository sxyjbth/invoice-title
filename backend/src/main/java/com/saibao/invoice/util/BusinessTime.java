package com.saibao.invoice.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 业务时间统一使用中国标准时区，避免服务器或容器采用 UTC 时写入错误的本地时间。
 */
public final class BusinessTime {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private BusinessTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }
}
