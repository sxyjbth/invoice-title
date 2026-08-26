package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Size(max = 5000, message = "员工选择不能超过 5000 个")
    @Schema(description = "部分可见模式下最终选中的员工目录主键 ID；空列表表示无人可见，null 仅用于兼容旧版请求")
    private List<@Positive(message = "员工目录主键 ID 必须大于 0") Long> selectedEmployeeIds;

    /** @deprecated 请使用 {@link #selectedEmployeeIds} 提交最终员工集合。 */
    @Deprecated
    @Schema(description = "已废弃：允许查看的部门目录主键 ID 列表", deprecated = true)
    private List<Long> departmentIds = new ArrayList<>();

    /** @deprecated 请使用 {@link #selectedEmployeeIds} 提交最终员工集合。 */
    @Deprecated
    @Schema(description = "已废弃：本次批量取消的部门目录主键 ID", deprecated = true)
    private List<Long> revokedDepartmentIds = new ArrayList<>();

    /** @deprecated 请使用 {@link #selectedEmployeeIds} 提交最终员工集合。 */
    @Deprecated
    @Valid
    @Schema(description = "已废弃：单独允许查看的员工规则", deprecated = true)
    private List<EmployeePermissionRuleDTO> employeeRules = new ArrayList<>();

    /** @deprecated 请使用 {@link #selectedEmployeeIds} 提交最终员工集合。 */
    @Deprecated
    @Schema(description = "已废弃：取消部门后又明确单独启用的员工目录主键 ID", deprecated = true)
    private List<Long> reenabledEmployeeIds = new ArrayList<>();

    /** @deprecated 请使用 {@link #selectedEmployeeIds} 提交最终员工集合。 */
    @Deprecated
    @Schema(description = "已废弃：在最终已选部门中明确单独关闭的员工目录主键 ID", deprecated = true)
    private List<Long> departmentExcludedEmployeeIds = new ArrayList<>();
}
