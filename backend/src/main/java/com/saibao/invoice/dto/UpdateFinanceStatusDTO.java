package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 超级管理员启用或停用财务账号参数。 */
@Data
@Schema(description = "更新财务账号状态参数")
public class UpdateFinanceStatusDTO {
    @Pattern(regexp = "ENABLED|DISABLED")
    @Schema(description = "账号状态：ENABLED-启用，DISABLED-停用", requiredMode = Schema.RequiredMode.REQUIRED, example = "DISABLED")
    private String status;
}
