package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 主体管理列表数据。 */
@Data
@Schema(description = "展示主体信息")
public class InvoiceSubjectVO {
    @Schema(description = "主体主键 ID") private Long id;
    @Schema(description = "主体编码") private String subjectCode;
    @Schema(description = "主体名称") private String subjectName;
    @Schema(description = "状态：ENABLED-启用，DISABLED-停用") private String status;
    @Schema(description = "展示排序") private Integer sortNo;
    @Schema(description = "关联抬头数量") private Long titleCount;
    @Schema(description = "有效授权对象数量；接入钉钉通讯录后可进一步展开部门覆盖人数") private Long employeeCount;
    @Schema(description = "最后更新人的钉钉用户 ID") private String updatedBy;
    @Schema(description = "最后更新时间") private LocalDateTime updatedAt;
}
