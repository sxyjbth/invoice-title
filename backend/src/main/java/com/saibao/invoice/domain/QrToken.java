package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 短时二维码令牌领域对象。 */
@Data
public class QrToken {
    private Long id;
    private String token;
    private Long titleId;
    private Long versionId;
    /** 生成二维码的本地员工目录主键，天然包含企业维度。 */
    private Long employeeId;
    /** 旧版本兼容字段；新令牌不再用裸 dingUserId 做权限判断。 */
    private String dingUserId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
