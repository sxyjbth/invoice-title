package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 超级管理员创建财务账号的请求参数。 */
@Data
@Schema(description = "创建财务账号参数")
public class CreateFinanceAccountDTO {
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9._-]{4,50}$", message = "账号只能包含字母、数字、点、下划线或短横线，长度 4-50 位")
    @Schema(description = "登录账号，系统内唯一", requiredMode = Schema.RequiredMode.REQUIRED, example = "finance.wang")
    private String username;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "财务人员姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "王财务")
    private String displayName;

    @NotBlank
    @Size(min = 8, max = 72)
    @Schema(description = "初始密码，8-72 位且必须同时包含字母和数字", requiredMode = Schema.RequiredMode.REQUIRED, example = "Finance@123")
    private String initialPassword;
}
