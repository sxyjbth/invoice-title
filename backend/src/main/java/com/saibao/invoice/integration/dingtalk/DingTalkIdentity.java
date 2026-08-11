package com.saibao.invoice.integration.dingtalk;

/** 钉钉免登码换取到的可信用户身份。 */
public record DingTalkIdentity(String dingUserId, String unionId) {
}
