package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 财务端可选择的钉钉部门。 */
@Data
@Schema(description = "钉钉部门目录信息")
public class DingDepartmentVO {
    @Schema(description = "企业业务编码") private String corpCode;
    @Schema(description = "企业名称") private String corpName;
    @Schema(description = "部门目录主键 ID") private Long id;
    @Schema(description = "钉钉部门 ID") private String dingDepartmentId;
    @Schema(description = "部门名称") private String departmentName;
    @Schema(description = "上级钉钉部门 ID") private String parentDepartmentId;
    @Schema(description = "状态：ENABLED-有效，DISABLED-已停用") private String status;
    @Schema(description = "部门内在职员工数量") private Long employeeCount;
}
