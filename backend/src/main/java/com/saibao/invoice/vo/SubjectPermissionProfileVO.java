package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 主体查看权限配置及实际可见人数。 */
@Data
@Schema(description = "主体查看权限配置")
public class SubjectPermissionProfileVO {
    @Schema(description = "主体主键 ID") private Long subjectId;
    @Schema(description = "主体名称") private String subjectName;
    @Schema(description = "是否全员可见") private Boolean allEmployeeVisible;
    @Schema(description = "实际可见人数：全员模式为全部在职员工数，部分可见模式为最终精确员工集合中的在职员工数") private Long visibleCount;
    @Schema(description = "部分可见模式下最终选中的员工目录主键 ID")
    private List<Long> selectedEmployeeIds;
    @Schema(description = "部分可见模式下最终选中的在职员工明细")
    private List<DingEmployeeVO> selectedEmployees;
    @Schema(description = "按企业分组的最终已选在职员工")
    private List<EmployeeSelectionGroupVO> employeeGroups;
    @Deprecated
    @Schema(description = "已废弃：兼容旧版客户端，固定返回空列表", deprecated = true) private List<DingDepartmentVO> departments;
    @Deprecated
    @Schema(description = "已废弃：兼容旧版客户端；当前仅返回精确员工允许规则", deprecated = true) private List<EmployeePermissionRuleVO> employeeRules;
    @Deprecated
    @Schema(description = "已废弃：兼容旧版客户端，固定返回空列表", deprecated = true)
    private List<Long> departmentExcludedEmployeeIds;
    @Deprecated
    @Schema(description = "已废弃：兼容旧版客户端，固定返回空列表", deprecated = true)
    private List<Long> partiallySelectedDepartmentIds;
}
