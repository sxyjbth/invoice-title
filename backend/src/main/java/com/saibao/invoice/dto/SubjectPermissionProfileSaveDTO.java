package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 主体查看权限整体保存请求。 */
@Data
@Schema(description = "主体查看权限整体保存请求")
public class SubjectPermissionProfileSaveDTO {
    @NotNull(message = "全员可见开关不能为空")
    @Schema(description = "是否允许全部在职员工查看", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean allEmployeeVisible;

    @Schema(description = "允许查看的部门目录主键 ID 列表；服务端自动解析钉钉部门 ID")
    private List<Long> departmentIds = new ArrayList<>();

    @Valid
    @Schema(description = "员工级允许或拒绝规则；优先级高于部门规则")
    private List<EmployeePermissionRuleDTO> employeeRules = new ArrayList<>();
}
