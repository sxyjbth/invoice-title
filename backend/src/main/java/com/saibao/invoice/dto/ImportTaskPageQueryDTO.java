package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 批量导入任务分页查询参数。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "批量导入任务分页查询参数")
public class ImportTaskPageQueryDTO extends PageQueryDTO {

    @Schema(description = "任务状态：PENDING-待处理，VALIDATING-校验中，COMPLETED-已完成，PARTIAL_FAILED-部分失败，FAILED-全部失败", example = "COMPLETED")
    private String status;

    @Schema(description = "文件名或任务编号关键字", example = "invoice-title")
    private String keyword;
}
