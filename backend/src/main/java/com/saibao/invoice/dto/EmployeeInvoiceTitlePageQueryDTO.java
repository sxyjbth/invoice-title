package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 员工端按主体权限查询抬头的分页参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "员工有权查看的发票抬头分页查询条件")
public class EmployeeInvoiceTitlePageQueryDTO extends PageQueryDTO {

    @Schema(description = "可选的主体 ID；为空时查询员工全部有权主体", example = "1")
    private Long subjectId;

    @Schema(description = "公司名称或纳税人识别号关键字")
    private String keyword;
}
