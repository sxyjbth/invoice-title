package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 新增员工或部门的主体授权请求。 */
@Data
@Schema(description = "主体查看权限保存请求")
public class SubjectPermissionSaveDTO {
    @Schema(description = "授权对象所属企业业务编码：sebo-赛宝，walden-瓦尔登", example = "sebo")
    private String targetCorpCode;

    @Schema(description = "授权主体 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "授权主体不能为空")
    private Long subjectId;

    @Schema(description = "授权对象类型：USER-员工，DEPARTMENT-部门", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权对象类型不能为空")
    private String targetType;

    @Schema(description = "钉钉 userId 或 departmentId", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "钉钉对象 ID 不能为空")
    private String targetId;

    @Schema(description = "授权对象显示名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "授权对象名称不能为空")
    private String targetName;

    @Schema(description = "操作人的钉钉用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作人不能为空")
    private String operatorUserId;
}
