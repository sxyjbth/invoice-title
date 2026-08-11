package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 超级管理员重置财务密码参数。 */
@Data
@Schema(description = "重置财务密码参数")
public class ResetFinancePasswordDTO {
    @NotBlank
    @Size(min = 8, max = 72)
    @Schema(description = "管理员指定的新密码，8-72 位且必须同时包含字母和数字", requiredMode = Schema.RequiredMode.REQUIRED, example = "Reset@789")
    private String newPassword;
}
