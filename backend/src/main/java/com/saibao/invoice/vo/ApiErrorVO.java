package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 统一业务错误返回。 */
@Schema(description = "接口错误")
public record ApiErrorVO(
        @Schema(description = "可供用户理解的错误说明", example = "账号或密码错误") String message
) {
}
