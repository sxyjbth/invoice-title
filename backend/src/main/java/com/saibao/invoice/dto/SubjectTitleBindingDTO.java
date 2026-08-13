package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 财务端为主体绑定发票抬头的请求参数。 */
@Data
@Schema(description = "主体绑定抬头请求")
public class SubjectTitleBindingDTO {
    @NotNull(message = "请选择要绑定的发票抬头")
    @Schema(description = "要绑定的发票抬头主键 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long titleId;

    @NotBlank(message = "操作人不能为空")
    @Schema(description = "操作人的账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "finance.wang")
    private String operatorUserId;
}
