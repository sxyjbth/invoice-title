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
    @Schema(description = "按全员、部门和员工正向授权并集计算后的实际可见人数") private Long visibleCount;
    @Schema(description = "已授权部门") private List<DingDepartmentVO> departments;
    @Schema(description = "单独允许查看的员工规则") private List<EmployeePermissionRuleVO> employeeRules;
    @Schema(description = "在已授权部门中被单独关闭的员工目录主键 ID；已覆盖其全部当前部门授权边")
    private List<Long> departmentExcludedEmployeeIds;
    @Schema(description = "存在员工排除记录的已授权部门目录主键 ID")
    private List<Long> partiallySelectedDepartmentIds;
}
