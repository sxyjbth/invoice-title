package com.saibao.invoice.exception;

/** 二维码令牌不存在或已超过有效期。 */
public class QrTokenExpiredException extends IllegalStateException {
    public QrTokenExpiredException() {
        super("二维码已过期，请重新获取二维码");
    }
}
