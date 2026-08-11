package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 部门通讯录分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "钉钉部门目录分页查询条件")
public class DepartmentDirectoryPageQueryDTO extends PageQueryDTO {
    @Schema(description = "部门名称模糊关键字")
    private String keyword;
}
