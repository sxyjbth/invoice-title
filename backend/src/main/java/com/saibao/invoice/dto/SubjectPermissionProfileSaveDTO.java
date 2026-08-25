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

    @Schema(description = "兼容前端上报的本次取消部门 ID 列表；服务端会基于数据库原部门与本次部门独立推导撤销集合，不依赖此字段保障权限一致性")
    private List<Long> revokedDepartmentIds = new ArrayList<>();

    @Valid
    @Schema(description = "单独允许查看的员工规则；与全员、部门授权共同组成可见范围")
    private List<EmployeePermissionRuleDTO> employeeRules = new ArrayList<>();

    @Schema(description = "取消部门后又明确单独启用的员工目录主键 ID 列表；必须同时出现在员工允许规则中")
    private List<Long> reenabledEmployeeIds = new ArrayList<>();
}
