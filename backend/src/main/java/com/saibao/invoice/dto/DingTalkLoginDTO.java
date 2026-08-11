package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 员工端钉钉免登请求。 */
@Data
@Schema(description = "员工端钉钉免登参数")
public class DingTalkLoginDTO {
    @NotBlank(message = "企业业务编码不能为空")
    @Schema(description = "钉钉企业业务编码：sebo-赛宝，walden-瓦尔登",
            example = "sebo", requiredMode = Schema.RequiredMode.REQUIRED)
    private String corpCode;

    @NotBlank(message = "钉钉免登码不能为空")
    @Schema(description = "钉钉 JSAPI requestAuthCode 返回的一次性免登码",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String authCode;
}
