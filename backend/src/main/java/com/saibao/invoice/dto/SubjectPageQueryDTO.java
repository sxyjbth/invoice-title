package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 主体管理分页查询条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "主体管理分页查询条件")
public class SubjectPageQueryDTO extends PageQueryDTO {
    @Schema(description = "主体名称或主体编码关键字", example = "杭州")
    private String keyword;

    @Schema(description = "主体状态：ENABLED-启用，DISABLED-停用", example = "ENABLED")
    private String status;
}
