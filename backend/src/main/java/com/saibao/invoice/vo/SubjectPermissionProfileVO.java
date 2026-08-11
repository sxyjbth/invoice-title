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
    @Schema(description = "按部门规则和员工覆盖规则计算后的实际可见人数") private Long visibleCount;
    @Schema(description = "已授权部门") private List<DingDepartmentVO> departments;
    @Schema(description = "员工级允许或拒绝规则") private List<EmployeePermissionRuleVO> employeeRules;
}
