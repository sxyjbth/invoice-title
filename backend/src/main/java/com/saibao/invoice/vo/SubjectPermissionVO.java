package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 主体权限列表数据。 */
@Data
@Schema(description = "主体查看权限信息")
public class SubjectPermissionVO {
    @Schema(description = "授权对象所属企业业务编码") private String targetCorpCode;
    @Schema(description = "权限主键 ID") private Long id;
    @Schema(description = "主体 ID") private Long subjectId;
    @Schema(description = "主体名称") private String subjectName;
    @Schema(description = "对象类型：USER-员工，DEPARTMENT-部门") private String targetType;
    @Schema(description = "钉钉对象 ID") private String targetId;
    @Schema(description = "对象显示名称") private String targetName;
    @Schema(description = "权限状态：ENABLED-有效，DISABLED-停用") private String status;
    @Schema(description = "权限来源：MANUAL-财务维护，DING_SYNC-钉钉同步") private String source;
    @Schema(description = "最后更新时间") private LocalDateTime updatedAt;
}
