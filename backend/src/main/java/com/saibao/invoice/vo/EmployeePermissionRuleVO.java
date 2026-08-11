package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 带员工目录信息的员工级权限规则。 */
@Data
@Schema(description = "员工级主体查看权限规则")
public class EmployeePermissionRuleVO {
    @Schema(description = "企业业务编码") private String corpCode;
    @Schema(description = "企业名称") private String corpName;
    @Schema(description = "员工目录主键 ID") private Long employeeId;
    @Schema(description = "钉钉 userId") private String dingUserId;
    @Schema(description = "员工工号") private String employeeNo;
    @Schema(description = "员工姓名") private String employeeName;
    @Schema(description = "部门名称") private String departmentName;
    @Schema(description = "手机号") private String mobile;
    @Schema(description = "权限效果：ALLOW-允许查看，DENY-禁止查看；员工规则优先于部门规则")
    private String effect;
}
