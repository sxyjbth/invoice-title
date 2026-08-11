package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 批量导入任务返回数据。 */
@Data
@Schema(description = "批量导入任务")
public class ImportTaskVO {
    @Schema(description = "任务主键 ID", example = "1")
    private Long id;
    @Schema(description = "任务编号", example = "IMP202608101518001234")
    private String taskNo;
    @Schema(description = "原始 Excel 文件名", example = "invoice-title-import.xlsx")
    private String originalFileName;
    @Schema(description = "任务状态：PENDING-待处理，VALIDATING-校验中，COMPLETED-已完成，PARTIAL_FAILED-部分失败，FAILED-全部失败", example = "PARTIAL_FAILED")
    private String status;
    @Schema(description = "有效数据总行数", example = "20")
    private Integer totalCount;
    @Schema(description = "成功生成草稿数", example = "18")
    private Integer successCount;
    @Schema(description = "失败行数", example = "2")
    private Integer failureCount;
    @Schema(description = "开始处理时间")
    private LocalDateTime startedAt;
    @Schema(description = "处理完成时间")
    private LocalDateTime finishedAt;
    @Schema(description = "发起人的钉钉用户 ID", example = "ding-user-finance")
    private String createdBy;
    @Schema(description = "任务创建时间")
    private LocalDateTime createdAt;
}
