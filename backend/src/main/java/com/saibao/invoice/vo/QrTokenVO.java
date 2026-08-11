package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 员工端临时二维码令牌。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "临时抬头二维码令牌")
public class QrTokenVO {

    @Schema(description = "随机且不可枚举的临时访问令牌")
    private String token;

    @Schema(description = "二维码承载的抬头访问路径")
    private String accessPath;

    @Schema(description = "过期时间，创建后 10 分钟")
    private LocalDateTime expiresAt;
}
