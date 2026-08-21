package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 全员可见开关即时更新请求。 */
@Data
@Schema(description = "全员可见开关即时更新请求")
public class AllEmployeeVisibleUpdateDTO {

    @NotNull(message = "全员可见开关不能为空")
    @Schema(
            description = "是否允许全部在职员工查看该主体及其已发布抬头",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "true")
    private Boolean allEmployeeVisible;
}
