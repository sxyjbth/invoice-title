package com.saibao.invoice.integration.dingtalk;

/** 钉钉业务响应异常；仅保留错误码和安全错误信息，不携带密钥或 access_token。 */
public class DingTalkApiException extends IllegalStateException {
    private final String errorCode;

    public DingTalkApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
