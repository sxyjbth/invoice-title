package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 财务端可选择的钉钉员工。 */
@Data
@Schema(description = "钉钉员工目录信息")
public class DingEmployeeVO {
    @Schema(description = "企业业务编码：sebo-赛宝，walden-瓦尔登") private String corpCode;
    @Schema(description = "员工所属钉钉企业名称") private String corpName;
    @Schema(description = "员工目录主键 ID") private Long id;
    @Schema(description = "钉钉 userId") private String dingUserId;
    @Schema(description = "员工工号") private String employeeNo;
    @Schema(description = "员工姓名") private String employeeName;
    @Schema(description = "所属部门目录主键 ID") private Long departmentId;
    @Schema(description = "所属的全部有效部门目录主键 ID，用于多部门权限联动")
    private List<Long> departmentIds;
    @Schema(description = "所属部门名称") private String departmentName;
    @Schema(description = "手机号") private String mobile;
    @Schema(description = "状态：ACTIVE-在职，INACTIVE-离职或停用") private String status;
    @Schema(description = "员工对查询主体的最终查看权限：true-启用，false-关闭；未传主体 ID 时为空")
    private Boolean permissionEnabled;
}
