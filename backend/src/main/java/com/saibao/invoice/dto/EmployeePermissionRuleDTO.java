package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 员工级主体查看权限规则。 */
@Data
@Schema(description = "员工级主体查看权限规则")
public class EmployeePermissionRuleDTO {
    @NotNull(message = "员工目录 ID 不能为空")
    @Schema(description = "钉钉员工目录主键 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long employeeId;

    @NotNull(message = "权限效果不能为空")
    @Pattern(regexp = "ALLOW", message = "权限效果只能是 ALLOW")
    @Schema(description = "权限效果：ALLOW-单独允许员工查看",
            requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"ALLOW"})
    private String effect;
}
