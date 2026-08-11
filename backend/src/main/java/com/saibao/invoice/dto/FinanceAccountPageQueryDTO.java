package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 财务账号分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "财务账号分页查询参数")
public class FinanceAccountPageQueryDTO extends PageQueryDTO {
    @Schema(description = "账号或姓名关键字", example = "王财务") private String keyword;
    @Schema(description = "账号状态：ENABLED-启用，DISABLED-停用", example = "ENABLED") private String status;
}
