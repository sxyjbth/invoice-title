package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 员工通讯录分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "钉钉员工目录分页查询条件")
public class EmployeeDirectoryPageQueryDTO extends PageQueryDTO {
    @Schema(description = "企业业务编码；为空时查询全部企业", example = "sebo")
    private String corpCode;

    @Schema(description = "姓名、工号、部门名称或手机号模糊关键字")
    private String keyword;
    @Schema(description = "部门目录主键 ID；传入后按员工的直接任职部门查询，为空时查询全部部门", example = "1")
    private Long departmentId;

    @Positive
    @Schema(description = "主体主键 ID；传入后返回员工对该主体的最终查看权限", example = "1")
    private Long subjectId;

    @Pattern(regexp = "ENABLED|DISABLED", message = "权限状态只支持 ENABLED 或 DISABLED")
    @Schema(description = "最终查看权限筛选：ENABLED-已启用，DISABLED-已关闭；需同时传入主体 ID", example = "ENABLED")
    private String permissionStatus;
}
