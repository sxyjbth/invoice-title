package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 财务端发票抬头分页查询条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "发票抬头分页查询条件")
public class InvoiceTitlePageQueryDTO extends PageQueryDTO {

    @Schema(description = "关键字，支持公司名称或纳税人识别号模糊匹配", example = "杭州赛宝")
    private String keyword;

    @Schema(description = "状态：DRAFT-草稿，PUBLISHED-已发布，DISABLED-已停用", example = "PUBLISHED")
    private String status;
}
