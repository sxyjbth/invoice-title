package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 导入失败行分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "导入失败行分页查询参数")
public class ImportRowErrorPageQueryDTO extends PageQueryDTO {

    @NotNull
    @Schema(description = "导入任务主键 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long taskId;
}
