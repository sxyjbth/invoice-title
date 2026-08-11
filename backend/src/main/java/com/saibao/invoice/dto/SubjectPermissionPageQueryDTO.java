package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 主体权限分页查询条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "主体查看权限分页查询条件")
public class SubjectPermissionPageQueryDTO extends PageQueryDTO {
    @Schema(description = "主体 ID", example = "1")
    private Long subjectId;

    @Schema(description = "授权对象名称或钉钉对象 ID 关键字")
    private String keyword;

    @Schema(description = "授权对象类型：USER-员工，DEPARTMENT-部门")
    private String targetType;

    @Schema(description = "权限状态：ENABLED-有效，DISABLED-停用")
    private String status;
}
