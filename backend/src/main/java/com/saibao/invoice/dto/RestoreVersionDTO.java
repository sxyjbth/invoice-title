package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 恢复历史版本请求。 */
@Data
@Schema(description = "恢复历史版本为新草稿的请求")
public class RestoreVersionDTO {

    @Schema(description = "当前操作人的钉钉用户 ID", example = "ding-user-finance", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作人不能为空")
    private String operatorUserId;
}
