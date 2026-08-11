package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 所有列表接口共用的服务端分页参数。 */
@Data
@Schema(description = "分页查询公共参数")
public class PageQueryDTO {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    @Min(value = 1, message = "pageNum 不能小于 1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，可选 10、20、50、100，最大 100", example = "20", defaultValue = "20")
    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = 100, message = "pageSize 不能大于 100")
    private Integer pageSize = 20;

    @Schema(hidden = true)
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
