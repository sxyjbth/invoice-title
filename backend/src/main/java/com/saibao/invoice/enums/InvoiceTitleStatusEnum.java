package com.saibao.invoice.enums;

import lombok.Getter;

/** 发票抬头及版本状态。 */
@Getter
public enum InvoiceTitleStatusEnum {
    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    DISABLED("DISABLED", "已停用");

    private final String code;
    private final String description;

    InvoiceTitleStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
