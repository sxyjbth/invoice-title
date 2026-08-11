package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 新增或编辑主体请求。 */
@Data
@Schema(description = "主体保存请求")
public class InvoiceSubjectSaveDTO {
    @Schema(description = "主体编码，业务内唯一", example = "HZ", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "主体编码不能为空")
    private String subjectCode;

    @Schema(description = "主体名称", example = "杭州主体", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "主体名称不能为空")
    private String subjectName;

    @Schema(description = "主体状态：ENABLED-启用，DISABLED-停用", example = "ENABLED")
    private String status = "ENABLED";

    @Schema(description = "展示排序，数值越小越靠前", example = "10")
    private Integer sortNo = 0;

    @Schema(description = "操作人的钉钉用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作人不能为空")
    private String operatorUserId;
}
