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

    @Schema(description = "本次批量取消的部门目录主键 ID；服务端会与数据库原部门和本次部门推导的撤销集合合并，关闭这些部门的全部在职成员")
    private List<Long> revokedDepartmentIds = new ArrayList<>();

    @Valid
    @Schema(description = "单独允许查看的员工规则；与全员、部门授权共同组成可见范围")
    private List<EmployeePermissionRuleDTO> employeeRules = new ArrayList<>();

    @Schema(description = "取消部门后又明确单独启用的员工目录主键 ID 列表；必须同时出现在员工允许规则中")
    private List<Long> reenabledEmployeeIds = new ArrayList<>();

    @Schema(description = "在最终已选部门中明确单独关闭的员工目录主键 ID；服务端按员工全部已选部门归属生成授权边例外")
    private List<Long> departmentExcludedEmployeeIds = new ArrayList<>();
}
