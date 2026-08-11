package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 导入失败行返回数据。 */
@Data
@Schema(description = "导入失败行")
public class ImportRowErrorVO {
    @Schema(description = "失败记录主键 ID", example = "1")
    private Long id;
    @Schema(description = "Excel 行号，从 2 开始", example = "3")
    private Integer rowNo;
    @Schema(description = "该行纳税人识别号，无法解析时为空", example = "91110400MADFF1HE1T")
    private String taxpayerId;
    @Schema(description = "错误码：REQUIRED_MISSING-必填缺失，DUPLICATE_TAXPAYER_ID-税号重复，SUBJECT_NOT_FOUND-主体不存在，ROW_PROCESS_FAILED-处理失败", example = "REQUIRED_MISSING")
    private String errorCode;
    @Schema(description = "可供财务修正的错误说明", example = "纳税人识别号不能为空")
    private String errorMessage;
    @Schema(description = "失败行原始数据 JSON")
    private String rawDataJson;
    @Schema(description = "失败记录创建时间")
    private LocalDateTime createdAt;
}
