package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 网页财务端账号密码登录参数。 */
@Data
@Schema(description = "网页财务端登录参数")
public class FinanceLoginDTO {
    @NotBlank
    @Schema(description = "登录账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "finance.wang")
    private String username;
    @NotBlank
    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "Finance@123")
    private String password;
}
