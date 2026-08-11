package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 当前登录财务修改本人密码参数。 */
@Data
@Schema(description = "修改本人密码参数")
public class ChangeOwnPasswordDTO {
    @NotBlank
    @Schema(description = "当前密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;
    @NotBlank
    @Size(min = 8, max = 72)
    @Schema(description = "新密码，8-72 位且必须同时包含字母和数字", requiredMode = Schema.RequiredMode.REQUIRED, example = "Changed@456")
    private String newPassword;
}
