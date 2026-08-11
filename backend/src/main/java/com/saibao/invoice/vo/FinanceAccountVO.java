package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 网页财务端账号返回数据，不包含密码摘要。 */
@Data
@Schema(description = "网页财务端账号")
public class FinanceAccountVO {
    @Schema(description = "账号主键 ID", example = "2") private Long id;
    @Schema(description = "登录账号", example = "finance.wang") private String username;
    @Schema(description = "姓名", example = "王财务") private String displayName;
    @Schema(description = "角色：SUPER_ADMIN-超级管理员，FINANCE-财务人员", example = "FINANCE") private String roleType;
    @Schema(description = "状态：ENABLED-启用，DISABLED-停用", example = "ENABLED") private String status;
    @Schema(description = "密码最后修改时间") private LocalDateTime passwordChangedAt;
    @Schema(description = "最后登录时间") private LocalDateTime lastLoginAt;
    @Schema(description = "账号创建时间") private LocalDateTime createdAt;
}
